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
 * Node for editing the reading of a <CODE>READING_KANJI</CODE> element.
 *
 * @author Michael Koch
 */
public class ReadingTextNode extends ReadingTranslationNode {
    /**
     * Child index of the <CODE>READING_KANJI</CODE> element this node
     * represents.
     */
    private byte elementNumber;

    /**
     * Creates a node for editing the reading of a READING_KANJI element.
     *
     * @param parent Parent of this node in the annotation tree.
     * @param elementNumber Child index of the <CODE>READING_KANJI</CODE> element this node
     *                      represents.
     * @param displayDescription If <CODE>true</CODE>, the String "Reading" will be added
     *                           to the description
     */
    public ReadingTextNode( InnerNode parent, byte elementNumber, boolean displayDescription) {
        super( parent, "", "");
        this.elementNumber = elementNumber;
        text = AnnotationNode.getElementText( getElement());
        
        if (displayDescription)
            description = JGloss.messages.getString( "annotationeditor.reading");
        else {
            String base = AnnotationNode.getElementText
                (((AnnotationNode) parent.getParent()).getAnnotationElement().getElement( 0)
                 .getElement( elementNumber).getElement( 1));
            description = JGloss.messages.getString
                ( "annotationeditor.reading.kanji", new String[] { base });
        }
    }

    protected Element getElement() {
        try {
            // wanted element is the reading child of the READING_KANJI element which is child
            // "elementNumber" of the word part of the annotation element.
            return ((AnnotationNode) parent.getParent()).getAnnotationElement()
                .getElement( 0).getElement( elementNumber).getElement( 0);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println( elementNumber);
            ((AbstractDocument.AbstractElement) ((AnnotationNode) parent.getParent()).getAnnotationElement()).dump( System.out, 2);
            ex.printStackTrace();
            return null;
        }
    }
} // class ReadingTextNode
