/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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
 * Component which allows the user to edit visual-related preferences. This will normally embedded
 * in the application preferences dialog. There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getComponent() getComponent()} method.
 *
 * @author Michael Koch
 */
public class StyleDialog extends Box {
    /**
     * The single application-wide instance.
     */
    private static StyleDialog box;

    /**
     * Returns the single application-wide instance.
     *
     * @return The StyleDialog component.
     */
    public static StyleDialog getComponent() {
        if (box == null)
            box = new StyleDialog();
        return box;
    }

    /**
     * Four japanese characters used to test the fonts.
     */
    private final static String JAPANESE_CHARS_TEST = "A\u3042\u30a2\u660e\u3002";
    /**
     * Default japanese font used if the generic fonts like SansSerif, Dialog etc. don't contain Japanese
     * characters.
     */
    private static String knownJapaneseFont;

    /**
     * Map from Swing L&F property keys to the default L&F fonts.
     */
    private static Map defaultLFFonts;

    /**
     * Adds a new style sheet which will then automatically track any changes made to the
     * user settings. The stylesheet can have styles in addition to the ones set in the dialog
     * by using <CODE>additionalStyles</CODE>, which is a mapping from a HTML tag name as string
     * to a CSS style fragment. Supported tags are "body", "anno", "reading" and "trans".
     *
     * @param s The style sheet to add.
     * @param additionalStyles Additional CSS styles.
     */
    public void addStyleSheet( StyleSheet s, Map additionalStyles) {
        synchronized (styleSheets) {
            styleSheets.add( s);
            styleSheets.add( additionalStyles);
            applyPreferences( s, additionalStyles);
        }
    }

