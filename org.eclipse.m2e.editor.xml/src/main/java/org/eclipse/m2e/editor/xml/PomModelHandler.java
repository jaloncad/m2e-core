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

package org.eclipse.m2e.editor.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolver;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.document.IEncodedDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.text.BasicStructuredDocument;
import org.eclipse.wst.sse.core.internal.undo.StructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.ModelQueryImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceTable;
import org.eclipse.wst.xml.core.internal.document.DOMModelImpl;
import org.eclipse.wst.xml.core.internal.encoding.XMLDocumentLoader;
import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;
import org.eclipse.wst.xml.core.internal.modelhandler.XMLModelLoader;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryAdapterFactoryForXML;
import org.eclipse.wst.xml.core.internal.modelquery.XMLModelQueryAssociationProvider;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapter;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapterImpl;



@SuppressWarnings("restriction")
public class PomModelHandler extends ModelHandlerForXML {

  private static final String HANDLER_ID = "org.eclipse.m2e.core.pomFile.handler";

  private static final String ASSOCIATED_CONTENT_TYPE_ID = "org.eclipse.m2e.core.pomFile"; //$NON-NLS-1$

  private static final String POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0"; //$NON-NLS-1$

  private static final String POM_XSD = "http://maven.apache.org/xsd/maven-4.0.0.xsd"; //$NON-NLS-1$

  public PomModelHandler() {
    super();
    setId(HANDLER_ID);
    setAssociatedContentTypeId(ASSOCIATED_CONTENT_TYPE_ID);
  }

  @Override
  public IModelLoader getModelLoader() {
    return new PomModelLoader();
  }

  
  /* (non-Javadoc)
   * @see org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML#getDocumentLoader()
   */
  @Override
  public IDocumentLoader getDocumentLoader() {
    return new PomXMLDocumentLoader();
  }
  
  private static class PomModelLoader extends XMLModelLoader {

    @SuppressWarnings("unchecked")
    @Override
    public List getAdapterFactories() {
      List result = new ArrayList();
      INodeAdapterFactory factory = new ModelQueryAdapterFactoryForPom();
      result.add(factory);
      return result;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.xml.core.internal.modelhandler.XMLModelLoader#getDocumentLoader()
     */
    @Override
    public IDocumentLoader getDocumentLoader() {
      if (documentLoaderInstance == null) {
        documentLoaderInstance = new PomXMLDocumentLoader();
      }
      return super.getDocumentLoader();
    }
    
    public IStructuredModel newModel() {
      return new DOMModelImpl();
    }    
  }
  
  private static class PomXMLDocumentLoader extends XMLDocumentLoader {

    /* (non-Javadoc)
     * @see org.eclipse.wst.xml.core.internal.encoding.XMLDocumentLoader#newEncodedDocument()
     */
    @Override
    protected IEncodedDocument newEncodedDocument() {
      IEncodedDocument doc = super.newEncodedDocument();
      if (doc instanceof BasicStructuredDocument) {
        BasicStructuredDocument bsd = (BasicStructuredDocument) doc;
        System.out.println("setting undo manager");
        bsd.setUndoManager(new StructuredTextUndoManager());
      }
      return doc;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.xml.core.internal.encoding.XMLDocumentLoader#newInstance()
     */
    @Override
    public IDocumentLoader newInstance() {
      return new PomXMLDocumentLoader();
    }
    
  }
  
  static class ModelQueryAdapterFactoryForPom extends ModelQueryAdapterFactoryForXML {

    protected ModelQueryAdapterImpl modelQueryAdapterImpl;

    @Override
    protected INodeAdapter createAdapter(INodeNotifier target) {
      if(modelQueryAdapterImpl == null) {
        ModelQueryAdapter mqa = (ModelQueryAdapter) super.createAdapter(target);
        modelQueryAdapterImpl = new ModelQueryAdapterImpl(mqa.getCMDocumentCache(), new PomModelQueryImpl(mqa
            .getCMDocumentCache(), mqa.getIdResolver()), mqa.getIdResolver());
      }
      return modelQueryAdapterImpl;
    }

  }

  static class PomModelQueryImpl extends ModelQueryImpl {

    public PomModelQueryImpl(CMDocumentCache cache, URIResolver idResolver) {
      super(new PomModelQueryAssociationProvider(cache, idResolver));
    }

  }

  static class PomModelQueryAssociationProvider extends XMLModelQueryAssociationProvider {

    public PomModelQueryAssociationProvider(CMDocumentCache cache, URIResolver idResolver) {
      super(cache, idResolver);
    }

    @Override
    public CMDocument getCMDocument(String publicId, String systemId, String type) {
      if("".equals(publicId) && "".equals(systemId)) { //$NON-NLS-1$ //$NON-NLS-2$
        return null;
      }

      return super.getCMDocument(publicId, systemId, type);
    }

    @Override
    public CMElementDeclaration getCMElementDeclaration(Element element) {
      CMElementDeclaration result = super.getCMElementDeclaration(element);

      if(result == null) {
        NamespaceTable namespaceTable = new NamespaceTable(element.getOwnerDocument());
        List list = NamespaceTable.getElementLineage(element);
        Element rootElement = (Element) list.get(0);
        namespaceTable.addElement(rootElement);

        documentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_ASYNC_LOAD, false);
        documentManager.addCMDocumentReference(POM_NAMESPACE, POM_XSD, "XSD"); //$NON-NLS-1$
        namespaceTable.addNamespaceInfo("", POM_NAMESPACE, ""); //$NON-NLS-1$ //$NON-NLS-2$

        if(namespaceTable.isNamespaceEncountered()) {
          result = getCMElementDeclaration(element, list, namespaceTable);
        }
      }

      return result;
    }
  }

}
