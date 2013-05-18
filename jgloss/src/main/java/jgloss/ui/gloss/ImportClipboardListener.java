/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.gloss;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Action;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jgloss.ui.util.UIUtilities;

/**
 * Updates the status of the import clipboard action corresponding to certain events.
 * The import clipboard action should only be enabled if the system clipboard contains a
 * string. This listener checks the status of the clipboard and updates the action state if
 * the window the listener is attached to is brought to the foreground and/or if the menu the
 * listener is attached to is expanded.
 */
class ImportClipboardListener extends WindowAdapter implements MenuListener {
    private final Action importClipboard;

    public ImportClipboardListener( Action _importClipboard) {
        this.importClipboard = _importClipboard;
    }

    private void checkUpdate() {
        // enable the import clipboard menu item if the clipboard contains some text
        importClipboard.setEnabled( UIUtilities.clipboardContainsString());
    }

    @Override
	public void windowActivated( WindowEvent e) {
        checkUpdate();
    }
    @Override
	public void menuSelected( MenuEvent e) {
        checkUpdate();
    }
    @Override
	public void menuDeselected( MenuEvent e) {}
    @Override
	public void menuCanceled( MenuEvent e) {}
} // class ImportClipboardListener