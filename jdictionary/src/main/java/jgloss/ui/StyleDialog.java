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

import static jgloss.ui.util.FontUtilities.canDisplayJapanese;
import static jgloss.ui.util.SwingWorkerProgressFeedback.showProgress;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.util.UIUtilities;

/**
 * Component which allows the user to edit visual-related preferences. This will normally embedded
 * in the application preferences dialog. There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getStyleDialog() getStyleDialog()} method.
 *
 * @author Michael Koch
 */
public class StyleDialog extends Box implements PreferencesPanel {
	private static final Logger LOGGER = Logger.getLogger(StyleDialog.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;

	/**
     * The single application-wide instance.
     */
    private static StyleDialog box;

    /**
     * Returns the single application-wide instance.
     *
     * @return The StyleDialog component.
     */
    public static StyleDialog getStyleDialog() {
        if (box == null) {
	        box = new StyleDialog();
        }
        return box;
    }

    /**
     * Default japanese font used if the generic fonts like SansSerif, Dialog etc. don't contain Japanese
     * characters.
     */
    protected static String knownJapaneseFont;

    /**
     * Map from Swing L&F property keys to the default L&F fonts.
     */
    protected static Map<Object, Font> defaultLFFonts;

    @Override
	public String getTitle() { return JGloss.MESSAGES.getString( "style.title"); }
    @Override
	public Component getComponent() { return this; }
    
    protected JButton autodetect;

    protected JRadioButton generalFontDefault;
    protected JRadioButton generalFontCustom;

    protected JComboBox generalFont;
    protected JComboBox wordLookupFont;

    protected JComboBox wordLookupFontSize;
    
    /**
     * An Icon which paints itself as a single color.
     *
     * @author Michael Koch
     */
    public static class ColorIcon implements Icon {
        /**
         * Width of the icon.
         */
        protected int width;
        /**
         * Height of the icon.
         */
        protected int height;
        /**
         * Color of the icon.
         */
        protected Color color;
        
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
        @Override
		public int getIconWidth() { return width; }
        /**
         * Returns the height of this icon.
         *
         * @return The height of this icon.
         */
        @Override
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

        @Override
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

        // NOTE: getAvailableFontFamilyNames and getAllFonts do not list all available fonts
        // under JDK 1.4 and Linux (I have no idea why). Therefore, not all fonts may be available
        // from the font popups, and insertAndSelect is used whenever a value in a font
        // JComboBox is selected.
        String[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();

        autodetect = new JButton( JGloss.MESSAGES.getString( "style.autodetect"));
        autodetect.addActionListener( new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    autodetectFontsAction( getAutodetectedFonts());
                }
            });

        generalFont = new JComboBox( allFonts);
        generalFont.setEditable( false);
        generalFontDefault = new JRadioButton( JGloss.MESSAGES.getString( "style.general.default"));
        generalFontCustom = new JRadioButton( JGloss.MESSAGES.getString( "style.general.custom"));
        ButtonGroup bg = new ButtonGroup();
        bg.add( generalFontDefault);
        bg.add( generalFontCustom);
        generalFontCustom.addChangeListener( new ChangeListener() {
                @Override
				public void stateChanged( ChangeEvent e) {
                    generalFont.setEnabled( generalFontCustom.isSelected());
                }
            });

        wordLookupFont = new JComboBox( allFonts);
        wordLookupFont.setEditable( false);

        wordLookupFontSize = new JComboBox( JGloss.PREFS.getList( Preferences.FONTSIZES_WORDLOOKUP, ','));
        wordLookupFontSize.setEditable( true);

        Box b;
        Box b2;

        // general font
        JPanel p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
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
        p.setBorder( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString
                                                       ( "style.wordlookup")));
        b = Box.createVerticalBox();
        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.font")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( wordLookupFont);
        b2.add( Box.createHorizontalStrut( 5));
        b2.add( new JLabel( JGloss.MESSAGES.getString( "style.text.size")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( wordLookupFontSize);
        b.add( UIUtilities.createFlexiblePanel( b2, true));
        b.add( Box.createVerticalStrut( 7));
        p.add( b);
        this.add( p);
        this.add( Box.createVerticalStrut( 3));

        insertAdditionalControls( allFonts);

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
     * Let subclasses add additional user interface controls.
     */
    protected void insertAdditionalControls( String[] allFonts) {}

    /**
     * Loads the preferences and initializes the dialog accordingly.
     */
    @Override
	public void loadPreferences() {
        if (JGloss.PREFS.getBoolean( Preferences.FONT_GENERAL_USEDEFAULT, true)) {
	        generalFontDefault.setSelected( true);
        } else {
	        generalFontCustom.setSelected( true);
        }
        generalFont.setEnabled( !JGloss.PREFS.getBoolean( Preferences.FONT_GENERAL_USEDEFAULT, true));

        insertAndSelect( generalFont, JGloss.PREFS.getString( Preferences.FONT_GENERAL));
        insertAndSelect( wordLookupFont, JGloss.PREFS.getString( Preferences.FONT_WORDLOOKUP));
        
        wordLookupFontSize.setSelectedItem
            ( Integer.toString( JGloss.PREFS.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12)));
    }

    /**
     * Saves the current dialog settings.
     */
    @Override
	public void savePreferences() {
        String font = (String) generalFont.getSelectedItem();
        if (font != null) {
	        JGloss.PREFS.set( Preferences.FONT_GENERAL, font);
        }
        JGloss.PREFS.set( Preferences.FONT_GENERAL_USEDEFAULT, generalFontDefault.isSelected());

        font = (String) wordLookupFont.getSelectedItem();
        if (font != null) {
	        JGloss.PREFS.set( Preferences.FONT_WORDLOOKUP, font);
        }

        String size = (String) wordLookupFontSize.getSelectedItem();
        try {
            int s = Integer.parseInt( size);
            JGloss.PREFS.set( Preferences.FONT_WORDLOOKUP_SIZE, s);
        } catch (Exception ex) { 
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            wordLookupFontSize.setSelectedItem( JGloss.PREFS.getString( Preferences.FONT_WORDLOOKUP_SIZE));
        }
    }

    /**
     * Applies the settings from the application preferences to all style sheets.
     */
    @Override
	public void applyPreferences() {
        // apply custom font settings
        applyUIFont();
    }

    /**
     * Applies the UI font setting.
     */
    public static synchronized void applyUIFont() {
        if (JGloss.PREFS.getBoolean( Preferences.FONT_GENERAL_USEDEFAULT, true)) {
            // restore java default fonts
            if (defaultLFFonts != null) {
                for (Map.Entry<Object, Font> entry : defaultLFFonts.entrySet()) {
                    UIManager.put( entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            // save java default fonts
            if (defaultLFFonts == null) {
                defaultLFFonts = new TreeMap<Object,Font>();
                for (Map.Entry<Object, Object> defaults : UIManager.getDefaults().entrySet()) {
                    Object value = defaults.getValue();
                    if (value instanceof Font) {
                        defaultLFFonts.put( defaults.getKey(), (Font) value);
                    }
                }
            }
            // set custom font
            for (Map.Entry<Object, Font> defaultFont : defaultLFFonts.entrySet()) {
                UIManager.put( defaultFont.getKey(), deriveGeneralFont( defaultFont.getValue()));
            }
        }
    }

    protected JComboBox[] getAutodetectedFonts() {
        return new JComboBox[] { wordLookupFont };
    }

    /**
     * Triggered if the Auto-Configure Fonts button is pressed. This method goes farther than
     * {@link #autodetectFonts() autodetectFonts} in that it gives the user feedback on what
     * was changed and it tests all available fonts (which can be slow).
     */
    protected void autodetectFontsAction( JComboBox[] fonts) {
        // Test if all selected fonts can display japanese characters, if yes, no further action
        // is neccessary.
        boolean canDisplayJapanese = 
            (canDisplayJapanese( "Dialog") && canDisplayJapanese( "DialogInput") &&
             generalFontDefault.isSelected() ||
             canDisplayJapanese( (String) generalFont.getSelectedItem()) &&
             generalFontCustom.isSelected());
        
        if (canDisplayJapanese) {
            for (JComboBox font2 : fonts) {
                canDisplayJapanese = canDisplayJapanese( (String) font2.getSelectedItem());
                if (!canDisplayJapanese) {
	                break;
                }
            }
        }
        
        if (canDisplayJapanese) {
            // no configuration neccessary
            JOptionPane.showMessageDialog( this, 
                                           JGloss.MESSAGES.getString( "style.autodetect.nochange"),
                                           JGloss.MESSAGES.getString( "style.autodetect.title"),
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
        if (canDisplayJapanese( (String) generalFont.getSelectedItem())) {
	        japaneseFont = (String) generalFont.getSelectedItem();
        } else {
            for (JComboBox font2 : fonts) {
                if (canDisplayJapanese( (String) font2.getSelectedItem())) {
                    japaneseFont = (String) wordLookupFont.getSelectedItem();
                    break;
                }
            }
        }

        if (japaneseFont != null) {
            selectJapaneseFont( japaneseFont);
            return;
        }

        // Try out all fonts. Since this is slow, do this in its own thread.
        FontTester fontTester = new FontTester(this);
        showProgress(fontTester, this);
        fontTester.execute();
    }

    /**
     * Replaces the font selection of all fonts which can't display Japanese with the specified
     * font. Only the current dialog settings are modified, not the user preferences.
     * Also displays a dialog with the font name.
     */
    protected void selectJapaneseFont(String fontname) {
        if (canDisplayJapanese( "Dialog") && canDisplayJapanese( "DialogInput")) {
            generalFontDefault.setSelected( true);
        }
        else {
            generalFontCustom.setSelected( true);
            insertAndSelect( generalFont, fontname);
        }
        insertAndSelect( wordLookupFont, fontname);

        JOptionPane.showMessageDialog( this, 
                                       JGloss.MESSAGES.getString( "style.autodetect.selectedfont",
                                                                  fontname),
                                       JGloss.MESSAGES.getString( "style.autodetect.title"),
                                       JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Select an item from an immutable combo box, adding the item to the combo box model if
     * neccessary. This is used because with JDK 1.4 under Linux, <code>getAvailableFontFamilyNames</code>
     * and <code>getAllFonts</code> don't list all fonts, and a font read from the preferences file
     * or selected by auto-detection which is to be selected, might not be in the model of the font
     * combo box.
     */
    protected void insertAndSelect( JComboBox box, Object object) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) box.getModel();
        if (model.getIndexOf( object) == -1) {
	        model.insertElementAt( object, 0);
        }
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
        if (defaultFont == null) {
	        // no font available, leave settings as is
            return;
        }

        // test general font
        if (!canDisplayJapanese( "Dialog") || !canDisplayJapanese( "DialogInput")) {
            JGloss.PREFS.set( Preferences.FONT_GENERAL_USEDEFAULT, false);
        }
        if (!canDisplayJapanese( JGloss.PREFS.getString( Preferences.FONT_GENERAL))) {
	        JGloss.PREFS.set( Preferences.FONT_GENERAL, defaultFont);
        }
        if (!canDisplayJapanese( JGloss.PREFS.getString( Preferences.FONT_WORDLOOKUP))) {
	        JGloss.PREFS.set( Preferences.FONT_WORDLOOKUP, defaultFont);
        }
        if (!canDisplayJapanese( JGloss.PREFS.getString( Preferences.FONT_TEXT))) {
	        JGloss.PREFS.set( Preferences.FONT_TEXT, defaultFont);
        }
        if (!canDisplayJapanese( JGloss.PREFS.getString( Preferences.FONT_READING))) {
	        JGloss.PREFS.set( Preferences.FONT_READING, defaultFont);
        }
        if (!canDisplayJapanese( JGloss.PREFS.getString( Preferences.FONT_TRANSLATION))) {
	        JGloss.PREFS.set( Preferences.FONT_TRANSLATION, defaultFont);
        }
    }

    /**
     * Returns the name of a font which can display Japanese characters. A list of fonts
     * with known names is tested for availability, and the first available font is returned. If
     * none of the fonts is available, <CODE>null</CODE> is returned.
     */
    public static String getKnownJapaneseFont() {
        if (knownJapaneseFont != null) {
	        return knownJapaneseFont;
        }

        String[] fonts = JGloss.PREFS.getList( Preferences.FONT_DEFAULTFONTS, ',');
        for (String font2 : fonts) {
            if (canDisplayJapanese( font2)) {
                knownJapaneseFont = font2;
                return knownJapaneseFont;
            }
        }

        // no japanese font available
        return null;
    }


    /**
     * Create a font with the font name taken from the general font settings and the style and size
     * taken from the original font. The font size is adjusted by the amount of FONT_GENERAL_SIZEDIFF
     * to increase readability of japanese characters.
     */
    public static Font deriveGeneralFont( Font original) {
        return new Font( JGloss.PREFS.getString( Preferences.FONT_GENERAL),
                         original.getStyle(), original.getSize() + 
                         JGloss.PREFS.getInt( Preferences.FONT_GENERAL_SIZEDIFF, 1));
    }
} // class StyleDialog
