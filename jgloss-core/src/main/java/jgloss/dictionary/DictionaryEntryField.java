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

package jgloss.dictionary;

/**
 * Typesafe enumeration of field types of dictionary entries. Used as parameter in method
 * {@link IndexBuilder#addEntry IndexBuilder.addEntry}.
 */
public class DictionaryEntryField {
    /**
     * The index entry is in the word field of a dictionary entry.
     */
    public static final DictionaryEntryField WORD = new DictionaryEntryField( "WORD");
    /**
     * The index entry is in the reading field of a dictionary entry.
     */
    public static final DictionaryEntryField READING = new DictionaryEntryField( "READING");
    /**
     * The index entry is in the translation field of a dictionary entry.
     */
    public static final DictionaryEntryField TRANSLATION = new DictionaryEntryField( "TRANSLATION");
    /**
     * The index entry is in some other field of a dictionary entry.
     */
    public static final DictionaryEntryField OTHER = new DictionaryEntryField( "OTHER");

    private final String type;

    protected DictionaryEntryField( String _type) {
        this.type = _type;
    }

    @Override
	public String toString() { return type; }
} // class DictionaryEntryField
