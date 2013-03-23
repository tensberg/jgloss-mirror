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
 */

package jgloss.ui.html;

import javax.swing.text.Element;
import javax.swing.text.Position;

/**
 * Implements an operation on a document element.
 *
 * @see DocumentTreeWalker
 */
interface ElementProcessor {

    /**
     * @return <code>true</code> if the processor might change the document.
     */
    boolean writesToDocument();

    /**
     * @return <code>true</code> if iteration over children of an element should
     *         be last-to-first instead of first-to-last. Useful if
     *         {@link #processElement(JGlossHTMLDoc, Element)} changes the
     *         elements which are iterated over.
     */
    boolean iterateBackwards();

    void documentProcessingStarts(Position start, Position end);

    void documentProcessingEnds();

    /**
     * Process the element of the tree in some way. This method will only be
     * called for an element if its region (the area between its start and end
     * offset) intersects with the region specified by the {@link #start start}
     * and {@link #end end} offsets of the walker object. If the element has
     * children, the return value of the method determines if the algorithm
     * should recurse over the children.
     *
     * @return <code>true</code> if the tree walker should recurse over the
     *         children of the element.
     */
    boolean processElement(JGlossHTMLDoc doc, Element elem);
}
