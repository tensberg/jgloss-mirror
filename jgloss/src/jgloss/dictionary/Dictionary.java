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

import jgloss.dictionary.attribute.Attribute;

import java.util.Iterator;

/**
 * Generic interface to a dictionary with an arbitrary backend.
 *
 * @author Michael Koch
 */
public interface Dictionary {
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
     * @param searchmode The requested search mode. The search mode must be supported by this
     *                   dictionary.
     * @param parameters Search parameters as required by the {@link SearchMode searchmode}.
     *        The parameters must be valid for the selected search mode according to
     *        {@link SearchParameters#isValid(Object[]) SearchParameters.isValid}.
     * @exception SearchException if the search mode is not supported or there was an error 
     *            during the search.
     */
    ResultIterator search( SearchMode searchmode, Object[] parameters) throws SearchException;

    /**
     * Test if this dictionary supports searches of a certain type. If it fully supports the
     * search mode, the search is executed according to the selected mode. If it is partially
     * supported, the search result will be approximated using a different search mode. For example,
     * an "any match" search could be approximated by a "starts with" or an "exact match" search,
     * which will give some results but not find all theroretically possible matches. If a search
     * mode is not supported, calling
     * {@link #search(SearchMode,Object[]) search) with this search mode will throw an exception.
     *
     * @param searchmode The search mode to test.
     * @param fully If <code>true</code>, test if the search mode is fully supported, if 
     *        <code>false</code>, test if it is partially supported.
     */
    boolean supports( SearchMode searchmode, boolean fully);

    /**
     * Test if the dictionary supports an attribute, that is, entries in this dictionary may
     * have the attribute set.
     *
     * @param attribute The attribute to test.
     */
    boolean supports( Attribute attribute);

    /**
     * Return the search fields for which a search of the given mode is supported for
     * this dictionary implementation. If the given search mode is supported and takes
     * a {@link SearchFieldSelection SearchFieldSelection} parameter, at least one search field must be
     * selected in the {@link SearchFieldSelection SearchFieldSelection} object returned.
     * 
     *
     * @exception IllegalArgumentException if the search mode is unsupported or does not take a 
     *            {@link SearchFieldSelection SearchFieldSelection} as parameter.
     */
    SearchFieldSelection getSupportedFields( SearchMode searchmode);

    /**
     * Called when the dictionary is no longer needed. This gives the dictionary the
     * opportunity to dispose of used resources like open files or TCP/IP connections.
     * After a call to this method the dictionary can no longer be used.
     */
    void dispose();
} // interface Dictionary
