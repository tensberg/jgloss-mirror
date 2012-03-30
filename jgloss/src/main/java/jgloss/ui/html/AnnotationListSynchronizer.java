package jgloss.ui.html;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;

import jgloss.ui.annotation.AnnotationListModel;

/**
 * Update the annotation list in response to changes in the JGloss HTML document. The container
 * which manages both the JGloss HTML document and the annotation list corresponding to it
 * must create an instance of this class to glue the two objects together. In the current JGloss
 * implementation, this is done in class {@link jgloss.ui.gloss.JGlossFrame JGlossFrame}.
 *
 * @see jgloss.ui.html.JGlossHTMLDoc
 * @see jgloss.ui.annotation.AnnotationListModel
 * @author Michael Koch
 */
public class AnnotationListSynchronizer implements DocumentListener {
    private final AnnotationListModel annotationModel;

    public AnnotationListSynchronizer(JGlossHTMLDoc _doc, AnnotationListModel _annotationModel) {
        _doc.addDocumentListener(this);
        annotationModel = _annotationModel;
    }

    @Override
	public void insertUpdate(DocumentEvent e) {
        // find out if an annotation element was inserted

        Element el = e.getDocument().getDefaultRootElement();
        while (el!=null && el.getStartOffset() <= e.getOffset() &&
               el.getEndOffset() >= e.getOffset()+e.getLength()) {
            DocumentEvent.ElementChange change = e.getChange( el);
            if (change != null) {
	            handleInsert( change.getChildrenAdded());
            }

            el = el.getElement( el.getElementIndex( e.getOffset()));
        }
    }

    private void handleInsert( Element[] children) {
        for (Element element : children) {
	        handleInsert( element);
        }
    }

    private void handleInsert( Element e) {
        if (e.getAttributes().getAttribute( StyleConstants.NameAttribute)
            .equals( AnnotationTags.ANNOTATION)) {
	        annotationModel.addAnnotationFor( e);
        } else {
	        for ( int i=0; i<e.getElementCount(); i++) {
	            handleInsert( e.getElement( i));
            }
        }
    }

    @Override
	public void removeUpdate( DocumentEvent e) {
        // find out if an annotation element was removed

        Element el = e.getDocument().getDefaultRootElement();
        while (el != null) {
            DocumentEvent.ElementChange change = e.getChange( el);
            if (change != null) {
	            handleRemove( change.getChildrenRemoved());
            }
            el = el.getElement( el.getElementIndex( e.getOffset()));
        }
    }

    private void handleRemove( Element[] children) {
        for (Element element : children) {
	        handleRemove( element);
        }
    }

    private void handleRemove( Element e) {
        if (e.getAttributes().getAttribute( StyleConstants.NameAttribute)
            .equals( AnnotationTags.ANNOTATION)) {
	        annotationModel.removeAnnotationFor( e);
        } else {
	        for ( int i=0; i<e.getElementCount(); i++) {
	            handleRemove( e.getElement( i));
            }
        }
    }

    @Override
	public void changedUpdate( DocumentEvent e) {}
} // class AnnotationListSynchronizer
