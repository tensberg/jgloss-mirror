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
 *
 */

package jgloss.dictionary;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

/**
 * Dictionary implementation for KANJIDIC-style dictionary files. For a documentation
 * of the format see <a href="http://ftp.cc.monash.edu.au/pub/nihongo/kanjidic_doc.html">
 * http://ftp.cc.monash.edu.au/pub/nihongo/kanjidic_doc.html</a>.
 */
public class KanjiDic implements Dictionary {
    /**
     * Localizable message resource.
     */
    private final static ResourceBundle messages = 
        ResourceBundle.getBundle( "resources/messages-dictionary");

    /**
     * Pathname to the dictionary file.
     */
    protected String dicfile;
    /**
     * Name of the dictionary file without path component.
     */
    protected String name;

    /**
     * Map from a word to an entry of list of entries. Contains a key for all kanjis,
     * readings and translations.
     */
    protected Map entries;

    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
    public final static DictionaryFactory.Implementation implementation = 
        new DictionaryFactory.Implementation() {
                public float isInstance( String descriptor) {
                    try {
                        BufferedReader r = new BufferedReader( new InputStreamReader( new FileInputStream
                            ( descriptor), "EUC-JP"));
                        String l = null;
                        int lines = 0;
                        do {
                            l = r.readLine();
                            lines++;
                            // skip empty lines and comments
                        } while (l!=null && (l.length()==0 || l.charAt( 0)<128) && lines<100);
                        r.close();
                        // test if second character is a space and is followed by a
                        // 4-digit hexadecimal number
                        if (lines<100 && l!=null && l.length()>7 && l.charAt( 1)==' ' &&
                            Integer.parseInt( l.substring( 2, 6), 16)>0)
                            return getMaxConfidence();
                    } catch (IOException ex) {
                    } catch (NumberFormatException ex) {}
                    
                    return ZERO_CONFIDENCE;
                }
                
                public float getMaxConfidence() { return 1.0f; }
                
                public Dictionary createInstance( String descriptor) 
                    throws DictionaryFactory.InstantiationException {
                    try {
                        return new KanjiDic( descriptor);
                    } catch (IOException ex) {
                        throw new DictionaryFactory.InstantiationException( ex.getLocalizedMessage(), ex);
                    }
                }

                public String getName() { return "KANJIDIC"; }

                public Class getDictionaryClass( String descriptor) { return KanjiDic.class; }
            };

    /**
     * Used during entry parsing in constructor.
     */
    private final static List readingsl = new ArrayList( 10);
    /**
     * Used during entry parsing in constructor.
     */
    private final static List nanoril = new ArrayList( 10);
    /**
     * Used during entry parsing in constructor.
     */
    private final static List translationsl = new ArrayList( 10);

    /**
     * Represents a single entry in the kanji dictionary file. Only a subset of the fields
     * defined in the KANJIDIC specification is supported.
     */
    public class Entry {
        /**
         * Value of a number field where no data is available in the dictionary file. 
         */
        public static final byte NOT_AVAILABLE = -1;

        /**
         * Kanji of this entry.
         */
        protected char kanji;
        /**
         * List of readings of this entry. <CODE>null</CODE>, if no readings are defined.
         */
        protected String[] readings;
        /**
         * List of the nanori (name) readings of this entry. 
         * <CODE>null</CODE>, if no nanori readings are defined.
         */
        protected String[] nanoriReadings;
        /**
         * Name of the radical. This field is only defined if the kanji is a radical and the name
         * of the radical is not a reading. Otherwise, it is <CODE>null</CODE>.
         */
        protected String radicalname;
        /**
         * List of translations of this entry. <CODE>null</CODE>, if no translations are defined.
         */
        protected String[] translations;

        /**
         * Strokecount of this kanji.
         */
        protected byte strokecount;
        /**
         * The radical (bushu) number.
         */
        protected short bnum;
        /**
         * The historical or classical radical number. This field is only defined if it
         * differs from the bushu number.
         */
        protected short cnum;
        /**
         * The frequency-of-use ranking.
         */
        protected short frequency;

