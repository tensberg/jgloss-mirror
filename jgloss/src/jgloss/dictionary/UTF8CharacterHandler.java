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
import java.nio.BufferUnderflowException;
import java.nio.charset.CharacterCodingException;

/**
 * Character encoding handler for UTF-8 encoded text.
 *
 * @author Michael Koch
 */
public class UTF8CharacterHandler implements EncodedCharacterHandler {
    public UTF8CharacterHandler() {}

    public int readCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                 CharacterCodingException {
        byte b = buf.get();
        int c;
        // the dictionary is UTF-8 encoded; if the highest bit is set, more than one byte must be read
        if ((b&0x80) == 0) {
            // ASCII character
            c = b;
        }
        else if ((b&0xe0) == 0xc0) { // 2-byte encoded char
            byte b2 = buf.get();
            if ((b2&0xc0) == 0x80) // valid second byte
                c = ((b&0x1f) << 6) | (b&0x3f);
            else {
                throw new CharacterCodingException( "invalid 2-byte character");
            }
        }
        else if ((b&0xf0) == 0xe0) { // 3-byte encoded char
            byte b2 = buf.get();
            byte b3 = buf.get();
            if (((b2&0xc0) == 0x80) &&
                ((b3&0xc0) == 0x80)) { // valid second and third byte
                c = ((b&0x0f) << 12) | ((b2&0x3f) << 6) | (b3&0x3f);
            }
            else {
                c = '?';
                throw new CharacterCodingException( "invalid 3-byte character");
            }
        }
        else { // 4-6 byte encoded char or invalid char
            // FIXME: support 4-6 byte characters
            throw new CharacterCodingException( "4-6 byte chars not supported by this implementation");
        }

        return c;
    }

    public int convertCharacter( int c) {
        // convert katakana->hiragana
        if (StringTools.isKatakana( (char) c))
            c -= 96; // katakana-hiragana difference is 96 code points
        else if ((c >= 'A') && (c <= 'Z')) // lowercase for ASCII letters
            c |= 0x20;
        else if (c>127 && c<256) // lowercase for latin umlauts
            c = Character.toLowerCase( (char) c); // this method is slow, only use it for the special case

        return c;
    }

    public CharacterClass getCharacterClass( int c, boolean inWord) {
        if (c>=0x4e00 && c<0xa000)
            return CharacterClass.KANJI;
        else if (c>=0x3000 && c<0x3100)
            return CharacterClass.KANA; // katakana, hiragana
        else if (c == '-')
            return (inWord ? CharacterClass.ROMAN_WORD : CharacterClass.OTHER);
        else if (Character.isLetterOrDigit( (char) c)) // any other characters
            return CharacterClass.ROMAN_WORD;
        else
            return CharacterClass.OTHER; // not in word
    }
} // class UTF8EncodingConverter
