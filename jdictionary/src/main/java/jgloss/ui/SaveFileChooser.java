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

package jgloss.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jgloss.JGloss;

/**
 * File chooser used for save dialogs. If the accepted file already exists, the user
 * is asked if the file should be overwritten.
 *
 * @author Michael Koch
 */
public class SaveFileChooser extends JFileChooser {
    private static final long serialVersionUID = 1L;

	public SaveFileChooser( String path) {
        super( path);
    }

    /**
     * Returns the selected file. If the currently selected file filter is of class
     * {@link ExtensionFileFilter ExtensionFileFilter}, the selected extension will be added
     * to the file name.
     */
    @Override
	public File getSelectedFile() {
        File f = super.getSelectedFile();
        if (f != null && 
            getFileFilter() instanceof ExtensionFileFilter) {
	        f = ((ExtensionFileFilter) getFileFilter()).addExtension( f);
        }

        return f;
    }

    /**
     * Intercepts the normal approval to show a overwrite confirmation dialog if neccessary.
     * If the user cancels the overwriting the selection will not be approved.
     */
    @Override
	public void approveSelection() {
        File f = getSelectedFile();
        if (f!=null && f.exists()) {
            String overwrite = JGloss.MESSAGES.getString( "button.overwrite");
            String cancel = JGloss.MESSAGES.getString( "button.cancel");
            int choice = JOptionPane.showOptionDialog
                ( this, JGloss.MESSAGES.getString
                  ( "filechooser.overwrite", f.getName() ),
                  JGloss.MESSAGES.getString( "filechooser.overwrite.title"),
                  JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                  new Object[] { overwrite, cancel }, cancel);

            if (choice != JOptionPane.YES_OPTION) {
	            return;
            }
        }

        super.approveSelection();
    }
} // class SaveFileChooser
