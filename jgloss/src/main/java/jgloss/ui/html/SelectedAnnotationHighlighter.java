package jgloss.ui.html;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jgloss.ui.annotation.Annotation;
import jgloss.ui.gloss.AnnotationList;

/**
 * Highlights the annotation currently selected in a {@link AnnotationList AnnotationList}
 * in a {@link JGlossEditor JGlossEditor}.
 *
 * @author Michael Koch
 */
public class SelectedAnnotationHighlighter implements ListSelectionListener {
    protected JGlossEditor editor;

    public SelectedAnnotationHighlighter(AnnotationList _list, JGlossEditor _editor) {
        _list.addListSelectionListener(this);
        editor = _editor;
        highlightSelection(_list);
    }

    @Override
	public void valueChanged( ListSelectionEvent e) {
        if (e.getFirstIndex() >= 0) {
            highlightSelection((AnnotationList) e.getSource()); // HERE
        }
        // else: content of currently selected annotation changed; ignore
    }

    protected void highlightSelection(AnnotationList list) {
        Annotation anno = (Annotation) list.getSelectedValue();
        if (anno != null) {
            editor.highlightText( anno.getStartOffset(),
                                  anno.getEndOffset());
            // Move the caret to the beginning of the annotation. While the caret
            // is not visible, this has the side effect of clearing a text selection
            // the user has made, which is what is wanted.
            editor.setCaretPosition(anno.getStartOffset());
            editor.makeVisible(anno.getStartOffset(), anno.getEndOffset());
        }
        else {
            editor.removeHighlight();
        }
    }
} // class SelectedAnnotationHighlighter
