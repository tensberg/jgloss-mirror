/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.dictionary.UnsupportedSearchModeException;
import jgloss.util.StringTools;
import jgloss.util.UTF8ResourceBundleControl;

/**
 * Parser implementation which annotates kanji words.
 * <P>
 * The parser will look for a character which is either a kanji or katakana. Then it
 * will search for the first character which is not a kanji/katakana. If this word
 * consists of katakana characters, it will be looked up immediately and the result added
 * to the annotation list. If it is a kanji word, if the following
 * character is the reading annotation start delimiter,  all following characters until the
 * reading annotation end delimiter are treated as reading for the word. 
 * Hiragana characters directly following the word or the reading annotation will be used as possible
 * verb/adjective inflections. The <CODE>createAnnotations</CODE> method will then be called
 * with this word/reading/hiragana tuple.
 * </P>
 *
 * @author Michael Koch
 */
public class KanjiParser extends AbstractParser {
    private final Dictionary[] dictionaries;
    /**
     * Cache which stores previously looked-up words.
     */
    private Map<String, Boolean> lookupCache;
    /**
     * Number of dictionary lookups so far.
     */
    private int lookups = 0;
    /**
     * Number of lookup results taken from the cache.
     */
    private int cacheHits = 0;
    private Object[] searchParameters;
    private StringBuilder searchKey;

    private final static String PARSER_NAME = 
        ResourceBundle.getBundle( "messages-parser", new UTF8ResourceBundleControl())
        .getString( "parser.kanji.name");

    /**
     * Creates a new parser which will use the given dictionaries, use no reading annotation
     * delimiters and will cache dictionary lookups and not ignore newlines.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     */
    public KanjiParser( Dictionary[] dictionaries, Set<String> exclusions) {
        this( dictionaries, exclusions, true, false, true);
    }

    /**
     * Creates a new parser which will use the given dictionaries, use no reading annotation
     * delimiters and will cache dictionary lookups and not ignore newlines.
     *
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     */
    public KanjiParser( Dictionary[] dictionaries, Set<String> exclusions, boolean firstOccurrenceOnly) {
        this( dictionaries, exclusions, true, false, firstOccurrenceOnly);
    }

    /**
     * Creates a new parser which will use the given dictionaries.
     * 
     * @param dictionaries The dictionaries used for word lookups.
     * @param exclusions Set of words which should not be annotated. May be <CODE>null</CODE>.
     * @param cacheLookups <CODE>true</CODE> if dictionary lookups should be cached.
     * @param ignoreNewlines If this is <CODE>true</CODE>, 0x0a and 0x0d characters in the parsed text
     *                       will be ignored and the character immediately before and after the newline
     *                       will be treated as if forming a single word.
     */
    public KanjiParser( Dictionary[] dictionaries, Set<String> exclusions,
                        boolean cacheLookups, boolean ignoreNewlines, boolean firstOccurrenceOnly) {
        super( exclusions, ignoreNewlines, firstOccurrenceOnly);
        this.dictionaries = dictionaries;
        if (cacheLookups) {
	        lookupCache = new HashMap<String, Boolean>( 5000);
        }
    }

    /**
     * Parses the text, returning a list with annotations for words in the text.
     *
     * @param text The text to parse.
     * @return A list with annotations for the text. If no annotations were created, the empty
     *         list will be returned.
     * @exception SearchException If an error occurrs during a dictionary lookup.
     */
    @Override
	public List<TextAnnotation> parse( char[] text, int start, int length) throws SearchException {
        int end = start + length;
        List<TextAnnotation> out = new ArrayList<TextAnnotation>( length/3);
        
        final byte OUTSIDE = 0;
        final byte IN_KATAKANA = 1;
        final byte IN_KANJI = 2;
        final byte IN_INFLECTION = 3;
        int mode = OUTSIDE;
        int wordStart = 0;
        StringBuilder word = new StringBuilder();
        StringBuilder inflection = new StringBuilder();
        Character.UnicodeBlock ub;
        boolean compverb = false;
        for ( int i=start; i<end; i++) {
            parsePosition = i; // tell the world where we are in parsing (see getParsePosition())
            if (Thread.interrupted()) {
	            throw new ParsingInterruptedException();
            }
            
            if (ignoreNewlines && 
                (text[i]==0x0a || text[i]==0x0d)) {
	            continue;
            }

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
                    createAnnotations( wordStart, word.toString(),
                                       true, true, out);
                    if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
	                    mode = IN_KANJI;
                    } else {
	                    mode = OUTSIDE;
                    }
                    word.setLength( 0);
                    wordStart = i;
                }
                break;

