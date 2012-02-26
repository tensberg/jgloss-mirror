/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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
import jgloss.dictionary.DictionaryEntryFormatter;
import jgloss.dictionary.WadokuJT;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.AttributeNameFormatter;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.DefaultAttributeFormatter;
import jgloss.dictionary.attribute.InformationAttributeFormatter;
import jgloss.dictionary.attribute.ReferenceAttributeFormatter;
import jgloss.dictionary.attribute.WordFormatter;
import jgloss.util.DefaultListFormatter;
import jgloss.util.ListFormatter;

// TODO: make formats configurable by the user

/**
 * Format definition for rendering of dictionary entries.
 *
 * @author Michael Koch
 */
class DictionaryEntryFormat {
    interface Decorator {
        int WORD = 0;
        int READING = 1;
        int TRANSLATION_ROM = 2;
        int TRANSLATION_CRM = 3;
        int TRANSLATION_SYN = 4;

        int POSITION_BEFORE = 0;
        int POSITION_AFTER = 1;

        ListFormatter decorateList( ListFormatter formatter, int type);

        AttributeFormatter decorateAttribute( AttributeFormatter formatter, Attribute<?> type,
                                              int position);
        ListFormatter decorateList( ListFormatter formatter, Attribute<?> type, int position);
    } // interface Decorator

    static class IdentityDecorator implements Decorator {
        IdentityDecorator() {}

        @Override
		public ListFormatter decorateList( ListFormatter formatter, int type) {
            return formatter;
        }

        @Override
		public AttributeFormatter decorateAttribute( AttributeFormatter formatter, Attribute<?> type,
                                                     int position) {
            return formatter;
        }

        @Override
		public ListFormatter decorateList( ListFormatter formatter, Attribute<?> type, int position) {
            return formatter;
        }
    } // class IdentityDecorator

    public static final String PROTOCOL_REF = "ref";
    public static final String PROTOCOL_SYN = "syn";
    public static final String PROTOCOL_ANT = "ant";
    public static final String PROTOCOL_ALT_READING = "ar";

    private static final DefaultListFormatter word = 
        new DefaultListFormatter( " ", "; ", "");
    private static final DefaultListFormatter reading = 
        new DefaultListFormatter( " [", "; ", "]");
    private static final DefaultListFormatter rom = 
        new DefaultListFormatter( "", "", ".", " [n]", ". [n]", ".");
    private static final DefaultListFormatter crm = 
        new DefaultListFormatter( " ", "; ", "");
    private static final DefaultListFormatter syn = 
        new DefaultListFormatter( "", "/", "");

    public static ListFormatter getWordFormatter() { return new DefaultListFormatter( word); }
    public static ListFormatter getReadingFormatter() { return new DefaultListFormatter( reading); }
    public static ListFormatter getTranslationRomFormatter() { return new DefaultListFormatter( rom); }
    public static ListFormatter getTranslationCrmFormatter() { return new DefaultListFormatter( crm); }
    public static ListFormatter getTranslationSynonymFormatter() { return new DefaultListFormatter( syn); }

    public static AttributeFormatter getAttributeFormatter( Attribute<?> att) {
        return getAttributeFormatter( att, false, new IdentityDecorator(), Decorator.POSITION_BEFORE);
    }

    public static AttributeFormatter getAttributeFormatter( Attribute<?> att, boolean nameOnly) {
        return getAttributeFormatter( att, nameOnly, new IdentityDecorator(), Decorator.POSITION_BEFORE);
    }

    public static AttributeFormatter getAttributeFormatter( Attribute<?> att,
                                                            Decorator decorator, int position) {
        return getAttributeFormatter( att, false, decorator, position);
    }

    public static AttributeFormatter getAttributeFormatter( Attribute<?> att, boolean nameOnly,
                                                            Decorator decorator, int position) {
        ListFormatter commaList = decorator.decorateList
            ( new DefaultListFormatter( ","), att, position);

        AttributeFormatter format = null;

        if (nameOnly || att==Attributes.EXAMPLE)
            format = new AttributeNameFormatter( " {", "}");

        else if (att == Attributes.PART_OF_SPEECH ||
                 att == Attributes.USAGE ||
                 att == Attributes.CATEGORY)
            format = new DefaultAttributeFormatter
                ( " (", ")", "", false, commaList);

        else if (att == Attributes.ABBREVIATION)
            format = new WordFormatter( JGloss.messages.getString( "abbr.word"),
                                        "", JGloss.messages.getString( "abbr.lang_and_word"),
                                        decorator.decorateList( new DefaultListFormatter( " (", ",", ")"),
                                                                att, position));

        else if (att == Attributes.GAIRAIGO)
            format = new WordFormatter( "", JGloss.messages.getString( "gairaigo.lang"),
                                        JGloss.messages.getString( "gairaigo.lang_and_word"),
                                        decorator.decorateList( new DefaultListFormatter( " (", ",", ")"),
                                                                att, position));

        else if (att == Attributes.REFERENCE)
            format = new ReferenceAttributeFormatter
                ( " \u21d2", "", commaList);

        else if (att == Attributes.SYNONYM)
            format = new ReferenceAttributeFormatter
                ( " \u21d2", "", commaList);

        else if (att == Attributes.ANTONYM)
            format = new ReferenceAttributeFormatter
                ( " \u21d4", "", commaList);

        else if (att == WadokuJT.ALT_READING)
            format = new ReferenceAttributeFormatter
                ( " \u2192", "", commaList);

        else if (att == Attributes.EXPLANATION)
            format = new InformationAttributeFormatter
                ( " (", ")", "", false, commaList);

        // else: attribute not supported

        if (format != null)
            format = decorator.decorateAttribute( format, att, position);

        return format;
    }

