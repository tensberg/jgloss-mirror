/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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
 * Utility functions for handling Japanese characters and strings.
 *
 * @author Michael Koch
 */
public abstract class StringTools {
    /**
     * Returns the unicode block of a character. The test is optimized to work faster than
     * <CODE>Character.UnicodeBlock.of</CODE> for Japanese characters, but will work slower
     * for other scripts.
     */
    public static Character.UnicodeBlock unicodeBlockOf( char c) {
        if (c>=0x4e00 && c<0xa000)
            return Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
        else if (c>=0x30a0 && c<0x3100)
            return Character.UnicodeBlock.KATAKANA;
        else if (c>=0x3040) // upper bound is 0x30a0
            return Character.UnicodeBlock.HIRAGANA;
        else if (c>=0x3000) // upper bound is 0x3040
            return Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
        else if (c < 0x80)
            return Character.UnicodeBlock.BASIC_LATIN;
        else return Character.UnicodeBlock.of( c);
    }

    public static boolean isKatakana( char c) {
        return (c>=0x30a0 && c<0x3100);
    }

    public static boolean isHiragana( char c) {
        return (c>=0x3040 && c<0x30a0);
    }

    public static boolean isCJKUnifiedIdeographs( char c) {
        return (c>=0x4e00 && c<0xa000);
    }

    public static boolean isCJKSymbolsAndPunctuation( char c) {
        return (c>=0x3000 && c<0x3040);
    }

    /**
     * Returns a new string with all katakana characters in the original string converted to
     * hiragana.
     */
    public static String toHiragana( String s) {
        StringBuffer out = new StringBuffer( s);
        for ( int i=0; i<out.length(); i++) {
            char c = out.charAt( i);
            if (isKatakana( c))
                out.setCharAt( i, (char) (c-96));
        }
        return out.toString();
    }

    /**
     * Returns a new string with all hiragana characters in the original string converted to
     * katakana.
     */
    public static String toKatakana( String s) {
        StringBuffer out = new StringBuffer( s);
        for ( int i=0; i<out.length(); i++) {
            char c = out.charAt( i);
            if (isHiragana( c))
                out.setCharAt( i, (char) (c+96));
        }
        return out.toString();
    }
} // class StringTools
