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

package jgloss.ui.util;

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * Listener which calls {@link #componentShown(HierarchyEvent)} and
 * {@link #componentHidden(HierarchyEvent)} when the visibility of a component
 * on screen has changed.
 *
 * @see Component#addHierarchyListener(HierarchyListener)
 */
public abstract class ShowingChangedAdapter implements HierarchyListener {

    @Override
    public void hierarchyChanged(HierarchyEvent event) {
        if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (event.getComponent().isShowing()) {
                componentShown(event);
            } else {
                componentHidden(event);
            }
        }
    }

    /**
     * Called when the component has been shown on the screen.
     */
    protected void componentShown(HierarchyEvent event) {
    }

    /**
     * Called when the component has been hidden from the screen.
     */
    protected void componentHidden(HierarchyEvent event) {
    }

}