        /**
         * Creates a new entry with the given values.
         */
        protected Entry( char kanji, String[] readings, String[] nanoriReadings,
                      String radicalname, String[] translations, byte strokecount,
                      short bnum, short cnum, short frequency) {
            this.kanji = kanji;
            this.readings = readings;
            this.nanoriReadings = nanoriReadings;
            this.radicalname = radicalname;
            this.translations = translations;
            this.strokecount = strokecount;
            this.bnum = bnum;
            this.cnum = cnum;
            this.frequency = frequency;
        }

        /**
         * Creates a new entry for a line in the dictionary file. Only a subset of the fields
         * are used.
         *
         * @param dicline A line from the dictionary file in the format as specified in the
         *        KANJIDIC documentation.
         * @param extendedInformation <code>true</code>, if bushu number, classical radical number,
         *        frequency of use and stroke count should be parsed.
         */
        protected Entry( String dicline, boolean extendedInformation) {
            synchronized (readingsl) {
                readingsl.clear();
                nanoril.clear();
                translationsl.clear();
                List currentl = readingsl;

                kanji = dicline.charAt( 0);
                
                strokecount = NOT_AVAILABLE;
                bnum = NOT_AVAILABLE;
                cnum = NOT_AVAILABLE;
                frequency = NOT_AVAILABLE;
                
                // iterate over all fields (delimited by a ' ')
                int from = 7; // skip kanji and ASCII kanji code
                int to = dicline.indexOf( ' ', from + 1);
                if (to == -1) // last field
                    to = dicline.length();
                while (to > from) {
                    char c = dicline.charAt( from); // first char in field determines type
                    if (c < 128) { // ASCII character: reading only if c=='-'
                        switch (c) {
                        case '-': // reading (for kanji used as suffix)
                                // radicalname never starts with -
                            currentl.add( dicline.substring( from, to));
                            break;
                            
                        case '{': // translation, enclosed in {}
                                // translations can contain spaces
                            to = dicline.indexOf( '}', from+1) + 1;
                            translationsl.add( dicline.substring( from+1, to-1));
                            break;
                            
                        case 'T': // type change for following readings
                            if (dicline.charAt( from+1) == '1') // nanori readings
                                currentl = nanoril;
                            else if (dicline.charAt( from+1) == '2') // radical name
                                currentl = null;
                            break;
                        }

                        // only parse other fields if extended information is wanted by caller
                        if (extendedInformation) try {
                            switch (c) {
                            case 'B': // bushu number
                                bnum = Short.parseShort( dicline.substring( from+1, to));
                                break;
                                
                            case 'C': // classical radical number
                                cnum = Short.parseShort( dicline.substring( from+1, to));
                                break;
                                
                            case 'F': // frequency of use
                                frequency = Short.parseShort( dicline.substring( from+1, to));
                                break;
                                
                            case 'S': // stroke count
                                // If there is more than one stroke count, all but the first
                                // are common miscounts. These entries are currently not used.
                                if (strokecount == NOT_AVAILABLE) {
                                    strokecount = Byte.parseByte( dicline.substring( from+1, to));
                                }
                                break;
                                
                                // all other entry types are currently not used
                            }
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                            System.err.println( "WARNING: malformed dictionary entry " + dicline);
                        }
                    }
                    else {
                        if (currentl != null)
                            currentl.add( dicline.substring( from, to));
                        else
                            radicalname = dicline.substring( from, to);
                    }
                    
                    // move to the next entry
                    from = to + 1;
                    to = dicline.indexOf( ' ', from + 1);
                    if (to == -1) // last field
                        to = dicline.length();
                }
                
                if (readingsl.size() > 0) {
                    readings = new String[readingsl.size()];
                    readingsl.toArray( readings);
                }
                if (nanoril.size() > 0) {
                    nanoriReadings = new String[nanoril.size()];
                    nanoril.toArray( nanoriReadings);
                }
                if (translationsl.size() > 0) {
                    translations = new String[translationsl.size()];
                    translationsl.toArray( translations);
                }
            }
        }

