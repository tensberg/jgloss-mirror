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
 * Parses Japanese text using the ChaSen morphological analyzer program.
 * ChaSen must be installed to use this parser. You can find information
 * about ChaSen at 
 * <a href="http://chasen.aist-nara.ac.jp/">http://chasen.aist-nara.ac.jp/</a>.
 *
 * @author Michael Koch
 */
public class ChasenParser implements Parser {
    public static void main( String[] args) throws Exception {
        System.out.println( isChasenExecutable( getDefaultExecutable()));
        ChasenParser p = new ChasenParser
            ( new Dictionary[] { new EDict( "/usr/share/edict/edict", true) }, null);
        StringWriter w = new StringWriter();
        BufferedReader r = new BufferedReader( new InputStreamReader
            ( new FileInputStream( "/home/michael/japan/karinodouji/kari.noannotations.txt"), "EUC-JP"));
        String line;
        while ((line=r.readLine()) != null) {
            w.write( line);
            w.write( "\n");
        }
        r.close();
        w.close();
        
        p.parse( w.toString().toCharArray());
        p.reset();
    }

    private final static ResourceBundle messages =
        ResourceBundle.getBundle( "resources/messages-dictionary");

    private final static String PARSER_NAME = 
        messages.getString( "parser.chasen.name");

    private static String defaultChasenExecutable = "/usr/local/bin/chasen";

    /**
     * Path to chasen program.
     */
    private String chasenExecutable;
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

    private Dictionary[] dictionaries;
    private Set exclusions;

    private int parsePosition;

    private Map lookupCache;

    private boolean ignoreNewlines;

    public ChasenParser( Dictionary[] dictionaries, Set exclusions) {
        this( defaultChasenExecutable, dictionaries, exclusions, true);
    }

    public ChasenParser( Dictionary[] dictionaries, Set exclusions, boolean cacheLookups) {
        this( defaultChasenExecutable, dictionaries, exclusions, cacheLookups);
    }

    public ChasenParser( String chasenExecutable, Dictionary[] dictionaries, Set exclusions) {
        this( chasenExecutable, dictionaries, exclusions, true);
    }

    public ChasenParser( String chasenExecutable, Dictionary[] dictionaries, Set exclusions,
                         boolean cacheLookups) {
        this.chasenExecutable = chasenExecutable;
        this.dictionaries = dictionaries;
        this.exclusions = exclusions;
        ignoreNewlines = false;
        if (cacheLookups)
            lookupCache = new HashMap( 5000);
    }

    public static void setDefaultExecutable( String chasenExecutable) {
        defaultChasenExecutable = chasenExecutable;
    }

    public static String getDefaultExecutable() {
        return defaultChasenExecutable;
    }

    /**
     * Test if the chasen program is available at the specified path. The test is done by
     * calling the program with the "-V" (for version) option.
     *
     * @param chasenExecutable Full path to the chasen binary.
     */
    public static boolean isChasenExecutable( String chasenExecutable) {
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
                // chasen skips normal spaces
                if (text[i] == ' ')
                    text[i] = '\u3000'; // Japanese space
            }
            chasenIn.write( text);
            chasenIn.write( (char) 0x0a); // chasen will start parsing when end of line is encountered
            chasenIn.flush();

