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
import java.io.*;

/**
 * Parser implementation which annotates kanji words.
 *
 * @author Michael Koch
 */
public class KanjiParser implements ReadingAnnotationParser {
    public static void main( String args[]) throws Exception {
        Dictionary[] d = new Dictionary[1];
        //d[0] = new EDict( "/usr/share/edict/edict", false);
        d[0] = new EDict( "largedict", false);
        /*d[1] = new EDict( "/usr/share/edict/enamdict", false);
        d[2] = new EDict( "/usr/share/edict/j_places", false);
        d[3] = new EDict( "/usr/share/edict/compverb", false);
        d[4] = new KanjiDic( "/usr/share/edict/kanjidic");*/

        Reader r = new InputStreamReader
            ( new FileInputStream( "/home/michael/japan/karinodouji/KARIDOUJI.txt"),
              "Shift_JIS");
        CharArrayWriter ca = new CharArrayWriter();
        char[] buf = new char[4096];
        int len;
        while ((len=r.read( buf)) > 0) {
            ca.write( buf, 0, len);
        }

        ca.close();
        r.close();
        char[] in = ca.toCharArray();
        
        KanjiParser p = new KanjiParser( d, null);
        p.parse( in);
        p.reset();

        long mean = 0;
        for ( int i=0; i<10; i++) {
            System.out.println( "pass " + i);
            long start = System.currentTimeMillis();
            p.parse( in);
            long t = System.currentTimeMillis() - start;
            mean += t;
            System.out.println( "time " + t);
            System.out.println( "lookups " + p.getLookups());
            System.out.println( "cache hits " + p.getCacheHits());
            p.reset();
        }
        System.out.println( "\nmean " + mean/10);
    }

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
     * Flag if 0x0a and 0x0d characters should be ignored in parsed text.
     */
    private boolean ignoreNewlines;

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

    private final static String PARSER_NAME = 
        ResourceBundle.getBundle( "resources/messages-dictionary")
        .getString( "parser.kanji.name");

    public KanjiParser( Dictionary[] dictionaries) {
        this( dictionaries, null, '\0', '\0', true, false);
    }

    /**
     * Creates a new parser which will use the given dictionaries, use no reading annotation
     * delimiters and will cache dictionary lookups and not ignore newlines.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     */
    public KanjiParser( Dictionary[] dictionaries, Set exclusions) {
        this( dictionaries, exclusions, '\0', '\0', true, false);
    }

