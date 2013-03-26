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

package jgloss.ui.gloss;

import java.awt.Window;

import javax.swing.AbstractAction;

/**
 * Base class for an action which writes to a new or empty existing document.
 */
public abstract class DocumentAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final JGlossFrame target;

    protected DocumentAction(JGlossFrame target) {
        this.target = target;
    }

    /**
     * @return Parent window of the given target, or <code>null</code> if no
     *         target was given.
     */
    protected Window getParentWindow() {
        return target != null ? target.frame : null;
    }

    /**
     * Returns the target frame for an action which needs an empty frame.
     * Returns a new {@link JGlossFrame} if the given target is
     * <code>null</code> or already contains a document.
     */
    protected JGlossFrame getFrame() {
        return DocumentActions.getFrame(target);
    }

}
