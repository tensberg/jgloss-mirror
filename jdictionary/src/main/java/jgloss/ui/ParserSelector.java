/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.ui.util.UIUtilities;

/**
 * Panel which provides an interface to select a specific parser. Available parsers are registered
 * globally via {@link #registerParser(Class,String) registerParser}. The user can select one
 * of the registered parsers, and a new instance of the selected parser can be created with
 * {@link #createParser(jgloss.dictionary.Dictionary[],Set) createParser}.
 *
 * @author Michael Koch
 */
public class ParserSelector extends JPanel {
	
	private static class ParserSelection {
		private final Class<? extends Parser> parserClass;
		private final String displayName;

		ParserSelection(Class<? extends Parser> parserClass, String displayName) {
			this.parserClass = parserClass;
			this.displayName = displayName;
		}
		
		public Class<? extends Parser> getParserClass() {
	        return parserClass;
        }
		
		public String getDisplayName() {
	        return displayName;
        }
	}
	
	private static final Logger LOGGER = Logger.getLogger(ParserSelector.class.getPackage().getName());

	private static final long serialVersionUID = 1L;
	
    /**
     * List of registered parsers and their display names.
     */
    private static java.util.List<ParserSelection> parsers = new ArrayList<ParserSelection>( 10);

    /**
     * Registers a parser class with the parser selector. Instances of
     * <CODE>ParserSelector</CODE> created before a new parser is registered will not be updated.
     *
     * @param parserClass Class implementing the {@link jgloss.parser.Parser Parser} interface.
     * @param displayName Name of the parser shown to the user.
     * @exception ClassCastException if the parser class does not implement the 
     *            {@link jgloss.parser.Parser Parser} interface.
     * @exception NoSuchMethodException if the parser class has no constructor of the form 
     *            <CODE>(Dictionary[] dictionaries,Set exclusions)</CODE>.
     */
    public static void registerParser(Class<? extends Parser> parserClass, String displayName) {
        parsers.add(new ParserSelection(parserClass, displayName));
    }

    private final static String PARSER_CLASS_PROPERTY = "parser class";

    JRadioButton[] parserButtons;
    private final JCheckBox firstOccurrenceOnly;
    private final JCheckBox detectParagraphs;
    /**
     * Widget to select the reading annotation delimiters.
     */
    private JComboBox readingBrackets;

    /**
     * Creates a new parser selector which shows the currently registered parsers.
     *
     * @param showReadingAnnotationSelector If this is <CODE>true</CODE>, a widget will be shown
     *        which lets the user choose the brackets which delimit reading annotations.
     */
    public ParserSelector( boolean showReadingAnnotationSelector) {
        this( showReadingAnnotationSelector, '\0', '\0');
    }

