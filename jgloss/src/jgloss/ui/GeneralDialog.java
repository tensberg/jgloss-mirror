/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import jgloss.*;
import jgloss.ui.doc.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.StyleSheet;

/**
 * Component which allows the user to edit general preferences. This will normally embedded
 * in the application preferences dialog. There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getComponent() getComponent()} method.
 *
 * @author Michael Koch
 */
public class GeneralDialog extends Box {
    /**
     * The single application-wide instance.
     */
    private static GeneralDialog box;

    /**
     * Returns the single application-wide instance.
     *
     * @return The GeneralDialog component.
     */
    public static GeneralDialog getComponent() {
        if (box == null)
            box = new GeneralDialog();
        return box;
    }

    private JCheckBox enableEditing;

    private JRadioButton startFrame;
    private JRadioButton startWordLookup;

    /**
     * Creates the style dialog.
     */
    public GeneralDialog() {
        super( BoxLayout.Y_AXIS);

        Box all = Box.createVerticalBox();

        JPanel p = new JPanel( new GridLayout( 2, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "general.startup")));
        ButtonGroup bg = new ButtonGroup();
        startFrame = new JRadioButton( JGloss.messages.getString( "general.startup.jglossframe"));
        bg.add( startFrame);
        p.add( startFrame);
        startWordLookup = new JRadioButton( JGloss.messages.getString( "general.startup.wordlookup"));
        bg.add( startWordLookup);
        p.add( startWordLookup);
        all.add( p);
        all.add( Box.createVerticalStrut( 2));

        // enable editing
        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        enableEditing = new JCheckBox( JGloss.messages.getString( "general.editor.enableediting"));
        b.add( enableEditing);
        b.add( Box.createHorizontalGlue());
        all.add( b);
        all.add( Box.createVerticalStrut( 2));

        this.add( JGlossFrame.createSpaceEater( all, false));

        loadPreferences();
    }

    /**
     * Loads the preferences and initializes the dialog accordingly.
     */
    public void loadPreferences() {
        if (JGloss.prefs.getBoolean( Preferences.STARTUP_WORDLOOKUP))
            startWordLookup.setSelected( true);
        else
            startFrame.setSelected( true);

        enableEditing.setSelected( JGloss.prefs.getBoolean( Preferences.EDITOR_ENABLEEDITING));
    }

    /**
     * Saves the current dialog settings.
     */
    public void savePreferences() {
        JGloss.prefs.set( Preferences.STARTUP_WORDLOOKUP, startWordLookup.isSelected());
        JGloss.prefs.set( Preferences.EDITOR_ENABLEEDITING, enableEditing.isSelected());
    }

    public void applyPreferences() {}
} // class GeneralDialog
