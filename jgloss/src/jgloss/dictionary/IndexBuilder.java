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
 * Creator for an index structure which will then be used by a {@link Index Index} instance.
 * An <code>IndexBuilder</code> will be used by a <code>Indexable</code> instance, usually a
 * dictionary, to create a particular index type. 
 *
 * @author Michael Koch
 */
public interface IndexBuilder {
    /**
     * Typesafe enumeration of field types of dictionary entries. Used as parameter in method
     * {@link IndexBuilder#addEntry IndexBuilder.addEntry}.
     */
    public class DictionaryField {
        /**
         * The index entry is in the word field of a dictionary entry.
         */
        public static final DictionaryField WORD = new DictionaryField( "WORD");
        /**
         * The index entry is in the reading field of a dictionary entry.
         */
        public static final DictionaryField READING = new DictionaryField( "READING");
        /**
         * The index entry is in the translation field of a dictionary entry.
         */
        public static final DictinoaryField TRANSLATION = new DictionaryField( "TRANSLATION");
        /**
         * The index entry is in some other field of a dictionary entry.
         */
        public static final DictionaryField OTHER = new DictionaryField( "OTHER");

        private String type;

        private DictionaryField( String _type) {
            this.type = _type;
        }

        public String toString() { return type; }
    } // class DictionaryField

    void startBuildIndex( IndexContainer container, Indexable dictionary);
    /**
     * Add an entry to the index structure. This method is called repeatedly by the index
     * container.
     *
     * @param location Location of the index entry, encoded as integer value in a 
     *                 <code>Indexable</code>-dependent way.
     * @param length Length of the index entry, encoded in a 
     *               <code>Indexable</code>-dependent way. <code>(location+length)</code> is the
     *               first location not belonging to the index entry.
     * @param field Dictionary entry field in which the index entry is contained.
     */
    void addEntry( int location, int length, DictionaryField field);
    void endBuildIndex();
} // interface IndexBuilder
