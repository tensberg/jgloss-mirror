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
 *
 */

package jgloss.ui.gloss;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.html.StyleSheet;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.StyleDialog;
import jgloss.ui.html.AnnotationTags;
import jgloss.ui.html.JGlossEditor;
import jgloss.ui.util.UIUtilities;

/**
 * Style dialog which also manages JGloss document styles.
 *
 * @author Michael Koch
 */
public class DocumentStyleDialog extends StyleDialog {
	private static final Logger LOGGER = Logger.getLogger(DocumentStyleDialog.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;

	/**
     * The single application-wide instance.
     */
    private static DocumentStyleDialog box;

    /**
     * Returns the single application-wide instance.
     *
     * @return The StyleDialog component.
     */
    public static DocumentStyleDialog getDocumentStyleDialog() {
        if (box == null) {
	        box = new DocumentStyleDialog();
        }
        return box;
    }

    /**
     * CSS background-color property value for body color.
     */
    private final static String BACKGROUND_COLOR = "white";

    protected JComboBox textFont;
    protected JComboBox readingFont;
    protected JComboBox translationFont;

    protected JComboBox textFontSize;
    protected JComboBox readingFontSize;
    protected JComboBox translationFontSize;

    protected JCheckBox textUseColor;
    protected JCheckBox readingUseColor;
    protected JCheckBox translationUseColor;

    protected JButton textColor;
    protected JButton readingColor;
    protected JButton translationColor;
    protected JButton highlightColor;

    /**
     * The styles currently applied to the documents. Map from managed style sheet to style string.
     * Will be updated when {@link #applyPreferences() applyPreferences} is called.
     */
    protected Map<StyleSheet, String> currentStyles;

    /**
     * The list of managed style sheets.
     */
    protected List<StyleSheet> styleSheets;


    protected DocumentStyleDialog() {
        super();
    }

    @Override
	protected void insertAdditionalControls( String[] allFonts) {
        currentStyles = new HashMap<StyleSheet, String>( 10);
        styleSheets = new ArrayList<StyleSheet>( 10);

        textFont = new JComboBox( allFonts);
        textFont.setEditable( false);
        readingFont = new JComboBox( allFonts);
        readingFont.setEditable( false);
        translationFont = new JComboBox( allFonts);
        translationFont.setEditable( false);

        textFontSize = new JComboBox( JGloss.PREFS.getList( Preferences.FONTSIZES_KANJI, ','));
        textFontSize.setEditable( true);
        readingFontSize = new JComboBox( JGloss.PREFS.getList( Preferences.FONTSIZES_READING, ','));
        readingFontSize.setEditable( true);
        translationFontSize = new JComboBox( JGloss.PREFS.getList
                                             ( Preferences.FONTSIZES_TRANSLATION, ','));
        translationFontSize.setEditable( true);

        ActionListener colorActionListener = new ActionListener() {
                @Override
				public void actionPerformed( ActionEvent e) {
                    doColorChooser( (JButton) e.getSource());
                }
            };
        textColor = new JButton( new ColorIcon());
        textColor.addActionListener( colorActionListener);
        readingColor = new JButton( new ColorIcon());
        readingColor.addActionListener( colorActionListener);
        translationColor = new JButton( new ColorIcon());
        translationColor.addActionListener( colorActionListener);

        textUseColor = new JCheckBox( JGloss.MESSAGES.getString( "style.text.usecolor"), true);
        textUseColor.addChangeListener( new ChangeListener() {
                @Override
				public void stateChanged( ChangeEvent e) {
                    textColor.setEnabled( textUseColor.isSelected());
                }
            });
        readingUseColor = new JCheckBox( JGloss.MESSAGES.getString( "style.text.usecolor"), true);
        readingUseColor.addChangeListener( new ChangeListener() {
                @Override
				public void stateChanged( ChangeEvent e) {
                    readingColor.setEnabled( readingUseColor.isSelected());
                }
            });
        translationUseColor = new JCheckBox( JGloss.MESSAGES.getString( "style.text.usecolor"), true);
        translationUseColor.addChangeListener( new ChangeListener() {
                @Override
				public void stateChanged( ChangeEvent e) {
                    translationColor.setEnabled( translationUseColor.isSelected());
                }
            });
        
        Box b;
        Box b2;
        JPanel p;

        // text
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
                                                       ( "style.text")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( textFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( textFontSize);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        b.add( Box.createVerticalStrut( 7));
        b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( textUseColor);
        b2.add( textColor);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        // reading
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
                                                       ( "style.reading")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( readingFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( readingFontSize);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        b.add( Box.createVerticalStrut( 7));
        b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( readingUseColor);
        b2.add( readingColor);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        // translation
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
                                                       ( "style.translation")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( translationFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( translationFontSize);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        b.add( Box.createVerticalStrut( 7));
        b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( translationUseColor);
        b2.add( translationColor);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        p.add( b);
        this.add( p);
        
        this.add( Box.createVerticalStrut( 2));
        // highlight color
        b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        b.add( new JLabel( JGloss.MESSAGES.getString( "style.highlight.color")));
        b.add( Box.createHorizontalStrut( 3));
        highlightColor = new JButton( new ColorIcon());
        highlightColor.addActionListener( colorActionListener);
        b.add( highlightColor);
        b.add( Box.createHorizontalGlue());
        this.add( b);
    }

    /**
     * Adds a new style sheet which will then automatically track any changes made to the
     * user settings. The stylesheet can have styles in addition to the ones set in the dialog
     * by using <CODE>additionalStyles</CODE>, which is a mapping from a HTML tag name as string
     * to a CSS style fragment. Supported tags are "body", "anno", "reading" and "trans".
     *
     * @param s The style sheet to add.
     * @param additionalStyles Additional CSS styles.
     */
    public void addStyleSheet( StyleSheet s) {
        synchronized (styleSheets) {
            styleSheets.add( s);
            applyPreferences( s);
        }
    }

    /**
     * Removes a style sheet from the list of managed style sheets. 
     *
     * @param s The style sheet to remove.
     */
    public void removeStyleSheet( StyleSheet s) {
        synchronized (styleSheets) {
            int i = styleSheets.indexOf( s);
            if (i != -1) {
                styleSheets.remove( i); // Style Sheet
                currentStyles.remove( s);
            }
        }
    }

    @Override
	public void loadPreferences() {
        super.loadPreferences();
        
        insertAndSelect( textFont, JGloss.PREFS.getString( Preferences.FONT_TEXT));
        insertAndSelect( readingFont, JGloss.PREFS.getString( Preferences.FONT_READING));
        insertAndSelect( translationFont, JGloss.PREFS.getString( Preferences.FONT_TRANSLATION));

        textFontSize.setSelectedItem
            ( Integer.toString( JGloss.PREFS.getInt( Preferences.FONT_TEXT_SIZE, 12)));
        readingFontSize.setSelectedItem
            ( Integer.toString( JGloss.PREFS.getInt( Preferences.FONT_READING_SIZE, 12)));
        translationFontSize.setSelectedItem
            ( Integer.toString( JGloss.PREFS.getInt( Preferences.FONT_TRANSLATION_SIZE, 12)));

        textUseColor.setSelected( JGloss.PREFS.getBoolean( Preferences.FONT_TEXT_USECOLOR, true));
        readingUseColor.setSelected( JGloss.PREFS.getBoolean( Preferences.FONT_READING_USECOLOR, true));
        translationUseColor.setSelected( JGloss.PREFS.getBoolean( Preferences.FONT_TRANSLATION_USECOLOR, 
                                                                  true));

        ((ColorIcon) textColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.PREFS.getInt( Preferences.FONT_TEXT_BGCOLOR, 0xffffff))));
        ((ColorIcon) readingColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.PREFS.getInt( Preferences.FONT_READING_BGCOLOR, 0xffffff))));
        ((ColorIcon) translationColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.PREFS.getInt( Preferences.FONT_TRANSLATION_BGCOLOR, 0xffffff))));
        ((ColorIcon) highlightColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.PREFS.getInt( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xffffff))));
    }

    @Override
	public void savePreferences() {
        super.savePreferences();

        String font = (String) textFont.getSelectedItem();
        if (font != null) {
	        JGloss.PREFS.set( Preferences.FONT_TEXT, font);
        }
        font = (String) readingFont.getSelectedItem();
        if (font != null) {
	        JGloss.PREFS.set( Preferences.FONT_READING, font);
        }
        font = (String) translationFont.getSelectedItem();
        if (font != null) {
	        JGloss.PREFS.set( Preferences.FONT_TRANSLATION, font);
        }

        String size = (String) textFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.PREFS.set( Preferences.FONT_TEXT_SIZE, s);
        } catch (Exception ex) { 
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            textFontSize.setSelectedItem( JGloss.PREFS.getString( Preferences.FONT_TEXT_SIZE));
        }
        size = (String) readingFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.PREFS.set( Preferences.FONT_READING_SIZE, s);
        } catch (Exception ex) { 
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            textFontSize.setSelectedItem( JGloss.PREFS.getString( Preferences.FONT_READING_SIZE));
        }
        size = (String) translationFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.PREFS.set( Preferences.FONT_TRANSLATION_SIZE, s);
        } catch (Exception ex) { 
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            textFontSize.setSelectedItem( JGloss.PREFS.getString( Preferences.FONT_TRANSLATION_SIZE));
        }

        JGloss.PREFS.set( Preferences.FONT_TEXT_USECOLOR, textUseColor.isSelected());
        Color color = ((ColorIcon) textColor.getIcon()).getColor();
        JGloss.PREFS.set( Preferences.FONT_TEXT_BGCOLOR, color.getRGB() & 0xffffff);
        JGloss.PREFS.set( Preferences.FONT_READING_USECOLOR, readingUseColor.isSelected());
        color = ((ColorIcon) readingColor.getIcon()).getColor();
        JGloss.PREFS.set( Preferences.FONT_READING_BGCOLOR, color.getRGB() & 0xffffff);
        JGloss.PREFS.set( Preferences.FONT_TRANSLATION_USECOLOR, translationUseColor.isSelected());
        color = ((ColorIcon) translationColor.getIcon()).getColor();
        JGloss.PREFS.set( Preferences.FONT_TRANSLATION_BGCOLOR, color.getRGB() & 0xffffff);
        color = ((ColorIcon) highlightColor.getIcon()).getColor();
        JGloss.PREFS.set( Preferences.ANNOTATION_HIGHLIGHT_COLOR, color.getRGB() & 0xffffff);
    }

    @Override
	public void applyPreferences() {
        super.applyPreferences();

        // apply document view styles
        synchronized (styleSheets) {
            for (StyleSheet styleSheet : styleSheets) {
	            applyPreferences(styleSheet);
            }
        }
    }

    /**
     * Displays the color chooser if a button with a color icon is selected.
     * The color of the icon will be set to the selected color.
     *
     * @param b The button, which must have a <CODE>ColorIcon</CODE>.
     */
    protected void doColorChooser( JButton b) {
        String title;
        if (b == highlightColor) {
	        title = "style.highlight.colorchooser.title";
        } else {
	        title = "style.text.colorchooser.title";
        }

        Color nc = JColorChooser.showDialog( SwingUtilities.getRoot( this),
                                             JGloss.MESSAGES.getString( title),
                                             ((ColorIcon) b.getIcon()).getColor());
        if (nc != null) {
            ((ColorIcon) b.getIcon()).setColor( nc);
            b.repaint();
        }                                     
    }

    /**
     * Applies the settings from the application preferences to a single style sheet.
     *
     * @param s The style sheet.
     */
    protected void applyPreferences( StyleSheet s) {
        if (s == null) {
	        return;
        }

        String style = "body { background-color: " + BACKGROUND_COLOR + "; ";
        if (JGloss.PREFS.getString( Preferences.FONT_TEXT).length()!=0) {
            style += "font-family: " + JGloss.PREFS.getString( Preferences.FONT_TEXT) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.PREFS.getString( Preferences.FONT_TEXT_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { LOGGER.log(Level.SEVERE, ex.getMessage(), ex); }
        style += "}\n";
        
        style += AnnotationTags.BASETEXT.getId() + " { ";
        if (JGloss.PREFS.getBoolean( Preferences.FONT_TEXT_USECOLOR, true)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.PREFS.getInt( Preferences.FONT_TEXT_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            // this removes, among other settings, the current background color settings
            s.removeStyle( AnnotationTags.BASETEXT.getId());
        }
        style += "}\n";

        style += AnnotationTags.READING.getId() + " { ";
        if (JGloss.PREFS.getString( Preferences.FONT_READING).length()!=0) {
            style += "font-family: " + JGloss.PREFS.getString( Preferences.FONT_READING) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.PREFS.getString( Preferences.FONT_READING_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { LOGGER.log(Level.SEVERE, ex.getMessage(), ex); }
        if (JGloss.PREFS.getBoolean( Preferences.FONT_READING_USECOLOR, true)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.PREFS.getInt( Preferences.FONT_READING_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            style += "background-color: " + BACKGROUND_COLOR + "; ";
        }
        style += "}\n";

        style += AnnotationTags.TRANSLATION.getId() + " { ";
        if (JGloss.PREFS.getString( Preferences.FONT_TRANSLATION).length()!=0) {
            style += "font-family: " + JGloss.PREFS.getString( Preferences.FONT_TRANSLATION) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.PREFS.getString( Preferences.FONT_TRANSLATION_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { LOGGER.log(Level.SEVERE, ex.getMessage(), ex); }
        if (JGloss.PREFS.getBoolean( Preferences.FONT_TRANSLATION_USECOLOR, true)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.PREFS.getInt( Preferences.FONT_TRANSLATION_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            style += "background-color: " + BACKGROUND_COLOR + "; ";
        }

        style += "}\n";

        if (!style.equals( currentStyles.get( s))) { // only apply style if something changed
            s.addRule( style);
            currentStyles.put( s, style);
        }

        JGlossEditor.setHighlightColor
            ( new Color( Math.max( 0, JGloss.PREFS.getInt
                                   ( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xcccccc))));
    }

    @Override
	protected void selectJapaneseFont( String fontname) {
        insertAndSelect( textFont, fontname);
        insertAndSelect( readingFont, fontname);
        insertAndSelect( translationFont, fontname);
        super.selectJapaneseFont( fontname);
    }

    @Override
	protected JComboBox[] getAutodetectedFonts() {
        return new JComboBox[] { wordLookupFont, textFont, readingFont, translationFont };
    }
} // class DocumentStyleDialog
