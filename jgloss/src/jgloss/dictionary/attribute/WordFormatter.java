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
 * Format word attribute values.
 *
 * @author Michael Koch
 */
public class WordFormatter implements AttributeFormatter {
    protected static final ResourceBundle languages =
        ResourceBundle.getBundle( "resources/languages");

    protected MessageFormat wordFormat;
    protected MessageFormat langFormat;
    protected MessageFormat langAndWordFormat;
    protected Object[] langAndWord = new Object[2];
    protected StringBuffer tempFormat = new StringBuffer( 128);

    private ListFormatter formatter;

    public WordFormatter( String _wordFormat, String _langFormat, 
                          String _langAndWordFormat,
                          ListFormatter _formatter) {
        wordFormat = new MessageFormat( _wordFormat);
        langFormat = new MessageFormat( _langFormat);
        langAndWordFormat = new MessageFormat( _langAndWordFormat);
        formatter = _formatter;
    }

    public StringBuffer format( Attribute att, ValueList val, StringBuffer buf) {
        if (val==null || val.size()==0) {
            return buf;
        }

        formatter.newList( buf, val.size());
        for ( int i=0; i<val.size(); i++) {
            Word w = (Word) val.get( i);
            tempFormat.setLength( 0);
            
            String lang = null;
            if (w.getLanguageCode() != null) {
                try {
                    lang = languages.getString( w.getLanguageCode());
                } catch (MissingResourceException ex) {
                    System.err.println( "Warning: WordFormatter, missing language string for code " +
                                        w.getLanguageCode());
                    lang = w.getLanguageCode();
                }
            }
            
            if (lang == null) {
                langAndWord[0] = w.getWord();
                formatter.addItem( wordFormat.format( langAndWord, tempFormat, null)); 
            }
            else {
                langAndWord[0] = lang;
                if (w.getWord() == null) {
                    formatter.addItem( langFormat.format( langAndWord, tempFormat, null));
                }
                else {
                    langAndWord[1] = w.getWord();
                    formatter.addItem( langAndWordFormat.format( langAndWord, tempFormat, null));
                }
            }
        }
        formatter.endList();

        return buf;
    }
} // class WordFormatter
