
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

import jgloss.util.NumberTools;

/**
 * Character encoding handler for EUC-JP encoded text.
 *
 * @author Michael Koch
 */
public class EUCJPCharacterHandler implements EncodedCharacterHandler {

    @Override
	public int readCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                        IndexOutOfBoundsException,
                                                        CharacterCodingException {
        int c = NumberTools.byteToUnsignedByte( buffer.get());
        if (c > 127) { // 2/3-Byte Japanese
            boolean threebyte = false;
            if (c == 0x8f) {
	            threebyte = true;
            }
            // read second byte
            c = (c<<8) | NumberTools.byteToUnsignedByte( buffer.get());
            if (threebyte) {
	            c = (c<<8) | NumberTools.byteToUnsignedByte( buffer.get());
            }
        }

        return c;
    }

    @Override
	public int readPreviousCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                         IndexOutOfBoundsException,
                                                         CharacterCodingException {
        // NOTE: currently no support for 3-byte Kanji. Needs to be added as soon as I know
        //       how to recognize them when reading backwards.

        int position = buffer.position();
        int character;
        byte b = buffer.get( --position);
        if ((b&0x80) == 0) { // single byte character
            character = b;
        }
        else {
            byte b2 = buffer.get( --position);
            if ((b2&0x80) == 0) {
	            throw new CharacterCodingException();
            }

            character = NumberTools.byteToUnsignedByte( b2)<<8 | 
                NumberTools.byteToUnsignedByte( b);
        }

        buffer.position( position);
        return character;
    }

    @Override
	public int convertCharacter( int c) {
        if ((c >= 'A') && (c <= 'Z')) {
	        c |= 0x20;
        }
        if ((c&0xff00) == 0xa500)
		 {
	        c &= 0xfeff; // converts 0xa5 to 0xa4
        }
        return c;
    }

    @Override
	public CharacterClass getCharacterClass( int c, boolean inWord) {
        if (c > 127) { // multibyte character in EUC-JP encoding
            if (c >= 0xb000) {
	            return CharacterClass.KANJI;
            }
            if ((c&0xff00) == 0xa500) {
	            return CharacterClass.HIRAGANA;
            } else {
	            return CharacterClass.KATAKANA;
            }
        }
        else { // ASCII character
            if (c>='a' && c<='z' ||
                c>='A' && c<='Z' ||
                c>='0' && c<='9' ||
                c=='\\' || // possible start of unicode escape
                (inWord && c=='-')) {
	            return CharacterClass.ROMAN_WORD; // word character
            }
			else {
	            return CharacterClass.OTHER; // not in index word
            }
        }
    }

    @Override
	public boolean canEncode(char c) {
        // ASCII and Kanji/Kana characters can be encoded
        // TODO: look up the real supported range, the given range also includes chinese and korean chars
        return (c<128 || c>=0x2e80 && c<0xa000);
    }

    @Override
	public String getEncodingName() { return "EUC-JP"; }
} // class EUCJPCharacterHandler