        /**
         * Returns the kanji of this entry.
         */
        public char getKanji() { return kanji; }
        /**
         * Returns the list of readings of this entry. <CODE>null</CODE>, if no readings are defined.
         */
        public String[] getReadings() { return readings; }
        /**
         * Returns the list of the nanori (name) readings of this entry. 
         * <CODE>null</CODE>, if no nanori readings are defined.
         */
        public String[] getNanoriReadings() { return nanoriReadings; }
        /**
         * Returns the name of the radical. This field is only defined if the kanji
         * is a radical and the name of the radical is not a reading. Otherwise, it is <CODE>null</CODE>.
         */
        public String getRadicalName() { return radicalname; }
        /**
         * Returns the list of translations of this entry.
         * <CODE>null</CODE>, if no translations are defined.
         */
        public String[] getTranslations() { return translations; }
        /**
         * Returns the strokecount of this kanji.
         */
        public byte getStrokecount() { return strokecount; }
        /**
         * Returns the radical (bushu) number.
         */
        public short getBNum() { return bnum; }
        /**
         * Returns the historical or classical radical number. This field is only defined if it
         * differs from the bushu number.
         */
        public short getCNum() { return cnum; }
        /**
         * Returns the frequency-of-use ranking.
         */
        public short getFrequency() { return frequency; }
    } // class KanjiDicEntry

    /**
     * Creates a new dictionary for a file in KANJIDIC format.
     *
     * @exception IOException when the dictionary file cannot be read.
     */
    public KanjiDic( String dicfile) throws IOException {
        entries = new HashMap( 25001);
        this.dicfile = dicfile;
        File dic = new File( dicfile);
        name = dic.getName();
        System.err.println( MessageFormat.format( messages.getString( "dictionary.load"),
                                                  new String[] { name }));

        BufferedReader in = new BufferedReader( new InputStreamReader
            ( new BufferedInputStream( new FileInputStream( dic)), "EUC-JP"));
        String line;
        while ((line = in.readLine()) != null) {
            // an ASCII character at the beginning of the line is treated as start of a comment
            if (line.length()>0 && line.charAt( 0)>127) {
                Entry e = new Entry( line, false);
                // storing strings in the hashmap instead of entries decreases ram usage
                addEntry( new String( new char[] { e.getKanji() }), line);
                
                addReadings( line, e, e.getReadings());
                addReadings( line, e, e.getNanoriReadings());
                if (e.getRadicalName() != null)
                    addEntry( e.getRadicalName(), line);
                String[] translations = e.getTranslations();
                if (translations != null) {
                    for ( int i=0; i<translations.length; i++) {
                        addEntry( translations[i], line);
                    }
                }
            }
        }

        // compact all stored array lists to minimize memory usage
        for ( Iterator i=entries.entrySet().iterator(); i.hasNext(); ) {
            Object value = ((Map.Entry) i.next()).getValue();
            if (value instanceof ArrayList)
                ((ArrayList) value).trimToSize();
        }
    }

    /**
     * Adds an entry to the map of entries. If the map does not contain the key,
     * the entry will be put directly, otherwise all entries for this key are stored in
     * a list.
     *
     * @param key The key under which to store the entry.
     * @param entry The entry which will be stored.
     */
    protected void addEntry( Object key, String entry) {
        Object o = entries.get( key);
        if (o == entry)
            return;
        if (o == null) {
            entries.put( key, entry);
        }
        else if (o instanceof List) {
            List l = (List) o;
            // Entries are stored one after the other, so in the case of duplicate keys
            // only the last item of the entry list has to be tested for equality to prevent
            // duplicate insertions
            if (l.get( l.size()-1) != entry)
                ((List) o).add( entry);
        }
        else {
            List l = new ArrayList( 5);
            l.add( o);
            l.add( entry);
            entries.put( key, l);
        }
    }

    /**
     * Adds the entry to the map of entries for all readings given as parameter. If
     * the reading has an okurigana part (separated by a '.'), two entries will be generated.
     * One for the reading without the dot, one for the kanji+okurigana.
     *
     * @param e The entry to add.
     * @param readings List of readings. This can be normal or nanori readings.
     */
    protected void addReadings( String line, Entry e, String[] readings) {
        if (readings != null) {
            for ( int i=0; i<readings.length; i++) {
                int dot = readings[i].indexOf( '.');
                if (dot == -1)
                    addEntry( readings[i], line);
                else {
                    String end = readings[i].substring( dot+1);
                    addEntry( readings[i].substring( 0, dot) + end, line);
                    addEntry( e.getKanji() + end, line);
                }
            }
        }
    }

    /**
     * Returns the entry for the given kanji. If no match is found, <CODE>null</CODE> will be returned.
     */
    public KanjiDic.Entry lookup( char kanji) {
        return (KanjiDic.Entry) entries.get( new String( new char[] { kanji }));
    }

