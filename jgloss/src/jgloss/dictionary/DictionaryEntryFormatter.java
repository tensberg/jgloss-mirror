/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

package jgloss.dictionary;

import jgloss.util.ListFormatter;
import jgloss.dictionary.attribute.*;

import java.util.*;

/**
 * Format a dictionary entry as String. The class is designed to offer highly customizable
 * output.
 *
 * @author Michael Koch
 */
public class DictionaryEntryFormatter {
    /**
     * Position of an attribute in the formatted string. The positions are used with 
     * {@link DictionaryEntryFormatter#addAttributeFormat(Attribute,Position,AttributeFormatter)
     *        addAttributeFormat} to select where a particular attribute is inserted.
     * For most of the positions, the
     * position corresponds to the attribute set which is used to find an attribute. For
     * example, {@link #BEFORE_ROM BEFORE_ROM} is always used with attributes from a
     * {@link DictionaryEntry#getTranslationRomAttributes(int) getTranslationRomAttributes}
     * attribute set. The exceptions are {@link #BEFORE_FIELD2 BEFORE_FIELD2} and
     * {@link #BEFORE_FIELD2 BEFORE_FIELD2}, which are used with the general attribute set.
     */
    public static class Position {
        public static final Position BEFORE_WORDS = new Position( "BEFORE_WORDS");
        public static final Position AFTER_WORDS = new Position( "AFTER_WORDS");
        public static final Position BEFORE_WORD = new Position( "BEFORE_WORD");
        public static final Position AFTER_WORD = new Position( "AFTER_WORD");
        public static final Position BEFORE_READINGS = new Position( "BEFORE_READINGS");
        public static final Position AFTER_READINGS = new Position( "AFTER_READINGS");
        public static final Position BEFORE_READING = new Position( "BEFORE_READING");
        public static final Position AFTER_READING = new Position( "AFTER_READING");
        public static final Position BEFORE_TRANSLATIONS = new Position( "BEFORE_TRANSLATIONS");
        public static final Position AFTER_TRANSLATIONS = new Position( "AFTER_TRANSLATIONS");
        public static final Position BEFORE_ROM = new Position( "BEFORE_ROM");
        public static final Position AFTER_ROM = new Position( "AFTER_ROM");
        public static final Position BEFORE_CRM = new Position( "BEFORE_CRM");
        public static final Position AFTER_CRM = new Position( "AFTER_CRM");
        public static final Position BEFORE_TRANSLATION = new Position( "BEFORE_TRANSLATION");
        public static final Position AFTER_TRANSLATION = new Position( "AFTER_TRANSLATION");
        public static final Position BEFORE_ENTRY = new Position( "BEFORE_ENTRY");
        public static final Position AFTER_ENTRY = new Position( "AFTER_ENTRY");

        /**
         * Special position for general attributes. This position can be used
         * to place a general attribute before the second field instead of
         * using {@link #BEFORE_ENTRY BEFORE_ENTRY}
         * and {@link #AFTER_ENTRY AFTER_ENTRY} to place it at the beginning or end of the string.
         */
        public static final Position BEFORE_FIELD2 = new Position( "BEFORE_FIELD2");
        /**
         * Special position for general attributes. This position can be used
         * to place a general attribute before the third field instead of
         * using {@link #BEFORE_ENTRY BEFORE_ENTRY}
         * and {@link #AFTER_ENTRY AFTER_ENTRY} to place it at the beginning or end of the string.
         */
        public static final Position BEFORE_FIELD3 = new Position( "BEFORE_FIELD3");

        private String name;

        private Position( String _name) { name = _name; }
        public String toString() { return name; }
    } // class Position

    protected List formats = new ArrayList( 3);
    protected Map attributeFormats = new HashMap( 51);

    protected StringBuffer tempBuf = new StringBuffer( 128);
    protected StringBuffer tempBuf2 = new StringBuffer( 128);
    protected StringBuffer tempBuf3 = new StringBuffer( 128);
    protected StringBuffer tempBuf4 = new StringBuffer( 128);
    protected StringBuffer tempBuf5 = new StringBuffer( 128);

    public DictionaryEntryFormatter() {}

