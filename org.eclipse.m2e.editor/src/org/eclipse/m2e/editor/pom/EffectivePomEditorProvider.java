package org.eclipse.m2e.editor.pom;

import org.apache.maven.project.MavenProject;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class EffectivePomEditorProvider {
  
  private EffectivePomEditorProvider() {
  }
  
  public static IEditorInput createEditorInput(MavenProject project, String name) {
    BundleContext context = MavenEditorPlugin.getDefault().getBundle().getBundleContext();
    if (context == null) {
      return null;
    }
    ServiceReference ref = context.getServiceReference(Implementation.class.getName());
    if (ref == null) {
      return null;
    }
    Implementation service = (Implementation)context.getService(ref);
    if (service != null) {
      try {
        return service.createEditorInput(project, name);
      } finally {
        context.ungetService(ref);
      }
    }
    return null;
  }
  
  public static StructuredTextEditor createTextEditor() {
    BundleContext context = MavenEditorPlugin.getDefault().getBundle().getBundleContext();
    if (context == null) {
      return null;
    }
    ServiceReference ref = context.getServiceReference(Implementation.class.getName());
    if (ref == null) {
      return null;
    }
    Implementation service = (Implementation)context.getService(ref);
    if (service != null) {
      try {
        return service.createTextEditor();
      } finally {
        context.ungetService(ref);
      }
    }
    return null;
  }  
  

  
  public interface Implementation {
    
    IEditorInput createEditorInput(MavenProject project, String name);
    
    StructuredTextEditor createTextEditor();
    
  }
  
}
