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

import jgloss.JGloss;

import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Look up words from a text in dictionaries and returns a list of
 * reading or translation annotations.
 *
 * @author Michael Koch
 */
public class Parser {
    /**
     * Thrown if the parsing thread is interrupted by calling its <CODE>interrupt</CODE>
     * method.
     *
     * @author Michael Koch
     * @see java.lang.Thread#interrupt()
     */
    public static class ParsingInterruptedException extends SearchException {
        /**
         * Creates a new exception without a description.
         */
        public ParsingInterruptedException() {
            super();
        }

        /**
         * Creates a new exception with the given message.
         *
         * @param message A description of this exception.
         */
        public ParsingInterruptedException( String message) {
            super( message);
        }
    } // class ParsingInterruptedException

    /**
     * Set of words which should not be annotated.
     */
    private Set exclusions;
    /**
     * The current offset in the parsed text.
     */
    private int parsePosition;
    /**
     * Cache which stores previously looked-up words.
     */
    private Map lookupCache;
    /**
     * Number of dictionary lookups so far.
     */
    private int lookups = 0;
    /**
     * Number of lookup results taken from the cache.
     */
    private int cacheHits = 0;
    /**
     * Number of lookup results found in the cache but already garbage collected.
     */
    private int cacheGarbageCollected = 0;

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
     * Array of dictionaries the parser uses for word lookups.
     */
    private Dictionary[] dictionaries;
    /**
     * Character which signals the beginning of a reading annotation for a kanji word.
     */
    private char readingStart;
    /**
     * Character which signals the end of a reading annotation for a kanji word.
     */
    private char readingEnd;

    /**
     * Dummy dictionary which is used for Readings constructed from reading anntoations found in
     * the document. This is used to return a useful name for the dictionary.
     */
    private static final Dictionary documentDictionary = new Dictionary()  {
            public String getName() { return JGloss.messages.getString( "parser.dictionary.document"); }
            public List search( String expression, short mode) { return null; }
            public void dispose() {}
        };

    /**
     * Creates a new parser which will use the given dictionaries, use no reading annotation
     * delimiters and will cache dictionary lookups.
     *
     * @param dictionaries The dictionaries used for word lookups.
     */
    public Parser( Dictionary[] dictionaries) {
        this( dictionaries, null, '\0', '\0', true);
    }

    /**
     * Creates a new parser which will use the given dictionaries, use no reading annotation
     * delimiters and will cache dictionary lookups.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     */
    public Parser( Dictionary[] dictionaries, Set exclusions) {
        this( dictionaries, exclusions, '\0', '\0', true);
    }

    /**
     * Creates a new parser which will use the given dictionaries and use no reading annotation
     * delimiters.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     * @param cacheLookups <CODE>true</CODE> if dictionary lookups should be cached.
     */
    public Parser( Dictionary[] dictionaries, Set exclusions, boolean cacheLookups) {
        this( dictionaries, exclusions, '\0', '\0', cacheLookups);
    }

    /**
     * Creates a parser which will use the given dictionaries and reading annotation delimiters.
     * Dictionary lookup results will be cached.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     * @param readingStart Character which signals the beginning of a reading annotation.
     * @param readingEnd Character which signals the end of a reading annotation.
     */
    public Parser( Dictionary[] dictionaries, Set exclusions, char readingStart, char readingEnd) {
        this( dictionaries, exclusions, readingStart, readingEnd, true);
    }

