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

package jgloss.ui.download;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import jgloss.JGloss;
import jgloss.ui.Dictionaries;
import jgloss.ui.util.UIUtilities;

/**
 * Lets the user select the download directory. Saves the directory in the preferences.
 */
class ChooseDownloadDirectoryAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    
    private final Component parent;

    public ChooseDownloadDirectoryAction(Component parent) {
        this.parent = parent;
        UIUtilities.initAction(this, "choosedownloaddir.action");
    }
    
    public File getDownloadDirectory() {
        return Dictionaries.getDictionariesDir();
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        JFileChooser dirChooser = new JFileChooser(getDownloadDirectory());
        dirChooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setDialogTitle(JGloss.MESSAGES.getString("choosedownloaddir.title"));
        dirChooser.setApproveButtonText(JGloss.MESSAGES.getString("choosedownloaddir.approve"));
        int result = dirChooser.showDialog(parent, null);
        if (result == JFileChooser.APPROVE_OPTION) {
            Dictionaries.setDictionariesDir(dirChooser.getSelectedFile());
        }
    }

}
