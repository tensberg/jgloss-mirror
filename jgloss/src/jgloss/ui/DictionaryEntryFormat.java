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

// TODO: make formats configurable

/**
 * Format definition for rendering of dictionary entries.
 *
 * @author Michael Koch
 */
class DictionaryEntryFormat {

    private static final ListFormatter word = new ListFormatter( " ", "; ", "");
    private static final ListFormatter reading = new ListFormatter( " [", ": ", "]");
    private static final ListFormatter rom = new ListFormatter( "", " ", ".", " (n) ", ". (n) ", ".");
    private static final ListFormatter crm = new ListFormatter( "", "; ", "");
    private static final ListFormatter syn = new ListFormatter( "", "/", "");

    private static final DictionaryEntryFormatter formatter = new DictionaryEntryFormatter();
    private static final DictionaryEntryFormatter htmlFormatter = new DictionaryEntryFormatter();

    static {
        formatter.addWordFormat( word);
        formatter.addReadingFormat( reading);
        formatter.addTranslationFormat( rom, crm, syn);

        htmlFormatter.addWordFormat( word);
        htmlFormatter.addReadingFormat( reading);
        htmlFormatter.addTranslationFormat( rom, crm, syn);

        AttributeFormatter listFormat = new DefaultAttributeFormatter
            ( " (", ")", "", false, new ListFormatter( ","));
        AttributeFormatter informationFormat = new InformationAttributeFormatter
            ( " (", ")", "", false, new ListFormatter( ","));
        formatter.addAttributeFormat( Attributes.PART_OF_SPEECH,
                                      DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                      listFormat);
        htmlFormatter.addAttributeFormat( Attributes.PART_OF_SPEECH,
                                          DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                          listFormat);
        formatter.addAttributeFormat( Attributes.EXAMPLE,
                                      DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                      new DefaultAttributeFormatter( " {", "}", "", true, null));
        htmlFormatter.addAttributeFormat( Attributes.EXAMPLE,
                                          DictionaryEntryFormatter.Position.BEFORE_FIELD3,
                                          new DefaultAttributeFormatter( " {", "}", "", true, null));
        formatter.addAttributeFormat( Attributes.EXPLANATION, informationFormat, false);
        htmlFormatter.addAttributeFormat( Attributes.EXPLANATION, informationFormat, false);
    }

    public static ListFormatter getWordFormatter() { return word; }
    public static ListFormatter getReadingFormatter() { return reading; }
    public static ListFormatter getTranslationRomFormatter() { return rom; }
    public static ListFormatter getTranslationCrmFormatter() { return crm; }
    public static ListFormatter getTranslationSynFormatter() { return syn; }

    public static DictionaryEntryFormatter getFormatter() {
        return formatter;
    }

    public static DictionaryEntryFormatter getHTMLFormatter() {
        return htmlFormatter;
    }
} // class DictionaryEntryFormat
