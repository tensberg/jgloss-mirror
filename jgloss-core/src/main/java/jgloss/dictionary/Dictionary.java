/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.dictionary;

import java.util.Iterator;
import java.util.Set;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeValue;

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
     * Searches for entries in the dictionary. While the returned iterator itself does not have
     * to be thread-safe, it must be possible to iterate over multiple search iterators concurrently.
     *
     * @param searchmode The requested search mode. The search mode must be supported by this
     *                   dictionary.
     * @param parameters Search parameters as required by the {@link SearchMode searchmode}.
     *        The parameters must be valid for the selected search mode according to
     *        {@link List<SearchParameter>#isValid(Object[]) List<SearchParameter>.isValid}.
     * @return Iterator over the results of the search.
     * @exception SearchException if the search mode is not supported or there was an error
     *            during the search.
     */
    Iterator<DictionaryEntry> search( SearchMode searchmode, Object[] parameters) throws SearchException;

    /**
     * Test if this dictionary supports searches of a certain type. If it fully supports the
     * search mode, the search is executed according to the selected mode. If it is partially
     * supported, the search result will be approximated using a different search mode. For example,
     * an "any match" search could be approximated by a "starts with" or an "exact match" search,
     * which will give some results but not find all theroretically possible matches. If a search
     * mode is not supported, calling
     * {@link #search(SearchMode,Object[]) search} with this search mode will throw an exception.
     *
     * @param searchmode The search mode to test.
     * @param fully If <code>true</code>, test if the search mode is fully supported, if
     *        <code>false</code>, test if it is partially supported.
     */
    boolean supports( SearchMode searchmode, boolean fully);

    /**
     * Get a set of all attributes used by this dictionary. Any of these attributes may
     * appear in a dictionary entry generated by the dictionary.
     */
    Set<Attribute<?>> getSupportedAttributes();

    /**
     * Return the set of known attribute values for an attribute. If the set of possible
     * values of an attribute are constant and not dependent on a particular dictionary
     * entry, they will be returned. An example for a constant set of attribute values
     * are {@link jgloss.dictionary.attribute.PartOfSpeech PartOfSpeech} attributes.
     * An example for non-constant values,
     * which will not be returned by this method, are
     * {@link jgloss.dictionary.attribute.InformationAttributeValue InformationAttributeValues}.
     * In this case, an empty set is returned. For unsupported attributes,
     * <code>null</code> will be returned.
     */
    <T extends AttributeValue> Set<T> getAttributeValues( Attribute<T> att);

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
