/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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
import java.io.*;

/**
 * Parses Japanese text using the ChaSen morphological analyzer program.
 * ChaSen must be installed to use this parser. The ChaSen homepage is located at
 * <a href="http://chasen.aist-nara.ac.jp/">http://chasen.aist-nara.ac.jp/</a>.
 *
 * @author Michael Koch
 */
public class ChasenParser extends AbstractParser {
    private final static ResourceBundle messages =
        ResourceBundle.getBundle( "resources/messages-dictionary");

    private final static String PARSER_NAME = 
        messages.getString( "parser.chasen.name");

    /**
     * Command line parameter passed to chasen. Detemines the output format.
     */
    private final static String CHASEN_ARGS = "-F %m\\t%H\\t%Tn\\t%Fn\\t%?T/%M/n/\\t%Y1\\n";

    /**
     * Dummy dictionary which is used for Readings returned by chasen.
     */
    public static Dictionary DOCUMENT_DICTIONARY = new NullDictionary( PARSER_NAME);

    /**
     * Implementation of WordReadingPair which returns the <CODE>DOCUMENT_DICTIONARY</CODE> as
     * dictionary.
     */
    private static class ChasenWordReading implements WordReadingPair {
        private String word;
        private String reading;
        public ChasenWordReading( String word, String reading) {
            this.word = word;
            if (reading!=null && reading.equals( word))
                reading = null; // allowed per definition of WordReadingPair
            this.reading = reading;
        }

        public String getWord() { return word; }
        public String getReading() { return reading; }
        public Dictionary getDictionary() { return DOCUMENT_DICTIONARY; }
    } // class ChasenWordReading
    
    /**
     * Chasen instance used to parse text.
     */
    private Chasen chasen;
    /**
     * Path to the chasen executable, or <code>null</code> if the default executable is to be used.
     */
    private String chasenExecutable;
    /**
     * Cache of words looked up in the dictionaries.
     */
    private Map lookupCache;

    public ChasenParser( Dictionary[] dictionaries, Set exclusions) {
        this( null, dictionaries, exclusions, true, false);
    }

    public ChasenParser( Dictionary[] dictionaries, Set exclusions, boolean cacheLookups) {
        this( null, dictionaries, exclusions, cacheLookups, false);
    }

    public ChasenParser( String chasenExecutable, Dictionary[] dictionaries, Set exclusions) {
        this( chasenExecutable, dictionaries, exclusions, true, false);
    }

    public ChasenParser( String chasenExecutable, Dictionary[] dictionaries, Set exclusions,
                         boolean cacheLookups, boolean firstOccurrenceOnly) {
        super( dictionaries, exclusions, false, firstOccurrenceOnly);
        this.chasenExecutable = chasenExecutable;
        if (cacheLookups)
            lookupCache = new HashMap( 5000);
    }

    public String getName() { return PARSER_NAME; }

    public Locale getLanguage() { return Locale.JAPANESE; }

