/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import jgloss.JGloss;

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
    public static final Action SHOW_ACTION = createShowAction();

    private static Action createShowAction() {
        Action showAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    prefs.setVisible(true);
                }
            };
        UIUtilities.initAction( showAction, "main.menu.preferences");

        return showAction;
    }

    /**
     * The application-wide single instance.
     */
    private static PreferencesFrame prefs;
    
    /**
     * The frame which holds the preferences settings dialog.
     */
    private final JFrame frame;

    private final PreferencesPanel[] panels;
    
    public static void createFrame( PreferencesPanel[] panels) {
        assert EventQueue.isDispatchThread();
        assert prefs == null;
        
        prefs = new PreferencesFrame( panels);
    }

    /**
     * Shows the dialog.
     */
    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    /**
     * Initializes the preferences frame.
     */
    private PreferencesFrame( PreferencesPanel[] _panels) {
        panels = _panels;
        frame = new JFrame( JGloss.MESSAGES.getString( "prefs.title"));

        JPanel main = new JPanel( new BorderLayout());
        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalGlue());
        final Action ok = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    frame.setVisible(false);
                    savePreferences();
                    applyPreferences();
                }
            };
        ok.setEnabled( true);
        UIUtilities.initAction( ok, "button.ok");
        final Action cancel = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    frame.setVisible(false);
                    loadPreferences();
                    applyPreferences();
                }
            };
        cancel.setEnabled( true);
        UIUtilities.initAction( cancel, "button.cancel");
        final Action apply = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
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

        frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener( new WindowAdapter() {
                @Override
				public void windowClosing( WindowEvent e) {
                    cancel.actionPerformed( null);
                }
            });

        UIUtilities.setCancelAction( frame, cancel);

        JTabbedPane tab = new JTabbedPane();
        tab.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0));
        for (PreferencesPanel panel : panels) {
            tab.addTab( panel.getTitle(), panel.getComponent());
        }
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
        for (PreferencesPanel panel : panels) {
	        panel.savePreferences();
        }
    }

    /**
     * Loads the settings to the application <CODE>Preferences</CODE> instance.
     */
    public void loadPreferences() {
        for (PreferencesPanel panel : panels) {
	        panel.loadPreferences();
        }
    }

    /**
     * Applies the settings from the <CODE>Preferences</CODE> to the application.
     */
    public void applyPreferences() {
        for (PreferencesPanel panel : panels) {
	        panel.applyPreferences();
        }
    }
} // class PreferencesFrame
