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

package jgloss.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.AttributeSet;
import jgloss.util.ListFormatter;

/**
 * Format a dictionary entry as String. The class is designed to offer highly customizable
 * output.
 *
 * @author Michael Koch
 */
public class DictionaryEntryFormatter {
    /**
     * Position of an attribute in the formatted string. The positions are used with 
     * {@link DictionaryEntryFormatter#addAttributeFormat(Attribute,AttributeFormatter,
     *        DictionaryEntryFormatter.Position) addAttributeFormat}
     * to select where a particular attribute is inserted.
     * For most of the positions, the
     * position corresponds to the attribute set which is used to find an attribute. For
     * example, {@link #BEFORE_ROM BEFORE_ROM} is always used with attributes from a
     * {@link DictionaryEntry#getTranslationAttributes(int,int,int) getTranslationAttributes}
     * ROM attribute set. The exceptions are {@link #BEFORE_FIELD2 BEFORE_FIELD2} and
     * {@link #BEFORE_FIELD2 BEFORE_FIELD2}, which are used with the general attribute set.
     */
    public enum Position {
        BEFORE_WORDS,
        AFTER_WORDS,
        BEFORE_WORD,
        AFTER_WORD,
        BEFORE_READINGS,
        AFTER_READINGS,
        BEFORE_READING,
        AFTER_READING,
        BEFORE_TRANSLATIONS,
        AFTER_TRANSLATIONS,
        BEFORE_ROM,
        AFTER_ROM,
        BEFORE_CRM,
        AFTER_CRM,
        BEFORE_TRANSLATION,
        AFTER_TRANSLATION,
        BEFORE_ENTRY,
        AFTER_ENTRY,

        /**
         * Special position for general attributes. This position can be used
         * to place a general attribute before the second field instead of
         * using {@link #BEFORE_ENTRY BEFORE_ENTRY}
         * and {@link #AFTER_ENTRY AFTER_ENTRY} to place it at the beginning or end of the string.
         */
        BEFORE_FIELD2,
        /**
         * Special position for general attributes. This position can be used
         * to place a general attribute before the third field instead of
         * using {@link #BEFORE_ENTRY BEFORE_ENTRY}
         * and {@link #AFTER_ENTRY AFTER_ENTRY} to place it at the beginning or end of the string.
         */
        BEFORE_FIELD3;

    }

    protected List<Object[]> formats = new ArrayList<Object[]>( 3);
    protected Map<Position, List<Object[]>> attributeFormats = new HashMap<Position, List<Object[]>>( 51);

    protected StringBuilder tempBuf = new StringBuilder( 128);
    protected StringBuilder tempBuf2 = new StringBuilder( 128);
    protected StringBuilder tempBuf3 = new StringBuilder( 128);
    protected StringBuilder tempBuf4 = new StringBuilder( 128);
    protected StringBuilder tempBuf5 = new StringBuilder( 128);

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

    public void addAttributeFormat( Attribute<?> att, AttributeFormatter format, boolean before) {
        addAttributeFormat( att, format, null, before);
    }

    public void addAttributeFormat( Attribute<?> att, AttributeFormatter format,
                                    Position generalAttributePosition, boolean before) {
        if (att.appliesTo( DictionaryEntry.AttributeGroup.GENERAL)) {
            if (generalAttributePosition == null) {
	            generalAttributePosition = before ? Position.BEFORE_ENTRY : Position.AFTER_ENTRY;
            }
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

    public void addAttributeFormat( Attribute<?> att, AttributeFormatter formatter, Position pos) {
        List<Object[]> fl = attributeFormats.get( pos);
        if (fl == null) { // first attribute at this position
            fl = new ArrayList<Object[]>( 3);
            attributeFormats.put( pos, fl);
        }
        fl.add( new Object[] { att, formatter });
    }

    public StringBuilder format( DictionaryEntry de, StringBuilder buf) {
        formatAttributes( de, buf, Position.BEFORE_ENTRY, de.getGeneralAttributes());

        for ( int i=0; i<formats.size(); i++) {
            if (i == 1) {
	            formatAttributes( de, buf, Position.BEFORE_FIELD2, de.getGeneralAttributes());
            } else if (i > 1) {
	            formatAttributes( de, buf, Position.BEFORE_FIELD3, de.getGeneralAttributes());
            }

            Object[] format = formats.get( i);
            if (format[0] == DictionaryEntryField.WORD) {
	            formatWords( de, buf, (ListFormatter) format[1]);
            } else if (format[0] == DictionaryEntryField.READING) {
	            formatReadings( de, buf, (ListFormatter) format[1]);
            } else {
	            formatTranslations( de, buf, (ListFormatter) format[1],
                                    (ListFormatter) format[2],
                                    (ListFormatter) format[3]);
            }
        }

        formatAttributes( de, buf, Position.AFTER_ENTRY, de.getGeneralAttributes());
        return buf;
    }

    protected StringBuilder formatWords( DictionaryEntry de, StringBuilder buf,
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

    protected StringBuilder formatReadings( DictionaryEntry de, StringBuilder buf,
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

    protected StringBuilder formatTranslations( DictionaryEntry de, StringBuilder buf,
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

    protected StringBuilder formatAttributes( DictionaryEntry de, StringBuilder buf,
                                             Position pos, AttributeSet atts) {
        if (atts.isEmpty())
		 {
	        return buf; // nothing to do
        }
        
        List<Object[]> formats = attributeFormats.get( pos);
        if (formats == null)
		 {
	        return buf; // nothing to do
        }

        for ( int i=0; i<formats.size(); i++) {
            Object[] o = formats.get( i);
            Attribute<?> att = (Attribute<?>) o[0];
            if (atts.containsKey( att, false)) {
                ((AttributeFormatter) o[1]).format( att, atts.getAttribute( att, false), buf);
            }
        }

        return buf;
    }
} // class DictionaryEntryFormatter
