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

package jgloss.dictionary;

/**
 * Typesafe enumeration of search fields. Search fields are the fields of a dictionary entry
 * which may be searched: word, reading or translations. The translation additionally has
 * different search modes, which are enumerated in subclass 
 * {@link TranslationSearchField TranslationSearchField}.
 *
 * @author Michael Koch
 */
public class SearchField {
    private String name;

    public static final SearchField WORD = new SearchField( "WORD");
    public static final SearchField READING = new SearchField( "READING");

    protected SearchField( String _name) {
        this.name = _name;
    }

    public String toString() { return name; }
} // class SearchField