    public static DictionaryEntryFormatter createFormatter() {
        return createFormatter( null);
    }

    public static DictionaryEntryFormatter createFormatter( Decorator decorator) {
        DictionaryEntryFormatter out = new DictionaryEntryFormatter();

        if (decorator == null)
            decorator = new IdentityDecorator();

        out.addWordFormat( decorator.decorateList( new DefaultListFormatter( word),
                                                   Decorator.WORD));
        out.addReadingFormat( decorator.decorateList( new DefaultListFormatter( reading),
                                                      Decorator.READING));
        out.addTranslationFormat( decorator.decorateList( new DefaultListFormatter( rom),
                                                          Decorator.TRANSLATION_ROM), 
                                  decorator.decorateList( new DefaultListFormatter( crm),
                                                          Decorator.TRANSLATION_CRM),
                                  decorator.decorateList( new DefaultListFormatter( syn),
                                                          Decorator.TRANSLATION_SYN));

        addAttributeFormats( out, decorator);
        
        return out;
    }

    private static void addAttributeFormats( DictionaryEntryFormatter out,
                                             Decorator decorator) {
        out.addAttributeFormat( Attributes.PART_OF_SPEECH, 
                                getAttributeFormatter( Attributes.PART_OF_SPEECH, decorator,
                                                       Decorator.POSITION_BEFORE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        out.addAttributeFormat( Attributes.ABBREVIATION,
                                getAttributeFormatter( Attributes.ABBREVIATION, true, decorator,
                                                       Decorator.POSITION_BEFORE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3, true);
        out.addAttributeFormat( Attributes.EXAMPLE,
                                getAttributeFormatter( Attributes.EXAMPLE, decorator,
                                                       Decorator.POSITION_BEFORE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        out.addAttributeFormat( Attributes.USAGE,
                                getAttributeFormatter( Attributes.USAGE, decorator,
                                                       Decorator.POSITION_BEFORE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3, true);
        out.addAttributeFormat( Attributes.CATEGORY,
                                getAttributeFormatter( Attributes.CATEGORY, decorator,
                                                       Decorator.POSITION_BEFORE),
                                DictionaryEntryFormatter.Position.BEFORE_FIELD3, true);


        out.addAttributeFormat( Attributes.GAIRAIGO, 
                                getAttributeFormatter( Attributes.GAIRAIGO, decorator,
                                                       Decorator.POSITION_AFTER),
                                false);
        out.addAttributeFormat( Attributes.EXPLANATION, 
                                getAttributeFormatter( Attributes.EXPLANATION, decorator,
                                                       Decorator.POSITION_AFTER), false);
        out.addAttributeFormat( Attributes.REFERENCE,
                                getAttributeFormatter( Attributes.REFERENCE, decorator,
                                                       Decorator.POSITION_AFTER), false);
        out.addAttributeFormat( Attributes.SYNONYM,
                                getAttributeFormatter( Attributes.SYNONYM, decorator,
                                                       Decorator.POSITION_AFTER), false);
        out.addAttributeFormat( Attributes.ANTONYM,
                                getAttributeFormatter( Attributes.ANTONYM, decorator,
                                                       Decorator.POSITION_AFTER), false);
        out.addAttributeFormat( WadokuJT.ALT_READING,
                                getAttributeFormatter( WadokuJT.ALT_READING, decorator,
                                                       Decorator.POSITION_AFTER), false);

        out.addAttributeFormat( Attributes.ABBREVIATION,
                                getAttributeFormatter( Attributes.ABBREVIATION, decorator,
                                                       Decorator.POSITION_AFTER),
                                DictionaryEntryFormatter.Position.AFTER_ENTRY, false);
    }
} // class DictionaryEntryFormat