    /**
     * Creates a parser which will use the given dictionaries and reading annotation delimiters.
     * Dictionary lookup results will be cached and newlines will not be ignored.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     * @param readingStart Character which signals the beginning of a reading annotation.
     * @param readingEnd Character which signals the end of a reading annotation.
     */
    public KanjiParser( Dictionary[] dictionaries, Set exclusions, char readingStart, char readingEnd) {
        this( dictionaries, exclusions, readingStart, readingEnd, true, false);
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
     * @param ignoreNewlines If this is <CODE>true</CODE>, 0x0a and 0x0d characters in the parsed text
     *                       will be ignored and the character immediately before and after the newline
     *                       will be treated as if forming a single word.
     * @see Reading
     * @see SoftReference
     */
    public KanjiParser( Dictionary[] dictionaries, Set exclusions, char readingStart, char readingEnd,
                   boolean cacheLookups, boolean ignoreNewlines) {
        this.dictionaries = dictionaries;
        this.exclusions = exclusions;
        this.readingStart = readingStart;
        this.readingEnd = readingEnd;
        this.ignoreNewlines = ignoreNewlines;

        if (cacheLookups)
            lookupCache = new HashMap( 5000);
    }

    /**
     * Parses the text, returning a list with annotations for words in the text.
     * <P>
     * The parser will look for a character which is either a kanji or katakana. Then it
     * will search for the first character which is not a kanji/katakana. If this word
     * consists of katakana characters, it will be looked up immediately and the result added
     * to the annotation list. If it is a kanji word, if the following
     * character is the reading annotation start delimiter,  all following characters until the
     * reading annotation end delimiter are treated as reading for the word. 
     * Hiragana characters directly following the word or the reading annotation will be used as possible
     * verb/adjective inflections. The <CODE>findTranslation</CODE> method will then be called
     * with this word/reading/hiragana tuple and the result added to the annotation list.
     * </P>
     *
     * @param text The text to parse.
     * @return A list with annotations for the text. If no annotations were created, the empty
     *         list will be returned.
     * @exception SearchException If an error occurrs during a dictionary lookup.
     * @see #findTranslations(int,String,String,String,boolean,boolean,boolean)
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
        boolean compverb = false;
        for ( int i=0; i<text.length; i++) {
            parsePosition = i; // tell the world where we are in parsing (see getParsePosition())
            if (Thread.currentThread().interrupted())
                throw new ParsingInterruptedException();
            
            if (ignoreNewlines && 
                (text[i]==0x0a || text[i]==0x0d))
                continue;

            ub = StringTools.unicodeBlockOf( text[i]);
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
                                                      reading.toString(), true, true, true));
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
                    text[i] != '\u3005') { // '\u3005' is the kanji repeat mark
                    // end of current word, look for possible inflection and enter new mode
                    if (ub == Character.UnicodeBlock.HIRAGANA) {
                        // catch possible composite verb
                        if (!compverb && mode!=END_READING && word.length()==1 && i<text.length-1 &&
                            StringTools.isCJKUnifiedIdeographs( text[i+1]) &&
                            (i == text.length-2 ||
                             StringTools.isHiragana( text[i+2]))) {
                            compverb = true;
                            // add hiragana char to word
                        }
                        else {
                            // catch possible verb/adjective inflections
                            mode = IN_INFLECTION;
                            inflection = new StringBuffer().append( text[i]);
                        }
                    }
                    else if (text[i]==readingStart && readingStart!='\0') {
                        mode = IN_READING;
                    }
                    else {
                        out.addAll( findTranslations( wordStart, word.toString(),
                                                      reading.toString(), true, true, true));
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
                    List result = findTranslations( wordStart, word.toString(), reading.toString(),
                                                    inflection.toString(), !compverb, !compverb, true);
                    // the tests for the setting of the compverb flag guarantee that the
                    // lookup of a compverb will always happen IN_INFLECTION, and that reading is empty
                    if (compverb) {
                        compverb = false;
                        if (result.size() == 0) {
                            // try first part of compverb
                            result = findTranslations( wordStart, word.substring( 0, 1), null,
                                                       word.substring( 1, 2), false, false, true);
                            out.addAll( result);
                            // reparse last part, which may be the start of a new compverb
                            mode = OUTSIDE;
                            word = new StringBuffer();
                            inflection = new StringBuffer();
                            i = wordStart + 1; // the for loop will set i to wordStart + 2
                            continue;
                        }
                    }
                    out.addAll( result);

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
                    out.addAll( findTranslations( wordStart, word.toString(), null, true, true, true));
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
            out.addAll( findTranslations( wordStart, word.toString(), reading.toString(), true, true,
                                          true));
        else if (mode == IN_INFLECTION) {
            List result = findTranslations( wordStart, word.toString(), reading.toString(),
                                            inflection.toString(), !compverb, !compverb, true);
            if (compverb) {
                if (result.size() == 0) {
                    // try first part of compverb
                    result = findTranslations( wordStart, word.substring( 0, 1), null,
                                               word.substring( 1, 2), false, false, true);
                    out.addAll( result);
                    // try last part
                    result = findTranslations( wordStart+2, word.substring( 2, 3), null,
                                               inflection.toString(), false, false, true);
                }
            }
            out.addAll( result);
        }

        return out;
    }

    public int getParsePosition() { return parsePosition; }

    /**
     * Finds all translations for this word. Verb de-inflection is not used.
     * This is equivalent to calling <CODE>findTranslations</CODE> with a 0 offset, reading and
     * inflection set to <CODE>null</CODE> and without trying prefixes or suffixes.
     *
     * @param word Word to look up.
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @param useExclusions <CODE>true</CODE> if words in the exclusion list should be ignored.
     * @see #findTranslations(int,String,String,String,boolean,boolean,boolean)
     * @see Reading
     * @see Translation
     */
    public List findTranslations( String word, boolean useExclusions) throws SearchException {
        return findTranslations( 0, word, null, null, false, false, useExclusions);
    }

    /**
     * Finds all translations for this word and inflection. Verb de-inflection is not used.
     * This is equivalent to calling <CODE>findTranslations</CODE> with a 0 offset, reading and
     * set to <CODE>null</CODE> and without trying prefixes or suffixes.
     *
     * @param word Word to look up.
     * @param inflection String of hiragana characters which might contain a verb/adjective inflection.
     *                   May be <CODE>null</CODE>.
     * @param useExclusions <CODE>true</CODE> if words in the exclusion list should be ignored.
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see #findTranslations(int,String,String,String,boolean,boolean,boolean)
     * @see Reading
     * @see Translation
     */
    public List findTranslations( String word, String inflection, boolean useExclusions)
        throws SearchException {
        return findTranslations( 0, word, null, inflection, false, false, useExclusions);
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
     * @param useExclusions <CODE>true</CODE> if words in the exclusion list should be ignored.
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see #findTranslations(int,String,String,String,boolean,boolean,boolean)
     * @see Reading
     * @see Translation
     */
    private List findTranslations( int wordStart, String word, String reading,
                                   boolean tryPrefixes, boolean trySuffixes,
                                   boolean useExclusions) throws SearchException {
        return findTranslations( wordStart, word, reading, null, tryPrefixes, trySuffixes,
                                 useExclusions);
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
     *                    repeat the search with the remaining suffix of <CODE>word</CODE>.
     * @param useExclusions <CODE>true</CODE> if words in the exclusion list should be ignored.
     * @return A list of reading and translation annotations for the search word.
     * @exception SearchException If a dictionary lookup failed.
     * @see Reading
     * @see Translation
     * @see Conjugation
     */
    private List findTranslations( int wordStart, String word, final String reading,
                                   String inflection, boolean tryPrefixes,
                                   boolean trySuffixes, boolean useExclusions) throws SearchException {
        List translations = new ArrayList( 6);
        if (reading!=null && reading.length()>0) {
            final String fword = word;
            translations.add( new Reading( wordStart, word.length(), new WordReadingPair() {
                    public String getWord() { return fword; }
                    public String getReading() { return reading; }
                    public Dictionary getDictionary() { return DOCUMENT_DICTIONARY; }
                }));
        }

        Conjugation[] conjugations = null;
        if (inflection != null)
            conjugations = Conjugation.findConjugations( inflection);

        boolean stop = false;
        while (!stop) {
            //System.out.println( "Looking up " + word + ":" + reading + ":" + inflection);
            stop = true;

            boolean match = false;
            // try to find exact match with conjugation
            if (conjugations != null) {
                if (useExclusions && exclusions!=null) {
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
                                break; // don't search for shorter entries
                            }
                        }
                    }
                }
            }
            
            if (!match) {
                // try to find exact match without conjugation
                if (useExclusions && exclusions!=null && exclusions.contains( word)) {
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

            if (!match && tryPrefixes && StringTools.isCJKUnifiedIdeographs( word.charAt( 0)) &&
                word.length()>1) {
                // Still no luck? If this is a kanji compound, try prefixes of the word.
                List[] entries = new List[dictionaries.length];
                int[] lengths = new int[dictionaries.length];
                int maxlength = 0;

                for ( int i=word.length()-1; i>0; i--) {
                    String subword = word.substring( 0, i);
                    for ( int j=0; j<entries.length; j++) {
                        entries[j] = search( dictionaries[j], subword, Dictionary.SEARCH_EXACT_MATCHES);
                        if (entries[j].size() > 0) {
                            maxlength = i;
                            match = true;
                        }
                    }
                    if (match)
                        break;
                }

                if (match) { // match found
                    for ( int i=0; i<dictionaries.length; i++) {
                        for ( Iterator j=entries[i].iterator(); j.hasNext(); ) {
                            WordReadingPair wrp = (WordReadingPair) j.next();
                            if (!(useExclusions && exclusions!=null &&
                                  exclusions.contains( wrp.getWord()))) {
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
                }
                else
                    maxlength = 1; // skip first char and lookup remainder
                    
                // continue search with suffix of word
                if (trySuffixes) {
                    word = word.substring( maxlength);
                    wordStart += maxlength;
                    stop = false;
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
        String key = String.valueOf( mode) + ":" + word + ":" + d.getName();
        //System.out.println( "searching " + key);
        List result;

        if (lookupCache!=null &&
            lookupCache.containsKey( key)) {
            result = (List) lookupCache.get( key);
            cacheHits++;
            return result;
        }

        // if we get here, it was not in the cache
        result = d.search( word, mode);
        if (lookupCache != null) {
            if (result.size() > 0)
                lookupCache.put( key, result);
            else
                lookupCache.put( key, Collections.EMPTY_LIST);
        }

        return result;
    }

    /**
     * Empties the dictionary lookup cache. Call this after you have parsed a long text
     * and want to free some memory.
     * Additionally, this will reset the values of lookups, cacheHits and cacheGarbageCollected
     * to 0.
     */
    public void reset() {
        if (lookupCache != null)
            lookupCache.clear();

        lookups = 0;
        cacheHits = 0;
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

    /**
     * Test if the parser skips newlines in the imported text.
     */
    public boolean getIgnoreNewlines() { return ignoreNewlines; }

    /**
     * Set if the parser should skip newlines in the imported text.
     */
    public void setIgnoreNewlines( boolean ignoreNewlines) {
        this.ignoreNewlines = ignoreNewlines;
    }

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

    public String getName() { return PARSER_NAME; }

    public Locale getLanguage() {
        return Locale.JAPANESE;
    }
} // class Parser
