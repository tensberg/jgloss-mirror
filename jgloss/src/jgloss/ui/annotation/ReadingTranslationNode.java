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
public abstract class ReadingTranslationNode extends EditableTextNode {
    public ReadingTranslationNode( InnerNode parent, String description, String text) {
        super( parent, description, text);
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

        super.setText( text);

        try {
            Element reading = getElement();
            int start = reading.getStartOffset();
            int end = reading.getEndOffset();
            JGlossDocument doc = ((JGlossDocument) reading.getDocument());
            doc.insertString( end, text, reading.getAttributes());
            doc.remove( start, end-start);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the changeable text of this node. If the text equals " ", the empty string
     * will be returned.
     *
     * @see #setText(String)
     */
    public String getText() {
        if (" ".equals( text))
            return "";
        else
            return text;
    }

    /**
     * Returns the element which displays the reading/translation in the JGloss document.
     * The element object may change if the annotations are manipulated, so you should not
     * use it over longer intervals.
     */
    protected abstract Element getElement();
} // class ReadingTranslationNode