    /**
     * Parses the text, returning a list with annotations for words in the text.
     *
     * @param text The text to parse.
     * @return A list with annotations for the text. If no annotations were created, the empty
     *         list will be returned.
     * @exception SearchException If an error occurrs during a dictionary lookup.
     */
    public synchronized List parse( char[] text) throws SearchException {
        parsePosition = 0;
        List annotations = new ArrayList( text.length/3);

        try {
            if (chasen == null) {
                // start chasen process
                if (chasenExecutable != null)
                    chasen = new Chasen( chasenExecutable, CHASEN_ARGS, '\t');
                else // use default executable
                    chasen = new Chasen( CHASEN_ARGS, '\t');
            }

            Chasen.Result result = chasen.parse( text);
            while (parsePosition<=text.length && result.hasNext()) {
                // test for outside interruption
                if (Thread.currentThread().interrupted()) {
                    result.discard();
                    throw new ParsingInterruptedException();
                }
                
                // chasen skips spaces, so we have to adjust parsePosition here
                while (parsePosition<text.length && text[parsePosition]==' ')
                    parsePosition++;
                
                Object resultLine = result.next();
                // System.err.println( resultLine);
                if (resultLine.equals( Chasen.EOS)) { // end of line in input text
                    parsePosition++;
                }
                else {
                    List resultList = (List) resultLine;
                    String surfaceInflected = (String) resultList.get( 0);
                    // don't annotate romaji (may be interpreted as meishi by chasen)
                    if (surfaceInflected.charAt( 0) < 256) {
                        parsePosition += surfaceInflected.length();
                        continue;
                    }
                    String partOfSpeech = (String) resultList.get( 1);
                    String inflectionType = (String) resultList.get( 2);
                    String inflectedForm = (String) resultList.get( 3);
                    String surfaceBase = (String) resultList.get( 4);
                    String readingBase = (String) resultList.get( 5);
                    
                    if (partOfSpeech.equals( "\u540d\u8a5e") || // meishi (noun)
                        partOfSpeech.equals( "\u5f62\u5bb9\u52d5\u8a5e") || // keiyoudoushi (adjectival noun)
                        partOfSpeech.equals( "\u9023\u4f53\u8a5e") || // rentaishi (pre-noun adjectival)
                        partOfSpeech.equals( "\u526f\u8a5e") || // fukushi (adverb)
                        partOfSpeech.equals( "\u63a5\u7d9a\u8a5e") // sezzokushi (conjunction)
                        ) {
                        // Search the surface form in all dictionaries. surfaceInflected and
                        // surfaceBase are identical. If no match is found,
                        // kanji substrings of the word will be tried.
                        int from = 0;
                        int to = surfaceInflected.length();
                        do {
                            String word = surfaceInflected.substring( from, to);
                            if (ignoreWord( word)) {
                                // continue search with remaining suffix
                                // (loop will terminate if this was remainder of word)
                                from = to;
                                // only try kanji suffixes
                                while (from<surfaceInflected.length() &&
                                       !StringTools.isKanji
                                       ( surfaceInflected.charAt( from)))
                                    from++;
                                to = surfaceInflected.length();
                                readingBase = null;
                                continue;
                            }

                            List translations = search( word);
                            if (translations.size() != 0) {
                                if (firstOccurrenceOnly)
                                    annotatedWords.add( word);
                                if (readingBase != null) {
                                    readingBase = StringTools.toHiragana( readingBase);
                                    if (!readingBase.equals( word)) {
                                        // don't add reading for hiragana
                                        annotations.add( new Reading
                                            ( parsePosition + from, to-from,
                                              new ChasenWordReading( word, readingBase)));
                                    }
                                }
                                                                      
                                for ( Iterator i=translations.iterator(); i.hasNext(); ) {
                                    WordReadingPair wrp = (WordReadingPair) i.next();
                                    if (wrp instanceof DictionaryEntry) {
                                        annotations.add( new Translation
                                            ( parsePosition + from, 
                                              to - from,
                                              (DictionaryEntry) wrp));
                                    }
                                    else {
                                        annotations.add( new Reading( parsePosition + from,
                                                                      to - from, wrp));
                                    }
                                }
                                // continue search with remaining suffix
                                // (loop will terminate if this was remainder of word)
                                from = to;
                                // only try kanji suffixes
                                while (from<surfaceInflected.length() &&
                                       !StringTools.isKanji
                                       ( surfaceInflected.charAt( from)))
                                    from++;
                                to = surfaceInflected.length();
                                // There is no way to determine how much of the reading belongs
                                // to the annotated substring, so readingBase becomes invalid
                                readingBase = null;
                            }
                            else {
                                readingBase = null; // invalid for substrings
                                // no match found, try shorter prefix
                                do {
                                    to = to - 1;
                                } while (to>from &&
                                         // only try kanji prefixes
                                         !StringTools.isKanji
                                         ( surfaceInflected.charAt( to-1)));
                                if (to == from) {
                                    // no match found for prefix, try next suffix
                                    do {
                                        from++;
                                    } while (from<surfaceInflected.length() &&
                                             // only try kanji suffixes
                                             !StringTools.isKanji
                                             ( surfaceInflected.charAt( from)));
                                    to = surfaceInflected.length();
                                }
                            }
                        } while (from<to && from<surfaceInflected.length());
                    }
                    else if (partOfSpeech.equals( "\u52d5\u8a5e") ||  // doushi (verb)
                             partOfSpeech.equals( "\u5f62\u5bb9\u8a5e") // keiyoushi ("true" adjective)
                             ) {
                        if (ignoreWord( surfaceBase)) {
                            parsePosition += surfaceInflected.length();
                            continue;
                        }

                        List translations = search( surfaceBase);
                        if (translations.size() > 0) {
                            if (firstOccurrenceOnly)
                                annotatedWords.add( surfaceBase);
                            Conjugation c = getConjugation( surfaceInflected, surfaceBase,
                                                            partOfSpeech + "\u3001" + inflectedForm);
                            readingBase = StringTools.toHiragana( readingBase);
                            if (!readingBase.equals( surfaceBase)) {
                                // don't add reading for hiragana
                                annotations.add( new Reading
                                    ( parsePosition, surfaceInflected.length(),
                                      new ChasenWordReading( surfaceBase, readingBase), c));
                            }
                            for ( Iterator i=translations.iterator(); i.hasNext(); ) {
                                WordReadingPair wrp = (WordReadingPair) i.next();
                                if (wrp instanceof DictionaryEntry) {
                                    annotations.add( new Translation
                                        ( parsePosition, surfaceInflected.length(),
                                          (DictionaryEntry) wrp, c));
                                }
                                else {
                                    annotations.add( new Reading( parsePosition, 
                                                                  surfaceInflected.length(), wrp, c));
                                }
                            }                               
                        }
                    }

                    parsePosition += surfaceInflected.length();
                }
            }
        } catch (IOException ex) {
            throw new SearchException( ex);
        }

        return annotations;
    }

