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
     * Test if c is either in the character class of CJK unified ideographs or is the kanji repeat mark.
     */
    public static boolean isKanji( char c) {
        return (c>=0x4e00 && c<0xa000) || // CJK unified ideographs
            c == '\u3005'; // kanji repeat mark
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

    /**
     * Test if a string contains any kanji characters. The test is done using the 
     * {@link isKanji(char) isKanji} method.
     */
    public static boolean containsKanji( String word) {
        for ( int i=0; i<word.length(); i++) {
            if (isKanji( word.charAt( i)))
                return true;
        }

        return false;
    }

    public static String[][] splitWordReading( String word, String reading) {
        return splitWordReading( word, word, reading);
    }

    /**
     * Split a kanji/kana compound word in kanji and kana parts. Readings are added to the kanji
     * substrings. To decide which reading each kanji substring has, hiragana substrings in the
     * kanji/kana words are searched in the reading string, and the remaining parts are interpreted
     * as reading of the kanji substrings.
     *
     * @param inflectedWord Inflected form of the kanji/kana word. Everything after the last kanji
     *        character is treated as inflected form and added to the output array as last element.
     * @param baseWord Dictionary form of the kanji/kana word.
     * @param baseReading Reading (in hiragana) of the word in base form.
     * @return Array with the word split in kanji/kana substrings. For every kanji substring, a
     *         kanji/reading string pair is contained in the array. For every kana substring,
     *         a single string is contained.
     */
    public static String[][] splitWordReading( String inflectedWord, String baseWord, String baseReading) {
        System.err.println( "splitting " + inflectedWord + "/" + baseWord + "/" + baseReading);
        List result = new ArrayList( baseWord.length()/2);
        int hStart = 0; // hiragana start
        int hEnd; // hiragana end
        int kStart = 0; // kanji start
        int kStartReading = 0; // kanji start in reading string
        int hStartReading = 0; // hiragana start in reading
        do {
            // search start of hiragana substring
            while (hStart<baseWord.length() && !isHiragana( baseWord.charAt( hStart)))
                hStart++;
            hEnd = hStart + 1;
            // search end of hiragana substring
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

    private static void print( String[][] s) {
        for ( int i=0; i<s.length; i++) {
            System.err.print( s[i][0]);
            if (s[i].length > 1)
                System.err.print( "(" + s[i][1] + ")");
            System.err.println();
        }
    }
} // class StringTools
