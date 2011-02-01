/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.actions.OpenPomAction.MavenStorageEditorInput;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * intended to be called by the pom editor only.
 * @author mkleint
 *
 */
public final class EffectivePomEditorProvider {
  
  private EffectivePomEditorProvider() {
  }
  
  public static IEditorInput createEditorInput(MavenProject project, String name) {
    BundleContext context = MavenEditorPlugin.getDefault().getBundle().getBundleContext();
    if (context == null) {
      return createDefaultEditorInput(project, name);
    }
    ServiceReference ref = context.getServiceReference(Implementation.class.getName());
    if(ref != null) {
      Implementation service = (Implementation) context.getService(ref);
      if(service != null) {
        try {
          return service.createEditorInput(project, name);
        } finally {
          context.ungetService(ref);
        }
      }
    }
    return createDefaultEditorInput(project, name);
  }
  
  public static StructuredTextEditor createTextEditor() {
    BundleContext context = MavenEditorPlugin.getDefault().getBundle().getBundleContext();
    if (context == null) {
      return createDefaultEditor();
    }
    ServiceReference ref = context.getServiceReference(Implementation.class.getName());
    if (ref == null) {
      return createDefaultEditor();
    }
    Implementation service = (Implementation)context.getService(ref);
    if (service != null) {
      try {
        return service.createTextEditor();
      } finally {
        context.ungetService(ref);
      }
    }
    return createDefaultEditor();
  }
  
  private static StructuredTextEditor createDefaultEditor() {
    return new StructuredTextEditor();    
  }
  
  private static IEditorInput createDefaultEditorInput(MavenProject mavenProject, String name) {
    StringWriter sw = new StringWriter();
    byte[] bytes = new byte[0];
    try {
      new MavenXpp3Writer().write(sw, mavenProject.getModel());
      String content = sw.toString();
      bytes = content.getBytes("UTF-8");
    } catch (IOException ie) {
      MavenLogger.log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1,
          Messages.MavenPomEditor_error_failed_effective, ie));
    }
    return new MavenStorageEditorInput(name, name, null, bytes); //$NON-NLS-1$
  }
  

  /**
   * spi to be implemented by external plugin. only one such implementation is expected at a time.
   * @author mkleint
   *
   */
  public interface Implementation {
    
    IEditorInput createEditorInput(MavenProject project, String name);
    
    StructuredTextEditor createTextEditor();
    
  }
  
}
