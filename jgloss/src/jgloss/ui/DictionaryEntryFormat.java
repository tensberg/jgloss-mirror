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
    public static final String PROTOCOL_SYN = "syn";

    private static final ListFormatter word = new ListFormatter( " ", "; ", "");
    private static final ListFormatter reading = new ListFormatter( " [", "; ", "]");
    private static final ListFormatter rom = new ListFormatter( "", " ", ".", " (n) ", ". (n) ", ".");
    private static final ListFormatter crm = new ListFormatter( "", "; ", "");
    private static final ListFormatter syn = new ListFormatter( "", "/", "");

    public static ListFormatter getWordFormatter() { return new ListFormatter( word); }
    public static ListFormatter getReadingFormatter() { return new ListFormatter( reading); }
    public static ListFormatter getTranslationRomFormatter() { return new ListFormatter( rom); }
    public static ListFormatter getTranslationCrmFormatter() { return new ListFormatter( crm); }
    public static ListFormatter getTranslationSynonymFormatter() { return new ListFormatter(  syn); }

    public static DictionaryEntryFormatter createFormatter() {
        DictionaryEntryFormatter out = new DictionaryEntryFormatter();

        out.addWordFormat( new ListFormatter( word));
        out.addReadingFormat( new ListFormatter( reading));
        out.addTranslationFormat( new ListFormatter( rom), new ListFormatter( crm),
                                  new ListFormatter( syn));

        addAttributeFormats( out);

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

        addAttributeFormats( out);

        out.addAttributeFormat( Attributes.SYNONYM,
                                new HTMLReferenceAttributeFormatter
                                ( PROTOCOL_SYN, " \u21d2", "", new ListFormatter( ","),
                                  references), false);
        
        return out;
    }

    private static void addAttributeFormats( DictionaryEntryFormatter out) {
        ListFormatter commaList = new ListFormatter( ",");
        ListFormatter commaBracketList = new ListFormatter( " (", ",", ")");
        AttributeFormatter listFormat = new DefaultAttributeFormatter
            ( " (", ")", "", false, commaList);
        AttributeFormatter informationFormat = new InformationAttributeFormatter
            ( " (", ")", "", false, commaList);
        AttributeFormatter attName = new AttributeNameFormatter( " {", "}");

        out.addAttributeFormat( Attributes.PART_OF_SPEECH,
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                listFormat);
        out.addAttributeFormat( Attributes.ABBREVIATION,
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                attName);
        out.addAttributeFormat( Attributes.EXAMPLE,
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                attName);
        out.addAttributeFormat( Attributes.CATEGORY,
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                listFormat);
        out.addAttributeFormat( Attributes.GAIRAIGO, new GairaigoFormatter( commaBracketList), false);
        out.addAttributeFormat( Attributes.EXPLANATION, informationFormat, false);
    }
} // class DictionaryEntryFormat
