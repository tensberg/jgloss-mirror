package jgloss.ui;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.doc.AnnotationTags;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.StyleSheet;

public class DocumentStyleDialog extends StyleDialog {
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
        if (box == null)
            box = new DocumentStyleDialog();
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
    protected Map currentStyles;

    /**
     * The list of managed style sheets.
     */
    protected java.util.List styleSheets;


    protected DocumentStyleDialog() {
        super();
    }

    protected void insertAdditionalControls( String[] allFonts) {
        currentStyles = new HashMap( 10);
        styleSheets = new ArrayList( 10);

        textFont = new JComboBox( allFonts);
        textFont.setEditable( false);
        readingFont = new JComboBox( allFonts);
        readingFont.setEditable( false);
        translationFont = new JComboBox( allFonts);
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
        
        Box b;
        Box b2;
        JPanel p;

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
        b.add( new JLabel( JGloss.messages.getString( "style.highlight.color")));
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
                currentStyles.remove( s);
            }
        }
    }

    public void loadPreferences() {
        super.loadPreferences();
        
        insertAndSelect( textFont, JGloss.prefs.getString( Preferences.FONT_TEXT));
        insertAndSelect( readingFont, JGloss.prefs.getString( Preferences.FONT_READING));
        insertAndSelect( translationFont, JGloss.prefs.getString( Preferences.FONT_TRANSLATION));

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

    public void savePreferences() {
        super.savePreferences();

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

    public void applyPreferences() {
        super.applyPreferences();

        // apply document view styles
        synchronized (styleSheets) {
            for ( Iterator i=styleSheets.iterator(); i.hasNext(); )
                applyPreferences( (StyleSheet) i.next(), (Map) i.next());
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
     * Applies the settings from the application preferences to a single style sheet.
     *
     * @param s The style sheet.
     * @param additionalStyles Style sheet-specific additional CSS styles.
     */
    protected void applyPreferences( StyleSheet s, Map additionalStyles) {
        if (s == null)
            return;

        String style = "body { background-color: " + BACKGROUND_COLOR + "; ";
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
            style += "background-color: " + BACKGROUND_COLOR + "; ";
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
            style += "background-color: " + BACKGROUND_COLOR + "; ";
        }

        if (additionalStyles.containsKey( AnnotationTags.TRANSLATION.getId()))
            style += additionalStyles.get( AnnotationTags.TRANSLATION.getId()).toString();
        style += "}\n";

        if (!style.equals( currentStyles.get( s))) { // only apply style if something changed
            s.addRule( style);
            currentStyles.put( s, style);
        }

        JGlossEditor.setHighlightColor
            ( new Color( Math.max( 0, JGloss.prefs.getInt
                                   ( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xcccccc))));
    }

    protected void selectJapaneseFont( String fontname) {
        insertAndSelect( textFont, fontname);
        insertAndSelect( readingFont, fontname);
        insertAndSelect( translationFont, fontname);
        super.selectJapaneseFont( fontname);
    }

    protected JComboBox[] getAutodetectedFonts() {
        return new JComboBox[] { wordLookupFont, textFont, readingFont, translationFont };
    }
} // class DocumentStyleDialog