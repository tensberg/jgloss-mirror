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

    private static String defaultChasenExecutable = "/usr/local/bin/chasen";

    /**
     * Cache used by {@link #isChasenExecutable(String) isChasenExecutable} to store the
     * name of the last succesfully tested chasen executable. 
     */
    private static String lastChasenExecutable = null;

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
     * Path to chasen program.
     */
    private String chasenExecutable;
    /**
     * Name of the character encoding ChaSen uses on this computer.
     */
    private static String platformEncoding;
    /**
     * Chasen process used to parse the text.
     */
    private Process chasen;
    /**
     * Reader for stdout of chasen process.
     */
    private BufferedReader chasenOut;
    /**
     * Reader for stdin of chasen process.
     */
    private BufferedWriter chasenIn;
    /**
     * Cache of words looked up in the dictionaries.
     */
    private Map lookupCache;

    public ChasenParser( Dictionary[] dictionaries, Set exclusions) {
        this( defaultChasenExecutable, dictionaries, exclusions, true, false);
    }

    public ChasenParser( Dictionary[] dictionaries, Set exclusions, boolean cacheLookups) {
        this( defaultChasenExecutable, dictionaries, exclusions, cacheLookups, false);
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

    /**
     * Sets the path to the default ChaSen executable. This executable will be used when the
     * executable path is not passed in with the constructor.
     */
    public static void setDefaultExecutable( String chasenExecutable) {
        defaultChasenExecutable = chasenExecutable;
    }

    /**
     * Returns the path to the default ChaSen executable.
     */
    public static String getDefaultExecutable() {
        return defaultChasenExecutable;
    }

    /**
     * Test if the chasen program is available at the specified path. The test is done by
     * calling the program with the "-V" (for version) option. If the path to the executable
     * is the same as in the previous test, and this test was successfull, the test will not be
     * repeated.
     *
     * @param chasenExecutable Full path to the chasen executable.
     */
    public static boolean isChasenExecutable( String chasenExecutable) {
        // If the last call to isChasenExecutable was successful, the name of the
        // executable is stored in "lastChasenExecutable".
        if (lastChasenExecutable != null &&
            lastChasenExecutable.equals( chasenExecutable))
            return true;

        try {
            final Process p = Runtime.getRuntime().exec( chasenExecutable + " -V");
            Thread wait = new Thread() {
                    public void run() {
                        try {
                            p.waitFor();
                        } catch (InterruptedException ex) {
                            // destroy process if it didn't terminate normally in time
                            p.destroy();
                        }
                    }
                };
            wait.start();

            try {
                // give executed process 3 seconds to terminate normally
                wait.join( 3000);
            } catch (InterruptedException ex) {}
            if (wait.isAlive()) {
                // something went wrong
                wait.interrupt();
                try {
                    wait.join();
                } catch (InterruptedException ex) {}
                return false;
            }
            else {
                // normal termination
                if (p.exitValue() != 0)
                    return false;
                BufferedReader out = new BufferedReader
                    ( new InputStreamReader( p.getInputStream()));
                String line = out.readLine();
                out.close();
                if (line==null || !line.startsWith( "ChaSen"))
                    return false;

                lastChasenExecutable = chasenExecutable;
                return true;
            }
        } catch (IOException ex) {
            // specified program probably doesn't exist
            return false;
        }
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
        if (chasen == null)
            startChasen();

        List annotations = new ArrayList( text.length/3);
        try {
            // replace Dos or Mac line ends with unix line ends to make sure EOS is
            // treated correctly
            for ( int i=0; i<text.length; i++) {
                if (text[i] == 0x0d)
                    text[i] = 0x0a;
            }
            chasenIn.write( text);
            chasenIn.write( (char) 0x0a); // chasen will start parsing when end of line is encountered
            chasenIn.flush();

            String line;
            while (parsePosition<=text.length && (line=chasenOut.readLine())!=null) {
                // test for outside interruption
                if (Thread.currentThread().interrupted())
                    throw new ParsingInterruptedException();

                // chasen skips spaces, so we have to adjust parsePosition here
                while (parsePosition<text.length && text[parsePosition]==' ')
                    parsePosition++;

                //System.out.println( line);
                if (line.equals( "EOS")) { // end of line in input text
                    parsePosition++;
                }
                else {
                    int s;
                    int t = line.indexOf( '\t');
                    String surfaceInflected = line.substring( 0, t);
                    // don't annotate romaji (may be interpreted as meishi by chasen)
                    if (surfaceInflected.charAt( 0) < 256) {
                        parsePosition += surfaceInflected.length();
                        continue;
                    }
                    s = t + 1;
                    t = line.indexOf( '\t', s);
                    String partOfSpeech = line.substring( s, t);
                    s = t + 1;
                    t = line.indexOf( '\t', s);
                    String inflectionType = line.substring( s, t);
                    s = t + 1;
                    t = line.indexOf( '\t', s);
                    String inflectedForm = line.substring( s, t);
                    s = t + 1;
                    t = line.indexOf( '\t', s);
                    String surfaceBase = line.substring( s, t);
                    s = t + 1;
                    String readingBase = line.substring( s);
                    
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
                    else if (partOfSpeech.equals( "\u52d5\u8a5e") ||  // douji (verb)
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
        stopChasen();

        if (lookupCache != null)
            lookupCache.clear();

        super.reset();
    }

    /**
     * Starts the chasen process.
     */
    protected void startChasen() throws SearchException {
        // Initialize platform encoding if not done already. This avoids running chasen
        // twice at the same time.
        getChasenPlatformEncoding();
        try {
            chasen = Runtime.getRuntime().exec( chasenExecutable +
                                                " -F %m\\t%H\\t%Tn\\t%Fn\\t%?T/%M/n/\\t%Y1\\n",
                                                new String[] { "LANG=ja", "LC_ALL=ja_JP" });
            chasenOut = new BufferedReader( new InputStreamReader
                ( chasen.getInputStream(), getChasenPlatformEncoding()));
            chasenIn = new BufferedWriter( new OutputStreamWriter
                ( chasen.getOutputStream(), getChasenPlatformEncoding()));
        } catch (IOException ex) {
            throw new SearchException( "error starting chasen: " + ex.getClass().getName() + ", " 
                                       + ex.getLocalizedMessage());
        }
    }

    /**
     * Terminates the chasen process.
     */
    protected synchronized void stopChasen() {
        if (chasen != null) {
            // terminate chasen process by writing EOT on its input stream
            try {
                chasenIn.flush(); // should be empty
                chasen.getOutputStream().close(); // this should terminate the executable
                // read remaining input (should be empty)
                byte[] buf = new byte[512];
                while (chasen.getInputStream().available() > 0)
                    chasen.getInputStream().read( buf);
                while (chasen.getErrorStream().available() > 0)
                    chasen.getErrorStream().read( buf);
                Thread wait = new Thread() {
                        public void run() {
                            try {
                                chasen.waitFor();
                            } catch (InterruptedException ex) {
                                System.err.println( "abnormal termination of chasen");
                                chasen.destroy();
                            }
                            chasen = null;
                        }
                    };
                wait.start();

                // clear lingering interruption flag just to be sure
                Thread.currentThread().interrupted();

                try {
                    // give chasen process 5 seconds to terminate normally
                    wait.join( 5000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                if (wait.isAlive()) {
                    System.err.println( "chasen did not terminate");
                    // something went wrong
                    wait.interrupt();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
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

    /**
     * Test which character encoding ChaSen uses for its input and output streams. On
     * Linux this will probably be EUC-JP and Shift-JIS on Windows. The test is done by running
     * chasen with the -lf option, which makes it list the conjugation forms, and checking the encoding
     * of the output. The test is only done the first time the method is called, the result is
     * cached and reused on further calls.
     *
     * @return Canonical name of the encoding, or <CODE>null</CODE> if the test failed.
     */
    protected String getChasenPlatformEncoding() {
        if (platformEncoding != null)
            return platformEncoding;

        try {
            Process chasen = Runtime.getRuntime().exec( chasenExecutable + " -lf");
            InputStreamReader reader = CharacterEncodingDetector.getReader( chasen.getInputStream());
            platformEncoding = reader.getEncoding();

            // skip all input lines
            char[] buf = new char[512];
            while (reader.ready())
                reader.read(buf);
            try {
                chasen.waitFor();
            } catch (InterruptedException ex) {}
            reader.close();
            chasen.getInputStream().close();
            chasen.getErrorStream().close();
            return platformEncoding;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
} // class ChasenParser
