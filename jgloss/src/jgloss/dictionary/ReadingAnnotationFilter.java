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
 */

package jgloss.dictionary;

import java.util.*;

/**
 * Filter reading annotations for kanji from a text fragment. Reading annotations placed in a
 * text after a kanji string in parentheses are removed from the text and a reading annotation is
 * added to the list of annotations.
 *
 * @author Michael Koch
 */
public class ReadingAnnotationFilter {
    /**
     * Character which signals the beginning of a reading annotation for a kanji word.
     */
    protected char readingStart;
    /**
     * Character which signals the end of a reading annotation for a kanji word.
     */
    protected char readingEnd;
    /**
     * Character used to separate two adjacent kanji substrings.
     */
    protected char kanjiSeparator;

    /**
     * Dummy dictionary which is used for Readings constructed from reading anntoations found in
     * the document. This is used to return a descriptive name for the dictionary.
     */
    public static final Dictionary DOCUMENT_DICTIONARY = new Dictionary() {
            public final String DOCUMENT_DICTIONARY_NAME = 
                ResourceBundle.getBundle( "resources/messages-dictionary")
                .getString( "parser.dictionary.document");
            public String getName() { return DOCUMENT_DICTIONARY_NAME; }
            public List search( String expression, short mode) { return null; }
            public void dispose() {}
        };

    /**
     * Creates a reading annotation filter for a text which uses the specified characters as
     * delimiters.
     *
     * @param readingStart Character at the beginning of a reading annotation.
     * @param readingEnd Character at the end of a reading annotation.
     * @param kanjiSeparator Character used to separate two adjacent kanji substrings.
     */
    public ReadingAnnotationFilter( char readingStart, char readingEnd, char kanjiSeparator) {
        this.readingStart = readingStart;
        this.readingEnd = readingEnd;
        this.kanjiSeparator = kanjiSeparator;
    }

    /**
     * Filter the reading annotations from a text array. The text without the annotation fragments
     * is returned in a new array, the original text array is not modified. A {@link Reading Reading}
     * annotation object is generated for every reading in the text.
     *
     * @param text Text to filter.
     * @param readings List of reading annotations from the text, with start offsets in the returned array.
     * @return Text without the reading annotations.
     */
    public char[] filter( char[] text, List readings) {
        char[] outtext = new char[text.length];
        int outtextIndex = 0; // current position in outtext
        int kanjiStart = -1; // start index of current kanji substring in text, or -1
        int kanjiOutStart = -1; // start index of current kanji substring in outtext
        int annotationStart = -1; // start index of current reading in text
        boolean inReading = false;
        
        for ( int textIndex = 0; textIndex<text.length; textIndex++) {
            if (inReading) {
                if (text[textIndex] == readingEnd) {
                    // valid reading annotation
                    final String word = new String( text, kanjiStart,
                                                    annotationStart-kanjiStart);
                    final String reading = new String( text, annotationStart+1,
                                                       textIndex-annotationStart-1);
                    if (kanjiStart>0 && text[kanjiStart-1]==kanjiSeparator) {
                        // remove kanji separator char from outtext
                        System.arraycopy( outtext, kanjiOutStart, outtext, kanjiOutStart-1, word.length());
                        outtextIndex--;
                        kanjiOutStart--;
                    }
                    readings.add( new Reading( kanjiOutStart, word.length(), new WordReadingPair() {
                            public String getWord() { return word; }
                            public String getReading() { return reading; }
                            public Dictionary getDictionary() { return DOCUMENT_DICTIONARY; }
                        }));

                    
                    inReading = false;
                    kanjiStart = -1;
                }
                else if (text[textIndex] == '\n') {
                    // annotations are not allowed to span multiple lines. Parse again as normal
                    // text.
                    textIndex = annotationStart - 1; // will be set to annotationStart in next iteration
                    kanjiStart = -1;
                    inReading = false;
                }
            }
            else {
                if (text[textIndex]==readingStart && kanjiStart!=-1) {
                    inReading = true;
                    annotationStart = textIndex;
                    continue; // don't copy readingStart to outtext
                }
                if (StringTools.isKanji( text[textIndex])) {
                    if (kanjiStart == -1) {
                        kanjiStart = textIndex;
                        kanjiOutStart = outtextIndex;
                    }
                }
                else
                    kanjiStart = -1;
                
                // copy all non-annotation chars to outtext
                outtext[outtextIndex++] = text[textIndex];
            }
        }

        // truncate outtext to actual length by copying to new array
        text = new char[outtextIndex];
        System.arraycopy( outtext, 0, text, 0, outtextIndex);
        return text;
    }

    /**
     * Sets the character which signals the beginning of a reading annotation for a kanji word.
     */
    public void setReadingStart( char readingStart) {
        this.readingStart = readingStart;
    }

    /**
     * Sets the character which signals the end of a reading annotation for a kanji word.
     */
    public void setReadingEnd( char readingEnd) {
        this.readingEnd = readingEnd;
    }

    /**
     * Returns the character which signals the beginning of a reading annotation for a kanji word.
     */
    public char getReadingStart() { return readingStart; }
    /**
     * Returns the character which signals the end of a reading annotation for a kanji word.
     */
    public char getReadingEnd() { return readingEnd; }

} // class ReadingAnnotationFilter