    /**
     * Creates a new parser which will use the given dictionaries.<BR>
     * The characters
     * <CODE>readingStart</CODE> and <CODE>readingEnd</CODE> will be used to delimit
     * reading annotations for kanji words. That is, if after a kanji word <CODE>readingStart</CODE>
     * is encountered, the text to <CODE>readingEnd</CODE> will be used to create a
     * <CODE>Reading</CODE> annotation for the kanji word.<BR>
     * If a cache is to be used, all dictionary lookup results will be stored in a <CODE>TreeMap</CODE>,
     * with the entries stored in a <CODE>SoftReference</CODE>. This means that the cache entries
     * can be garbage collected at any time, so the memory overhead of the cache should not be
     * problematic.
     * 
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     * @param readingStart Character which signals the beginning of a reading annotation.
     * @param readingEnd Character which signals the end of a reading annotation.
     * @param cacheLookups <CODE>true</CODE> if dictionary lookups should be cached.
     * @see Reading
     * @see SoftReference
     */
    public Parser( Dictionary[] dictionaries, Set exclusions, char readingStart, char readingEnd,
                   boolean cacheLookups) {
        this.dictionaries = dictionaries;
        this.exclusions = exclusions;
        this.readingStart = readingStart;
        this.readingEnd = readingEnd;

        if (cacheLookups)
            lookupCache = new TreeMap();
    }

    /**
     * Parses the text, returning a list with annotations for words in the text.
     * <P>
     * The parser will look for a character which is either a kanji or katakana. Then it
     * will search for the first character which is not a kanji/katakana. If this word
     * consists of katakana characters, it will be looked up immediately and the result added
     * to the annotation list. If it is a kanji word, if the following
     * character is the reading annotation start delimiter,  all following characters until the
     * reading annotation end delimiter are treated as reading for the word. Hiragana
     * characters directly following the word or the reading annotation will be used as possible
     * verb/adjective inflections. The <CODE>findTranslation</CODE> method will then be called
     * with this word/reading/hiragana pair and the result added to the annotation list.
     * </P>
     *
     * @param text The text to parse.
     * @return A list with annotations for the text. If no annotations were created, the empty
     *         list will be returned.
     * @exception SearchException If an error occurrs during a dictionary lookup.
     * @see #findTranslations(int,String,String,String,boolean,boolean)
     */
    public List parse( String text) throws SearchException {
        return parse( text.toCharArray());
    }

