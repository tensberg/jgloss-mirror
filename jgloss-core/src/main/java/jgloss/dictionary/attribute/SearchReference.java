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

import java.util.Iterator;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.SearchMode;

/**
 * Reference other dictionary entries by storing search parameters and performing the
 * search if the entries are requested.
 *
 * @author Michael Koch
 */
public class SearchReference implements ReferenceAttributeValue {
    private final String title;
    private final Dictionary dictionary;
    private final SearchMode searchMode;
    private final Object[] searchParameters;

    public SearchReference( String _title, Dictionary _dictionary, SearchMode _searchMode,
                            Object[] _searchParameters) {
        title = _title;
        dictionary = _dictionary;
        searchMode = _searchMode;
        searchParameters = _searchParameters;
    }   

    @Override
	public Iterator<DictionaryEntry> getReferencedEntries() throws SearchException {
        return dictionary.search( searchMode, searchParameters);
    }

    @Override
	public String getReferenceTitle() { return title; }

    public Dictionary getDictionary() { return dictionary; }
    public SearchMode getSearchMode() { return searchMode; }
    public Object[] getSearchParameters() { return searchParameters; }

    @Override
	public String toString() { return "\u2192" + title; }
} // class SearchReference
