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

    private JTextField chasenLocation;

    private ParserSelector importClipboardParserSelector;
    private Class importClipboardParser;
    private char readingStart;
    private char readingEnd;

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

        importClipboardParserSelector = new ParserSelector( true);
        importClipboardParserSelector.setBorder( BorderFactory.createTitledBorder 
                                                 ( JGloss.messages.getString( "general.parserselector")));
        all.add( importClipboardParserSelector);
        all.add( Box.createVerticalStrut( 2));

        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        b.add( new JLabel( JGloss.messages.getString( "general.chasen.label")));
        chasenLocation = new JTextField( JGloss.prefs.getString( Preferences.CHASEN_LOCATION));
        chasenLocation.setInputVerifier( new InputVerifier() {
                private String lastInput = chasenLocation.getText();
                public boolean verify( JComponent input) { return true; }
                public boolean shouldYieldFocus( JComponent input) {
                    if (!lastInput.equals( chasenLocation.getText())) {
                        testChasenLocation();
                        lastInput = chasenLocation.getText();
                    }
                    return true;
                }
            });
        b.add( chasenLocation);
        JButton chasenLocationChoice = new JButton( JGloss.messages.getString( "button.choose"));
        chasenLocationChoice.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e) {
                    chooseChasenLocation();
                }
            });
        b.add( chasenLocationChoice);
        b.add( Box.createHorizontalStrut( 3));
        all.add( b);
        all.add( Box.createVerticalStrut( 2));

        // enable editing
        b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        enableEditing = new JCheckBox( JGloss.messages.getString( "general.editor.enableediting"));
        b.add( enableEditing);
        b.add( Box.createHorizontalGlue());
        all.add( b);
        all.add( Box.createVerticalStrut( 2));

        this.add( UIUtilities.createSpaceEater( all, false));

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
        chasenLocation.setText( JGloss.prefs.getString( Preferences.CHASEN_LOCATION));
        importClipboardParserSelector.setEnabled( ChasenParser.class,
                                                  ChasenParser.isChasenExecutable
                                                  ( chasenLocation.getText()));
        try {
            importClipboardParserSelector.setSelected( Class.forName
                                                       ( JGloss.prefs.getString
                                                         ( Preferences.IMPORTCLIPBOARD_PARSER)));
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        importClipboardParserSelector.setReadingBrackets
            ( JGloss.prefs.getString( Preferences.IMPORTCLIPBOARD_READINGBRACKETS).charAt( 0),
              JGloss.prefs.getString( Preferences.IMPORTCLIPBOARD_READINGBRACKETS).charAt( 1));
    }

    /**
     * Saves the current dialog settings.
     */
    public void savePreferences() {
        JGloss.prefs.set( Preferences.STARTUP_WORDLOOKUP, startWordLookup.isSelected());
        JGloss.prefs.set( Preferences.EDITOR_ENABLEEDITING, enableEditing.isSelected());
        JGloss.prefs.set( Preferences.CHASEN_LOCATION, chasenLocation.getText());
        JGloss.prefs.set( Preferences.IMPORTCLIPBOARD_PARSER,
                          importClipboardParserSelector.getSelectedParser().getName());
        if (importClipboardParserSelector.getReadingStart() != '\0' &&
            importClipboardParserSelector.getReadingEnd() != '\0')
            JGloss.prefs.set( Preferences.IMPORTCLIPBOARD_READINGBRACKETS,
                              new String( new char[] { importClipboardParserSelector.getReadingStart(),
                                                       importClipboardParserSelector.getReadingEnd() }));
    }

    public void applyPreferences() {
        ChasenParser.setDefaultExecutable( chasenLocation.getText());
        importClipboardParser = importClipboardParserSelector.getSelectedParser();
        readingStart = importClipboardParserSelector.getReadingStart();
        readingEnd = importClipboardParserSelector.getReadingEnd();
    }

    public Parser createImportClipboardParser( jgloss.dictionary.Dictionary[] dictionaries,
                                               Set exclusions) {
        return ParserSelector.createParser( importClipboardParser, dictionaries, exclusions,
                                            readingStart, readingEnd);
    }

    private void chooseChasenLocation() {
        JFileChooser chooser = new JFileChooser( JGloss.getCurrentDir());
        chooser.setDialogTitle( JGloss.messages.getString( "general.chasen.chooser.title"));
        chooser.setFileHidingEnabled( true);
        chooser.setMultiSelectionEnabled( false);
        chooser.setFileSelectionMode( JFileChooser.FILES_ONLY);
        int r = chooser.showDialog( this, JGloss.messages.getString( "button.select"));
        if (r == JFileChooser.APPROVE_OPTION) {
            chasenLocation.setText( chooser.getSelectedFile().getAbsolutePath());
            testChasenLocation();
        }
    }

    private void testChasenLocation() {
        if (!ChasenParser.isChasenExecutable( chasenLocation.getText())) {
            JOptionPane.showMessageDialog( this, JGloss.messages.getString
                                           ( "warning.chasen"), JGloss.messages.getString
                                           ( "warning.chasen.title"),
                                           JOptionPane.WARNING_MESSAGE);
            importClipboardParserSelector.setEnabled( ChasenParser.class, false);
        }
        else
            importClipboardParserSelector.setEnabled( ChasenParser.class, true);
    }
} // class GeneralDialog