    /**
     * Parses the text, returning a list with annotations for words in the text.
     * <P>
     * The parser will look for a character which is either a kanji or katakana. Then it
     * will search for the first character which is not a kanji/katakana. If this word
     * consists of katakana characters, it will be looked up immediately and the result added
     * to the annotation list. If it is a kanji word, if the following
     * character is the reading annotation start delimiter,  all following characters until the
     * reading annotation end delimiter are treated as reading for the word. Hiragana
     * characters directly following the word or the reading annotation will be used as possible
     * verb/adjective inflections. The <CODE>findTranslation</CODE> method will then be called
     * with this word/reading/hiragana pair and the result added to the annotation list.
     * </P>
     *
     * @param text The text to parse.
     * @return A list with annotations for the text. If no annotations were created, the empty
     *         list will be returned.
     * @exception SearchException If an error occurrs during a dictionary lookup.
     * @see #findTranslations(int,String,String,String,boolean,boolean)
     */
    public List parse( char[] text) throws SearchException {
        List out = new ArrayList( text.length/3);

        final byte OUTSIDE = 0;
        final byte IN_KATAKANA = 1;
        final byte IN_KANJI = 2;
        final byte IN_INFLECTION = 3;
        final byte IN_READING = 4;
        final byte END_READING = 5; // reading end delimiter was encoutered
        int mode = OUTSIDE;
        int wordStart = 0;
        StringBuffer word = new StringBuffer();
        StringBuffer reading = new StringBuffer();
        StringBuffer inflection = new StringBuffer();
        Character.UnicodeBlock ub;
        for ( int i=0; i<text.length; i++) {
            parsePosition = i; // tell the world where we are in parsing (see getParsePosition())
            if (Thread.currentThread().interrupted())
                throw new ParsingInterruptedException();

            ub = Character.UnicodeBlock.of( text[i]);
            switch (mode) {
            case OUTSIDE:
                if (ub == Character.UnicodeBlock.KATAKANA) {
                    mode = IN_KATAKANA;
                }
                else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                    mode = IN_KANJI;
                }
                wordStart = i;
                break;     
                
            case IN_KATAKANA: // currently in Katakana word
                if (ub != Character.UnicodeBlock.KATAKANA) {
                    if (readingStart!='\0' && text[i]==readingStart) {
                        mode = IN_READING;
                    }
                    else {
                        out.addAll( findTranslations( wordStart, word.toString(),
                                                      reading.toString(), true, true));
                        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                            mode = IN_KANJI;
                        else
                            mode = OUTSIDE;
                        word = new StringBuffer();
                        reading = new StringBuffer();
                        wordStart = i;
                    }
                }
                break;

            case END_READING: // previous character ended reading block, treat as if kanji ended
            case IN_KANJI: // currently in Kanji compound
                if (ub != Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS &&
                    text[i] != '\u3005') { // '\u3005' is the repeat mark
                    // end of current word, look for possible inflection and enter new mode
                    if (ub == Character.UnicodeBlock.HIRAGANA) { 
                        // catch possible verb/adjective inflections
                        mode = IN_INFLECTION;
                        inflection = new StringBuffer().append( text[i]);
                    }
                    else if (text[i]==readingStart && readingStart!='\0') {
                        mode = IN_READING;
                    }
                    else {
                        out.addAll( findTranslations( wordStart, word.toString(),
                                                      reading.toString(), true, true));
                        if (ub == Character.UnicodeBlock.KATAKANA)
                            mode = IN_KATAKANA;
                        else
                            mode = OUTSIDE;
                        word = new StringBuffer();
                        reading = new StringBuffer();
                        wordStart = i;
                    }
                }
                break;

            case IN_INFLECTION: // currently in possible inflection
                if (ub != Character.UnicodeBlock.HIRAGANA) {
                    out.addAll( findTranslations( wordStart, word.toString(), 
                                                  reading.toString(), inflection.toString(), true, true));
                    if (ub == Character.UnicodeBlock.KATAKANA)
                        mode = IN_KATAKANA;
                    else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                        mode = IN_KANJI;
                    else
                        mode = OUTSIDE;
                    wordStart = i;
                    word = new StringBuffer();
                    reading = new StringBuffer();
                }
                else
                    inflection.append( text[i]);
                break;

            case IN_READING: // currently in reading annotation
                if (text[i]=='\n' || (i==text.length-1 && text[i] != readingEnd)) {
                    // Don't allow readings to span multiple lines to avoid runaways when
                    // this is not really a reading. Ignore the text in the "reading" string buffer. 
                    out.addAll( findTranslations( wordStart, word.toString(), null, true, true));
                    // "unread" the string that is not really a reading and try to parse the
                    // content of it
                    i -= reading.length();
                    word = new StringBuffer();
                    reading = new StringBuffer();
                    mode = OUTSIDE;
                } 
                else if (text[i] != readingEnd) {
                    reading.append( text[i]);
                }
                else
                    mode = END_READING;
            }

            if (mode==IN_KANJI || mode==IN_KATAKANA) {
                word.append( text[i]);
            }
        }

        // look up last word in buffer
        if (mode==IN_KATAKANA || mode==IN_KANJI || mode==END_READING)
            out.addAll( findTranslations( wordStart, word.toString(), reading.toString(), true, true));
        else if (mode == IN_INFLECTION)
            out.addAll( findTranslations( wordStart, word.toString(), reading.toString(), 
                                          inflection.toString(), true, true));

