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
import jgloss.dictionary.*;

import javax.swing.tree.TreeNode;

/**
 * This node is created for a reading annotation. It is a child of 
 * {@link AnnotationNode AnnotationNode}
 *
 * @author Michael Koch
 */
public class ReadingAnnotationNode extends LeafNode {
    /**
     * The reading text annotation of this node.
     */
    private Reading reading;
    /**
     * The text of the reading of this node.
     */
    private String readingText;
    /**
     * The reading plus some descriptive text.
     */
    private String description;

    /**
     * Creates a new reading annotation node with the specified reading.
     *
     * @param parent Parent of this node.
     * @param reading Reading to use.
     */
    public ReadingAnnotationNode( InnerNode parent, Reading reading) {
        super( parent);
        this.reading = reading;
        readingText = reading.getReading();
        if (reading.getConjugation() != null) {
            // cut off inflection
            readingText = readingText.substring( 0, readingText.length() - 
                                                 reading.getConjugation().getDictionaryForm().length());
        }

        description = JGloss.messages.getString( "annotationeditor.readingannotation",
                                                 new Object[] {
                                                     reading.getWordReadingPair()
                                                     .getDictionary().getName(),
                                                     readingText });
        if (reading.getConjugation() != null) {
            description += " (" + reading.getConjugation().getType() + ")";
        }
    }

    /**
     * Returns a description of this node. This is the reading plus some descriptive text.
     *
     * @return A description of this node.
     */
    public String toString() { return description; }
    /**
     * Returns the reading used for this node.
     *
     * @return The reading used for this node.
     */
    public String getReadingText() { return readingText; }
    /**
     * Returns the reading annotation this node represents.
     *
     * @return The reading annotation this node represents.
     */
    public Reading getReading() { return reading; }
} // class ReadingAnnotationNode
