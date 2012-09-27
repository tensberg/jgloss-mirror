/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.gloss;

import javax.swing.JMenu;

import jgloss.ui.LookupFrame;
import jgloss.ui.LookupModel;
import jgloss.ui.util.UIUtilities;

public class JGlossLookupFrame extends LookupFrame {
    private static final long serialVersionUID = 1L;
    
	/**
     * Static instance of the actions which create or open a JGloss document. If an action
     * is invoked, a new <CODE>JGlossFrame</CODE> will be created as the target of the
     * action.
     */
    private static final DocumentActions ACTIONS = new DocumentActions( null);

	public JGlossLookupFrame( LookupModel _model) {
        super( _model);
    }

    @Override
	protected void createFileMenuItems( JMenu menu) {
        menu.add( UIUtilities.createMenuItem( ACTIONS.importDocument));
        menu.add( UIUtilities.createMenuItem( ACTIONS.importClipboard));
        addWindowListener( ACTIONS.importClipboardListener);
        menu.addMenuListener( ACTIONS.importClipboardListener);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( ACTIONS.open));
        JMenu openRecent = JGlossFrame.OPEN_RECENT.createMenu( ACTIONS.openRecentListener);
        menu.add( openRecent);
        menu.addSeparator();

        super.createFileMenuItems( menu);
    }
} // class JGlossLookupFrame