    /**
     * Ends the chasen application and clears the lookup cache.
     */
    public void reset() {
        if (chasen != null)
            chasen.dispose();

        if (lookupCache != null)
            lookupCache.clear();

        super.reset();
    }

    /**
     * Look up an entry in a dictionary. If a cache is used, it will be looked up there first, and
     * search results will be stored in the cache.
     */
    private List search( String word) throws SearchException {
        List result = null;

        if (lookupCache != null)
            result = (List) lookupCache.get( word);

        if (result == null) {
            result = new ArrayList( dictionaries.length*2);
            for ( int i=0; i<dictionaries.length; i++) {
                result.addAll( dictionaries[i].search( word, Dictionary.SEARCH_EXACT_MATCHES,
                                                       Dictionary.RESULT_DICTIONARY_ENTRIES));
            }
            if (lookupCache != null) {
                if (result.size() > 0)
                    lookupCache.put( word, result);
                else {
                    lookupCache.put( word, Collections.EMPTY_LIST);
                }
            }
        }

        return result;
    }

    /**
     * Build a conjugation object encapsulating the information generated by ChaSen.
     */
    private Conjugation getConjugation( String inflected, String base, String type) {
        // search last kanji in inflected form, everything after it is inflection
        int i = inflected.length();
        while (i>0 && !StringTools.isKanji( inflected.charAt( i-1))) {
            i--;
        }
        if (i == 0) {
            // Annotated hiragana word. Use point of divergence as inflection start.
            while (i<inflected.length() && i<base.length() &&
                   inflected.charAt( i)==base.charAt( i))
                i++;
        }
        inflected = inflected.substring( i);
        base = base.substring( i);
        
        return Conjugation.getConjugation( inflected, base, type);
    }

    /**
     * Overridden to terminate a chasen process if it is still running.
     */
    protected void finalize() {
        reset();
    }
} // class ChasenParser
