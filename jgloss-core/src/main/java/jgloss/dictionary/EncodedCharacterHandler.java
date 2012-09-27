/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

/**
 * Methods for handling Japanese text encoded as byte buffer using some character encoding.
 * <p>
 * The methods use <code>ints</code> to represent character values. Subclasses may guarantee that
 * only values in <code>char</code> or <code>short</code> range are used. 
 * Implementation classes of this interface do not have to use unicode code points as character
 * values; in this case directly typecasting the values to a Java
 * <code>char</code> will not give the expected results. The only guarantee is that the
 * ordering of <code>int</code> character values is equivalent to the alphabetical ordering of
 * the represented characters.
 * </p>
 *
 * @author Michael Koch
 */
public interface EncodedCharacterHandler {
    /**
     * Decode the character at the current buffer position. Returns the character value;
     * which does not have to be a unicode code point. Comparison of two integer values returned
     * by this method is equivalent to the alphabetical comparison of the represented characters.
     * When the method returns, the buffer's <code>position()</code> will be at the start of the
     * next character.
     *
     * @param buffer The buffer which contains the encoded character.
     * @return The decoded character.
     * @exception BufferUnderflowException if the end of the buffer is reached before a character
     *            is completely decoded.
     * @exception CharacterCodingException if the bytes at the current buffer position are not
     *            a legal encoded character.
     */
    int readCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                 IndexOutOfBoundsException,
                                                 CharacterCodingException;

    /**
     * Decode the character before the character at the current buffer position. 
     * Returns the character value;
     * which does not have to be a unicode code point. Comparison of two integer values returned
     * by this method is equivalent to the alphabetical comparison of the represented characters.
     * When the method returns, the buffer's <code>position()</code> will be at the start of the
     * character returned. Calling this method multiple times will effectively read the encoded
     * string backwards.
     *
     * @param buffer The buffer which contains the encoded character.
     * @return The decoded character.
     * @exception BufferUnderflowException if the end of the buffer is reached before a character
     *            is completely decoded.
     * @exception CharacterCodingException if the bytes at the current buffer position are not
     *            a legal encoded character.
     */
    int readPreviousCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                         IndexOutOfBoundsException,
                                                         CharacterCodingException;

    /**
     * Modify a character returned by {@link #readCharacter(ByteBuffer) readCharacter} to make
     * different character classes compare equal. This is used for searching and indexing to
     * treat certain character classes as identical with respect to comparison. Examples are
     * uppercase and lowercase western characters, or katakana and hiragana. What characters
     * are converted is dependent of the class implementing this interface and may be
     * configured by modifying the object's state.
     *
     * @param character The character to convert, as returned by 
     *                  {@link #readCharacter(ByteBuffer) readCharacter}.
     * @return The converted character.
     */
    int convertCharacter( int character);

    /**
     * Test the character class of a character returned by 
     * {@link #readCharacter(ByteBuffer) readCharacter}. These are specialized character classes
     * which do not directly map to any unicode character classes. They are used during index
     * creation to decide if the current character is part of an indexable word.
     *
     * @param character The character to test.
     * @param inWord <code>true</code>, if the character before the current character was in
     *        character class {@link CharacterClass#ROMAN_WORD ROMAN_WORD}. This may influence
     *        the character class of the tested character.
     * @return The character class of the tested character.
     */
    CharacterClass getCharacterClass( int character, boolean inWord);

    /**
     * Returns whether or not the character encoding can encode the given
     * character. 
     */
    boolean canEncode(char c);
    

    /**
     * Return the name of the encoding supported by this handler. The name must be compatible
     * with class <code>java.nio.charset.Charset</code>.
     */
    String getEncodingName();
} // interface EncodedCharacterHandler
