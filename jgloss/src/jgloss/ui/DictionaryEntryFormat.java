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

package jgloss.ui;

import jgloss.JGloss;
import jgloss.dictionary.*;
import jgloss.dictionary.attribute.*;
import jgloss.util.ListFormatter;

import java.util.Map;

// TODO: make formats configurable

/**
 * Format definition for rendering of dictionary entries.
 *
 * @author Michael Koch
 */
class DictionaryEntryFormat {
    public static final String PROTOCOL_REF = "ref";
    public static final String PROTOCOL_SYN = "syn";
    public static final String PROTOCOL_ANT = "ant";
    public static final String PROTOCOL_ALT_READING = "ar";

    private static final ListFormatter word = new ListFormatter( " ", "; ", "");
    private static final ListFormatter reading = new ListFormatter( " [", "; ", "]");
    private static final ListFormatter rom = new ListFormatter( "", "", ".", " [n]", ". [n]", ".");
    private static final ListFormatter crm = new ListFormatter( " ", "; ", "");
    private static final ListFormatter syn = new ListFormatter( "", "/", "");

    public static ListFormatter getWordFormatter() { return new ListFormatter( word); }
    public static ListFormatter getReadingFormatter() { return new ListFormatter( reading); }
    public static ListFormatter getTranslationRomFormatter() { return new ListFormatter( rom); }
    public static ListFormatter getTranslationCrmFormatter() { return new ListFormatter( crm); }
    public static ListFormatter getTranslationSynonymFormatter() { return new ListFormatter( syn); }

    public static AttributeFormatter getAttributeFormatter( Attribute att) {
        return getAttributeFormatter( att, false);
    }

    public static AttributeFormatter getAttributeFormatter( Attribute att, boolean nameOnly) {
        if (nameOnly || att==Attributes.EXAMPLE)
            return new AttributeNameFormatter( " {", "}");

        if (att == Attributes.PART_OF_SPEECH ||
            att == Attributes.USAGE ||
            att == Attributes.CATEGORY)
            return new DefaultAttributeFormatter
                ( " (", ")", "", false, new ListFormatter( ","));

        if (att == Attributes.ABBREVIATION)
            return new WordFormatter( JGloss.messages.getString( "abbr.word"),
                                      "", JGloss.messages.getString( "abbr.lang_and_word"),
                                      new ListFormatter( " (", ",", ")"));

        if (att == Attributes.GAIRAIGO)
            return new WordFormatter( "", JGloss.messages.getString( "gairaigo.lang"),
                                      JGloss.messages.getString( "gairaigo.lang_and_word"),
                                      new ListFormatter( " (", ",", ")"));

        if (att == Attributes.REFERENCE)
            return new ReferenceAttributeFormatter
                ( " \u21d2", "", new ListFormatter( ","));

        if (att == Attributes.SYNONYM)
            return new ReferenceAttributeFormatter
                ( " \u21d2", "", new ListFormatter( ","));

        if (att == Attributes.ANTONYM)
            return new ReferenceAttributeFormatter
                ( " \u21d4", "", new ListFormatter( ","));

        if (att == WadokuJT.ALT_READING)
            return new ReferenceAttributeFormatter
                ( " \u2192", "", new ListFormatter( ","));

        if (att == Attributes.EXPLANATION)
            return new InformationAttributeFormatter
                ( " (", ")", "", false, new ListFormatter( ","));

        return null; // attribute not supported
    }