    /**
     * Returns a list of <CODE>KanjiDic.Entry</CODE> objects for the given key.
     * The key can be a kanji, a reading or a
     * translation. If no match is found, the empty list will be returned.
     */
    public List lookup( String key) {
        List r = null;
        Object o = entries.get( key);
        if (o == null)
            return Collections.EMPTY_LIST;
        else if (o instanceof List) {
            // create list of entries from list of strings
            List original = (List) o;
            r = new ArrayList( original.size());
            for ( Iterator i=original.iterator(); i.hasNext(); )
                r.add( new Entry( (String) i.next(), true));
        }
        else {
            r = new ArrayList( 1);
            r.add( new Entry( (String) o, true));
        }
        return r;
    }

    /**
     * Searches for an entry matching expression. Currently only exact match searches are
     * fully supported, the other search modes will return a superset of exact match search,
     * but not all matches which should be returned.
     */
    public Iterator search( SearchMode mode, Object[] parameters) throws SearchException {
        if (mode instanceof ExpressionSearchModes)
            return searchExpression( mode, (String) parameters[0], 
                                     (SearchFieldSelection) parameters[1]);
        else
            throw new UnsupportedSearchModeException( mode);
    }

    public Iterator searchExpression( SearchMode mode, String expression, 
                                      SearchFieldSelection fields) {
        List result = new ArrayList( 10);

        Object o = entries.get( expression);
        if (o != null) {
            if (o instanceof List) // list of entries
                for ( Iterator i=((List) o).iterator(); i.hasNext(); ) {
                    constructResult( expression, result, (String) i.next(), searchmode, resultmode);
                }
            else { // single entry
                constructResult( expression, result, (String) o, searchmode, resultmode);
            }
        }

        return result;
    }

    public boolean supports( SearchMode searchmode, boolean fully) {
        if (searchmode==ExpressionSearchModes.EXACT)
            return true;
        else if (fully && searchmode instanceof ExpressionSearchModes)
            return true;
        else
            return false;
    }

    public SearchFieldSelection getSupportedFields( SearchMode searchmode) {
        return new SearchFieldSelection( true, true, true, true, false);
    }

    /**
     * Creates the DictionaryEntry/WordReadingPair objects for the entry and adds them to
     * the list of results. The entry is checked against the search mode.
     *
     * @param expression The expression which is looked up.
     * @param result List of results to which the created objects are added.
     * @param entry The entry for which the objects will be created.
     * @param searchmode The search mode.
     */
    protected void constructResult( String expression, List result, String entry, short searchmode,
                                    short resultmode) {
        Entry e = new Entry( entry, true);
        final String kanji = new String( new char[] { e.getKanji() });
        boolean kanjiMatches = kanji.equals( expression);
        
        if (e.getTranslations() != null) { // create DictionaryEntries
            String[] translations = e.getTranslations();
            boolean translationMatches = false;
            if (!kanjiMatches) {
                for ( int i=0; i<translations.length; i++) {
                    if (translations[i].equals( expression)) {
                        translationMatches = true;
                        break;
                    }
                }
            }
            
            // the construction of the dictionary entry can change the kanji part by adding
            // okurigana, so we have to re-test the match for exact matches.
            if (e.getReadings() != null) { // may be null if there are only nanori readings
                String[] readings = e.getReadings();
                for ( int i=0; i<readings.length; i++) {
                    DictionaryEntry de = createDictionaryEntry( kanji, readings[i], translations);
                    boolean readingMatches = false;
                    if (!(kanjiMatches || translationMatches))
                        readingMatches = expression.equals( de.getReading()) ||
                            expression.equals( de.getWord());
                    if ((kanjiMatches && (searchmode==SEARCH_STARTS_WITH || 
                                          searchmode==SEARCH_ANY_MATCHES ||
                                          de.getWord().equals( kanji))) || 
                        translationMatches || readingMatches)
                        addResult( result, de, resultmode);
                }
            }
        }
        else { // create WordReadingPairs
            if (e.getReadings() != null) { // may be null if there are only nanori readings
                String[] readings = e.getReadings();
                for ( int i=0; i<readings.length; i++) {
                    WordReadingPair wrp = createWordReadingPair( kanji, readings[i]);
                    boolean readingMatches = false;
                    if (!kanjiMatches)
                        readingMatches = expression.equals( wrp.getReading()) ||
                            expression.equals( wrp.getWord());
                    if ((kanjiMatches && (searchmode==SEARCH_STARTS_WITH || 
                                          searchmode==SEARCH_ANY_MATCHES ||
                                          wrp.getWord().equals( kanji))) ||
                        readingMatches)
                        addResult( result, wrp, resultmode);
                }
            }
        }

        if (e.getNanoriReadings() != null) {
            String[] readings = e.getNanoriReadings();
            String[] nanori = new String[] { messages.getString( "kanjidic.nanori") };
            for ( int i=0; i<readings.length; i++) {
                DictionaryEntry de = createDictionaryEntry( kanji, readings[i], nanori);
                boolean readingMatches = false;
                if (!kanjiMatches)
                    readingMatches = expression.equals( de.getReading()) ||
                        expression.equals( de.getWord());
                if ((kanjiMatches && (searchmode==SEARCH_STARTS_WITH || searchmode==SEARCH_ANY_MATCHES ||
                                      de.getWord().equals( kanji))) || readingMatches)
                    addResult( result, de, resultmode);
            }
        }
        if (e.getRadicalName() != null) {
            DictionaryEntry de = createDictionaryEntry( kanji, e.getRadicalName(),
                                                        new String[] { messages.getString
                                                        ( "kanjidic.radicalname") });
            boolean readingMatches = false;
            if (!kanjiMatches)
                readingMatches = expression.equals( de.getReading()) ||
                    expression.equals( de.getWord());
            if ((kanjiMatches && (searchmode==SEARCH_STARTS_WITH || searchmode==SEARCH_ANY_MATCHES ||
                                  de.getWord().equals( kanji))) || readingMatches)
                addResult( result, de, resultmode);
        }
    }

