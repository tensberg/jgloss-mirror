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

import java.nio.ByteBuffer;

/**
 * Interface for classes which support indexing of words, usually {@link Dictionary Dictionary}
 * subclasses. {@link IndexBuilder IndexBuilder} and {@link Index Index} classes use the methods
 * provided by this interface to compare index entries, which are usually strings of characters
 * encoded in an implementation-dependent format.
 *
 * @author Michael Koch
 * @see Index
 * @see IndexBuilder
 */
public interface Indexable {
    /**
     * Stores a single character and the position of the character following it.
     * Since the character encoding
     * is implementation-dependent and may be a multibyte encoding, index positions do not directly
     * correspond to character positions.
     */
    public class CharData {
        public int character = 0;
        public int position = 0;

        public CharData() {}
    } // class CharData

    /**
     * Compares two index entries. The comparison imposes a total ordering on index entries
     * contained by the <code>Indexable</code> object. Since the index entries are usually
     * strings of text, it is expected that it is a lexicographical ordering, although there
     * is no guarantee that the <code>compare</code> method will impose the same ordering
     * as <code>String.compareTo</code>.
     *
     * @param pos1 Position of the first index entry.
     * @param pos2 Position of the second index entry.
     * @return <code>&lt;0</code> if the entry at pos1 is smaller than the entry at pos2;
     *         <code>&gt;0</code> if it is greater and <code>0</code> if the entries at both
     *         positions is equal.
     */
    int compare( int pos1, int pos2) throws IndexException;
    /**
     * Compare the data in a buffer to an index entry. The data in the byte buffer must be
     * encoded in a way that is compatible to the encoding of the index entries. For example,
     * if index entries are stored by the <code>Indexable</code> class as EUC-JP encoded
     * text, the buffer must also contain EUC-JP encoded text. The ordering by this
     * <code>compare</code> method must be consistent with {@link #compare(int,int) compare(int,int)}.
     * The only allowed difference is that comparisons may be truncated to the length of the buffer.
     * That is, a comparison may return equality even if the index entry is longer than the data
     * in the buffer (the buffer data is a prefix of the index entry). This is allowed to make
     * substring searches possible.
     *
     * @return <code>&lt;0</code> if the data in the buffer is smaller than the index entry;
     *         <code>&gt;0</code> if it is greater and <code>0</code> buffer data and the entry
     *         are identical.
     */
    int compare( ByteBuffer data, int position) throws IndexException;
    /**
     * Decode the character at a given position in the indexable data. Also returns the position of the
     * next character.
     *
     * @param outResult The result of the method invocation will be stored in the object.
     *                  This prevents the need to create an object every time the method is invoked.
     *                  If <code>null</code> is passed, a new instance will be created.
     */
    CharData getChar( int position, CharData outResult) throws IndexException;

    EncodedCharacterHandler getEncodedCharacterHandler();
} // interface indexable
