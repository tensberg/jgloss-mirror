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
import jgloss.ui.*;

import java.awt.*;

import javax.swing.*;


public class ExportFileChooser extends SaveFileChooser {
    public static final int WRITE_READING = 0;
    public static final int WRITE_TRANSLATIONS = 1;
    public static final int WRITE_HIDDEN = 2;
    public static final int ENCODING_CHOOSER = 3;

    protected static final String PREFERENCES_KEY = "preferences key";

    protected Box accessory;

    protected JCheckBox writeReading;
    protected JCheckBox writeTranslations;
    protected JCheckBox writeHidden;
    protected JComboBox encodingChooser;

    public ExportFileChooser( String path, String title) {
        super( path);
        setDialogTitle( title);
        setFileHidingEnabled( true);
        setFileView( CustomFileView.getFileView());
    }

    public void addElement( int type, String prefsKey) {
        JComponent element = null;
        switch (type) {
        case WRITE_READING:
            writeReading = createCheckbox( JGloss.messages.getString( "export.writereading"),
                                           prefsKey);
            element = writeReading;
            break;
        case WRITE_TRANSLATIONS:
            writeTranslations = createCheckbox( JGloss.messages.getString( "export.writetranslations"),
                                                prefsKey);
            element = writeTranslations;
            break;
        case WRITE_HIDDEN:
            writeHidden = createCheckbox( JGloss.messages.getString( "export.writehidden"),
                                          prefsKey);
            element = writeHidden;
            break;
        case ENCODING_CHOOSER:
            encodingChooser = createEncodingChooser( prefsKey);
            break;
        }

        if (element != null)
            addCustomElement( element);
    }

    public void addCustomElement( Component element) {
        if (accessory == null)
            createAccessory();

        accessory.add( UIUtilities.createSpaceEater( element, true));
        accessory.add( Box.createVerticalStrut( 3));
    }

    public int showSaveDialog( Component parent) {
        if (accessory != null) {
            JPanel container = new JPanel( new GridLayout( 1, 1));
            container.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3));
            container.add( UIUtilities.createSpaceEater( accessory, false));
            setAccessory( container);
        }

        int result = super.showSaveDialog( parent);
        if (result == JFileChooser.APPROVE_OPTION)
            savePreferences();

        return result;
    }

    public boolean getWriteReading() {
        return writeReading.isSelected();
    }

    public boolean getWriteTranslations() {
        return writeTranslations.isSelected();
    }

    public boolean getWriteHidden() {
        return writeHidden.isSelected();
    }

    public String getEncoding() {
        return (String) encodingChooser.getSelectedItem();
    }

    protected void createAccessory() {
        accessory = Box.createVerticalBox();
    }

    protected JCheckBox createCheckbox( String message, String prefsKey) {
        JCheckBox out = new JCheckBox( message);
        out.setSelected( JGloss.prefs.getBoolean( prefsKey, true));
        out.putClientProperty( PREFERENCES_KEY, prefsKey);
        return out;
    }

    protected JComboBox createEncodingChooser( String prefsKey) {
        Box b = Box.createHorizontalBox();
        b.add( new JLabel( JGloss.messages.getString( "export.encodings")));
        b.add( Box.createHorizontalStrut( 3));
        JComboBox encodings = new JComboBox( JGloss.prefs.getList( Preferences.ENCODINGS, ','));
        encodings.setSelectedItem( JGloss.prefs.getString( prefsKey));
        encodings.setEditable( true);
        b.add( encodings);

        encodings.putClientProperty( PREFERENCES_KEY, prefsKey);
        addCustomElement( b);

        return encodings;
    }

    protected void savePreferences() {
        JGloss.setCurrentDir( getCurrentDirectory().getAbsolutePath());
        if (writeReading != null)
            JGloss.prefs.set( (String) writeReading.getClientProperty( PREFERENCES_KEY),
                              writeReading.isSelected());
        if (writeTranslations != null)
            JGloss.prefs.set( (String) writeTranslations.getClientProperty( PREFERENCES_KEY),
                              writeTranslations.isSelected());
        if (writeHidden != null)
            JGloss.prefs.set( (String) writeHidden.getClientProperty( PREFERENCES_KEY),
                              writeHidden.isSelected());
        if (encodingChooser != null)
            JGloss.prefs.set( (String) encodingChooser.getClientProperty( PREFERENCES_KEY),
                              (String) encodingChooser.getSelectedItem());
    }
} // class ExportFileChooser
