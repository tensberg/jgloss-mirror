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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import jgloss.util.StringTools;

/**
 * Character encoding handler for UTF-8 encoded text.
 *
 * @author Michael Koch
 */
public class UTF8CharacterHandler implements EncodedCharacterHandler {
    private int[] charData = new int[6];

    public UTF8CharacterHandler() {}

    public int readCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                        IndexOutOfBoundsException,
                                                        CharacterCodingException {
        byte b = buffer.get();
        int length = 1;
        if ((b&0x80) == 0) {
            return b; // single byte character
        }
        else if ((b&0xe0) == 0xc0) { // %110xxxxx
            length = 2;
            charData[0] = b & 0x1f;
        }
        else if ((b&0xf0) == 0xe0) { // %1110xxxx
            length = 3;
            charData[0] = b & 0x0f;
        }
        else if ((b&0xf8) == 0xf0) { // %11110xxx
            length = 4;
            charData[0] = b & 0x07;
        }
        else if ((b&0xfc) == 0xf8) { // % 111110xx
            length = 5;
            charData[0] = b & 0x03;
        }
        else if ((b&0xfe) == 0xfc) { // % 1111110x
            length = 6;
            charData[0] = b & 0x01;
        }
        for ( int i=1; i<length; i++) {
            b = buffer.get();
            if ((b&0xc0) != 0x80)
                throw new CharacterCodingException();
            charData[i] = b & 0x3f;
        }

        return decode( charData, 0, length);
    }

    public int readPreviousCharacter( ByteBuffer buffer) throws BufferUnderflowException,
                                                                CharacterCodingException {
        int position = buffer.position();
        byte b = buffer.get( --position);
        if ((b&0x80) == 0) {
            buffer.position( position);
            return b; // single byte character
        }
        if ((b&0xc0) != 0x80)
            throw new CharacterCodingException();
        
        charData[5] = b & 0x3f;
        int length = 2;
        
        do {
            b = buffer.get( --position);
            if ((b&0xc0) != 0x80)
                break;
            charData[6-length] = b & 0x3f;
            length++;
        } while (length <= 6);
        
        if (length==2 && (b&0xe0)==0xc0)
            charData[4] = b&0x1f;
        else if (length==3 && (b&0xf0)==0xe0)
            charData[3] = b&0x0f;
        else if (length==4 && (b&0xf8)==0xf0)
            charData[2] = b&0x07;
        else if (length==5 && (b&0xfc)==0xf8)
            charData[1] = b&0x03;
        else if (length==6 && (b&0xfe)==0xfc)
            charData[0] = b&0x01;
        else // invalid length or wrong length marker in byte b
            throw new CharacterCodingException();
        
        buffer.position( position);
        return decode( charData, 6-length, length);
    }

    protected int decode( int[] charData, int offset, int length) throws CharacterCodingException {
        // catch overlong UTF-8 sequences 
        // (sequences that are longer than necessary to encode a character)
        if (length==2 && (charData[offset]&0xfe)==0 ||
            charData[offset]==0 &&
            (length==3 && (charData[offset+1]&0x20)==0 ||
             length==4 && (charData[offset+1]&0x30)==0 ||
             length==5 && (charData[offset+1]&0x38)==0 ||
             length==6 && (charData[offset+1]&0x3c)==0))
            throw new CharacterCodingException();

        int c = charData[offset];
        for ( int i=1; i<length; i++)
            c = c<<6 | charData[offset+i];

        // catch illegal data ranges
        if (c>=0xd800 && c<=0xdfff ||
            c==0xfffe || c==0xffff)
            throw new CharacterCodingException();

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
        else if (c>=0x3040 && c<0x30a0)
            return CharacterClass.HIRAGANA;
        else if (c>=0x30a0 && c<0x3100)
            return CharacterClass.KATAKANA;
        else if (c == '-')
            return (inWord ? CharacterClass.ROMAN_WORD : CharacterClass.OTHER);
        else if (Character.isLetterOrDigit( (char) c)) // any other characters
            return CharacterClass.ROMAN_WORD;
        else
            return CharacterClass.OTHER; // not in word
    }

    public String getEncodingName() { return "UTF-8"; }
} // class UTF8EncodingConverter