    /**
     * Creates a dictionary entry. If the reading has an okurigana part (separated by a '.'),
     * the word will be kanji+okurigana and the dot in the reading will be removed.
     *
     * @param kanji The kanji of the entry.
     * @param reading The reading of the entry.
     * @param translations The translations of the entry.
     */
    protected DictionaryEntry createDictionaryEntry( String kanji, String reading,
                                                     String[] translations) {
        int dot = reading.indexOf( '.');
        if (dot == -1)
            return new DefaultDictionaryEntry( kanji, reading, translations, this);
        else
            return new DefaultDictionaryEntry( kanji + reading.substring( dot+1),
                                               reading.substring( 0, dot) + reading.substring( dot+1),
                                               translations, this);
    }

    /**
     * Creates a word-reading pair. If the reading has an okurigana part (separated by a '.'),
     * the word will be kanji+okurigana and the dot in the reading will be removed.
     *
     * @param kanji The kanji of the entry.
     * @param reading The reading of the entry.
     */
    protected WordReadingPair createWordReadingPair( String kanji, String reading) {
        final String k;
        final String r;
        int dot = reading.indexOf( '.');
        if (dot == -1) {
            k = kanji;
            r = reading;
        }
        else {
            k = kanji + reading.substring( dot+1);
            r = reading.substring( 0, dot) + reading.substring( dot+1);
        }
        return new WordReadingPair() {
                public String getWord() { return k; }
                public String getReading() { return r; }
                public Dictionary getDictionary() { return KanjiDic.this; }
                public String toString() { return DefaultDictionaryEntry.toString( this); }
            };
    }

    /**
     * Add a dictionary entry to the list of results in a form determined by the result mode.
     */
    protected void addResult( List result, WordReadingPair entry, short resultmode) {
        result.add( entry);
    }
    
    /**
     * Returns the path to the dictionary file.
     *
     * @return The path to the dictionary file.
     */
    public String getDictionaryFile() {
        return dicfile;
    }

    /**
     * Returns the name of this dictionary. This is the filename of the dictionary file.
     *
     * @return The name of this dictionary.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this dictionary.
     *
     * @return A string representation of this dictionary.
     */
    public String toString() {
        return "KANJIDIC " + name;
    }

    public void dispose() {}

    public boolean equals( Object o) {
        try {
            return new File(((KanjiDic) o).dicfile).equals( new File( dicfile));
        } catch (ClassCastException ex) {
            return false;
        }
    }
} // class KanjiDic
