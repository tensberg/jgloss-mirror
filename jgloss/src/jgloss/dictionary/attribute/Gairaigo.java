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

import java.util.ResourceBundle;
import java.util.Locale;

public class Gairaigo implements AttributeValue {
    public static final String JAPANESE_ENGLISH = "je";
    public static final String AINU = "ai";

    protected static final ResourceBundle MESSAGES =
        ResourceBundle.getBundle( "resources/messages-dictionary");

    protected String code;
    protected String originalWord;

    public Gairaigo( String _code) {
        this( _code, null);
    }

    public Gairaigo( String _code, String _originalWord) {
        code = _code;
        originalWord = _originalWord;
    }

    public String getLanguageCode() { return code; }

    public String getOriginalWord() { return originalWord; }

    public String toString() { return code + ":\"" + getOriginalWord() + "\""; }
} // class Gairaigo
