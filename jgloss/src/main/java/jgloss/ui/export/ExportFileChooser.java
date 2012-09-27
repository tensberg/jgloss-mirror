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

package jgloss.ui.export;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import jgloss.JGloss;
import jgloss.ui.CustomFileView;
import jgloss.ui.SaveFileChooser;
import jgloss.ui.util.UIUtilities;

/**
 * File chooser with user interface elements typical for file export.
 *
 * @author Michael Koch
 */
class ExportFileChooser extends SaveFileChooser {
    private static final long serialVersionUID = 1L;
    
	protected Box accessory;
    protected List<UIParameter> uiparameters;

    /**
     * Creates a new file chooser for selecting a export output file.
     *
     * @param path Directory initially visible in the chooser.
     * @param title Title of the dialog.
     */
    public ExportFileChooser( String path, String title, List<UIParameter> _uiparameters) {
        super( path);
        setDialogTitle( title);
        setFileHidingEnabled( true);
        setFileView( CustomFileView.getFileView());
        accessory = Box.createVerticalBox();
        uiparameters = _uiparameters;
        
        for (UIParameter uiparameter : uiparameters) {
            addComponent(uiparameter.getComponent());
        }

        loadFromPrefs();
    }


    /**
     * Adds an arbitrary element to the file chooser. The element will be added below all previously
     * added elements.
     */
    private void addComponent( Component element) {
        accessory.add( UIUtilities.createFlexiblePanel( element, true));
        accessory.add( Box.createVerticalStrut( 3));
    }

    /**
     * Runs the dialog. If the dialog is approved, the ui element settings will be saved.
     */
    @Override
	public int showSaveDialog( Component parent) {
        if (accessory != null) {
            JPanel container = new JPanel( new GridLayout( 1, 1));
            container.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3));
            container.add( UIUtilities.createFlexiblePanel( accessory, false));
            setAccessory( container);
        }

        int result = super.showSaveDialog( parent);
        if (result == JFileChooser.APPROVE_OPTION) {
	        saveToPrefs();
        }

        return result;
    }

    private void loadFromPrefs() {
        for (UIParameter uiparameter : uiparameters) {
        	uiparameter.loadFromPrefs();
        }
    }

    private void saveToPrefs() {
        JGloss.getApplication().setCurrentDir( getCurrentDirectory().getAbsolutePath());
        for (UIParameter uiparameter : uiparameters) {
        	uiparameter.saveToPrefs();
        }
    }
} // class ExportFileChooser
