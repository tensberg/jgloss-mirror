/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

import java.util.List;

/**
 * Generic interface to a dictionary with an arbitrary backend.
 *
 * @author Michael Koch
 */
public interface Dictionary {
    /**
     * Only return matches which exactly match the search string.
     */
    short SEARCH_EXACT_MATCHES = 0;
    /**
     * Return all matches where the search string is a prefix of the match. 
     */
    short SEARCH_STARTS_WITH = 1;
    /**
     * Return all matches where the search string is a postfix of the match.
     */
    short SEARCH_ENDS_WITH = 2;
    /**
     * Return all matches where the search string is a substring of the match.
     */
    short SEARCH_ANY_MATCHES = 3;

    /**
     * Returns a short descriptive name of the dictionary. For dictionaries based
     * on a dictionary file this could be the filename.
     *
     * @return A short descriptive name of the dictionary.
     */
    String getName();

    /**
     * Searches for entries in the dictionary.
     *
     * @param expression The string to search for.
     * @param mode The search mode. One of <CODE>SEARCH_EXACT_MATCHES, SEARCH_STARTS_WITH,
     *             SEARCH_ENDS_WITH</CODE> or <CODE>SEARCH_ANY_MATCHES</CODE>.
     * @return A list of dictionary entries which match the expression given the search modes.
     *         Items in the list are instances of <CODE>WordReadingPair</CODE> or 
     *         <CODE>DictionaryEntry</CODE> (note that <CODE>DictionaryEntry</CODE> implements
     *         <CODE>WordReadingPair</CODE>). If no match is found, the empty list will be returned.
     * @exception SearchException if there was an error during the search.
     * @see DictionaryEntry
     * @see WordReadingPair
     */
    List search( String expression, short mode) throws SearchException;

    /**
     * Called when the dictionary is no longer needed. This gives the dictionary the
     * opportunity to dispose of used resources like open files or TCP/IP connections.
     * After a call to this method the dictionary can no longer be used.
     */
    void dispose();
} // interface Dictionary
