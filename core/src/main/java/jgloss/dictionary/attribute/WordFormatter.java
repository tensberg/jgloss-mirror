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

package jgloss.dictionary.attribute;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jgloss.util.ListFormatter;
import jgloss.util.UTF8ResourceBundleControl;

/**
 * Format word attribute values.
 *
 * @author Michael Koch
 */
public class WordFormatter extends DefaultAttributeFormatter {
    protected static final ResourceBundle languages =
        ResourceBundle.getBundle( "languages", new UTF8ResourceBundleControl());

    protected MessageFormat wordFormat;
    protected MessageFormat langFormat;
    protected MessageFormat langAndWordFormat;
    protected Object[] langAndWord = new Object[2];

    public WordFormatter( String _wordFormat, String _langFormat, 
                          String _langAndWordFormat,
                          ListFormatter _formatter) {
        super( _formatter);
        wordFormat = new MessageFormat( _wordFormat);
        langFormat = new MessageFormat( _langFormat);
        langAndWordFormat = new MessageFormat( _langAndWordFormat);
    }

    @Override
	public StringBuilder format( Attribute att, AttributeValue val, StringBuilder buf) {
        Word w = (Word) val;
            
        String lang = null;
        if (w.getLanguageCode() != null) {
            try {
                lang = languages.getString( w.getLanguageCode());
            } catch (MissingResourceException ex) {
                System.err.println( "Debug: WordFormatter, missing language string for code " +
                                    w.getLanguageCode());
                lang = w.getLanguageCode();
            }
        }
            
        if (lang == null) {
            langAndWord[0] = w.getWord();
            buf.append(wordFormat.format( langAndWord)); 
        }
        else {
            langAndWord[0] = lang;
            if (w.getWord() == null) {
            	buf.append(langFormat.format( langAndWord));
            }
            else {
                langAndWord[1] = w.getWord();
                buf.append(langAndWordFormat.format( langAndWord));
            }
        }

        return buf;
    }
} // class WordFormatter
