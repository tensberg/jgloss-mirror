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
 * Character encoding handler for EUC-JP encoded text.
 *
 * @author Michael Koch
 */
public class EUCJPCharacterHandler implements EncodedCharacterHandler {
    public EUCJPCharacterHandler() {}

    public int readCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                        CharacterCodingException {
        int c = byteToUnsignedByte( buffer.get());
        if (c > 127) { // 2/3-Byte Japanese
            boolean threebyte = false;
            if (c == 0x8f) // JIS X 0212 3-Byte Kanji
                threebyte = true;
            // read second byte
            c = (c<<8) | byteToUnsignedByte( buffer.get());
            if (threebyte) // read third byte
                c = (c<<8) | byteToUnsignedByte( buffer.get());
        }

        return c;
    }

    public int convertCharacter( int c) {
        if ((c >= 'A') && (c <= 'Z')) // uppercase -> lowercase conversion
            c |= 0x20;
        if (c&0xff00 == 0xa500) // convert katakana to hiragana
            c &= 0xfeff; // converts 0xa5 to 0xa4
    }

    public CharacterClass getCharacterClass( int c, boolean inWord) {
        if (c > 127) { // multibyte character in EUC-JP encoding
            if (c >= 0xb000) // 2- or 3-byte kanji
                return CharacterClass.KANJI;
            // otherwise kana
            return CharacterClass.KANA;
        }
        else { // ASCII character
            if (c>='a' && c<='z' ||
                c>='A' && c<='Z' ||
                c>='0' && c<='9' ||
                c=='\\' || // possible start of unicode escape
                (inWord && c=='-'))
                return CharacterClass.ROMAN_WORD; // word character
            else
                return CharacterClass.OTHER; // not in index word
        }
    }
} // class EUCJPCharacterHandler
