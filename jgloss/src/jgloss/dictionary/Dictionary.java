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
     * Results must be {@link WordReadingPair WordReadingPair} or 
     * {@link DictionaryEntry DictionaryEntry} instances.
     */
    short RESULT_DICTIONARY_ENTRIES = 0;
    /**
     * Results can be in a format specified by the dictionary.
     */
    short RESULT_NATIVE = 1;

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
     * @param searchmode Determines how the expression must match an entry.
     *              One of {@link SEARCH_EXACT_MATCHES SEARCH_EXACT_MATCHES},
     *              {@link SEARCH_STARTS_WITH SEARCH_STARTS_WITH},
     *              {@link SEARCH_ENDS_WITH SEARCH_ENDS_WITH} or 
     *              {@link SEARCH_ANY_MATCHES SEARCH_ANY_MATCHES}. Not every dictionary may support
     *              all match modes.
     * @param resultmode Determines the type of objects in the results list. One of
     *             {@link RESULT_DICTIONARY_ENTRIES RESULT_DICTIONARY_ENTRIES} or
     *             {@link RESULT_NATIVE RESULT_NATIVE}.
     * @return A list of dictionary entries which match the expression given the search modes.
     *         The type of objects in the list is determined by the result mode.
     *         If no match is found, the empty list will be returned.
     * @exception SearchException if there was an error during the search.
     * @see DictionaryEntry
     * @see WordReadingPair
     */
    List search( String expression, short searchmode, short resultmode) throws SearchException;

    /**
     * Called when the dictionary is no longer needed. This gives the dictionary the
     * opportunity to dispose of used resources like open files or TCP/IP connections.
     * After a call to this method the dictionary can no longer be used.
     */
    void dispose();
} // interface Dictionary
