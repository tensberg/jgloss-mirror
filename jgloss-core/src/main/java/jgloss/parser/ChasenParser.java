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
 * $Id$
 */

package jgloss.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import jgloss.dictionary.SearchException;
import jgloss.util.StringTools;
import jgloss.util.UTF8ResourceBundleControl;

/**
 * Parses Japanese text using the ChaSen morphological analyzer program.
 * ChaSen must be installed to use this parser. The ChaSen homepage is located at
 * <a href="http://chasen.aist-nara.ac.jp/">http://chasen.aist-nara.ac.jp/</a>.
 *
 * @author Michael Koch
 */
public class ChasenParser extends AbstractParser {
    private final static String PARSER_NAME = 
        ResourceBundle.getBundle( "messages-parser", new UTF8ResourceBundleControl()).getString( "parser.chasen.name");

    /**
     * Command line parameter passed to chasen. Detemines the output format.
     */
    private final static String CHASEN_ARGS = "-F %m\\t%H\\t%Tn\\t%Fn\\t%M\\t%Y1\\t%y1\\n";
    
    /**
     * Chasen instance used to parse text.
     */
    private Chasen chasen;
    /**
     * Path to the chasen executable, or <code>null</code> if the default executable is to be used.
     */
    private final String chasenExecutable;
    /**
     * Cache of words looked up in the dictionaries.
     */
    private Map<String, Boolean> lookupCache;

    public ChasenParser( Set<String> exclusions) {
        this( null, exclusions, true);
    }

    public ChasenParser( Set<String> exclusions, boolean firstOccurrenceOnly) {
        this( null, exclusions, firstOccurrenceOnly);
    }

    public ChasenParser( String chasenExecutable, Set<String> exclusions,
                         boolean firstOccurrenceOnly) {
        super( exclusions, false, firstOccurrenceOnly);
        this.chasenExecutable = chasenExecutable;
    }

    @Override
	public String getName() { return PARSER_NAME; }

    @Override
	public Locale getLanguage() { return Locale.JAPANESE; }

    @Override
	public List<TextAnnotation> parse( char[] text, int start, int length) throws SearchException {
        
        // the parsePosition cannot be correct since the text was converted to HTML!
        parsePosition = start;

        int end = start + length;
        List<TextAnnotation> annotations = new ArrayList<TextAnnotation>( length/3);

        try {
            if (chasen == null) {
                // start chasen process
                if (chasenExecutable != null) {
	                chasen = new Chasen( chasenExecutable, CHASEN_ARGS, '\t');
                } else {
	                chasen = new Chasen( CHASEN_ARGS, '\t');
                }
            }

            Chasen.Result result = chasen.parse( text, start, length);
            while (parsePosition<=end && result.hasNext()) {
                // test for outside interruption
                if (Thread.interrupted()) {
                    result.discard();
                    throw new ParsingInterruptedException();
                }
                
                // chasen skips spaces, so we have to adjust parsePosition here
                while (parsePosition<end && text[parsePosition]==' ') {
	                parsePosition++;
                }
                
                Object resultLine = result.next();
                if (resultLine.equals( Chasen.EOS)) { // end of line in input text
                    parsePosition++;
                }
                else {
                    @SuppressWarnings("unchecked")
                    List<String> resultList = (List<String>) resultLine;
                    String surfaceInflected = resultList.get( 0);
                    // don't annotate romaji (may be interpreted as meishi by chasen)
                    if (surfaceInflected.charAt( 0) < 256) {
                        parsePosition += surfaceInflected.length();
                        continue;
                    }
                    String partOfSpeech = resultList.get( 1);
                    if (!annotate( partOfSpeech)) {
                        parsePosition += surfaceInflected.length();
                        continue;
                    }
                    String inflectionType = resultList.get( 2);
                    String inflectedForm = resultList.get( 3);
                    String surfaceBase = resultList.get( 4);
                    String readingBase = StringTools.toHiragana( resultList.get( 5));
                    String readingInflected = StringTools.toHiragana( resultList.get( 6));

                    if (!ignoreWord( surfaceBase)) {
                        annotations.add
                            ( new TextAnnotation( parsePosition, surfaceInflected.length(),
                                                  readingInflected, surfaceBase, readingBase,
                                                  constructGrammaticalType( partOfSpeech,
                                                                            inflectionType,
                                                                            inflectedForm)));
                        
                        if (firstOccurrenceOnly) {
	                        annotatedWords.add( surfaceBase);
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
     * Test if annotations should be generated for a particular part of speech.
     */
    protected boolean annotate( String partOfSpeech) {
        return partOfSpeech.equals( "\u540d\u8a5e") || // meishi (noun)
            partOfSpeech.equals( "\u5f62\u5bb9\u52d5\u8a5e") || // keiyoudoushi (adjectival noun)
            partOfSpeech.equals( "\u9023\u4f53\u8a5e") || // rentaishi (pre-noun adjectival)
            partOfSpeech.equals( "\u526f\u8a5e") || // fukushi (adverb)
            partOfSpeech.equals( "\u63a5\u7d9a\u8a5e") || // sezzokushi (conjunction)
            partOfSpeech.equals( "\u52d5\u8a5e") ||  // doushi (verb)
            partOfSpeech.equals( "\u5f62\u5bb9\u8a5e"); // keiyoushi ("true" adjective)
    }

    protected String constructGrammaticalType( String partOfSpeech, String inflectionType,
                                               String inflectedForm) {
        boolean noInflectionType = "n".equals( inflectionType);
        boolean noInflectedForm = "n".equals( inflectedForm);
        if (noInflectionType && noInflectedForm) {
	        return partOfSpeech;
        }

        StringBuilder out = new StringBuilder( partOfSpeech);
        if (!noInflectionType) {
            out.append( '\u3001');
            out.append( inflectionType);
        }
        if (!noInflectedForm) {
            out.append( '\u3001');
            out.append( inflectedForm);
        }
        
        return out.toString();
    }                                          

    /**
     * Ends the chasen application and clears the lookup cache.
     */
    @Override
	public void reset() {
        if (chasen != null) {
	        chasen.dispose();
        }

        if (lookupCache != null) {
	        lookupCache.clear();
        }

        super.reset();
    }

    /**
     * Overridden to terminate a chasen process if it is still running.
     */
    @Override
	protected void finalize() {
        reset();
    }
} // class ChasenParser
