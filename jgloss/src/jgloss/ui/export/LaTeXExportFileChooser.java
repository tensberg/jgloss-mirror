/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.ExtensionFileFilter;
import jgloss.ui.CustomFileView;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Vector;

import javax.swing.*;

/**
 * Export dialog which add latex-export specific elements.
 */
public class LaTeXExportFileChooser extends TemplateExportFileChooser {
    private JComboBox fontSizeChooser;

    public LaTeXExportFileChooser( String path) {
        super( path, JGloss.messages.getString( "export.latex.title"));
        setFileFilter( new ExtensionFileFilter( "tex", 
                                                JGloss.messages.getString
                                                ( "filefilter.description.latex")));
        addTemplateChooser();
        addFontSizeChooser();
    }

    public String getFontSize() {
        return (String) fontSizeChooser.getSelectedItem();
    }

    protected void addTemplateChooser() {
        super.addTemplateChooser( Preferences.EXPORT_LATEX_TEMPLATE, "export.latex.templates",
                                  Preferences.EXPORT_LATEX_USERTEMPLATES);
    }

    protected void addFontSizeChooser() {
        fontSizeChooser = new JComboBox( JGloss.prefs.getList( Preferences.EXPORT_LATEX_FONTSIZES, ','));
        fontSizeChooser.setEditable( false);
        fontSizeChooser.setSelectedItem( JGloss.prefs.getString(  Preferences.EXPORT_LATEX_FONTSIZE));
        fontSizeChooser.putClientProperty( PREFERENCES_KEY, Preferences.EXPORT_LATEX_FONTSIZE);

        Box b = Box.createHorizontalBox();
        b.add( new JLabel( JGloss.messages.getString( "export.latex.fontsize")));
        b.add( Box.createHorizontalStrut( 5));
        b.add( fontSizeChooser);
        b.add( new JLabel( JGloss.messages.getString( "export.latex.fontsize_pt")));
        addCustomElement( b);
    }

    /**
     * Save font size selection.
     */
    protected void savePreferences() {
        super.savePreferences();
        if (fontSizeChooser != null)
            JGloss.prefs.set( (String) fontSizeChooser.getClientProperty( PREFERENCES_KEY),
                              fontSizeChooser.getSelectedItem().toString());
    }
} // class LaTeXExportFileChooser
