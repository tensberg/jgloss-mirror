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
 * Look up words from a text in dictionaries and returns a list of
 * reading or translation annotations.
 *
 * @author Michael Koch
 */
public interface Parser {
    /**
     * Describes an annotation for a specific position in the parsed text.
     * Results returned by the parser are instances of this.
     *
     * @author Michael Koch
     */
    public interface TextAnnotation {
        /**
         * Returns the start offset of this annotation in the parsed text. 
         *
         * @return The start offset.
         */
        int getStart();
        /**
         * Returns the length of the annotated text.
         *
         * @return The length of the annotated text.
         */
        int getLength();
    }

    /**
     * Parses the text, returning a list with annotations for words in the text.
     *
     * @param text The text to parse.
     * @return A list with annotations for the text. If no annotations were created, the empty
     *         list will be returned.
     * @exception SearchException If an error occurrs during a dictionary lookup.
     */
    List parse( char[] text) throws SearchException;

    /**
     * Returns the position in the text the parser is currently parsing. This is not threadsafe.
     * If more than one thread is using this Parser object, the result of this method is
     * undetermined.
     *
     * @return The position in the text the parser is currently parsing.
     */
    int getParsePosition();

    /**
     * Clears any caches which may have been filled during parsing. Call this after you have
     * parsed some text to reclaim the memory. This also resets the occurrence cache used when
     * {@link #setAnnotateFirstOccurrenceOnly(boolean) setAnnotateFirstOccurenceOnly} is set to
     * <CODE>true</CODE>.
     */
    void reset();

    /**
     * Set if the parser should skip newlines in the imported text. This means that characters
     * separated by one or several newline characters will be treated as a single word.
     */
    void setIgnoreNewlines( boolean ignoreNewlines);

    /**
     * Test if the parser skips newlines in the imported text.
     */
    boolean getIgnoreNewlines();

    /**
     * Set if only the first occurrence of a word should be annotated. If this is set to
     * <CODE>true</CODE>, an annotated word will be cached and further occurrences will be ignored.
     * The cache of annotated words will be cleared when {@link #reset() reset} is called.
     */
    void setAnnotateFirstOccurrenceOnly( boolean firstOccurrence);

    /**
     * Test if only the first occurrence of a word should be annotated.
     */
    boolean getAnnotateFirstOccurrenceOnly();

    /**
     * Returns the name of the parser in a user-presentable form.
     */
    String getName();

    /**
     * Returns the language which the parser can parse.
     */
    Locale getLanguage();
} // class Parser
