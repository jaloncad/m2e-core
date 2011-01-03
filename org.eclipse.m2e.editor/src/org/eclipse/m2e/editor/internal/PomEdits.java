package org.eclipse.m2e.editor.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.m2e.core.internal.project.MavenMarkerManager;
import org.eclipse.m2e.editor.internal.PomEdits.Operation;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * this class contains tools for editing the pom files using dom tree operations.
 * @author mkleint
 *
 */
public class PomEdits {

  
  public static Element findChild(Element parent, String name) {
    return MavenMarkerManager.findChildElement(parent, name);
  }

  public static List<Element> findChilds(Element parent, String name) {
    return MavenMarkerManager.findChildElements(parent, name);
  }

  public static String getTextValue(Node element) {
    return MavenMarkerManager.getElementTextValue(element);
  }
  
  /**
   * node is expected to be the node containing <dependencies> node, so <project>, <dependencyManagement> etc..
   * @param node
   * @return
   */
  public static List<Element> findDependencies(Element node) {
    return findChilds(findChild(node, "dependencies"), "dependency");
  }
  
  /** for the root <project> node (or equivalent) finds or creates the <dm> and <dependencies> sections.
   * returns the <dependencies> section element.
   *  
   * @param root
   * @return
   */
  public static Element getManagedDependencies(Element root) {
    Element toRet = getChild(root, "dependencyManagement");
    toRet = getChild(toRet, "dependencies");
    return toRet;
  }
  
  /**
   * creates and adds new dependency to the parent.
   * @param parentList
   * @param groupId null or value
   * @param artifactId never null
   * @param version null or value
   * @return
   */
  public static Element createDependency(Element parentList, String groupId, String artifactId, String version) {
    Document doc = parentList.getOwnerDocument();
    Element dep = doc.createElement("dependency");
    parentList.appendChild(dep);
    
    if (groupId != null) {
      Element grid = doc.createElement("groupId");
      dep.appendChild(grid);
      grid.appendChild(doc.createTextNode(groupId));
    }
    Element artid = doc.createElement("artifactId");
    dep.appendChild(artid);
    artid.appendChild(doc.createTextNode(artifactId));
    if (version != null) {
      Element vers = doc.createElement("version");
      dep.appendChild(vers);
      vers.appendChild(doc.createTextNode(version));
    }
    format(dep);
    return dep;
  }
  
  /**
   * unlike the findChild() equivalent, this one creates the element if not present and returns it.
   * Therefore it shall only be invoked within the PomEdits.Operation
   * @param parent
   * @param name
   * @return
   */
  public static Element getChild(Element parent, String name) {
    Element toRet = findChild(parent, name);
    if (toRet == null) {
      toRet = parent.getOwnerDocument().createElement(name);
      parent.appendChild(toRet);
      format(toRet);
    }
    return toRet;
  }
  
  /**
   * proper remove of a child element
   * @param parent
   * @param name
   */
  public static void removeChild(Element parent, String name) {
    Element child = PomEdits.findChild(parent, name);
    if (child != null) {
      Node prev = child.getPreviousSibling();
      if (prev instanceof Text) {
        Text txt = (Text)prev;
        int lastnewline = txt.getData().lastIndexOf("\n");
        txt.setData(txt.getData().substring(0, lastnewline));
      }
      parent.removeChild(child);
    }
  }
  
  /**
   * formats the node (and content). please make sure to only format the node you have created..
   * @param newNode
   */
  public static void format(Node newNode) {
    if (newNode.getParentNode() != null && newNode.equals(newNode.getParentNode().getLastChild())) {
      //add a new line to get the newly generated content correctly formatted.
      newNode.getParentNode().appendChild(newNode.getParentNode().getOwnerDocument().createTextNode("\n"));
    }
    IStructuredFormatProcessor formatProcessor = new FormatProcessorXML();
    formatProcessor.formatNode(newNode);
  }

  /**
   * performs an modifying operation on top the  
   * @param file
   * @param operation
   * @throws IOException
   * @throws CoreException
   */
  public static void performOnDOMDocument(PomEdits.OperationTuple... fileOperations) throws IOException, CoreException {
    for(OperationTuple tuple : fileOperations) {
      IDOMModel domModel = null;
      //TODO we might want to attempt iterating opened editors and somehow initialize those
      // that were not yet initialized. Then we could avoid saving a file that is actually opened, but was never used so far (after restart)
      try {
        domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(tuple.getFile());
        domModel.aboutToChangeModel();
      IStructuredTextUndoManager undo = domModel.getStructuredDocument().getUndoManager();
      undo.beginRecording(domModel);
        try {
          tuple.getOperation().process(domModel.getDocument());
        } finally {
        undo.endRecording(domModel);
          domModel.changedModel();
        }
      } finally {
        if(domModel != null) {
          //saving shall only happen when the model is not held elsewhere (eg. in opened view)
          if(domModel.isSaveNeeded() && domModel.getReferenceCountForEdit() == 1) {
            domModel.save();
          }
          domModel.releaseFromEdit();
        }
      }
    }
  }
  
  public static final class OperationTuple {
    private final PomEdits.Operation operation;
    private final IFile file;

    public OperationTuple(IFile file, PomEdits.Operation operation) {
      assert file != null;
      assert operation != null;
      this.file = file;
      this.operation = operation;
    }

    public IFile getFile() {
      return file;
    }

    public PomEdits.Operation getOperation() {
      return operation;
    }
  
  }
  
  /**
   * operation to perform on top of the DOM document. see performOnDOMDocument()
   * @author mkleint
   *
   */
  public static interface Operation {
    void process(Document document);    
  }
  
  /**
   * an Operation instance that aggregates multiple operations and performs then in given order.
   * @author mkleint
   *
   */
  public static final class CompoundOperation implements Operation {
    
    private final Operation[] operations;

    public CompoundOperation(Operation... operations) {
      this.operations = operations;
    }
    
    public void process(Document document) {
      for (Operation oper : operations) {
        oper.process(document);
      }
    }
    
  }
}
