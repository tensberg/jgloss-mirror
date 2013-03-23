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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;

/**
 * Recursively process the elements spanning a region of the document tree.
 */
class DocumentTreeWalker {
    private static final Logger LOGGER = Logger.getLogger(DocumentTreeWalker.class.getPackage().getName());

    private final JGlossHTMLDoc doc;

    private Position start;
    private Position end;
    private ElementProcessor processor;

    /**
     * Initialize the tree walker.
     *
     * @param _writeLock If <code>true</code>, the tree walker will aquire a write lock on the
     *        HTML document prior to recursing over the tree, otherwise a read lock will be used.
     */
    DocumentTreeWalker(JGlossHTMLDoc doc) {
        this.doc = doc;
    }

    /**
     * Starts the walk over the tree. The start and end offsets specify the region of elements
     * covered by the algorithm. This method will first aquire a read or write lock on the
     * HTML document, call {@link #walk(Element) walk} with the default root element and afterwards
     * release the lock held on the document.
     */
    void startWalk(int _start, int _end, ElementProcessor processor) {
        this.processor = processor;

        try {
            start = doc.createPosition(_start);
            end = doc.createPosition(_end);
        } catch (BadLocationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        doc.walkStarts(processor.writesToDocument());
        processor.documentProcessingStarts(start, end);

        try {
            walk(doc.getDefaultRootElement());
        } finally {
            processor.documentProcessingEnds();
            doc.walkEnds(processor.writesToDocument());
        }
    }

    private void walk(Element elem) {
        if (elem.getStartOffset() >= end.getOffset() || elem.getEndOffset() <= start.getOffset()) {
            return;
        }

        boolean recurse = processor.processElement(doc, elem);
        if (recurse) {
            recurse(elem);
        }
    }

    /**
     * Iterates forward over all children and calls {@link #walk(Element) walk} for every child.
     * This method is called by {@link #walk(Element) walk} if {@link #processElement(Element)
     * processElement} returns <code>true</code>.
     */
    private void recurse(Element elem) {
        if (processor.iterateBackwards()) {
            iterateBackwards(elem);
        } else {
            iterateForward(elem);
        }
    }

    private void iterateForward(Element elem) {
        for ( int i=0; i<elem.getElementCount(); i++) {
            walk(elem.getElement( i));
        }
    }

    private void iterateBackwards(Element elem) {
        for (int i = elem.getElementCount() - 1; i >= 0; i--) {
            walk(elem.getElement(i));
        }
    }

} // class TreeWalker