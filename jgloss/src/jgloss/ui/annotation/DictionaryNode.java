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

import jgloss.ui.Dictionaries;

import java.util.*;

import javax.swing.tree.TreeNode;

/**
 * Dictionary nodes are used as parents to all dictionary entries from a single dictionary.
 * They are children of {@link AnnotationNode AnnotationNode} and have 
 * {@link TranslationNode TranslationNodes} as children.
 *
 * @author Michael Koch
 */
class DictionaryNode extends InnerNode {
    /**
     * Name of the dictionary this node represents.
     */
    private String dictionary;

    /**
     * Creates a new node for a dictionary.
     *
     * @param parent Parent of this node. Should be an {@link AnnotationNode AnnotationNode}
     * @param dictionary Name of the dictionary this node represents.
     */
    public DictionaryNode( InnerNode parent, String dictionary) {
        super( parent, new Vector( Dictionaries.getComponent().getDictionaries().length));
        this.dictionary = dictionary;
    }

    /**
     * Returns the name of this dictionary.
     *
     * @return The name of this dictionary.
     */
    public String toString() { return dictionary; }

    /**
     * Returns a text representation of this node and its children. This is the name
     * of the dictionary plus the text of all children.
     *
     * @return A text representation.
     * @see TranslationNode#getText()
     */
    public String getText() {
        String out = dictionary;
        for ( Enumeration e=children(); e.hasMoreElements(); ) {
            out += "\n  " + ((TranslationNode) e.nextElement()).getText();
        }
        return out;
    }
} // class DictionaryNode
