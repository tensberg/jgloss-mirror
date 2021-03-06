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

import java.io.File;

import javax.swing.Action;

import jgloss.ui.OpenRecentMenu;
import jgloss.ui.welcome.ShowWelcomeDialogAction;

/**
 * Collection of actions to create and open JGloss documents.
 */
public class DocumentActions {
    /**
     * Imports a document into an empty JGlossFrame.
     */
    public final Action importDocument;
    /**
     * Imports the clipboard content into an empty JGlossFrame.
     */
    public final Action importClipboard;
    /**
     * Menu listener which will update the state of the import clipboard
     * action when the menu is selected.
     */
    public final ImportClipboardListener importClipboardListener;
    /**
     * Opens a document created by JGloss in an empty JGlossFrame.
     */
    public final Action open;
    /**
     * Listens to open recent selections. Use with
     * {@link OpenRecentMenu#createDocumentMenu(File,OpenRecentMenu.FileSelectedListener)
     *  OpenRecentMenu.createDocumentMenu}.
     */
    public final OpenRecentMenu.FileSelectedListener openRecentListener;

    public final Action showWelcomeDialog;

    /**
     * Creates a new instance of the actions which will invoke the methods
     * on the specified target. If the target is <CODE>null</CODE>, a new JGlossFrame
     * will be created on each invocation.
     */
    DocumentActions(JGlossFrame target) {
        importDocument = new ImportDocumentAction(target);
        importClipboard = new ImportClipboardAction(target);
        importClipboard.setEnabled( false);
        open = new OpenDocumentAction(target);

        openRecentListener = new OpenRecentDocumentListener(target);

        importClipboardListener = new ImportClipboardListener( importClipboard);
        showWelcomeDialog = new ShowWelcomeDialogAction(target);
    }

    /**
     * Returns the target frame for an import or open action. Returns a new {@link JGlossFrame} if
     * the given target is <code>null</code> or already contains a document.
     */
    public static JGlossFrame getFrame(JGlossFrame target) {
        JGlossFrame frame;

        if (target == null || !target.getModel().isEmpty()) {
            frame = new JGlossFrame();
        } else {
            frame = target;
        }

        return frame;
    }

}