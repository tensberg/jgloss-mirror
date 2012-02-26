/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.parser;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of common methods of the Parser interface.
 *
 * @author Michael Koch
 */
public abstract class AbstractParser implements Parser {
    /**
     * Offset in the array of chars currently parsed.
     */
    protected int parsePosition;
    /**
     * Flag if newlines in a text should be ignored by the parser.
     */
    protected boolean ignoreNewlines;
    /**
     * Set of words excluded from annotation by the user.
     */
    protected Set exclusions;
    /**
     * Set of words already annotated since parser creation or the last call to {@link #reset() reset}.
     * If {@link #firstOccurrenceOnly firstOccurrenceOnly} is set to <CODE>false</CODE>, the variable
     * is set to <CODE>null</CODE>. Derived classes are responsible for adding annotated words to
     * this set.
     */
    protected Set annotatedWords;
    /**
     * Flag if only the first occurrence of a word should be annotated.
     */
    protected boolean firstOccurrenceOnly;

    public AbstractParser( Set exclusions, boolean ignoreNewlines,
                           boolean firstOccurrenceOnly) {
        this.exclusions = exclusions;
        this.ignoreNewlines = ignoreNewlines;
        this.firstOccurrenceOnly = firstOccurrenceOnly;
        if (firstOccurrenceOnly)
            annotatedWords = new HashSet( 101);
    }

    /**
     * Returns the position in the text the parser is currently parsing. This is not threadsafe.
     * If more than one thread is using this Parser object, the result of this method is
     * undetermined.
     *
     * @return The position in the text the parser is currently parsing.
     */
    @Override
	public int getParsePosition() { 
        return parsePosition;
    }

    int mTick;
    
    public int getTick() {
        return mTick;
    }

    public void initTick() {
        mTick = 0;
    }
    
    public void tick(int i) {
        mTick += i;
    }

    /**
     * Clears any caches which may have been filled during parsing. Call this after you have
     * parsed some text to reclaim the memory. This implementation clears the word occurrence
     * cache if needed.
     */
    @Override
	public void reset() {
        if (annotatedWords != null)
            annotatedWords = new HashSet( 101);
    }

    /**
     * Set if the parser should skip newlines in the imported text. This means that characters
     * separated by one or several newline characters will be treated as a single word.
     */
    @Override
	public void setIgnoreNewlines( boolean ignoreNewlines) {
        this.ignoreNewlines = ignoreNewlines;
    }

    /**
     * Test if the parser skips newlines in the imported text.
     */
    @Override
	public boolean getIgnoreNewlines() { return ignoreNewlines; }

    /**
     * Set if only the first occurrence of a word should be annotated. If this is set to
     * <CODE>true</CODE>, an annotated word will be cached and further occurrences will be ignored.
     * The cache of annotated words will be cleared when {@link #reset() reset} is called.
     */
    @Override
	public void setAnnotateFirstOccurrenceOnly( boolean firstOccurrenceOnly) {
        this.firstOccurrenceOnly = firstOccurrenceOnly;
        if (firstOccurrenceOnly) {
            if (annotatedWords==null)
                annotatedWords = new HashSet( 101);
        }
        else
            annotatedWords = null;
    }

    /**
     * Test if only the first occurrence of a word should be annotated.
     */
    @Override
	public boolean getAnnotateFirstOccurrenceOnly() {
        return firstOccurrenceOnly;
    }

    /**
     * Test if the word should not be annotated, either because it appears in the set of ignored
     * words or the set of already annotated words.
     */
    protected boolean ignoreWord( String word) {
        return (exclusions!=null && exclusions.contains( word) ||
                annotatedWords!=null && annotatedWords.contains( word));
    }
} // class Parser