    /**
     * Updates the additional styles of a style sheet.
     *
     * @param s The style sheet to update.
     * @param additionalStyles Additional CSS styles.
     */
    public void updateAdditionalStyles( StyleSheet s, Map additionalStyles) {
        synchronized (styleSheets) {
            int i = styleSheets.indexOf( s);
            if (i != -1) {
                styleSheets.remove( i+1);
                styleSheets.add( i+1, additionalStyles);
                applyPreferences( s, additionalStyles);
            }
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
                styleSheets.remove( i); // additionalStyles
            }
        }
    }

    private JButton autodetect;

    private JRadioButton generalFontDefault;
    private JRadioButton generalFontCustom;

    private JComboBox generalFont;
    private JComboBox wordLookupFont;
    private JComboBox textFont;
    private JComboBox readingFont;
    private JComboBox translationFont;

    private JComboBox wordLookupFontSize;
    private JComboBox textFontSize;
    private JComboBox readingFontSize;
    private JComboBox translationFontSize;
    
    private JCheckBox textUseColor;
    private JCheckBox readingUseColor;
    private JCheckBox translationUseColor;

    private JButton textColor;
    private JButton readingColor;
    private JButton translationColor;
    private JButton highlightColor;

    /**
     * The styles currently applied to the documents. Map from managed style sheet to style string.
     * Will be updated when {@link #applyPreferences() applyPreferences} is called.
     */
    private Map currentStyles;

    /**
     * The list of managed style sheets.
     */
    private java.util.List styleSheets;

    /**
     * An Icon which paints itself as a single color.
     *
     * @author Michael Koch
     */
    private class ColorIcon implements Icon {
        /**
         * Width of the icon.
         */
        private int width;
        /**
         * Height of the icon.
         */
        private int height;
        /**
         * Color of the icon.
         */
        private Color color;
        
        /**
         * Creates a new white ColorIcon with a size of 20x10.
         */
        public ColorIcon() {
            this( 20, 10, Color.white);
        }

        /**
         * Creates a new ColorIcon with the specified size and color.
         *
         * @param width Width of this icon.
         * @param height Height of this icon.
         * @param color Color of this icon.
         */
        public ColorIcon( int width, int height, Color color) {
            this.width = width;
            this.height = height;
            this.color = color;
        }

        /**
         * Returns the width of this icon.
         *
         * @return The width of this icon.
         */
        public int getIconWidth() { return width; }
        /**
         * Returns the height of this icon.
         *
         * @return The height of this icon.
         */
        public int getIconHeight() { return height; }
        /**
         * Returns the color of this icon.
         *
         * @return The color of this icon
         */
        public Color getColor() { return color; }
        /**
         * Sets the new color of this icon.
         *
         * @param color The new color.
         */
        public void setColor( Color color) {
            this.color = color;
        }

        public void paintIcon( Component c, Graphics g, int x, int y) {
            g.setColor( color);
            g.setPaintMode();
            g.fillRect( x, y, width, height);
        }
    }

    /**
     * Creates the style dialog.
     */
    public StyleDialog() {
        super( BoxLayout.Y_AXIS);

        currentStyles = new HashMap( 10);
        styleSheets = new ArrayList( 10);

        // NOTE: getAvailableFontFamilyNames and getAllFonts do not list all available fonts
        // under JDK 1.4 and Linux (I have no idea why). Therefore, not all fonts may be available
        // from the font popups, and insertAndSelect is used whenever a value in a font
        // JComboBox is selected.
        String[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();

        autodetect = new JButton( JGloss.messages.getString( "style.autodetect"));
        autodetect.addActionListener( new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    autodetectFontsAction();
                }
            });

        generalFont = new JComboBox( allFonts);
        generalFont.setEditable( false);
        generalFontDefault = new JRadioButton( JGloss.messages.getString( "style.general.default"));
        generalFontCustom = new JRadioButton( JGloss.messages.getString( "style.general.custom"));
        ButtonGroup bg = new ButtonGroup();
        bg.add( generalFontDefault);
        bg.add( generalFontCustom);
        generalFontCustom.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e) {
                    generalFont.setEnabled( generalFontCustom.isSelected());
                }
            });

        wordLookupFont = new JComboBox( allFonts);
        wordLookupFont.setEditable( false);
        textFont = new JComboBox( allFonts);
        textFont.setEditable( false);
        readingFont = new JComboBox( allFonts);
        readingFont.setEditable( false);
        translationFont = new JComboBox( allFonts);
        translationFont.setEditable( false);

        wordLookupFontSize = new JComboBox( JGloss.prefs.getList( Preferences.FONTSIZES_WORDLOOKUP, ','));
        wordLookupFontSize.setEditable( true);
        textFontSize = new JComboBox( JGloss.prefs.getList( Preferences.FONTSIZES_KANJI, ','));
        textFontSize.setEditable( true);
        readingFontSize = new JComboBox( JGloss.prefs.getList( Preferences.FONTSIZES_READING, ','));
        readingFontSize.setEditable( true);
        translationFontSize = new JComboBox( JGloss.prefs.getList
                                             ( Preferences.FONTSIZES_TRANSLATION, ','));
        translationFontSize.setEditable( true);

        ActionListener colorActionListener = new ActionListener() {
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

        textUseColor = new JCheckBox( JGloss.messages.getString( "style.text.usecolor"), true);
        textUseColor.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e) {
                    textColor.setEnabled( textUseColor.isSelected());
                }
            });
        readingUseColor = new JCheckBox( JGloss.messages.getString( "style.text.usecolor"), true);
        readingUseColor.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e) {
                    readingColor.setEnabled( readingUseColor.isSelected());
                }
            });
        translationUseColor = new JCheckBox( JGloss.messages.getString( "style.text.usecolor"), true);
        translationUseColor.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e) {
                    translationColor.setEnabled( translationUseColor.isSelected());
                }
            });

        Box b;
        Box b2;

        // general font
        JPanel p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "style.general")));
        b = Box.createHorizontalBox();
        b.add( generalFontDefault);
        b.add( generalFontCustom);
        b.add( generalFont);
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        // word lookup
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "style.wordlookup")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.messages.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( wordLookupFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.messages.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( wordLookupFontSize);
        b.add( UIUtilities.createSpaceEater( b2, true));
        b.add( Box.createVerticalStrut( 7));
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        // text
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "style.text")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.messages.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( textFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.messages.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( textFontSize);
        b.add( UIUtilities.createSpaceEater( b2, true));
        b.add( Box.createVerticalStrut( 7));
        b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( textUseColor);
        b2.add( textColor);
        b.add( UIUtilities.createSpaceEater( b2, true));
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        // reading
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "style.reading")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.messages.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( readingFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.messages.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( readingFontSize);
        b.add( UIUtilities.createSpaceEater( b2, true));
        b.add( Box.createVerticalStrut( 7));
        b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( readingUseColor);
        b2.add( readingColor);
        b.add( UIUtilities.createSpaceEater( b2, true));
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        // translation
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "style.translation")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.messages.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( translationFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.messages.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( translationFontSize);
        b.add( UIUtilities.createSpaceEater( b2, true));
        b.add( Box.createVerticalStrut( 7));
        b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( translationUseColor);
        b2.add( translationColor);
        b.add( UIUtilities.createSpaceEater( b2, true));
        p.add( b);
        this.add( p);
        
        this.add( Box.createVerticalStrut( 2));
        // highlight color
        b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        b.add( new JLabel( JGloss.messages.getString( "style.highlight.color")));
        b.add( Box.createHorizontalStrut( 3));
        highlightColor = new JButton( new ColorIcon());
        highlightColor.addActionListener( colorActionListener);
        b.add( highlightColor);
        b.add( Box.createHorizontalGlue());
        this.add( b);

        // autodetect
        this.add( Box.createVerticalStrut( 3));
        b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        b.add( autodetect);
        b.add( Box.createHorizontalGlue());
        this.add( b);

        this.add( Box.createVerticalStrut( 2));

        loadPreferences();
    }

    /**
     * Displays the color chooser if a button with a color icon is selected.
     * The color of the icon will be set to the selected color.
     *
     * @param b The button, which must have a <CODE>ColorIcon</CODE>.
     */
    private void doColorChooser( JButton b) {
        String title;
        if (b == highlightColor)
            title = "style.highlight.colorchooser.title";
        else
            title = "style.text.colorchooser.title";

        Color nc = JColorChooser.showDialog( SwingUtilities.getRoot( this),
                                             JGloss.messages.getString( title),
                                             ((ColorIcon) b.getIcon()).getColor());
        if (nc != null) {
            ((ColorIcon) b.getIcon()).setColor( nc);
            b.repaint();
        }                                     
    }

    /**
     * Loads the preferences and initializes the dialog accordingly.
     */
    public void loadPreferences() {
        if (JGloss.prefs.getBoolean( Preferences.FONT_GENERAL_USEDEFAULT, true))
            generalFontDefault.setSelected( true);
        else
            generalFontCustom.setSelected( true);
        generalFont.setEnabled( !JGloss.prefs.getBoolean( Preferences.FONT_GENERAL_USEDEFAULT, true));

        insertAndSelect( generalFont, JGloss.prefs.getString( Preferences.FONT_GENERAL));
        insertAndSelect( wordLookupFont, JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP));
        insertAndSelect( textFont, JGloss.prefs.getString( Preferences.FONT_TEXT));
        insertAndSelect( readingFont, JGloss.prefs.getString( Preferences.FONT_READING));
        insertAndSelect( translationFont, JGloss.prefs.getString( Preferences.FONT_TRANSLATION));
        
        wordLookupFontSize.setSelectedItem
            ( Integer.toString( JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12)));
        textFontSize.setSelectedItem
            ( Integer.toString( JGloss.prefs.getInt( Preferences.FONT_TEXT_SIZE, 12)));
        readingFontSize.setSelectedItem
            ( Integer.toString( JGloss.prefs.getInt( Preferences.FONT_READING_SIZE, 12)));
        translationFontSize.setSelectedItem
            ( Integer.toString( JGloss.prefs.getInt( Preferences.FONT_TRANSLATION_SIZE, 12)));

        textUseColor.setSelected( JGloss.prefs.getBoolean( Preferences.FONT_TEXT_USECOLOR, true));
        readingUseColor.setSelected( JGloss.prefs.getBoolean( Preferences.FONT_READING_USECOLOR, true));
        translationUseColor.setSelected( JGloss.prefs.getBoolean( Preferences.FONT_TRANSLATION_USECOLOR, 
                                                                  true));

        ((ColorIcon) textColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.prefs.getInt( Preferences.FONT_TEXT_BGCOLOR, 0xffffff))));
        ((ColorIcon) readingColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.prefs.getInt( Preferences.FONT_READING_BGCOLOR, 0xffffff))));
        ((ColorIcon) translationColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.prefs.getInt( Preferences.FONT_TRANSLATION_BGCOLOR, 0xffffff))));
        ((ColorIcon) highlightColor.getIcon()).setColor( new Color
            ( Math.max( 0, JGloss.prefs.getInt( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xffffff))));
    }

    /**
     * Saves the current dialog settings.
     */
    public void savePreferences() {
        String font = (String) generalFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_GENERAL, font);
        JGloss.prefs.set( Preferences.FONT_GENERAL_USEDEFAULT, generalFontDefault.isSelected());

        font = (String) wordLookupFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_WORDLOOKUP, font);
        font = (String) textFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_TEXT, font);
        font = (String) readingFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_READING, font);
        font = (String) translationFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_TRANSLATION, font);

        String size = (String) wordLookupFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.prefs.set( Preferences.FONT_WORDLOOKUP_SIZE, s);
        } catch (Exception ex) { 
            ex.printStackTrace();
            textFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TEXT_SIZE));
        }
        size = (String) textFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.prefs.set( Preferences.FONT_TEXT_SIZE, s);
        } catch (Exception ex) { 
            ex.printStackTrace();
            textFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TEXT_SIZE));
        }
        size = (String) readingFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.prefs.set( Preferences.FONT_READING_SIZE, s);
        } catch (Exception ex) { 
            ex.printStackTrace();
            textFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_READING_SIZE));
        }
        size = (String) translationFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.prefs.set( Preferences.FONT_TRANSLATION_SIZE, s);
        } catch (Exception ex) { 
            ex.printStackTrace();
            textFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TRANSLATION_SIZE));
        }

        JGloss.prefs.set( Preferences.FONT_TEXT_USECOLOR, textUseColor.isSelected());
        Color color = ((ColorIcon) textColor.getIcon()).getColor();
        JGloss.prefs.set( Preferences.FONT_TEXT_BGCOLOR, color.getRGB() & 0xffffff);
        JGloss.prefs.set( Preferences.FONT_READING_USECOLOR, readingUseColor.isSelected());
        color = ((ColorIcon) readingColor.getIcon()).getColor();
        JGloss.prefs.set( Preferences.FONT_READING_BGCOLOR, color.getRGB() & 0xffffff);
        JGloss.prefs.set( Preferences.FONT_TRANSLATION_USECOLOR, translationUseColor.isSelected());
        color = ((ColorIcon) translationColor.getIcon()).getColor();
        JGloss.prefs.set( Preferences.FONT_TRANSLATION_BGCOLOR, color.getRGB() & 0xffffff);
        color = ((ColorIcon) highlightColor.getIcon()).getColor();
        JGloss.prefs.set( Preferences.ANNOTATION_HIGHLIGHT_COLOR, color.getRGB() & 0xffffff);
    }

    /**
     * Applies the settings from the application preferences to all style sheets.
     */
    public void applyPreferences() {
        // apply custom font settings
        applyUIFont();

        // apply document view styles
        synchronized (styleSheets) {
            for ( Iterator i=styleSheets.iterator(); i.hasNext(); )
                applyPreferences( (StyleSheet) i.next(), (Map) i.next());
        }
    }

    /**
     * Applies the UI font setting.
     */
    public static synchronized void applyUIFont() {
        if (JGloss.prefs.getBoolean( Preferences.FONT_GENERAL_USEDEFAULT, true)) {
            // restore java default fonts
            if (defaultLFFonts != null) {
                for ( Iterator i=defaultLFFonts.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) i.next();
                    UIManager.put( entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            // save java default fonts
            if (defaultLFFonts == null) {
                defaultLFFonts = new TreeMap();
                for ( Enumeration e=UIManager.getDefaults().keys(); e.hasMoreElements(); ) {
                    Object key = e.nextElement();
                    Object value = UIManager.getDefaults().get( key);
                    if (value instanceof Font) {
                        defaultLFFonts.put( key, value);
                    }
                }
            }
            // set custom font
            for ( Iterator i=defaultLFFonts.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry) i.next();
                UIManager.put( entry.getKey(), deriveGeneralFont( (Font) entry.getValue()));
            }
        }
    }

    /**
     * Applies the settings from the application preferences to a single style sheet.
     *
     * @param s The style sheet.
     * @param additionalStyles Style sheet-specific additional CSS styles.
     */
    private void applyPreferences( StyleSheet s, Map additionalStyles) {
        if (s == null)
            return;

        String style = "body { ";
        if (JGloss.prefs.getString( Preferences.FONT_TEXT).length()!=0) {
            style += "font-family: " + JGloss.prefs.getString( Preferences.FONT_TEXT) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.prefs.getString( Preferences.FONT_TEXT_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { ex.printStackTrace(); }
        if (additionalStyles.containsKey( "body"))
            style += additionalStyles.get( "body").toString();
        style += "}\n";
        
        style += AnnotationTags.BASETEXT.getId() + " { ";
        if (JGloss.prefs.getBoolean( Preferences.FONT_TEXT_USECOLOR, true)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.prefs.getInt( Preferences.FONT_TEXT_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            // this removes, among other settings, the current background color settings
            s.removeStyle( AnnotationTags.BASETEXT.getId());
        }
        if (additionalStyles.containsKey( AnnotationTags.BASETEXT.getId()))
            style += additionalStyles.get( AnnotationTags.BASETEXT.getId()).toString();
        style += "}\n";

        style += AnnotationTags.READING.getId() + " { ";
        if (JGloss.prefs.getString( Preferences.FONT_READING).length()!=0) {
            style += "font-family: " + JGloss.prefs.getString( Preferences.FONT_READING) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.prefs.getString( Preferences.FONT_READING_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { ex.printStackTrace(); }
        if (JGloss.prefs.getBoolean( Preferences.FONT_READING_USECOLOR, true)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.prefs.getInt( Preferences.FONT_READING_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            // this removes, among other settings, the current background color settings
            s.removeStyle( AnnotationTags.READING.getId());
        }
        if (additionalStyles.containsKey( AnnotationTags.READING.getId()))
            style += additionalStyles.get( AnnotationTags.READING.getId()).toString();
        style += "}\n";

        style += AnnotationTags.TRANSLATION.getId() + " { ";
        if (JGloss.prefs.getString( Preferences.FONT_TRANSLATION).length()!=0) {
            style += "font-family: " + JGloss.prefs.getString( Preferences.FONT_TRANSLATION) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.prefs.getString( Preferences.FONT_TRANSLATION_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { ex.printStackTrace(); }
        if (JGloss.prefs.getBoolean( Preferences.FONT_TRANSLATION_USECOLOR, true)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.prefs.getInt( Preferences.FONT_TRANSLATION_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            // this removes, among other settings, the current background color settings
            s.removeStyle( AnnotationTags.TRANSLATION.getId());
        }

        if (additionalStyles.containsKey( AnnotationTags.TRANSLATION.getId()))
            style += additionalStyles.get( AnnotationTags.TRANSLATION.getId()).toString();
        style += "}\n";

        if (!style.equals( currentStyles.get( s))) { // only apply style if something changed
            s.addRule( style);
            currentStyles.put( s, style);
        }

        JGlossEditor.setHighlightColor( new Color
            ( Math.max( 0, JGloss.prefs.getInt( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xcccccc))));
    }

    /**
     * Triggered if the Auto-Configure Fonts button is pressed. This method goes farther than
     * {@link #autodetectFonts() autodetectFonts} in that it gives the user feedback on what
     * was changed and it tests all available fonts (which can be slow).
     */
    private void autodetectFontsAction() {
        // Test if all selected fonts can display japanese characters, if yes, no further action
        // is neccessary.
        boolean canDisplayJapanese = 
            (canDisplayJapanese( "Dialog") && canDisplayJapanese( "DialogInput") &&
            generalFontDefault.isSelected() ||
            canDisplayJapanese( (String) generalFont.getSelectedItem()) &&
            generalFontCustom.isSelected()) &&
            canDisplayJapanese( (String) wordLookupFont.getSelectedItem()) &&
            canDisplayJapanese( (String) textFont.getSelectedItem()) &&
            canDisplayJapanese( (String) readingFont.getSelectedItem()) &&
            canDisplayJapanese( (String) translationFont.getSelectedItem());

        if (canDisplayJapanese) {
            // no configuration neccessary
            JOptionPane.showMessageDialog( this, 
                                           JGloss.messages.getString( "style.autodetect.nochange"),
                                           JGloss.messages.getString( "style.autodetect.title"),
                                           JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // try to use well-known font
        String japaneseFont = getKnownJapaneseFont();
        if (japaneseFont != null) {
            selectJapaneseFont( japaneseFont);
            return;            
        }

        // try to use one of the already selected fonts
        // the selected font may be of lower quality of one of the well-known fonts,
        // so do this step after the previous
        japaneseFont = null;
        if (canDisplayJapanese( (String) generalFont.getSelectedItem()))
            japaneseFont = (String) generalFont.getSelectedItem();
        else if (canDisplayJapanese( (String) wordLookupFont.getSelectedItem()))
            japaneseFont = (String) wordLookupFont.getSelectedItem();
        else if (canDisplayJapanese( (String) textFont.getSelectedItem()))
            japaneseFont = (String) textFont.getSelectedItem();
        else if (canDisplayJapanese( (String) readingFont.getSelectedItem()))
            japaneseFont = (String) readingFont.getSelectedItem();
        else if (canDisplayJapanese( (String) translationFont.getSelectedItem()))
            japaneseFont = (String) translationFont.getSelectedItem();

        if (japaneseFont != null) {
            selectJapaneseFont( japaneseFont);
            return;
        }

        // Try out all fonts. Since this is slow, do this in its own thread.
        Thread fontTester = new Thread() {
                public void run() {
                    String font = searchJapaneseFont();
                    if (font == null) {
                        JOptionPane.showMessageDialog
                            ( StyleDialog.this, 
                              JGloss.messages.getString( "style.autodetect.nofont"),
                              JGloss.messages.getString( "style.autodetect.title"),
                              JOptionPane.WARNING_MESSAGE);
                    }
                    else {
                        final String fontc = font;
                        EventQueue.invokeLater( new Runnable() {
                                public void run() {
                                    selectJapaneseFont( fontc);
                                }
                            });
                    }
                }
            };
        fontTester.start();
    }

    /**
     * Replaces the font selection of all fonts which can't display Japanese with the specified
     * font. Only the current dialog settings are modified, not the user preferences.
     * Also displays a dialog with the font name.
     */
    private void selectJapaneseFont( String fontname) {
        if (canDisplayJapanese( "Dialog") && canDisplayJapanese( "DialogInput")) {
            generalFontDefault.setSelected( true);
        }
        else {
            generalFontCustom.setSelected( true);
            insertAndSelect( generalFont, fontname);
        }
        insertAndSelect( wordLookupFont, fontname);
        insertAndSelect( textFont, fontname);
        insertAndSelect( readingFont, fontname);
        insertAndSelect( translationFont, fontname);

        JOptionPane.showMessageDialog( this, 
                                       JGloss.messages.getString( "style.autodetect.selectedfont",
                                                                  new String[] { fontname }),
                                       JGloss.messages.getString( "style.autodetect.title"),
                                       JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Select an item from an immutable combo box, adding the item to the combo box model if
     * neccessary. This is used because with JDK 1.4 under Linux, <code>getAvailableFontFamilyNames</code>
     * and <code>getAllFonts</code> don't list all fonts, and a font read from the preferences file
     * or selected by auto-detection which is to be selected, might not be in the model of the font
     * combo box.
     */
    private void insertAndSelect( JComboBox box, Object object) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) box.getModel();
        if (model.getIndexOf( object) == -1)
            model.insertElementAt( object, 0);
        box.setSelectedItem( object);
    }

    /**
     * Autodetect fonts used for the various font settings. If the currently selected font of a setting
     * can't display Japanese characters, a default font is set. If no Japanese default font can be
     * found on the system, the preference is not changed. This method will change the JGloss
     * preferences settings without updating the style dialog display. It should be called before
     * the style preferences dialog is instantiated.
     */
    public static void autodetectFonts() {
        String defaultFont = getKnownJapaneseFont();
        if (defaultFont == null)
            // no font available, leave settings as is
            return;

        // test general font
        if (!canDisplayJapanese( "Dialog") || !canDisplayJapanese( "DialogInput")) {
            JGloss.prefs.set( Preferences.FONT_GENERAL_USEDEFAULT, false);
        }
        if (!canDisplayJapanese( JGloss.prefs.getString( Preferences.FONT_GENERAL)))
            JGloss.prefs.set( Preferences.FONT_GENERAL, defaultFont);
        if (!canDisplayJapanese( JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP)))
            JGloss.prefs.set( Preferences.FONT_WORDLOOKUP, defaultFont);
        if (!canDisplayJapanese( JGloss.prefs.getString( Preferences.FONT_TEXT)))
            JGloss.prefs.set( Preferences.FONT_TEXT, defaultFont);
        if (!canDisplayJapanese( JGloss.prefs.getString( Preferences.FONT_READING)))
            JGloss.prefs.set( Preferences.FONT_READING, defaultFont);
        if (!canDisplayJapanese( JGloss.prefs.getString( Preferences.FONT_TRANSLATION)))
            JGloss.prefs.set( Preferences.FONT_TRANSLATION, defaultFont);
    }

    /**
     * Returns the name of a font which can display Japanese characters. A list of fonts
     * with known names is tested for availability, and the first available font is returned. If
     * none of the fonts is available, <CODE>null</CODE> is returned.
     */
    public static String getKnownJapaneseFont() {
        if (knownJapaneseFont != null)
            return knownJapaneseFont;

        String[] fonts = JGloss.prefs.getList( Preferences.FONT_DEFAULTFONTS, ',');
        for ( int i=0; i<fonts.length; i++) {
            if (canDisplayJapanese( fonts[i])) {
                knownJapaneseFont = fonts[i];
                return knownJapaneseFont;
            }
        }

        // no japanese font available
        return null;
    }

    /**
     * Searches the list of all available fonts for one which can display Japanese characters.
     * Returns the first font found. The method shows its progress in a dialog, and is interruptible
     * by the user. If no suitable font is found or n case of an interruption, 
     * <code>null</code> is returned.
     */
    private String searchJapaneseFont() {
        final Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAllFonts();
        final ProgressMonitor monitor = new ProgressMonitor
            ( this, JGloss.messages.getString( "style.autodetect.progress.description"),
              "______________________________________________", 1, allFonts.length);
        final int[] i = new int[1]; // mutable final array, accessible by nested class
        final Font[] currentFont = new Font[1]; // mutable final array, accessible by nested class
        // use timer to update progress bar
        javax.swing.Timer timer = new javax.swing.Timer( 1000, new ActionListener() {
                public void actionPerformed( ActionEvent e) {
                    monitor.setProgress( i[0]);
                    monitor.setNote( JGloss.messages.getString
                                     ( "style.autodetect.progress.font", 
                                       new Object[] { currentFont[0].getName() }));
                }
            });
        timer.setRepeats( true);
        timer.start();

        String font = null;
        for ( i[0]=0; i[0]<allFonts.length; i[0]++) {
            if (monitor.isCanceled())
                break;
            currentFont[0] = allFonts[i[0]];
            int length = currentFont[0].canDisplayUpTo( JAPANESE_CHARS_TEST);

            length = 0;

            if (length == -1 || // all chars succeeded (according to canDisplayUpTo specification)
                length == JAPANESE_CHARS_TEST.length()) { // all chars succeeded (behavior in Java 1.3)
                font = currentFont[0].getName();
                break;
            }
        }

        timer.stop();
        monitor.close();

        return font;
    }

    /**
     * Check if the font with the given name can display Japanese characters.
     */
    protected static boolean canDisplayJapanese( String fontName) {
        Font font = new Font( fontName, Font.PLAIN, 1);
        int length = font.canDisplayUpTo( JAPANESE_CHARS_TEST);
        return (length == -1 || // all chars succeeded (according to canDisplayUpTo specification)
                length == JAPANESE_CHARS_TEST.length()); // all chars succeeded (behavior in Java 1.3)
    }

    /**
     * Create a font with the font name taken from the general font settings and the style and size
     * taken from the original font. The font size is adjusted by the amount of FONT_GENERAL_SIZEDIFF
     * to increase readability of japanese characters.
     */
    public static Font deriveGeneralFont( Font original) {
        return new Font( JGloss.prefs.getString( Preferences.FONT_GENERAL),
                         original.getStyle(), original.getSize() + 
                         JGloss.prefs.getInt( Preferences.FONT_GENERAL_SIZEDIFF, 1));
    }
} // class StyleDialog
