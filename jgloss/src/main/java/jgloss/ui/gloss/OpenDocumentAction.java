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

package jgloss.ui.gloss;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import jgloss.JGloss;
import jgloss.ui.CustomFileView;
import jgloss.ui.util.UIUtilities;

/**
 * Opens a JGloss document chosen in the file chooser.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class OpenDocumentAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final JGlossFrame target;

    OpenDocumentAction(JGlossFrame target) {
        this.target = target;
        UIUtilities.initAction(this, "main.menu.open");
    }

    @Override
    public void actionPerformed( ActionEvent e) {
        JFileChooser f = new JFileChooser(JGloss.getApplication().getCurrentDir());
        f.addChoosableFileFilter(JGlossFrame.JGLOSS_FILE_FILTER);
        f.setFileHidingEnabled(true);
        f.setFileView(CustomFileView.getFileView());
        int r = f.showOpenDialog(target);
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.getApplication().setCurrentDir(f.getCurrentDirectory().getAbsolutePath());
            // test if the file is already open
            String path = f.getSelectedFile().getAbsolutePath();
            for (JGlossFrame frame : JGlossFrame.JGLOSS_FRAMES) {
                if (path.equals(frame.getModel().getDocumentPath())) {
                    frame.frame.setVisible(true);
                    return;
                }
            }

            // load the file
            JGlossFrame frame = DocumentActions.getFrame(target);
            OpenDocumentWorker.openDocument(frame, f.getSelectedFile());
        }
    }
}