    /**
     * Creates a new parser selector which shows the currently registered parsers.
     *
     * @param showReadingAnnotationSelector If this is <CODE>true</CODE>, a widget will be shown
     *        which lets the user choose the brackets which delimit reading annotations.
     * @param readingStart Start character of a reading annotation.
     * @param readingEnd End character of a reading annotation.
     */
    public ParserSelector( boolean showReadingAnnotationSelector, char readingStart, char readingEnd) {
        setLayout( new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        Box b = Box.createVerticalBox();
        parserButtons = new JRadioButton[parsers.size()];
        ButtonGroup bg = new ButtonGroup();
        int i = 0;
        for (ParserSelection parser : parsers) {
            parserButtons[i] = new JRadioButton(parser.getDisplayName());
            parserButtons[i].putClientProperty( PARSER_CLASS_PROPERTY, parser.getParserClass());
            bg.add( parserButtons[i]);
            b.add( parserButtons[i]);
            i++;
        }
        b.add( Box.createVerticalStrut( 5));
        firstOccurrenceOnly = new JCheckBox();
        UIUtilities.initButton( firstOccurrenceOnly, "parserselector.firstoccurrence");
        b.add( firstOccurrenceOnly);
        detectParagraphs = new JCheckBox();
        UIUtilities.initButton( detectParagraphs, "parserselector.detectparagraphs");
        b.add( detectParagraphs);
        this.add( b, c);
        parserButtons[0].setSelected( true);

        if (showReadingAnnotationSelector) {
            Vector<String> v = new Vector<String>();
            v.add( JGloss.MESSAGES.getString( "parserselector.noreadings"));
            String s = JGloss.PREFS.getString( Preferences.READING_BRACKET_CHARS);
            for ( i=0; i<s.length()-1; i+=2) {
	            v.add( s.substring( i, i+2));
            }
            readingBrackets = new JComboBox( v);
            readingBrackets.setEditable( true);
            if (readingStart!='\0' && readingEnd!='\0') {
	            setReadingBrackets( readingStart, readingEnd);
            }
            UIManager.getDefaults().addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                    @Override
					public void propertyChange( java.beans.PropertyChangeEvent e) { 
                        if (e.getPropertyName().equals( "ComboBox.font")) {
                            readingBrackets.setFont( (Font) e.getNewValue());
                        }
                    }
                });

            b = Box.createHorizontalBox();
            b.add( new JLabel( JGloss.MESSAGES.getString( "parserselector.readingbrackets")));
            b.add( Box.createHorizontalStrut( 3));
            b.add( readingBrackets);
            JPanel p = UIUtilities.createFlexiblePanel( b, true);
            p.setBorder( BorderFactory.createEmptyBorder( 5, 2, 0, 2));
            this.add( p, c);
        }

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2;
        c.gridy = GridBagConstraints.REMAINDER;
        this.add( new JPanel(), c);
    }

    /**
     * Creates a new instance of the parser class.
     */
    public static Parser createParser( Class<? extends Parser> parserClass, jgloss.dictionary.Dictionary[] dictionaries,
                                       Set<String> exclusions, boolean firstOccurrenceOnly) {
        // Try different combination of constructor parameters. The array contains several
        // parameter combinations with the parameters in "parameters[][0]" and the
        // corresponding type classes in "parameters[][1]".
        Object[][] parameters = 
            new Object[][] { { dictionaries, exclusions }, { exclusions }, {} };
        Class<?>[][] paramClasses =
            new Class[][] { { jgloss.dictionary.Dictionary[].class, Set.class},
                            { Set.class }, {} };

        // loop over the sets of parameters until a matching constructor is found.
        for ( int i=0; i<parameters.length; i++) {
	        try {
	            Constructor<? extends Parser> c = parserClass.getConstructor( paramClasses[i]);
	            Parser p = c.newInstance( parameters[i]);
	            p.setAnnotateFirstOccurrenceOnly( firstOccurrenceOnly);
	            return p;
	        } catch (NoSuchMethodException ex) {
	            // try to find a constructor with a different set of parameters
	        } catch (Exception ex) {
	            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
	        }
        }

        throw new IllegalArgumentException( "no suitable constructor in parser class " +
                                            parserClass.getName());
    }

    /**
     * Creates a reading annotation filter with the given reading brackets. If the reading
     * brackets are not valid, <CODE>null</CODE> is returned.
     */
    public static ReadingAnnotationFilter createReadingAnnotationFilter( char readingStart, 
                                                                         char readingEnd) {
        if (readingStart!='\0' && readingEnd!='\0') {
	        // FIXME: kanji separator '\uff5c' should be user-configurable
            return new ReadingAnnotationFilter( readingStart, readingEnd, '\uff5c');
        } else {
	        return null;
        }
    }

    /**
     * Returns the class of the currently selected parser.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Parser> getSelectedParser() {
        for (JRadioButton parserButton : parserButtons) {
	        if (parserButton.isSelected()) {
	            return (Class<? extends Parser>) parserButton.getClientProperty( PARSER_CLASS_PROPERTY);
            }
        }
        return null;
    }

    /**
     * Creates a new instance of the currently selected parser.
     */
    public Parser createParser( jgloss.dictionary.Dictionary[] dictionaries, Set<String> exclusions) {
        return createParser( getSelectedParser(), dictionaries, exclusions, 
                             firstOccurrenceOnly.isSelected());
    }

    /**
     * Creates a new reading annotation filter with the currently selected reading brackets.
     * If no reading brackets are selected, <CODE>null</CODE> is returned.
     */
    public ReadingAnnotationFilter createReadingAnnotationFilter() {
        return createReadingAnnotationFilter( getReadingStart(), getReadingEnd());
    }

    /**
     * Enables/disables a registered parser in this selector. At least one parser class 
     * must always be enabled. If the currently selected parser is disabled, the first enabled
     * parser in the list is selected.
     */
    public void setEnabled( Class<? extends Parser> parserClass, boolean enabled) {
        for ( int i=0; i<parserButtons.length; i++) {
            if (parserButtons[i].getClientProperty( PARSER_CLASS_PROPERTY).equals( parserClass)) {
                setEnabled( i, enabled);
                break;
            }
        }
    }

    /**
     * Enables/disables a registered parser in this selector. At least one parser class 
     * must always be enabled. If the currently selected parser is disabled, the first enabled
     * parser in the list is selected.
     */
    public void setEnabled( String displayName, boolean enabled) {
        for ( int i=0; i<parserButtons.length; i++) {
            if (parserButtons[i].getText().equals( displayName)) {
                setEnabled( i, enabled);
                break;
            }
        }
    }

    /**
     * Enables/disables a registered parser in this selector. At least one parser class 
     * must always be enabled. If the currently selected parser is disabled, the first enabled
     * parser in the list is selected.
     *
     * @param i Offset of the parser button in the <CODE>parserButtons</CODE> array.
     */
    private void setEnabled( int i, boolean enabled) {
        parserButtons[i].setEnabled( enabled);
        if (!enabled && parserButtons[i].isSelected()) {
            // select different, enabled parser
            for (JRadioButton parserButton : parserButtons) {
                if (parserButton.isEnabled()) {
                    parserButton.setSelected( true);
                    break;
                }
            }
        }
    }

    /**
     * Makes the parser the currently selected parser. If the parser is disabled, the selection
     * will not be changed.
     */
    public void setSelected( Class<? extends Parser> parserClass) {
        for (JRadioButton parserButton : parserButtons) {
            if (parserButton.getClientProperty( PARSER_CLASS_PROPERTY).equals( parserClass)) {
                if (parserButton.isEnabled()) {
	                parserButton.setSelected( true);
                }
                break;
            }
        }
    }

    /**
     * Makes the parser the currently selected parser. If the parser is disabled, the selection
     * will not be changed.
     */
    public void setSelected( String displayName) {
        for (JRadioButton parserButton : parserButtons) {
            if (parserButton.getText().equals( displayName)) {
                if (parserButton.isEnabled()) {
	                parserButton.setSelected( true);
                }
                break;
            }
        }
    }

    public char getReadingStart() {
        String s = (String) readingBrackets.getSelectedItem();
        if (s==null || s.equals( readingBrackets.getItemAt( 0)) || s.length()<2) {
	        return '\0';
        }
        return s.charAt( 0);
    }

    public char getReadingEnd() {
        String s = (String) readingBrackets.getSelectedItem();
        if (s==null || s.equals( readingBrackets.getItemAt( 0)) || s.length()<2) {
	        return '\0';
        }
        return s.charAt( 1);
    }

    public void setReadingBrackets( char readingStart, char readingEnd) {
        readingBrackets.setSelectedItem( new String( new char[] { readingStart, readingEnd }));
    }

    /**
     * Disable the use of a reading bracket filter by setting the selected reading brackets to
     * "none".
     */
    public void setNoReadingBrackets() {
        readingBrackets.setSelectedIndex( 0);
    }

    /**
     * Tests if no reading brackets are selected.
     */
    public boolean isNoReadingBrackets() {
        return (getReadingStart()=='\0' || getReadingEnd()=='\0');
    }

    public void setFirstOccurrenceOnly( boolean firstOccurrenceOnly) {
        this.firstOccurrenceOnly.setSelected( firstOccurrenceOnly);
    }

    public boolean isFirstOccurrenceOnly() {
        return firstOccurrenceOnly.isSelected();
    }

    public void setDetectParagraphs( boolean detect) {
        this.detectParagraphs.setSelected( detect);
    }

    /**
     * Determines if the user selected the detect paragraphs checkbox. If <code>true</code>, the
     * detect paragraphs option of the {@link jgloss.ui.xml.JGlossifyReader JGlossifyReader}
     * should be used when importing a text
     * document. While this option has no relation to the text parser backend, from a UI perspective
     * it makes sense to place the control here because the user does not make a difference between
     * text import and parsing.
     */
    public boolean isDetectParagraphs() {
        return detectParagraphs.isSelected();
    }
} // class ParserSelector
