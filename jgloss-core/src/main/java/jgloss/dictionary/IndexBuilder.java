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

/**
 * Creator for an index structure which will then be used by a {@link Index Index} instance.
 * An <code>IndexBuilder</code> will be used by a <code>Indexable</code> instance, usually a
 * dictionary, to create a particular index type. 
 *
 * @author Michael Koch
 */
public interface IndexBuilder {
    /**
     * Begin building a new index.
     *
     * @param container Container to which the index should be added.
     * @param dictionary Dictionary in which the index entries are stored.
     */ 
    void startBuildIndex( IndexContainer container, Indexable dictionary) throws IndexException;
    /**
     * Add an entry to the index structure. This method is called repeatedly by the index
     * container.
     *
     * @param location Location of the index entry, encoded as integer value in a 
     *                 <code>Indexable</code>-dependent way.
     * @param length Length of the index entry, encoded in a 
     *               <code>Indexable</code>-dependent way. <code>(location+length)</code> is the
     *               first location not belonging to the index entry.
     * @param field Dictionary entry field in which the index entry is contained. The index builder
     *              may ignore index entries for certain fields.
     * @return <code>true</code> if the entry was added to the index, <code>false</code> if it
     *         was ignored.
     */
    boolean addEntry( int location, int length, DictionaryEntryField field) throws IndexException;
    /**
     * End the index build.
     *
     * @param commit <code>true</code> if the generated index data should be stored, <code>false</code>
     *        if some error occurred during index creation and the index data should be discarded.
     */
    void endBuildIndex( boolean commit) throws IndexException;
} // interface IndexBuilder
