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

import java.util.*;

import javax.swing.tree.TreeNode;

/**
 * A leaf which contains a single translation word for a dictionary entry. Parent of a
 * <CODE>TranslationLeafNode</CODE> is a {@link TranslationNode TranslationNode}.
 *
 * @author Michael Koch
 */
public class TranslationLeafNode extends LeafNode {
    /**
     * The translation word.
     */
    private String translation;

    /**
     * Creates a translation node with the given translation.
     *
     * @param parent Parent of this node. Usually a {@link TranslationNode TranslationNode}.
     * @param translation Translation word represented by this node.
     */
    public TranslationLeafNode( InnerNode parent, String translation) {
        super( parent);
        this.translation = translation;
    }

    /**
     * Returns a string representation of this node. This will be the translation.
     *
     * @return A string representation.
     */
    public String toString() {
        return translation;
    }

    /**
     * Returns the translation this node represents.
     *
     * @return The translation this node represents.
     */
    public String getTranslation() { return translation; }
} // class TranslationLeafNode
