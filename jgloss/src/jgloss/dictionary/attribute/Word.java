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

/**
 * Word and code of originating language. This class can't be instantiated directly
 * because subclasses use different policies on which field (either word or langCode)
 * is optional.
 *
 * @author Michael Koch
 */
public abstract class Word implements AttributeValue {
    public static final String JAPANESE_ENGLISH = "je";
    public static final String JAPANESE_FRENCH = "jf";
    public static final String AINU = "ai";

    protected String word;
    protected String langCode;

    protected Word( String _word, String _langCode) {
        word = _word;
        langCode = _langCode;
    }

    public String getWord() { return word; }

    public String getLanguageCode() { return langCode; }

    public String toString() { return word + "(" + langCode + ")"; }
} // class Word