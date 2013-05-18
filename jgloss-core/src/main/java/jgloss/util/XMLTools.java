/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.util;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;

/**
 * Static utility functions for XML handling.
 *
 * @author Michael Koch
 */
public class XMLTools {
    // prevent instantiation
    private XMLTools() {}

    /**
     * Returns the text under the node.
     *
     * @see #getText(Node,StringBuilder)
     */
    public static String getText( Node node) {
        return getText( node, new StringBuilder()).toString();
    }

    /**
     * Add the data of all text nodes which are descendants of the node
     * to the string buffer. Ignorable whitespace is not recognized and will be
     * added to the string buffer.
     *
     * @return the provided string buffer.
     */
    public static StringBuilder getText( Node node, StringBuilder buf) {
        if (node instanceof CharacterData) {
            buf.append( ((CharacterData) node).getData());
        }

        Node child = node.getFirstChild();
        while (child != null) {
            getText( child, buf);
            child = child.getNextSibling();
        }
        
        return buf;
    }
} // class XMLTools
