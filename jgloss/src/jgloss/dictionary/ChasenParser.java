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
        Parser p = new ChasenParser( new Dictionary[] { new EDict( "/usr/share/edict/edict", true) }, null);
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
        System.out.println( "parsing " + text);

        List annotations = new ArrayList( text.length/3);
        List translations = new ArrayList( 10);
        try {
            // replace Dos or Mac line ends with unix line ends to make sure EOS is
            // treated correctly
            for ( int i=0; i<text.length; i++) {
                if (text[i] == 0x0d)
                    text[i] = 0x0a;
            }
            chasenIn.write( text);
            chasenIn.write( (char) 0x0a); // chasen will start parsing at end of line
            chasenIn.flush();

            String line;
            while (parsePosition<text.length-1 && (line=chasenOut.readLine())!=null) {
                System.out.println( line + " " + parsePosition + " " + text.length);
                if (line.equals( "EOS")) { // end of line in input text
                    parsePosition += 1;
                }
                else {
                    int s;
                    int t = line.indexOf( '\t');
                    String surfaceInflected = line.substring( 0, t);
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
                    String surfaceBase = line.substring( s);

                    if (partOfSpeech.equals( "\u540d\u8a5e") || // meishi (noun)
                        partOfSpeech.equals( "\u526f\u8a5e") || // fukushi (adverb)
                        partOfSpeech.equals( "\u9023\u4f53\u8a5e") // rentaishi (pre-noun adjectival)
                        ) {
                        // Search the surface form in all dictionaries. surfaceInflected and
                        // surfaceBase are identical. If no match is found and the word is all
                        // kanji, substrings of the word will be tried.
                        int from = 0;
                        int to = surfaceInflected.length();
                        do {
                            translations.clear();
                            translations = search( surfaceInflected.substring( from, to));
                            if (translations.size() != 0) {
                                for ( Iterator i=translations.iterator(); i.hasNext(); ) {
                                    WordReadingPair wrp = (WordReadingPair) i.next();
                                    if (wrp instanceof DictionaryEntry) {
                                        annotations.add( new Translation
                                            ( parsePosition + from, surfaceInflected.length(),
                                              (DictionaryEntry) wrp));
                                    }
                                    else {
                                        annotations.add( new Reading( parsePosition + from,
                                                                      surfaceInflected.length(), wrp));
                                    }
                                }
                                // continue search with remaining suffix
                                // (loop will terminate if this was remainder of word)
                                from = to;
                                to = surfaceInflected.length();
                            }
                            else {
                                // no match found, try shorter prefix
                                to = to - 1;
                                if (to == from) {
                                    // no match found for prefix, try next suffix
                                    from++;
                                    to = surfaceInflected.length();
                                }
                            }
                        } while (from<to && from<surfaceInflected.length());
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
                                                " -F %m\\t%H\\t%Tn\\t%Fn\\t%?T/%M/n/\\n");
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
     *
     * @param d Dictionary to use for the lookup.
     * @param word Word to look up.
     * @param mode One of the valid search modes defined in <CODE>Dictionary</CODE>.
     * @return The result of the lookup.
     * @exception SearchException if the lookup fails.
     * @see Dictionary
     */
    private List search( String word) throws SearchException {
        List result;

        if (lookupCache!=null &&
            lookupCache.containsKey( word)) {
            return (List) lookupCache.get( word);
        }

        // if we get here, it was not in the cache
        result = new ArrayList( dictionaries.length*2);
        for ( int i=0; i<dictionaries.length; i++)
            result.addAll( dictionaries[i].search( word, Dictionary.SEARCH_EXACT_MATCHES));
        if (lookupCache != null) {
            if (result.size() > 0)
                lookupCache.put( word, result);
            else
                lookupCache.put( word, Collections.EMPTY_LIST);
        }

        return result;
    }

    /**
     * Overridden to terminate a chasen process if it is still running.
     */
    protected void finalize() {
        reset();
    }
} // class ChasenParser
