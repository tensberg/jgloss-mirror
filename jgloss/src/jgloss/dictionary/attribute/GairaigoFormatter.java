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

package jgloss.dictionary.attribute;

import jgloss.util.ListFormatter;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.text.MessageFormat;

/**
 * Format gairaigo attribute values.
 *
 * @author Michael Koch
 */
public class GairaigoFormatter implements AttributeFormatter {
    private static final ResourceBundle abbreviations =
        ResourceBundle.getBundle( "resources/gairaigo-abbreviations");

    private final MessageFormat languageFormat = new MessageFormat
        ( abbreviations.getString( "format.short"));
    private final MessageFormat langAndWordFormat = new MessageFormat
        ( abbreviations.getString( "format.long"));
    private Object[] langAndWord = new Object[2];
    private StringBuffer tempFormat = new StringBuffer( 128);

    private ListFormatter formatter;

    public GairaigoFormatter( ListFormatter _formatter) {
        formatter = _formatter;
    }

    public StringBuffer format( Attribute att, ValueList val, StringBuffer buf) {
        if (val==null || val.size()==0) {
            formatter.newList( buf, 1);
            formatter.addItem( att.getName());
            formatter.endList();
        }
        else {
            formatter.newList( buf, val.size());
            for ( int i=0; i<val.size(); i++) {
                Gairaigo g = (Gairaigo) val.get( i);
                tempFormat.setLength( 0);
                try {
                    langAndWord[0] = abbreviations.getString( g.getLanguageCode());
                } catch (MissingResourceException ex) {
                    System.err.println( "Warning: GairaigoFormatter, missing language string for code " +
                                        g.getLanguageCode());
                    langAndWord[0] = g.getLanguageCode();
                }
                if (g.getOriginalWord() == null) {
                    formatter.addItem( languageFormat.format( langAndWord, tempFormat, null));
                }
                else {
                    langAndWord[1] = g.getOriginalWord();
                    formatter.addItem( langAndWordFormat.format( langAndWord, tempFormat, null));
                }
            }
            formatter.endList();
        }

        return buf;
    }
} // class GairaigoFormatter