            String line;
            while (parsePosition<=text.length && (line=chasenOut.readLine())!=null) {
                System.out.println( line + " " + parsePosition + " " + text.length);
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
                            if (exclusions!=null && exclusions.contains( word)) {
                                // continue search with remaining suffix
                                // (loop will terminate if this was remainder of word)
                                from = to;
                                // only try kanji suffixes
                                while (from<surfaceInflected.length() &&
                                       !StringTools.isCJKUnifiedIdeographs
                                       ( surfaceInflected.charAt( from)))
                                    from++;
                                to = surfaceInflected.length();
                                continue;
                            }

                            List translations = search( word, readingBase);
                            if (translations.size() != 0) {
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
                                       !StringTools.isCJKUnifiedIdeographs
                                       ( surfaceInflected.charAt( from)))
                                    from++;
                                to = surfaceInflected.length();
                                // Shorten readingBase if matching entry found.
                                // If matching entry exists, search() has moved it to position 0.
                                WordReadingPair wrp = (WordReadingPair) translations.get( 0);
                                String reading = wrp.getReading();
                                if (reading == null)
                                    reading = wrp.getWord();
                                if (readingBase.startsWith( StringTools.toKatakana( reading)))
                                    readingBase = readingBase.substring( reading.length());
                            }
                            else {
                                // no match found, try shorter prefix
                                do {
                                    to = to - 1;
                                } while (to>from &&
                                         // only try kanji prefixes
                                         !StringTools.isCJKUnifiedIdeographs
                                         ( surfaceInflected.charAt( to-1)));
                                if (to == from) {
                                    // no match found for prefix, try next suffix
                                    do {
                                        from++;
                                    } while (from<surfaceInflected.length() &&
                                             // only try kanji suffixes
                                             !StringTools.isCJKUnifiedIdeographs
                                             ( surfaceInflected.charAt( from)));
                                    to = surfaceInflected.length();
                                }
                            }
                        } while (from<to && from<surfaceInflected.length());
                    }
                    else if (partOfSpeech.equals( "\u52d5\u8a5e") ||  // douji (verb)
                             partOfSpeech.equals( "\u5f62\u5bb9\u8a5e") // keiyoushi ("true" adjective)
                             ) {
                        if (exclusions!=null && exclusions.contains( surfaceBase)) {
                            parsePosition += surfaceInflected.length();
                            continue;
                        }

                        List translations = search( surfaceBase, readingBase);
                        if (translations.size() > 0) {
                            Conjugation c = getConjugation( surfaceInflected, surfaceBase,
                                                            partOfSpeech + "\u3001" + inflectedForm);
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
            System.out.println( "finished iteration");
        } catch (IOException ex) {
            throw new SearchException( ex);
        }
        
        return annotations;
    }

    /**
     * Returns the position in the text the parser is currently parsing. This is not threadsafe.
     * If more than one thread is using this Parser object, the result of this method is
     * undetermined.
     *
     * @return The position in the text the parser is currently parsing.
     */
    public int getParsePosition() {
        return parsePosition;
    }

    /**
     * Clears any caches which may have been filled during parsing. Call this after you have
     * parsed some text to reclaim the memory.
     */
    public synchronized void reset() {
        if (chasen != null) {
            // terminate chasen process by writing EOT on its input stream
            try {
                chasenIn.flush(); // should be empty
                chasen.getOutputStream().write( 4); // EOT
                chasen.getOutputStream().flush();
                Thread wait = new Thread() {
                        public void run() {
                            try {
                                chasen.waitFor();
                            } catch (InterruptedException ex) {
                                System.err.println( "abnormal termination of chasen");
                                chasen.destroy();
                            }
                            System.err.println( "chasen termiator finished");
                        }
                    };
                try {
                    // give chasen process 3 seconds to terminate normally
                    wait.join( 3000);
                } catch (InterruptedException ex) {}
                if (wait.isAlive())
                    // something went wrong
                    wait.interrupt();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            chasen = null;

            if (lookupCache != null)
                lookupCache.clear();
        }
    }

    /**
     * Set if the parser should skip newlines in the imported text. This means that characters
     * separated by one or several newline characters will be treated as a single word.
     */
    public void setIgnoreNewlines( boolean ignoreNewlines) {
        this.ignoreNewlines = ignoreNewlines;
    }

    /**
     * Test if the parser skips newlines in the imported text.
     */
    public boolean getIgnoreNewlines() {
        return ignoreNewlines;
    }

    /**
     * Starts the chasen process.
     */
    protected void startChasen() throws SearchException {
        try {
            chasen = Runtime.getRuntime().exec( chasenExecutable +
                                                " -F %m\\t%H\\t%Tn\\t%Fn\\t%?T/%M/n/\\t%Y1\\n",
                                                new String[] { "LANG=ja", "LC_ALL=ja_JP" });
            chasenOut = new BufferedReader( new InputStreamReader( chasen.getInputStream(),
                                                                   "EUC-JP"));
            chasenIn = new BufferedWriter( new OutputStreamWriter( chasen.getOutputStream(),
                                                                   "EUC-JP"));
        } catch (IOException ex) {
            throw new SearchException( "error starting chasen");
        }
    }

    /**
     * Look up an entry in a dictionary. If a cache is used, it will be looked up there first, and
     * search results will be stored in the cache.
     */
    private List search( String word, String preferredReading) throws SearchException {
        List result = null;
        String cacheKey = word + "_" + preferredReading;

        System.out.println( "looking up " + word);
        if (lookupCache != null)
            result = (List) lookupCache.get( cacheKey);
        if (lookupCache != null)
            result = (List) lookupCache.get( word);

        if (result == null) {
            boolean readingFound = false;
            result = new ArrayList( dictionaries.length*2);
            for ( int i=0; i<dictionaries.length; i++) {
                List r = dictionaries[i].search( word, Dictionary.SEARCH_EXACT_MATCHES);
                // Reorder entries according to preferred reading.
                int matches = 0;
                for ( Iterator j=r.iterator(); j.hasNext(); ) {
                    WordReadingPair wrp = (WordReadingPair) j.next();
                    String reading = wrp.getReading();
                    if (reading == null)
                        reading = wrp.getWord();
                    if (StringTools.toKatakana( reading).equals( preferredReading)) {
                        if (readingFound)
                            result.add( wrp);
                        else
                            result.add( matches, wrp);
                        matches++;
                        j.remove();
                    }
                }
                if (matches>0 && !readingFound) {
                    readingFound = true;
                    result.addAll( matches, r);
                }
                else
                    result.addAll( r); // append
            }
            if (lookupCache != null) {
                if (result.size() > 0)
                    lookupCache.put( cacheKey, result);
                else {
                    lookupCache.put( cacheKey, Collections.EMPTY_LIST);
                    lookupCache.put( word, Collections.EMPTY_LIST);
                }
            }
        }

        return result;
    }

    private Conjugation getConjugation( String inflected, String base, String type) {
        // search last kanji in inflected form, everything after it is inflection
        int i = inflected.length();
        while (i>0 && !StringTools.isCJKUnifiedIdeographs( inflected.charAt( i-1))) {
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
        
        System.out.println( "conjugation " + inflected + " " + base + " " + type);
        return new Conjugation( inflected, base, type);
    }

    /**
     * Overridden to terminate a chasen process if it is still running.
     */
    protected void finalize() {
        reset();
    }
} // class ChasenParser
