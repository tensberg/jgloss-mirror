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
 * Parser which handles reading annotations contained in the
 * parsed text.
 *
 * @author Michael Koch
 */
public interface ReadingAnnotationParser extends Parser {
    /**
     * Dummy dictionary which is used for Readings constructed from reading anntoations found in
     * the document. This is used to return a descriptive name for the dictionary.
     */
    Dictionary DOCUMENT_DICTIONARY = new Dictionary() {
            private final static String name = 
                ResourceBundle.getBundle( "resources/messages-dictionary")
                .getString( "parser.dictionary.document");
            public String getName() { return "name"; }
            public List search( String expression, short mode) { return null; }
            public void dispose() {}
        };

    /**
     * Sets the character which signals the beginning of a reading annotation for a kanji word.
     */
    void setReadingStart( char readingStart);
    /**
     * Sets the character which signals the end of a reading annotation for a kanji word.
     */
    void setReadingEnd( char readingEnd);

    /**
     * Returns the character which signals the beginning of a reading annotation for a kanji word.
     */
    char getReadingStart();
    /**
     * Returns the character which signals the end of a reading annotation for a kanji word.
     */
    char getReadingEnd();
} // interfacte ReadingAnnotationParser