    public static DictionaryEntryFormatter createFormatter() {
        DictionaryEntryFormatter out = new DictionaryEntryFormatter();

        out.addWordFormat( new ListFormatter( word));
        out.addReadingFormat( new ListFormatter( reading));
        out.addTranslationFormat( new ListFormatter( rom), new ListFormatter( crm),
                                  new ListFormatter( syn));

        addBeforeAttributeFormats( out);

        out.addAttributeFormat( Attributes.EXPLANATION, 
                                getAttributeFormatter( Attributes.EXPLANATION), false);
        out.addAttributeFormat( Attributes.REFERENCE,
                                getAttributeFormatter( Attributes.REFERENCE), false);
        out.addAttributeFormat( Attributes.SYNONYM,
                                getAttributeFormatter( Attributes.SYNONYM), false);
        out.addAttributeFormat( Attributes.ANTONYM,
                                getAttributeFormatter( Attributes.ANTONYM), false);
        out.addAttributeFormat( WadokuJT.ALT_READING,
                                getAttributeFormatter( WadokuJT.ALT_READING), false);

        addAfterAttributeFormats( out);
        
        return out;
    }

    public static DictionaryEntryFormatter createHTMLFormatter( MarkerListFormatter.Group group,
                                                                Map references) {
        DictionaryEntryFormatter out = new DictionaryEntryFormatter();

        out.addWordFormat( new MarkerListFormatter( group, word));
        out.addReadingFormat( new MarkerListFormatter( group, reading));
        out.addTranslationFormat( new ListFormatter( rom), 
                                  new ListFormatter( crm),
                                  new MarkerListFormatter( group, syn));

        addBeforeAttributeFormats( out);

        ListFormatter commaList = new ListFormatter( ",");

        AttributeFormatter informationFormat = new InformationAttributeFormatter
            ( " (", ")", "", false, new MarkerListFormatter( group, commaList));
        out.addAttributeFormat( Attributes.EXPLANATION, informationFormat, false);

        out.addAttributeFormat( Attributes.REFERENCE,
                                new HTMLReferenceAttributeFormatter
                                ( PROTOCOL_REF, " \u21d2", "", commaList,
                                  references), false);
        out.addAttributeFormat( Attributes.SYNONYM,
                                new HTMLReferenceAttributeFormatter
                                ( PROTOCOL_SYN, " \u21d2", "", commaList,
                                  references), false);
        out.addAttributeFormat( Attributes.ANTONYM,
                                new HTMLReferenceAttributeFormatter
                                ( PROTOCOL_ANT, " \u21d4", "", commaList,
                                  references), false);
        out.addAttributeFormat( WadokuJT.ALT_READING,
                                new HTMLReferenceAttributeFormatter
                                ( PROTOCOL_ALT_READING, " \u2192", "", commaList,
                                references), false);

        addAfterAttributeFormats( out);

        return out;
    }

    private static void addBeforeAttributeFormats( DictionaryEntryFormatter out) {
        out.addAttributeFormat( Attributes.PART_OF_SPEECH, 
                                getAttributeFormatter( Attributes.PART_OF_SPEECH),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        out.addAttributeFormat( Attributes.ABBREVIATION,
                                getAttributeFormatter( Attributes.ABBREVIATION, true),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        out.addAttributeFormat( Attributes.ABBREVIATION,
                                getAttributeFormatter( Attributes.ABBREVIATION, true),
                                true);
        out.addAttributeFormat( Attributes.EXAMPLE,
                                getAttributeFormatter( Attributes.EXAMPLE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        out.addAttributeFormat( Attributes.USAGE,
                                getAttributeFormatter( Attributes.USAGE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3, true);
        out.addAttributeFormat( Attributes.CATEGORY,
                                getAttributeFormatter( Attributes.CATEGORY),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3, true);
        out.addAttributeFormat( Attributes.GAIRAIGO, 
                                getAttributeFormatter( Attributes.GAIRAIGO),
                                false);
    }

    private static void addAfterAttributeFormats( DictionaryEntryFormatter out) {
        out.addAttributeFormat( Attributes.ABBREVIATION,
                                getAttributeFormatter( Attributes.ABBREVIATION, false),
                                false);
        out.addAttributeFormat( Attributes.ABBREVIATION,
                                getAttributeFormatter( Attributes.ABBREVIATION, false),
                                DictionaryEntryFormatter.Position.AFTER_ENTRY);
    }
} // class DictionaryEntryFormat
