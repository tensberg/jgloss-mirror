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

import java.lang.reflect.Constructor;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Panel which provides an interface to select a specific parser. Available parsers are registered
 * globally via {@link #registerParser(Class,String) registerParser}. The user can select one
 * of the registered parsers, and a new instance of the selected parser can be created with
 * {@link #createParser(jgloss.dictionary.Dictionary[],Set) createParser}.
 *
 * @author Michael Koch
 */
public class ParserSelector extends JPanel {
    /**
     * List of registered parsers and their display names.
     */
    private static java.util.List parsers = new ArrayList( 10);

    /**
     * Registers a parser class with the parser selector. The parser class must have a constructor
     * of the form <CODE>(Dictionary[] dictionaries,Set exclusions)</CODE>. Instances of
     * <CODE>ParserSelector</CODE> created before a new parser is registered will not be updated.
     *
     * @param parserClass Class implementing the {@link jgloss.dictionary.Parser Parser} interface.
     * @param displayName Name of the parser shown to the user.
     * @exception ClassCastException if the parser class does not implement the 
     *            {@link jgloss.dictionary.Parser Parser} interface.
     * @exception NoSuchMethodException if the parser class has no constructor of the form 
     *            <CODE>(Dictionary[] dictionaries,Set exclusions)</CODE>.
     */
    public static void registerParser( Class parserClass, String displayName) throws NoSuchMethodException {
        if (!Parser.class.isAssignableFrom( parserClass))
            throw new ClassCastException();
        // test if the constructor is available
        parserClass.getConstructor( new Class[] { jgloss.dictionary.Dictionary[].class, Set.class });

        parsers.add( parserClass);
        parsers.add( displayName);
    }

    private final static String PARSER_CLASS_PROPERTY = "parser class";

    JRadioButton[] parserButtons;
    /**
     * Widget to select the reading annotation delimiters.
     */
    private JComboBox readingBrackets;

    /**
     * Creates a new parser selector which shows the currently registered parsers.
     *
     * @param showReadingAnnotationSelector If this is <CODE>true</CODE>, a widget will be shown
     *        which lets the user choose the brackets which delimit reading annotations. The widget
     *        is only active if the currently selected parser is a 
     *        {@link jgloss.Dictionary.ReadingAnnotationParser ReadingAnnotationParser}.
     */
    public ParserSelector( boolean showReadingAnnotationSelector) {
        this( showReadingAnnotationSelector, '\0', '\0');
    }

    /**
     * Creates a new parser selector which shows the currently registered parsers.
     *
     * @param showReadingAnnotationSelector If this is <CODE>true</CODE>, a widget will be shown
     *        which lets the user choose the brackets which delimit reading annotations. The widget
     *        is only active if the currently selected parser is a 
     *        {@link jgloss.Dictionary.ReadingAnnotationParser ReadingAnnotationParser}.
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
        parserButtons = new JRadioButton[parsers.size()/2];
        boolean hasReadingAnnotationParser = false;
        ButtonGroup bg = new ButtonGroup();
        int i = 0;
        for ( Iterator j=parsers.iterator(); j.hasNext(); ) {
            Class parser = (Class) j.next();
            String name = (String) j.next();
            hasReadingAnnotationParser |= ReadingAnnotationParser.class.isAssignableFrom( parser);
            parserButtons[i] = new JRadioButton( name);
            parserButtons[i].putClientProperty( PARSER_CLASS_PROPERTY, parser);
            bg.add( parserButtons[i]);
            b.add( parserButtons[i]);
            i++;
        }
        this.add( b, c);

        if (showReadingAnnotationSelector) {
            Vector v = new Vector();
            String s = JGloss.prefs.getString( Preferences.READING_BRACKET_CHARS);
            for ( i=0; i<s.length()-1; i+=2)
                v.add( s.substring( i, i+2));
            readingBrackets = new JComboBox( v);
            readingBrackets.setEditable( true);
            if (readingStart!='\0' && readingEnd!='\0')
                setReadingBrackets( readingStart, readingEnd);

            b = Box.createHorizontalBox();
            b.add( new JLabel( JGloss.messages.getString( "parserselector.readingbrackets")));
            b.add( Box.createHorizontalStrut( 3));
            b.add( readingBrackets);
            JPanel p = UIUtilities.createSpaceEater( b, true);
            p.setBorder( BorderFactory.createEmptyBorder( 5, 2, 0, 2));
            this.add( p, c);

            ItemListener il = new ItemListener() {
                    public void itemStateChanged( ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            JRadioButton rb = (JRadioButton) e.getItem();
                            readingBrackets.setEnabled( ReadingAnnotationParser.class.isAssignableFrom
                                                        ( (Class) rb.getClientProperty
                                                          ( PARSER_CLASS_PROPERTY)));
                        }
                    }
                };
            for ( i=0; i<parserButtons.length; i++)
                parserButtons[i].addItemListener( il);
        }

        parserButtons[0].setSelected( true); // must be done after registration of the ItemListener

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2;
        c.gridy = GridBagConstraints.REMAINDER;
        this.add( new JPanel(), c);
    }

    /**
     * Creates a new instance of the parser class.
     */
    public static Parser createParser( Class parserClass, jgloss.dictionary.Dictionary[] dictionaries,
                                       Set exclusions, char readingStart, char readingEnd) {
        if (parserClass == null)
            return null;

        try {
            Constructor c = parserClass
                .getConstructor( new Class[] { jgloss.dictionary.Dictionary[].class, Set.class });
            Parser p = (Parser) c.newInstance( new Object[] { dictionaries, exclusions });
            if (p instanceof ReadingAnnotationParser) {
                ReadingAnnotationParser rp = (ReadingAnnotationParser) p;
                rp.setReadingStart( readingStart);
                rp.setReadingEnd( readingEnd);
            }
            return p;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Returns the class of the currently selected parser.
     */
    public Class getSelectedParser() {
        for ( int i=0; i<parserButtons.length; i++)
            if (parserButtons[i].isSelected())
                return (Class) parserButtons[i].getClientProperty( PARSER_CLASS_PROPERTY);
        return null;
    }

    /**
     * Creates a new instance of the currently selected parser. If it is an instance of
     * {@link ReadingAnnotaionParser ReadingAnnotationParser}, the currently selected reading
     * brackets will be set.
     */
    public Parser createParser( jgloss.dictionary.Dictionary[] dictionaries, Set exclusions) {
        return createParser( getSelectedParser(), dictionaries, exclusions, getReadingStart(),
                             getReadingEnd());
    }

    /**
     * Enables/disables a registered parser in this selector. At least one parser class 
     * must always be enabled. If the currently selected parser is disabled, the first enabled
     * parser in the list is selected.
     */
    public void setEnabled( Class parserClass, boolean enabled) {
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
            for ( int j=0; j<parserButtons.length; j++) {
                if (parserButtons[j].isEnabled()) {
                    parserButtons[j].setSelected( true);
                    break;
                }
            }
        }
    }

    /**
     * Makes the parser the currently selected parser. If the parser is disabled, the selection
     * will not be changed.
     */
    public void setSelected( Class parserClass) {
        for ( int i=0; i<parserButtons.length; i++) {
            if (parserButtons[i].getClientProperty( PARSER_CLASS_PROPERTY).equals( parserClass)) {
                if (parserButtons[i].isEnabled())
                    parserButtons[i].setSelected( true);
                break;
            }
        }
    }

    /**
     * Makes the parser the currently selected parser. If the parser is disabled, the selection
     * will not be changed.
     */
    public void setSelected( String displayName) {
        for ( int i=0; i<parserButtons.length; i++) {
            if (parserButtons[i].getText().equals( displayName)) {
                if (parserButtons[i].isEnabled())
                    parserButtons[i].setSelected( true);
                break;
            }
        }
    }

    public char getReadingStart() {
        String s = (String) readingBrackets.getSelectedItem();
        if (!readingBrackets.isEnabled() || s==null || s.length()<2)
            return '\0';
        return s.charAt( 0);
    }

    public char getReadingEnd() {
        String s = (String) readingBrackets.getSelectedItem();
        if (!readingBrackets.isEnabled() || s==null || s.length()<2)
            return '\0';
        return s.charAt( 1);
    }

    public void setReadingBrackets( char readingStart, char readingEnd) {
        readingBrackets.setSelectedItem( new String( new char[] { readingStart, readingEnd }));
    }
} // class ParserSelector
