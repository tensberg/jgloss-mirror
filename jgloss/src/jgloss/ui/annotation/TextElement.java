/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.ui.html.JGlossHTMLDoc;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.BadLocationException;

class TextElement {
    private Element element;
    private String text = null;

    TextElement( Element _element) {
        element = _element;

        if (element.getElementCount() > 0)
            throw new IllegalArgumentException( "elements must not have children");
    }

    /**
     * Sets the text for this node. This will change the document to
     * show the new text. If the text is <CODE>null</CODE> or the empty string it will be
     * replaced by {@link JGlossHTMLDoc#EMPTY_ELEMENT_PLACEHOLDER EMPTY_ELEMENT_PLACEHOLDER}
     * to prevent this element from being removed from the document.
     *
     * @param text The new text for the element.
     */
    public void setText( String _text) {
        text = _text;

        try {
            int start = element.getStartOffset();
            int end = element.getEndOffset();
            Document doc = element.getDocument();

            // a minimal text must always be set so that the element will not be deleted
            String insert = (text==null || text.length()==0) ? 
                JGlossHTMLDoc.EMPTY_ELEMENT_PLACEHOLDER : text;
            doc.insertString( end, insert, element.getAttributes());

            doc.remove( start, end-start);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the text of this node. If the text equals 
     * {@link JGlossHTMLDoc#EMPTY_ELEMENT_PLACEHOLDER EMPTY_ELEMENT_PLACEHOLDER}, the empty string
     * will be returned.
     *
     * @see #setText(String)
     */
    public String getText() {
        if (text == null) try {
            text = element.getDocument().getText( element.getStartOffset(),
                                                  element.getEndOffset()-element.getStartOffset());
            if (text.equals( JGlossHTMLDoc.EMPTY_ELEMENT_PLACEHOLDER))
                text = "";
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        return text;
    }

    public Element getElement() { return element; }
} // class TextElement
