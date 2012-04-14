/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.util;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/** 
 * Action which {@link Cancelable#cancel() cancels} a {@link Cancelable}.
 * 
 * @author Michael Koch <tensberg@gmx.net> 
 */
public class CancelAction extends AbstractAction implements Action {

    private static final long serialVersionUID = 1L;

    private final Cancelable cancelable;

    public CancelAction(Cancelable cancelable) {
        this.cancelable = cancelable;
        UIUtilities.initAction(this, "button.cancel");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        cancelable.cancel();
    }
}