            case IN_KANJI: // currently in Kanji compound
                if (ub != Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS &&
                    text[i] != '\u3005') { // '\u3005' is the kanji repeat mark
                    // end of current word, look for possible inflection and enter new mode
                    if (ub == Character.UnicodeBlock.HIRAGANA) {
                        // catch possible composite verb
                        if (!compverb && word.length()==1 && i<end-1 &&
                            StringTools.isKanji( text[i+1]) &&
                            (i == end-2 ||
                             StringTools.isHiragana( text[i+2]))) {
                            compverb = true;
                            // add hiragana char to word
                        }
                        else {
                            // catch possible verb/adjective inflections
                            mode = IN_INFLECTION;
                            inflection = new StringBuilder().append( text[i]);
                        }
                    }
                    else {
                        createAnnotations( wordStart, word.toString(),
                                           true, true, out);
                        if (ub == Character.UnicodeBlock.KATAKANA) {
	                        mode = IN_KATAKANA;
                        } else {
	                        mode = OUTSIDE;
                        }
                        word.setLength( 0);
                        wordStart = i;
                    }
                }
                break;
                
            case IN_INFLECTION: // currently in possible inflection
                if (ub != Character.UnicodeBlock.HIRAGANA) {
                    boolean result = createAnnotations( wordStart, word.toString(),
                                                        inflection.toString(), !compverb, !compverb, out);
                    // the tests for the setting of the compverb flag guarantee that the
                    // lookup of a compverb will always happen IN_INFLECTION, and that reading is empty
                    if (compverb) {
                        compverb = false;
                        if (!result) {
                            // try first part of compverb
                            createAnnotations( wordStart, word.substring( 0, 1),
                                               word.substring( 1, 2), false, false, out);
                            // reparse last part, which may be the start of a new compverb
                            mode = OUTSIDE;
                            word = new StringBuilder();
                            inflection = new StringBuilder();
                            i = wordStart + 1; // the for loop will set i to wordStart + 2
                            continue;
                        }
                    }

                    if (ub == Character.UnicodeBlock.KATAKANA) {
	                    mode = IN_KATAKANA;
                    } else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
	                    mode = IN_KANJI;
                    } else {
	                    mode = OUTSIDE;
                    }
                    wordStart = i;
                    word.setLength( 0);
                } else {
	                inflection.append( text[i]);
                }
                break;
            }
            
            if (mode==IN_KANJI || mode==IN_KATAKANA) {
                word.append( text[i]);
            }
        }
        
        // look up last word in buffer
        if (mode==IN_KATAKANA || mode==IN_KANJI) {
	        createAnnotations( wordStart, word.toString(), true, true, out);
        } else if (mode == IN_INFLECTION) {
            boolean result = createAnnotations( wordStart, word.toString(),
                                                inflection.toString(), !compverb, !compverb, out);
            if (compverb) {
                if (!result) {
                    // try first part of compverb
                    createAnnotations( wordStart, word.substring( 0, 1),
                                       word.substring( 1, 2), false, false, out);
                    // try last part
                    createAnnotations( wordStart+2, word.substring( 2, 3),
                                       inflection.toString(), false, false, out);
                }
            }
        }

        return out;
    }

    private boolean createAnnotations( int wordStart, String word, boolean tryPrefixes, 
                                       boolean trySuffixes, List<TextAnnotation> out) throws SearchException {
        return createAnnotations( wordStart, word, null, tryPrefixes, trySuffixes, out);
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
     * @param inflection String of hiragana characters which might contain a verb/adjective inflection.
     *                   May be <CODE>null</CODE>.
     * @param tryPrefixes If no exact match is found, try prefixes of the word.
     * @param trySuffixes If <CODE>tryPrefixes</CODE> is <CODE>true</CODE> and a prefix is found,
     *                    repeat the search with the remaining suffix of <CODE>word</CODE>.
     * @return <code>true</code> if at least one annotation was generated or part of the word
     *         matched the exclusion list.
     * @exception SearchException If a dictionary lookup failed.
     */
    private boolean createAnnotations( int wordStart, String word,
                                       String inflection, boolean tryPrefixes,
                                       boolean trySuffixes, List<TextAnnotation> annotations) throws SearchException {
        boolean result = false;

        Conjugation[] conjugations = null;
        if (inflection != null) {
	        conjugations = Conjugation.findConjugations( inflection);
        }

        boolean stop = false;
        while (!stop) {
            stop = true;

            // try to find exact match with conjugation
            if (conjugations != null) {
                for (Conjugation conjugation : conjugations) {
                    if (ignoreWord( word + conjugation.getDictionaryForm())) {
                        return true;
                    }
                }
                
                for (Dictionary dictionary : dictionaries) {
                    for (Conjugation conjugation : conjugations) {
                        String dictionaryWord = word +
                            conjugation.getDictionaryForm();
                        if (hasMatch( dictionary, dictionaryWord)) {
                            String conjugatedForm = word +
                                conjugation.getConjugatedForm();
                            annotations.add( new TextAnnotation
                                             ( wordStart, word.length() + 
                                               conjugatedForm.length(),
                                               null, dictionaryWord, null, conjugation.getType()));
                            if (firstOccurrenceOnly) {
	                            annotatedWords.add( dictionaryWord);
                            }
                            return true;
                        }
                    }
                }
            }
            
            // try to find exact match without conjugation
            if (ignoreWord( word)) {
                return true;
            }
            else {
                for (Dictionary dictionary : dictionaries) {
                    if (hasMatch( dictionary, word)) {
                        annotations.add( new TextAnnotation
                                         ( wordStart, word.length(), word));
                        if (firstOccurrenceOnly) {
	                        annotatedWords.add( word);
                        }
                        return true;
                    }
                }
            }

            // Still no luck? If this is a kanji compound, try prefixes of the word.
            if (tryPrefixes && StringTools.isKanji( word.charAt( 0)) &&
                word.length()>1) {
                int matchlength; // length of the prefix
                String subword = null;

                for ( matchlength=word.length()-1; matchlength>0; matchlength--) {
                    subword = word.substring( 0, matchlength);
                    if (ignoreWord( subword)) {
	                    break;
                    }

                    for (Dictionary dictionarie : dictionaries) {
                        if (hasMatch( dictionarie, subword)) {
	                        break;
                        }
                    }
                }

                if (matchlength > 0) { // match found
                    if (!ignoreWord( subword)) {
                        if (firstOccurrenceOnly) {
	                        annotatedWords.add( subword);
                        }
                        annotations.add( new TextAnnotation
                                         ( wordStart, matchlength, subword));
                    }
                    result = true;
                }
				else {
	                matchlength = 1; // skip first char and look up remainder
                }
                    
                // continue search with suffix of word
                if (trySuffixes) {
                    word = word.substring( matchlength);
                    wordStart += matchlength;
                    stop = false;
                }
            }
        }

        return result;
    }

    /**
     * Test if the word can be found in the dictionary using exact match search.
     */
    private boolean hasMatch( Dictionary d, String word) throws SearchException {
        lookups++;
        if (searchKey == null) {
	        searchKey = new StringBuilder( 128);
        }
        searchKey.setLength( 0);
        searchKey.append( word);
        searchKey.append( ':');
        searchKey.append( d.getName());
        String key = searchKey.toString();

        if (lookupCache!=null &&
            lookupCache.containsKey( key)) {
            boolean result = lookupCache.get( key).booleanValue();
            cacheHits++;
            return result;
        }

        // if we get here, it was not in the cache
        if (searchParameters == null) {
            searchParameters = new Object[2];
            searchParameters[1] = new SearchFieldSelection( true, true, false, true, false);
        }

        searchParameters[0] = word;
        boolean result = false;
        try {
            result = d.search( ExpressionSearchModes.EXACT, searchParameters).hasNext();
        } catch (UnsupportedSearchModeException ex) {}

        if (lookupCache != null) {
            lookupCache.put( key, Boolean.valueOf( result));
        }

        return result;
    }

    /**
     * Clears the lookup cache.
     */
    @Override
	public void reset() {
        if (lookupCache != null) {
	        lookupCache.clear();
        }

        lookups = 0;
        cacheHits = 0;
        super.reset();
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

    @Override
	public String getName() { return PARSER_NAME; }

    @Override
	public Locale getLanguage() {
        return Locale.JAPANESE;
    }
} // class Parser