    public DictionaryEntryFormatter( ListFormatter _wordFormat,
                                     ListFormatter _readingFormat,
                                     ListFormatter _romFormat,
                                     ListFormatter _crmFormat,
                                     ListFormatter _synFormat) {
        addWordFormat( _wordFormat);
        addReadingFormat( _readingFormat);
        addTranslationFormat( _romFormat, _crmFormat, _synFormat);
    }

    public void addWordFormat( ListFormatter format) {
        formats.add( new Object[] { DictionaryEntryField.WORD, format });
    }

    public void addReadingFormat( ListFormatter format) {
        formats.add( new Object[] { DictionaryEntryField.READING, format });
    }

    public void addTranslationFormat( ListFormatter romFormat, ListFormatter crmFormat,
                                      ListFormatter synFormat) {
        formats.add( new Object[] { DictionaryEntryField.TRANSLATION, romFormat,
                                    crmFormat, synFormat });
    }

    public void addAttributeFormat( Attribute att, AttributeFormatter format, boolean before) {
        addAttributeFormat( att, format, null, before);
    }

    public void addAttributeFormat( Attribute att, AttributeFormatter format,
                                    Position generalAttributePosition, boolean before) {
        if (att.appliesTo( DictionaryEntry.AttributeGroup.GENERAL)) {
            if (generalAttributePosition == null)
                generalAttributePosition = before ? Position.BEFORE_ENTRY : Position.AFTER_ENTRY;
            addAttributeFormat( att, format, generalAttributePosition);
        }

        if (att.appliesTo( DictionaryEntry.AttributeGroup.WORD)) {
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_WORDS : Position.AFTER_WORDS);
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_WORD : Position.AFTER_WORD);
        }

        if (att.appliesTo( DictionaryEntry.AttributeGroup.READING)) {
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_READINGS : Position.AFTER_READINGS);
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_READING : Position.AFTER_READING);
        }

        if (att.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION)) {
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_TRANSLATIONS : Position.AFTER_TRANSLATIONS);
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_ROM : Position.AFTER_ROM);
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_CRM : Position.AFTER_CRM);
            addAttributeFormat( att, format,
                                before ? Position.BEFORE_TRANSLATION : Position.AFTER_TRANSLATION);
        }
    }

    public void addAttributeFormat( Attribute att, AttributeFormatter formatter, Position pos) {
        List fl = (List) attributeFormats.get( pos);
        if (fl == null) { // first attribute at this position
            fl = new ArrayList( 3);
            attributeFormats.put( pos, fl);
        }
        fl.add( new Object[] { att, formatter });
    }

    public StringBuffer format( DictionaryEntry de, StringBuffer buf) {
        formatAttributes( de, buf, Position.BEFORE_ENTRY, de.getGeneralAttributes());

        for ( int i=0; i<formats.size(); i++) {
            if (i == 1)
                formatAttributes( de, buf, Position.BEFORE_FIELD2, de.getGeneralAttributes());
            else if (i > 1)
                formatAttributes( de, buf, Position.BEFORE_FIELD3, de.getGeneralAttributes());

            Object[] format = (Object[]) formats.get( i);
            if (format[0] == DictionaryEntryField.WORD)
                formatWords( de, buf, (ListFormatter) format[1]);
            else if (format[0] == DictionaryEntryField.READING)
                formatReadings( de, buf, (ListFormatter) format[1]);
            else // translations
                formatTranslations( de, buf, (ListFormatter) format[1],
                                    (ListFormatter) format[2],
                                    (ListFormatter) format[3]);
        }

        formatAttributes( de, buf, Position.AFTER_ENTRY, de.getGeneralAttributes());
        return buf;
    }

    protected StringBuffer formatWords( DictionaryEntry de, StringBuffer buf,
                                        ListFormatter format) {
        formatAttributes( de, buf, Position.BEFORE_WORDS, de.getWordAttributes());

        format.newList( buf, de.getWordAlternativeCount());
        for ( int i=0; i<de.getWordAlternativeCount(); i++) {
            tempBuf.setLength( 0);
            formatAttributes( de, tempBuf, Position.BEFORE_WORD,
                              de.getWordAttributes( i));
            tempBuf.append( de.getWord( i));
            formatAttributes( de, tempBuf, Position.AFTER_WORD,
                              de.getWordAttributes( i));
            format.addItem( tempBuf);
        }
        format.endList();
        
        formatAttributes( de, buf, Position.AFTER_WORDS, de.getWordAttributes());

        return buf;
    }

    protected StringBuffer formatReadings( DictionaryEntry de, StringBuffer buf,
                                           ListFormatter format) {
        formatAttributes( de, buf, Position.BEFORE_READINGS, de.getReadingAttributes());

        format.newList( buf, de.getReadingAlternativeCount());
        for ( int i=0; i<de.getReadingAlternativeCount(); i++) {
            tempBuf.setLength( 0);
            formatAttributes( de, tempBuf, Position.BEFORE_READING,
                              de.getReadingAttributes( i));
            tempBuf.append( de.getReading( i));
            formatAttributes( de, tempBuf, Position.AFTER_READING,
                              de.getReadingAttributes( i));
            format.addItem( tempBuf);
        }
        format.endList();
        
        formatAttributes( de, buf, Position.AFTER_READINGS, de.getReadingAttributes());

        return buf;
    }

    protected StringBuffer formatTranslations( DictionaryEntry de, StringBuffer buf,
                                               ListFormatter romFormat, ListFormatter crmFormat,
                                               ListFormatter synFormat) {
        formatAttributes( de, buf, Position.BEFORE_TRANSLATIONS, de.getTranslationAttributes());

        romFormat.newList( buf, de.getTranslationRomCount());
        for ( int i=0; i<de.getTranslationRomCount(); i++) {
            tempBuf.setLength( 0);
            formatAttributes( de, tempBuf, Position.BEFORE_ROM,
                              de.getTranslationAttributes( i));
            tempBuf2.setLength( 0);
            crmFormat.newList( tempBuf2, de.getTranslationCrmCount( i));
            for ( int j=0; j<de.getTranslationCrmCount( i); j++) {
                tempBuf3.setLength( 0);
                formatAttributes( de, tempBuf3, Position.BEFORE_CRM,
                                  de.getTranslationAttributes( i, j));
                tempBuf4.setLength( 0);
                synFormat.newList( tempBuf4, de.getTranslationSynonymCount( i, j));
                for ( int k=0; k<de.getTranslationSynonymCount( i, j); k++) {
                    tempBuf5.setLength( 0);
                    formatAttributes( de, tempBuf5, Position.BEFORE_TRANSLATION,
                                      de.getTranslationAttributes( i, j, k));
                    tempBuf5.append( de.getTranslation( i, j, k));
                    formatAttributes( de, tempBuf5, Position.AFTER_TRANSLATION,
                                      de.getTranslationAttributes( i, j, k));
                    synFormat.addItem( tempBuf5);
                }
                tempBuf3.append( synFormat.endList());
                formatAttributes( de, tempBuf3, Position.AFTER_CRM,
                                  de.getTranslationAttributes( i, j));
                crmFormat.addItem( tempBuf3);
            }
            tempBuf.append( crmFormat.endList());
            formatAttributes( de, tempBuf, Position.AFTER_ROM,
                              de.getTranslationAttributes( i));
            romFormat.addItem( tempBuf);
        }
        romFormat.endList();

        formatAttributes( de, buf, Position.AFTER_TRANSLATIONS, de.getTranslationAttributes());

        return buf;
    }

    protected StringBuffer formatAttributes( DictionaryEntry de, StringBuffer buf,
                                             Position pos, AttributeSet atts) {
        if (atts.isEmpty())
            return buf; // nothing to do
        
        List formats = (List) attributeFormats.get( pos);
        if (formats == null)
            return buf; // nothing to do

        for ( int i=0; i<formats.size(); i++) {
            Object[] o = (Object[]) formats.get( i);
            Attribute att = (Attribute) o[0];
            if (atts.containsKey( att, false)) {
                ((AttributeFormatter) o[1]).format( att, atts.getAttribute( att, false), buf);
            }
        }

        return buf;
    }
} // class DictionaryEntryFormatter
