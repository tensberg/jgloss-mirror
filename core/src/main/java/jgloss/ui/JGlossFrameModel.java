package jgloss.ui;

import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.ui.xml.JGlossDocument;

public class JGlossFrameModel {
    /**
     * The JGloss annotated document this frame edits.
     */
    private JGlossDocument doc;
    /**
     * The HTML document rendering the edited JGloss document;
     */
    private JGlossHTMLDoc htmlDoc;
    private AnnotationListModel annotationListModel;
    /**
     * The name of the document. For files this is the file name.
     */
    private String documentName;
    /**
     * The full path name to the document. This will be <CODE>null</CODE> if the 
     * document is imported and not yet saved.
     */
    private String documentPath;
    /**
     * <CODE>true</CODE> if the document was modified since the last save.
     */
    private boolean documentChanged;

    JGlossFrameModel() {}

    public boolean isEmpty() { return doc==null; }
    public JGlossDocument getDocument() { return doc; }
    public JGlossHTMLDoc getHTMLDocument() { return htmlDoc; }
    public AnnotationListModel getAnnotationListModel() { return annotationListModel; }
    public String getDocumentName() { return documentName; }
    public String getDocumentPath() { return documentPath; }
    public boolean isDocumentChanged() { return documentChanged; }

    void setDocument( JGlossDocument _doc) {
        doc = _doc;
    }

    void setHTMLDocument( JGlossHTMLDoc _htmlDoc) {
        htmlDoc = _htmlDoc;
    }

    void setAnnotationListModel( AnnotationListModel _annotationListModel) {
        annotationListModel = _annotationListModel;
    }

    void setDocumentName( String _documentName) {
        documentName = _documentName;
    }

    void setDocumentPath( String _documentPath) {
        documentPath = _documentPath;
    }

    void setDocumentChanged( boolean _documentChanged) {
        documentChanged = _documentChanged;
    }
} // class JGlossFrameModel
