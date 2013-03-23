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
 * Convenience implementation of the {@link ElementProcessor} methods.
 * Subclasses can selectively override the methods they need.
 */
class ElementProcessorAdapter implements ElementProcessor {

    /**
     * {@inheritDoc}
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean writesToDocument() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean iterateBackwards() {
        return false;
    }

    /**
     * {@inheritDoc} This implementation does nothing.
     */
    @Override
    public void documentProcessingStarts(Position start, Position end) {
    }

    /**
     * {@inheritDoc} This implementation does nothing.
     */
    @Override
    public void documentProcessingEnds() {
    }

    /**
     * {@inheritDoc} This implementation does nothing and always returns
     * <code>true</code>.
     */
    @Override
    public boolean processElement(JGlossHTMLDoc doc, Element elem) {
        return true;
    }

}
