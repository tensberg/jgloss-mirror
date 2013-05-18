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

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.parser.Chasen;
import jgloss.parser.ChasenParser;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.ui.util.Icons;
import jgloss.ui.util.UIUtilities;

/**
 * Component which allows the user to edit general preferences. This will normally embedded
 * in the application preferences dialog. There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getInstance() getInstance()} method.
 *
 * @author Michael Koch
 */
public class GeneralDialog extends Box implements PreferencesPanel {
	private static final Logger LOGGER = Logger.getLogger(GeneralDialog.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;

	/**
     * The single application-wide instance.
     */
    private static GeneralDialog box;

    /**
     * Returns the single application-wide instance.
     *
     * @return The GeneralDialog component.
     */
    public static synchronized GeneralDialog getInstance() {
        if (box == null) {
	        box = new GeneralDialog();
        }
        return box;
    }

    private JCheckBox enableEditing;

    private JRadioButton startFrame;
    private JRadioButton startWordLookup;

    private JRadioButton clickTooltip;
    private JRadioButton clickSelect;

    private JTextField chasenLocation;

    private ParserSelector importClipboardParserSelector;
    private Class<? extends Parser> importClipboardParser;
    private boolean firstOccurrenceOnly;
    private char readingStart;
    private char readingEnd;

    /**
     * Creates the style dialog.
     */
    private GeneralDialog() {
        super( BoxLayout.Y_AXIS);

        Box all = Box.createVerticalBox();

        JPanel p = new JPanel( new GridLayout( 2, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
                                                       ( "general.startup")));
        ButtonGroup bg = new ButtonGroup();
        startFrame = new JRadioButton( JGloss.MESSAGES.getString( "general.startup.jglossframe"));
        bg.add( startFrame);
        p.add( startFrame);
        startWordLookup = new JRadioButton( JGloss.MESSAGES.getString( "general.startup.wordlookup"));
        bg.add( startWordLookup);
        p.add( startWordLookup);
        all.add( p);
        all.add( Box.createVerticalStrut( 2));

        p = new JPanel( new GridLayout( 2, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
                                                       ( "general.leftclick")));
        bg = new ButtonGroup();
        clickSelect = new JRadioButton( JGloss.MESSAGES.getString( "general.leftclick.select"));
        bg.add( clickSelect);
        p.add( clickSelect);
        clickTooltip = new JRadioButton( JGloss.MESSAGES.getString( "general.leftclick.tooltip"));
        bg.add( clickTooltip);
        p.add( clickTooltip);
        all.add( p);
        all.add( Box.createVerticalStrut( 2));

        importClipboardParserSelector = new ParserSelector( true);
        importClipboardParserSelector.setBorder( BorderFactory.createTitledBorder 
                                                 ( JGloss.MESSAGES.getString( "general.parserselector")));
        all.add( importClipboardParserSelector);
        all.add( Box.createVerticalStrut( 2));

        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        b.add( new JLabel( JGloss.MESSAGES.getString( "general.chasen.label")));
        b.add( Box.createHorizontalStrut( 2));
        chasenLocation = new JTextField( JGloss.PREFS.getString( Preferences.CHASEN_LOCATION));
        chasenLocation.setInputVerifier( new InputVerifier() {
                private String lastInput = chasenLocation.getText();
                @Override
				public boolean verify( JComponent input) { return true; }
                @Override
				public boolean shouldYieldFocus( JComponent input) {
                    if (!lastInput.equals( chasenLocation.getText())) {
                        testChasenLocation();
                        lastInput = chasenLocation.getText();
                    }
                    return true;
                }
            });
        b.add( chasenLocation);
        b.add( Box.createHorizontalStrut( 2));
        JButton chasenLocationChoice = new JButton( JGloss.MESSAGES.getString( "button.choose"));
        chasenLocationChoice.addActionListener( new ActionListener() {
                @Override
				public void actionPerformed( ActionEvent e) {
                    chooseChasenLocation();
                }
            });
        b.add( chasenLocationChoice);
        b.add( Box.createHorizontalStrut( 3));
        all.add( b);
        all.add( Box.createVerticalStrut( 2));

        // enable editing
        if ( JGloss.PREFS.getBoolean( Preferences.EDITOR_ENABLEEDITINGCHECKBOX, false)) {
            // this is a "hidden" control because the direct editing feature is buggy and can
            // break the current document if not used with care.
            b = Box.createHorizontalBox();
            b.add( Box.createHorizontalStrut( 3));
            enableEditing = new JCheckBox( JGloss.MESSAGES.getString( "general.editor.enableediting"));
            b.add( enableEditing);
            b.add( Box.createHorizontalGlue());
            all.add( b);
            all.add( Box.createVerticalStrut( 2));
        }

        this.add( UIUtilities.createFlexiblePanel( all, false));

        loadPreferences();
        applyPreferences();
    }

    @Override
	public String getTitle() { return JGloss.MESSAGES.getString( "general.title"); }
    @Override
	public Component getComponent() { return this; }

    /**
     * Loads the preferences and initializes the dialog accordingly.
     */
    @SuppressWarnings("unchecked")
    @Override
	public void loadPreferences() {
        if (JGloss.PREFS.getBoolean( Preferences.STARTUP_WORDLOOKUP, false)) {
	        startWordLookup.setSelected( true);
        } else {
	        startFrame.setSelected( true);
        }
        if (JGloss.PREFS.getBoolean( Preferences.LEFTCLICK_TOOLTIP, false)) {
	        clickTooltip.setSelected( true);
        } else {
	        clickSelect.setSelected( true);
        }

        if (enableEditing != null) {
	        enableEditing.setSelected( JGloss.PREFS.getBoolean( Preferences.EDITOR_ENABLEEDITING, false));
        }
        chasenLocation.setText( JGloss.PREFS.getString( Preferences.CHASEN_LOCATION));
        importClipboardParserSelector.setEnabled( ChasenParser.class,
                                                  Chasen.isChasenExecutable
                                                  ( chasenLocation.getText()));
        try {
            Class<?> selectedParser = Class.forName( JGloss.PREFS.getString( Preferences.IMPORTCLIPBOARD_PARSER));
            if (!Parser.class.isAssignableFrom(selectedParser)) {
            	throw new IllegalArgumentException(selectedParser + " must be a parser class");
            }
			importClipboardParserSelector.setSelected( (Class<? extends Parser>) selectedParser);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        importClipboardParserSelector.setFirstOccurrenceOnly
            ( JGloss.PREFS.getBoolean( Preferences.IMPORTCLIPBOARD_FIRSTOCCURRENCE, true));
        importClipboardParserSelector.setDetectParagraphs
            ( JGloss.PREFS.getBoolean( Preferences.IMPORTCLIPBOARD_DETECTPARAGRAPHS, true));
        String brackets = JGloss.PREFS.getString( Preferences.IMPORTCLIPBOARD_READINGBRACKETS);
        if (brackets.length() == 2) {
	        importClipboardParserSelector.setReadingBrackets
                ( brackets.charAt( 0), brackets.charAt( 1));
        } else {
	        importClipboardParserSelector.setNoReadingBrackets();
        }
    }

    /**
     * Saves the current dialog settings.
     */
    @Override
	public void savePreferences() {
        JGloss.PREFS.set( Preferences.STARTUP_WORDLOOKUP, startWordLookup.isSelected());
        JGloss.PREFS.set( Preferences.LEFTCLICK_TOOLTIP, clickTooltip.isSelected());
        if (enableEditing != null) {
	        JGloss.PREFS.set( Preferences.EDITOR_ENABLEEDITING, enableEditing.isSelected());
        }
        JGloss.PREFS.set( Preferences.CHASEN_LOCATION, chasenLocation.getText());
        JGloss.PREFS.set( Preferences.IMPORTCLIPBOARD_PARSER,
                          importClipboardParserSelector.getSelectedParser().getName());
        JGloss.PREFS.set( Preferences.IMPORTCLIPBOARD_FIRSTOCCURRENCE,
                          importClipboardParserSelector.isFirstOccurrenceOnly());
        JGloss.PREFS.set( Preferences.IMPORTCLIPBOARD_DETECTPARAGRAPHS,
                          importClipboardParserSelector.isDetectParagraphs());
        if (importClipboardParserSelector.isNoReadingBrackets()) {
	        JGloss.PREFS.set( Preferences.IMPORTCLIPBOARD_READINGBRACKETS, "");
        } else {
	        JGloss.PREFS.set( Preferences.IMPORTCLIPBOARD_READINGBRACKETS,
                              new String( new char[] { importClipboardParserSelector.getReadingStart(),
                                                       importClipboardParserSelector.getReadingEnd() }));
        }
    }

    @Override
	public void applyPreferences() {
        Chasen.setDefaultExecutable( chasenLocation.getText());
        importClipboardParser = importClipboardParserSelector.getSelectedParser();
        firstOccurrenceOnly = importClipboardParserSelector.isFirstOccurrenceOnly();
        readingStart = importClipboardParserSelector.getReadingStart();
        readingEnd = importClipboardParserSelector.getReadingEnd();
    }

    public Parser createImportClipboardParser( jgloss.dictionary.Dictionary[] dictionaries,
                                               Set<String> exclusions) {
        return ParserSelector.createParser( importClipboardParser, dictionaries, exclusions,
                                            firstOccurrenceOnly);
    }

    public ReadingAnnotationFilter createReadingAnnotationFilter() {
        if (readingStart!='\0' && readingEnd!='\0') {
	        // FIXME: kanji separator '\uff5c' should be user-configurable
            return new ReadingAnnotationFilter( readingStart, readingEnd, '\uff5c');
        } else {
	        return null;
        }
    }

    private void chooseChasenLocation() {
        JFileChooser chooser = new JFileChooser( JGloss.getApplication().getCurrentDir());
        chooser.setDialogTitle( JGloss.MESSAGES.getString( "general.chasen.chooser.title"));
        chooser.setFileHidingEnabled( true);
        chooser.setMultiSelectionEnabled( false);
        chooser.setFileSelectionMode( JFileChooser.FILES_ONLY);
        chooser.setFileView( new CustomFileView() {
                private final Icon CHASEN_ICON = Icons.getIcon("chasen.png");
                @Override
				public Icon getIcon( java.io.File f) {
                    String name = f.getName().toLowerCase();
                    if ((name.equals( "chasen") || name.equals( "chasen.exe")) &&
                        f.isFile()) {
	                    return CHASEN_ICON;
                    }
                    
                    return super.getIcon( f);
                }
            });
        int r = chooser.showDialog( this, JGloss.MESSAGES.getString( "button.select"));
        if (r == JFileChooser.APPROVE_OPTION) {
            chasenLocation.setText( chooser.getSelectedFile().getAbsolutePath());
            testChasenLocation();
        }
    }

    private void testChasenLocation() {
        if (!Chasen.isChasenExecutable( chasenLocation.getText())) {
            JOptionPane.showMessageDialog( this, JGloss.MESSAGES.getString
                                           ( "warning.chasen"), JGloss.MESSAGES.getString
                                           ( "warning.chasen.title"),
                                           JOptionPane.WARNING_MESSAGE);
            importClipboardParserSelector.setEnabled( ChasenParser.class, false);
        } else {
	        importClipboardParserSelector.setEnabled( ChasenParser.class, true);
        }
    }
} // class GeneralDialog
