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

    private JComboBox textFont;
    private JComboBox readingFont;
    private JComboBox translationFont;

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

        styleSheets = new ArrayList( 10);

        String [] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();

        textFont = new JComboBox( fontNames);
        textFont.setEditable( false);
        readingFont = new JComboBox( fontNames);
        readingFont.setEditable( false);
        translationFont = new JComboBox( fontNames);
        translationFont.setEditable( false);

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

        // text
        JPanel p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.messages.getString
                                                       ( "style.text")));
        Box b = Box.createVerticalBox();
        Box b2 = Box.createHorizontalBox();
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
        b2.add( Box.createHorizontalStrut( 5));
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
        b2.add( Box.createHorizontalStrut( 5));
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
        b2.add( Box.createHorizontalStrut( 5));
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
        textFont.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TEXT));
        readingFont.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_READING));
        translationFont.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TRANSLATION));
        
        textFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TEXT_SIZE));
        readingFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_READING_SIZE));
        translationFontSize.setSelectedItem( JGloss.prefs.getString( Preferences.FONT_TRANSLATION_SIZE));
        
        textUseColor.setSelected( JGloss.prefs.getBoolean( Preferences.FONT_TEXT_USECOLOR));
        readingUseColor.setSelected( JGloss.prefs.getBoolean( Preferences.FONT_READING_USECOLOR));
        translationUseColor.setSelected( JGloss.prefs.getBoolean( Preferences.FONT_TRANSLATION_USECOLOR));

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
        String font = (String) textFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_TEXT, font);
        font = (String) readingFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_READING, font);
        font = (String) translationFont.getSelectedItem();
        if (font != null)
            JGloss.prefs.set( Preferences.FONT_TRANSLATION, font);

        String size = (String) textFontSize.getSelectedItem();
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
        synchronized (styleSheets) {
            for ( Iterator i=styleSheets.iterator(); i.hasNext(); )
                applyPreferences( (StyleSheet) i.next(), (Map) i.next());
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
        
        style += AnnotationTags.KANJI.getId() + " { ";
        if (JGloss.prefs.getBoolean( Preferences.FONT_TEXT_USECOLOR)) {
            style += "background-color: #" + Integer.toHexString
                ( JGloss.prefs.getInt( Preferences.FONT_TEXT_BGCOLOR, 0xffffff)) + "; ";
        }
        else {
            // this removes, among other settings, the current background color settings
            s.removeStyle( AnnotationTags.KANJI.getId());
        }
        if (additionalStyles.containsKey( AnnotationTags.KANJI.getId()))
            style += additionalStyles.get( AnnotationTags.KANJI.getId()).toString();
        style += "}\n";

        style += AnnotationTags.READING.getId() + " { ";
        if (JGloss.prefs.getString( Preferences.FONT_READING).length()!=0) {
            style += "font-family: " + JGloss.prefs.getString( Preferences.FONT_READING) + "; ";
        }
        try {
            int size = Integer.parseInt( JGloss.prefs.getString( Preferences.FONT_READING_SIZE));
            style += "font-size: " + size + "pt; ";
        } catch (NumberFormatException ex) { ex.printStackTrace(); }
        if (JGloss.prefs.getBoolean( Preferences.FONT_READING_USECOLOR)) {
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
        if (JGloss.prefs.getBoolean( Preferences.FONT_TRANSLATION_USECOLOR)) {
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

        s.addRule( style);

        JGlossEditor.setHighlightColor( new Color
            ( Math.max( 0, JGloss.prefs.getInt( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xcccccc))));
    }
} // class StyleDialog
