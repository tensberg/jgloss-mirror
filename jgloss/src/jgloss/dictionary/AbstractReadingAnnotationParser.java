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
 * Implementation of common methods of the ReadingAnnotationParser interface.
 *
 * @author Michael Koch
 */
public abstract class AbstractReadingAnnotationParser extends AbstractParser 
    implements ReadingAnnotationParser {
    /**
     * Dummy dictionary which is used for Readings constructed from reading anntoations found in
     * the document. This is used to return a descriptive name for the dictionary.
     */
    public static Dictionary DOCUMENT_DICTIONARY = new Dictionary() {
            public final String DOCUMENT_DICTIONARY_NAME = 
                ResourceBundle.getBundle( "resources/messages-dictionary")
                .getString( "parser.dictionary.document");
            public String getName() { return DOCUMENT_DICTIONARY_NAME; }
            public List search( String expression, short mode) { return null; }
            public void dispose() {}
        };

    /**
     * Character which signals the beginning of a reading annotation for a kanji word.
     */
    protected char readingStart;
    /**
     * Character which signals the end of a reading annotation for a kanji word.
     */
    protected char readingEnd;

    public AbstractReadingAnnotationParser( Dictionary[] dictionaries, Set exclusions,
                                            boolean ignoreNewlines, boolean firstOccurrenceOnly,
                                            char readingStart, char readingEnd) {
        super( dictionaries, exclusions, ignoreNewlines, firstOccurrenceOnly);
        this.readingStart = readingStart;
        this.readingEnd = readingEnd;
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
} // class AbstractReadingAnnotationParser
