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
import jgloss.dictionary.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * The preferences frame allows the user to set the varying preferences for the JGloss
 * application. There exists only one application-wide preferences frame accessible by
 * {@link #getFrame() getFrame()}. The preferences frame  will include a <CODE>StyleDialog</CODE>
 * instance and <CODE>Dictionaries</CODE> instance.
 *
 * @author Michael Koch
 * @see StyleDialog
 * @see Dictionaries
 * @see jgloss.Preferences
 */
public class PreferencesFrame {
    /**
     * Action which displays the preferences dialog.
     */
    public final static Action showAction;

    static {
        showAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    getFrame().show();
                }
            };
        UIUtilities.initAction( showAction, "main.menu.preferences");
    }

    /**
     * The application-wide single instance.
     */
    private static PreferencesFrame prefs;
    
    /**
     * The frame which holds the preferences settings dialog.
     */
    private JFrame frame;

    /**
     * Returns the single application-wide preferences frame. If it is not yet constructed,
     * the method will create it and block until it is finished.
     *
     * @return The application-wide <CODE>PreferencesFrame</CODE> instance.
     */
    public static synchronized PreferencesFrame getFrame() {
        if (prefs == null)
            prefs = new PreferencesFrame();

        return prefs;
    }

    /**
     * Shows the dialog.
     */
    public void show() {
        frame.show();
    }

    /**
     * Initializes the preferences frame.
     */
    private PreferencesFrame() {
        frame = new JFrame( JGloss.messages.getString( "prefs.title"));

        JPanel main = new JPanel( new BorderLayout());
        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalGlue());
        final Action ok = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    frame.hide();
                    savePreferences();
                    applyPreferences();
                }
            };
        ok.setEnabled( true);
        UIUtilities.initAction( ok, "button.ok");
        final Action cancel = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    frame.hide();
                    loadPreferences();
                    applyPreferences();
                }
            };
        cancel.setEnabled( true);
        UIUtilities.initAction( cancel, "button.cancel");
        final Action apply = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    savePreferences();
                    applyPreferences();
                }
            };
        apply.setEnabled( true);
        UIUtilities.initAction( apply, "button.apply");
        b.add( new JButton( ok));
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( cancel));
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( apply));
        b.add( Box.createHorizontalStrut( 5));
        main.add( b, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    cancel.actionPerformed( null);
                }
            });

        UIUtilities.setCancelAction( frame, cancel);

        JTabbedPane tab = new JTabbedPane();
        tab.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0));
        tab.addTab( JGloss.messages.getString( "general.title"), GeneralDialog.getComponent());
        tab.addTab( JGloss.messages.getString( "style.title"), StyleDialog.getComponent());
        tab.addTab( JGloss.messages.getString( "dictionaries.title"), Dictionaries.getComponent());
        tab.addTab( JGloss.messages.getString( "exclusions.title"), ExclusionList.getComponent());
        main.add( tab, BorderLayout.CENTER);

        loadPreferences();
        applyPreferences();

        main.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10));
        frame.getContentPane().add( main);
        
        frame.pack();
        frame.setResizable( false);
    }

    /**
     * Saves the current settings to the application <CODE>Preferences</CODE> instance.
     */
    public void savePreferences() {
        GeneralDialog.getComponent().savePreferences();
        StyleDialog.getComponent().savePreferences();
        ExclusionList.getComponent().savePreferences();
        Dictionaries.getComponent().savePreferences();
    }

    /**
     * Loads the settings to the application <CODE>Preferences</CODE> instance.
     */
    public void loadPreferences() {
        GeneralDialog.getComponent().loadPreferences();
        StyleDialog.getComponent().loadPreferences();
        ExclusionList.getComponent().loadPreferences();
        Dictionaries.getComponent().loadPreferences();
    }

    /**
     * Applies the settings from the <CODE>Preferences</CODE> to the application.
     */
    public void applyPreferences() {
        GeneralDialog.getComponent().applyPreferences();
        StyleDialog.getComponent().applyPreferences();
        ExclusionList.getComponent().applyPreferences();
        Dictionaries.getComponent().applyPreferences();
    }
} // class PreferencesFrame
