/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id$
 *
 */

package jgloss.ui.annotation;

import jgloss.*;
import jgloss.ui.*;
import jgloss.ui.doc.*;

import javax.swing.text.*;

/**
 * A node which can display either the current reading or the current translation of an
 * annotation. The node allows the text to be changed.
 *
 * @author Michael Koch
 */
public class ReadingTranslationNode extends LeafNode {
    /**
     * The reading or translation element in the document which this node wraps.
     */
    private Element reading;
    /**
     * The text contained in the reading or translation element.
     */
    private String readingText;
    /**
     * The descriptive text of this node.
     */
    private String description;
    /**
     * Flag if this models a reading or translation annotation.
     */
    private boolean isReading;

    /**
     * Creates a new reading or annotation annotation node which wraps the given element.
     *
     * @param parent Parent of this node. Usually a {@link AnnotationNode AnnotationNode}.
     * @param reading Element in the document which this node wraps.
     * @param isReading <CODE>true</CODE> if this is a reading, <CODE>false</CODE> if this is a
     *               translation annotation.
     */
    public ReadingTranslationNode( InnerNode parent, Element reading, boolean isReading) {
        super( parent);
        this.reading = reading;
        try {
            this.readingText = reading.getDocument().getText( reading.getStartOffset(),
                                                              reading.getEndOffset()-
                                                              reading.getStartOffset());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        this.isReading = isReading;

        description = JGloss.messages.getString( isReading ? "annotationeditor.reading" :
                                                 "annotationeditor.translation");
    }

    /**
     * Returns a string representation of this node. This is the description plus the 
     * <CODE>readingText</CODE>.
     *
     * @return A string representation of this node.
     */
    public String toString() {
        return description + readingText;
    }

    /**
     * Returns the descriptive text for this node. This does not contain the <CODE>readingText</CODE>.
     *
     * @return The description of this node.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the reading text of this node.
     *
     * @return The reading text of this node.
     */
    public String getText() {
        return readingText;
    }

    /**
     * Sets the reading or translation text for this node. This will change the document to
     * show the new text. If the text is <CODE>null</CODE> or the empty string it will be
     * replaced by " " to prevent this element from being removed from the document.
     *
     * @param text The new text for the element.
     */
    public void setText( String text) {
        if (text==null || text.length()==0)
            text = " "; // a minimal text must always be set so that the element will not be deleted

        readingText = text;
        getRootNode().getModel().nodeChanged( this);
        try {
            int start = reading.getStartOffset();
            int end = reading.getEndOffset();
            JGlossDocument doc = ((JGlossDocument) reading.getDocument());
            doc.insertString( end, text, reading.getAttributes());
            doc.remove( start, end-start);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
} // class ReadingTranslationNode