        return out;
    }

    /**
     * Returns the position in the text the parser is currently parsing. This is not threadsafe.
     * If more than one thread is using this Parser object, the result of this method is
     * indeterministic.
     *
     * @return The position in the text the parser is currently parsing.
     */
    public int getParsePosition() { return parsePosition; }

    /**
     * Finds all translations for this word. Verb de-inflection is not used.
     * This is equivalent to calling <CODE>findTranslations</CODE> with a 0 offset, reading and
     * inflection set to <CODE>null</CODE> and without trying prefixes or suffixes.
     *
     * @param word Word to look up.
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see #findTranslations(int,String,String,String,boolean,boolean)
     * @see Reading
     * @see Translation
     */
    public List findTranslations( String word) throws SearchException {
        return findTranslations( 0, word, null, null, false, false);
    }

    /**
     * Finds all translations for this word and inflection. Verb de-inflection is not used.
     * This is equivalent to calling <CODE>findTranslations</CODE> with a 0 offset, reading and
     * set to <CODE>null</CODE> and without trying prefixes or suffixes.
     *
     * @param word Word to look up.
     * @param inflection String of hiragana characters which might contain a verb/adjective inflection.
     *                   May be <CODE>null</CODE>.
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see #findTranslations(int,String,String,String,boolean,boolean)
     * @see Reading
     * @see Translation
     */
    public List findTranslations( String word, String inflection) throws SearchException {
        return findTranslations( 0, word, null, inflection, false, false);
    }

    /**
     * Finds all translations for a word. This is equivalent to calling 
     * <CODE>findTranslations</CODE> with inflection set to <CODE>null</CODE>
     *
     * @param wordStart Offset of the search word in the text containing word. 
     * @param word The word to look up.
     * @param reading A reading annotation for the word. May be <CODE>null</CODE>. If not <CODE>null</CODE>,
     *             a <CODE>Reading</CODE> object will be created for the reading and made the first entry
     *             of the returned list.
     * @param inflection String of hiragana characters which might contain a verb/adjective inflection.
     *                   May be <CODE>null</CODE>.
     * @param tryPrefixes If no exact match is found, try prefixes of the word.
     * @param trySuffixes If <CODE>tryPrefixes</CODE> is <CODE>true</CODE> and a prefix is found,
     *                    repeat the search with the remaining suffix of <CODE>word</CODE>
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see #findTranslations(int,String,String,String,boolean,boolean)
     * @see Reading
     * @see Translation
     */
    private List findTranslations( int wordStart, String word, String reading,
                                   boolean tryPrefixes,
                                   boolean trySuffixes) throws SearchException {
        return findTranslations( wordStart, word, reading, null, tryPrefixes, trySuffixes);
    }

    /**
     * Finds all translations for a word. 
     * <P>If <CODE>inflections</CODE> is not <CODE>null</CODE>, this will first look up
     * all conjugations with an inflected form which is a prefix of <CODE>inflection</CODE>
     * (this includes the plain form). The dictionaries will then be searched for words
     * consisting of <CODE>word</CODE> plus the dictionary form of every conjugation found.
     * </P><P>
     * If no matches were found, an exact match search of <CODE>word</CODE> is done for all
     * dictionaries. If this again leads to no result, <CODE>word</CODE> begins with a kanji
     * and <CODE>tryPrefixes</CODE> is true, the dictionaries are searched for prefixes of
     * <CODE>word</CODE>, and the longest prefixes used as results. If such a prefix
     * is found and <CODE>trySuffixes</CODE>
     * is <CODE>true</CODE>, the whole search process is then repeated with <CODE>word</CODE>
     * without the matching prefix.
     * </P><P>
     * The search results will be returned as a list of <CODE>Reading</CODE> and <CODE>Translation</CODE>
     * objects.
     * </P>
     *
     * @param wordStart Offset of the search word in the text containing word. 
     * @param word The word to look up.
     * @param reading A reading annotation for the word. May be <CODE>null</CODE>. If not <CODE>null</CODE>,
     *             a <CODE>Reading</CODE> object will be created for the reading and made the first entry
     *             of the returned list.
     * @param inflection String of hiragana characters which might contain a verb/adjective inflection.
     *                   May be <CODE>null</CODE>.
     * @param tryPrefixes If no exact match is found, try prefixes of the word.
     * @param trySuffixes If <CODE>tryPrefixes</CODE> is <CODE>true</CODE> and a prefix is found,
     *                    repeat the search with the remaining suffix of <CODE>word</CODE>
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see Reading
     * @see Translation
     * @see Conjugation
     */
    private List findTranslations( int wordStart, String word, final String reading,
                                   String inflection, boolean tryPrefixes,
                                   boolean trySuffixes) throws SearchException {
        //System.out.println( "Looking up " + word + ":" + reading + ":" + inflection);
        List translations = new ArrayList( 6);
        if (reading!=null && reading.length()>0) {
            final String fword = word;
            translations.add( new Reading( wordStart, word.length(), new WordReadingPair() {
                    public String getWord() { return fword; }
                    public String getReading() { return reading; }
                    public Dictionary getDictionary() { return documentDictionary; }
                }));
        }

        Conjugation[] conjugations = null;
        if (inflection != null)
            conjugations = Conjugation.findConjugations( inflection);

        boolean stop = false;
        while (!stop) {
            stop = true;

            boolean match = false;
            // try to find exact match with conjugation
            if (conjugations != null) {
                if (exclusions != null) {
                    for ( int i=0; i<conjugations.length; i++) {
                        if (exclusions.contains( word + conjugations[i].getDictionaryForm())) {
                            match = true;
                            break;
                        }
                    }
                }
                
                if (!match) { // no exclusion found
                    for ( int j=0; j<dictionaries.length; j++) {
                        for ( int i=0; i<conjugations.length; i++) {
                            List t = search( dictionaries[j], word + conjugations[i].getDictionaryForm(),
                                             Dictionary.SEARCH_EXACT_MATCHES);
                            if (t.size() > 0) {
                                match = true;
                                for ( Iterator k=t.iterator(); k.hasNext(); ) {
                                    WordReadingPair wrp = (WordReadingPair) k.next();
                                    if (wrp instanceof DictionaryEntry) {
                                        translations.add( new Translation
                                            ( wordStart, word.length() + conjugations[i]
                                              .getConjugatedForm().length(),
                                              (DictionaryEntry) wrp, conjugations[i]));
                                    }
                                    else {
                                        translations.add( new Reading
                                            ( wordStart, word.length() + conjugations[i]
                                              .getConjugatedForm().length(),
                                              wrp, conjugations[i]));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (!match) {
                // try to find exact match without conjugation
                if (exclusions!=null && exclusions.contains( word)) {
                    match = true;
                }
                else {
                    for ( int i=0; i<dictionaries.length; i++) {
                        List t = search( dictionaries[i], word, Dictionary.SEARCH_EXACT_MATCHES);
                        if (t.size() > 0) {
                            match = true;
                            for ( Iterator k=t.iterator(); k.hasNext(); ) {
                                WordReadingPair wrp = (WordReadingPair) k.next();
                                if (wrp instanceof DictionaryEntry) {
                                    translations.add( new Translation( wordStart, word.length(),
                                                                       (DictionaryEntry) wrp));
                                }
                                else {
                                    translations.add( new Reading( wordStart, word.length(), wrp));
                                }
                            }
                        }
                    }
                }
            }

            if (!match && tryPrefixes && Character.UnicodeBlock.of( word.charAt( 0)) ==
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS && word.length()>1) {
                // Still no luck? If this is a kanji compound, try prefixes of the word.
                List[] entries = new List[dictionaries.length];
                int[] lengths = new int[dictionaries.length];
                int maxlength = 0;

                if (exclusions != null) {
                    // test if one of the prefixes is in the exclusion list
                    for ( int i=word.length()-1; i>0; i--) {
                        if (exclusions.contains( word.substring( 0, i))) {
                            maxlength = i;
                            break;
                        }
                    }
                }
                if (maxlength == 0) { // no exclusion found
                    for ( int i=0; i<dictionaries.length; i++) {
                        List r = search( dictionaries[i], word.substring( 0, 1), 
                                         Dictionary.SEARCH_STARTS_WITH);
                        entries[i] = new LinkedList();
                        for ( Iterator j=r.iterator(); j.hasNext(); ) {
                            WordReadingPair wrp = (WordReadingPair) j.next();
                            String wrpword = wrp.getWord();
                            if (wrpword.length() >= lengths[i]) {
                                if (word.startsWith( wrpword)) {
                                    if (wrpword.length() > lengths[i]) {
                                        // we have a new longest match for this dictionary
                                        // throw previous matches away
                                        lengths[i] = wrpword.length();
                                        maxlength = Math.max( wrpword.length(), maxlength);
                                        entries[i].clear();
                                    }
                                    
                                    entries[i].add( wrp);
                                }
                            }
                        }
                    }
                }

                if (maxlength > 0) { // match found
                    for ( int i=0; i<dictionaries.length; i++) {
                        if (entries[i]!=null && lengths[i]==maxlength) {
                            for ( Iterator j=entries[i].iterator(); j.hasNext(); ) {
                                WordReadingPair wrp = (WordReadingPair) j.next();
                                if (wrp instanceof DictionaryEntry) {
                                    translations.add( new Translation( wordStart, maxlength,
                                                                       (DictionaryEntry) wrp));
                                }
                                else {
                                    translations.add( new Reading( wordStart, maxlength, wrp));
                                }
                            }
                        }
                    }
                    
                    // continue search with suffix of word
                    if (trySuffixes) {
                        word = word.substring( maxlength);
                        wordStart += maxlength;
                        stop = false;
                    }
                }
            }
        }

        return translations;
    }

    /**
     * Look up an entry in a dictionary. If a cache is used, it will be looked up there first, and
     * search results will be stored in the cache.
     *
     * @param d Dictionary to use for the lookup.
     * @param word Word to look up.
     * @param mode One of the valid search modes defined in <CODE>Dictionary</CODE>.
     * @return The result of the lookup.
     * @exception SearchException if the lookup fails.
     * @see Dictionary
     */
    private List search( Dictionary d, String word, short mode) throws SearchException {
        lookups++;
        // put the name of the dictionary last to make string comparisons for the TreeMap short
        String key = String.valueOf( mode) + ":" + word + ":" + d.getName();
        List result;

        if (lookupCache!=null &&
            lookupCache.containsKey( key)) {
            SoftReference r = (SoftReference) lookupCache.get( key);
            if (r == null) { // previous search found nothing
                cacheHits++;
                return Collections.EMPTY_LIST;
            }
            result = (List) r.get();
            if (result == null) // search result was garbage collected
                cacheGarbageCollected++;
            else {
                cacheHits++;
                return result;
            }
        }

        // if we get to here, it was not in the cache
        result = d.search( word, mode);
        if (lookupCache != null) {
            if (result.size() > 0) {
                lookupCache.put( key, new SoftReference( result));
            }
            else {
                // make misses so that they will not be garbage collected, since they
                // don't need much space.
                lookupCache.put( key, null);
            }
        }

        return result;
    }

    /**
     * Empties the dictionary lookup cache. Call this after you have parsed a long text
     * and want to free some memory.<BR>
     * Additionally, this will reset the values of lookups, cacheHits and cacheGarbageCollected
     * to 0.
     */
    public void clearCache() {
        if (lookupCache != null)
            lookupCache.clear();

        lookups = 0;
        cacheHits = 0;
        cacheGarbageCollected = 0;
    }

    /**
     * Returns the character which signals the beginning of a reading annotation for a kanji word.
     */
    public char getReadingStart() { return readingStart; }
    /**
     * Returns the character which signals the end of a reading annotation for a kanji word.
     */
    public char getReadingEnd() { return readingEnd; }

    /**
     * Returns the number of dictionary lookups.
     *
     * @return Number of dictionary lookups.
     */
    public int getLookups() { return lookups; }
    /**
     * Returns the number of dictionary lookups where the result was found in the
     * lookup cache.
     *
     * @return The number of lookup cache hits.
     */
    public int getCacheHits() { return cacheHits; }
    /**
     * Returns the number of lookups where the result was stored in the cache but
     * already garbage collected by the time of the new lookup.
     *
     * @return The number of cache misses due to garbage collection.
     */
    public int getGarbageCollected() { return cacheGarbageCollected; }
} // class Parser
