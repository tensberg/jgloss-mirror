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

import java.util.*;

/**
 * Utility functions for handling Japanese characters and strings.
 *
 * @author Michael Koch
 */
public class StringTools {
    public static void main( String[] args) {
        System.out.println( "\u6f22\u5b57");
        splitWordReading( "\u6f22\u5b57", "\u304b\u3093\u3058");
        System.out.println( "\u601d\u3046");
        splitWordReading( "\u601d\u3046", "\u304a\u3082\u3046");
        System.out.println( "\u601d\u3044\u51fa");
        splitWordReading( "\u601d\u3044\u51fa", "\u304a\u3082\u3044\u3067");
        System.out.println( "\u601d\u3044\u51fa\u3059");
        splitWordReading( "\u601d\u3044\u51fa\u3059", "\u304a\u3082\u3044\u3060\u3059");
        System.out.println( "\u3044\u3044\u52a0\u6e1b");
        splitWordReading( "\u3044\u3044\u52a0\u6e1b", "\u3044\u3044\u304b\u3052\u3093");

        System.out.println( "\u75db\u3044");
        splitWordReading( "\u75db\u3044", "\u75db\u3044", "\u3044\u305f\u3044");
        System.out.println( "\u75db\u304f");
        splitWordReading( "\u75db\u304f", "\u75db\u3044", "\u3044\u305f\u3044");
    }
    
    private static void print( String[][] s) {
        for ( int i=0; i<s.length; i++) {
            System.out.print( s[i][0]);
            if (s[i].length > 1)
                System.out.print( "(" + s[i][1] + ")");
            System.out.println();
        }
    }

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

    public static String[][] splitWordReading( String word, String reading) {
        return splitWordReading( word, word, reading);
    }

    public static String[][] splitWordReading( String inflectedWord, String baseWord, String baseReading) {
        System.out.println( "splitting " + inflectedWord + "/" + baseWord + "/" + baseReading);
        List result = new ArrayList( baseWord.length()/2);
        int hStart = 0; // hiragana start
        int hEnd;
        int kStart = 0; // kanji start
        int kStartReading = 0; // kanji start in reading string
        int hStartReading = 0; // hiragana start in reading
        do {
            while (hStart<baseWord.length() && !isHiragana( baseWord.charAt( hStart)))
                hStart++;
            hEnd = hStart + 1;
            while (hEnd<baseWord.length() && isHiragana( baseWord.charAt( hEnd)))
                hEnd++;

            String kanji = baseWord.substring( kStart, hStart);
            if (kanji.length() > 0) {
                if (hStart < baseWord.length()) {
                    // Structure of word is some kanji characters followed by some hiragana characters
                    // followed by some kanji characters. Find hiragana character substring in reading.
                    // Characters before the substring must be reading of first kanji part of word.

                    String hiragana = baseWord.substring( hStart, hEnd);
                    // For every kanji character there must be at least one reading character, so
                    // start search at index kStartReading + kanji.length. The search can still
                    // lead to false results if the hiragana string also appears in the reading
                    // for the kanji.
                    hStartReading = baseReading.indexOf( hiragana, kStartReading + kanji.length());
                    // if this is -1, the string is malformed and an exception will be thrown
                    if (hEnd == baseWord.length()) {
                        // Remainder of word is possibly inflected hiragana.
                        // The inflected form might have a different length from the form
                        // in baseWord, so adjust the length here. This is the last iteration.
                        hEnd = inflectedWord.length();
                    }
                    String kanjiReading = baseReading.substring( kStartReading, hStartReading);
                    result.add( new String[] { kanji, kanjiReading });
                    result.add( new String[] { inflectedWord.substring( hStart, hEnd) });
                    kStartReading = hStartReading + hiragana.length();
                }
                else {
                    // remainder of word string is kanji, remainder of reading string must be
                    // reading for this kanji
                    result.add( new String[] { kanji, baseReading.substring( kStartReading) });
                }
            }
            else if (hStart < baseWord.length()) {
                // Structure of word is hiragana, possibly followed by kanji characters.
                // reading.substring( kStartReading) must begin with the same prefix. This
                // is not tested here.
                if (hEnd == baseWord.length()) {
                    // Remainder of word is (possibly inflected) hiragana.
                    // The inflected form might have a different length from the form
                    // in baseWord, so adjust the length here. This is the last iteration.
                    hEnd = inflectedWord.length();
                }
                String hiragana = inflectedWord.substring( hStart, hEnd);
                result.add( new String[] { hiragana });
                kStartReading += hiragana.length();
            }

            kStart = hEnd;
            hStart = hEnd+1;
        } while (kStart<baseWord.length() && 
                 kStart<inflectedWord.length()); // inflected word might be shorter than base word

        String[][] out = new String[result.size()][];
        out = (String[][]) result.toArray( out);
        print( out);
        return out;
    }
} // class StringTools
