/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.util.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.text.MessageFormat;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Base class for dictionaries stored in a local file with separate index file.
 * The J2 1.4 NIO API is used for file access.
 * <h3>JJDX index file format</h3>
 * <p>
 *   A JJDX index file is an array of byte offsets pointing to words in the corresponding
 *   dictionary file. Entries are sorted lexicographically to allow fast word lookup using
 *   binary search. The array is prefixed by a header containing information about the index file.
 *   The JJDX format itself is independent of a specific dictionary field format or character
 *   encoding.
 * </p>
 * <h4>JJDX file header</h4>
 * <p>
 *   The header of an index file consists of several fields containing information about
 *   the index file. Each field is a 4-byte long signed integer stored in big-endian byte order.
 * </p>
 * <table border="1"><tr><th>Field</th><th>Description</th><th>Since</th></tr>
 * <tr><td>Version</td>
 * <td>Version of this JJDX index file format</td>
 * <td>1001</td></tr>
 * <tr><td>Offset</td>
 * <td>
 *   Offset in bytes from the start of the index file to the first index entry.
 *   This is effectively the length of the header.
 *   The information can be used to find the first index entry even if the index
 *   format version differs from the one supported by this index.
 * </td><td>1001</td></tr>
 * <tr><td>Size</td>
 * <td>
 *   Number of index entries (excluding the header).
 *   The size of the index file is Offset+Size*4.
 * </td><td>1001</td></tr>
 * <tr><td>Byte-Order</td>
 * <td>
 *   Byte order used to store the index entries. 1 for big-endian, 2 for
 *   little-endian. (The header always uses big-endian.)
 * </td><td>2001</td></tr>
 * </table>
 * <p>
 *   Currently existing index file versions are 1001, used by JGloss 1.0.2 and earlier, and
 *   2001, used since JGloss 1.0.3.
 * </p>
 * <h4>Index entries</h4>
 * <p>
 *   Index entries are stored as an array of 4-byte long signed integers. Each index entry is the
 *   offset in bytes in the dictionary file to the indexed word (the length on the word is not
 *   specified in the index). The offset is 0-based. Since JJDX version 2001, the byte order
 *   of the index entries is specified in the header. Index entries in 1001 files are always stored 
 *   in big-endian format.
 * </p><p>
 *   Index entries are sorted lexicographically to enable fast lookups. 
 *   The precise rules of lexicographical comparison of two entries are not
 *   specified and depend on the dictionary implementation. For example, an implementation may
 *   apply uppercase to lowercase or katakana to hiragana conversion, changing the sorting order.
 *   If a different implementation wants to use the index file, it has to take care to replicate
 *   this behavior for searches to succeed.
 * </p>
 *
 * @author Michael Koch
 */
public abstract class FileBasedDictionary implements Dictionary {
    /**
     * Generic implementation for file-based dictionaries. The {@link #isInstance(String) isInstance}
     * test reads some bytes from the file to test, converts them to a string using the
     * superclass-supplied encoding and tests it against a regular expression.
     */
    public static class Implementation implements DictionaryFactory.Implementation {
        private String name;
        private String encoding;
        private boolean doEncodingTest;
        private java.util.regex.Pattern pattern;
        private float maxConfidence;
        private int lookAtLength;
        private Constructor dictionaryConstructor;

        /**
         * Creates a new implementation instance for some file based dictionary format.
         *
         * @param name Name of the dictionary format.
         * @param encoding Character encoding used by dictionary files.
         * @param doEncodingTest If <code>true</code>, Use the 
         *        {@link CharacterEncodingDetector CharacterEncodingDetector} to guess the encoding
         *        of the tested file and return <code>ZERO_CONFIDENCE</code> if it does not match
         *        the encoding.
         * @param pattern Regular expression to match against the start of a tested file.
         * @param maxConfidence The confidence which is returned when the <code>linePattern</code>
         *                      matches.
         * @param lookAtLength Number of bytes read from the tested file. If the file is shorter
         *                     than the given length, the file will be read completely.
         * @param dictionaryConstructor Constructor used to create a new dictionary instance for 
         *        a matching file. The constructor must take a single <code>File</code> as parameter.
         */
        public Implementation( String name, String encoding, boolean doEncodingTest,
                               java.util.regex.Pattern pattern,
                               float maxConfidence, int lookAtLength, 
                               Constructor dictionaryConstructor) {
            this.name = name;
            this.encoding = encoding;
            this.doEncodingTest = doEncodingTest;
            this.pattern = pattern;
            this.maxConfidence = maxConfidence;
            this.lookAtLength = lookAtLength;
            this.dictionaryConstructor = dictionaryConstructor;
        }

        /**
         * Test if the descriptor points to a dictionary file supported by this implementation.
         * The first {@link #lookAtLength lookAtLength} bytes are read from the file pointed to
         * by the descriptor. The byte array is converted to a string using {@link #encoding encoding}
         * and the {@link #pattern pattern} is tested against it. If the pattern matches, the file
         * is accepted and {@link #maxConfidence maxConfidence} is returned.
         *
         * @see #Implementation(String,String,java.util.regex.Pattern,float,int,
         *                      java.lang.reflect.Constructor)
         */
        public float isInstance( String descriptor) {
            try {
                File dic = new File( descriptor);
                int headlen = (int) Math.min( lookAtLength, dic.length());
                byte[] buffer = new byte[headlen];
                DataInputStream in = new DataInputStream( new FileInputStream( dic));
                try {
                    in.readFully( buffer);
                } finally {
                    in.close();
                }

                if (doEncodingTest && 
                    !encoding.equals( CharacterEncodingDetector.guessEncodingName( buffer)))
                    return ZERO_CONFIDENCE;
                if (pattern.matcher( new String( buffer, encoding)).find())
                    return maxConfidence;
            } catch (IOException ex) {}

            return ZERO_CONFIDENCE;
        }

        /**
         * Returns the confidence passed to the constructor.
         */
        public float getMaxConfidence() { return maxConfidence; }
        /**
         * Returns the dictionary format name passed to the constructor.
         */
        public String getName() { return name; }

        /**
         * Creates a new dictionary instance using 
         * {@link dictionaryConstructor dictionaryConstructor}.
         * The constructor is passed a <code>File</code> wrapping the <code>descriptor</code> as only
         * argument.
         */
        public Dictionary createInstance( String descriptor) 
            throws DictionaryFactory.InstantiationException {
            try {
                return (Dictionary) dictionaryConstructor.newInstance
                    ( new Object[] { new File( descriptor) });
            } catch (InvocationTargetException ex) {
                throw new DictionaryFactory.InstantiationException
                    ( (Exception) ((InvocationTargetException) ex).getTargetException());
            } catch (Exception ex) {
                // should never happen
                throw new DictionaryFactory.InstantiationException( ex);
            }
        }

        public Class getDictionaryClass( String descriptor) {
            return dictionaryConstructor.getDeclaringClass();
        }
    } // class Implementation

    /**
     * Filename extension of a JJDX-format index. Will be added to the filename of the
     * dictionary to derive the index file name.
     */
    public final static String INDEX_EXTENSION = ".jjdx";
    /**
     * Current version of the JJDX format.
     */
    public final static int JJDX_VERSION = 2001;
    /**
     * Field value meaning that the index is in big-endian format.
     */
    public final static int JJDX_BIG_ENDIAN = 1;
    /**
     * Field value meaning that the index is in little-endian format.
     */
    public final static int JJDX_LITTLE_ENDIAN = 2;
    /**
     * Offset in bytes to the first index entry in index version 2001. 4 header entries with
     * 4 bytes each.
     */
    public final static int INDEX_OFFSET = 4*4;

    /**
     * Localizable message resource.
     */
    protected final static ResourceBundle messages = 
        ResourceBundle.getBundle( "resources/messages-dictionary");

    /**
     * File which holds the dictionary.
     */
    protected File dicfile;
    /**
     * Channel used to access the dictionary.
     */
    protected FileChannel dicchannel;
    /**
     * Size of the dictionary in bytes. This equals dicchannel.size(), which is slow to access.
     */
    protected int dictionarySize;
    /**
     * Dictionary file mapped into a byte buffer.
     */
    protected MappedByteBuffer dictionary;
    /**
     * Decoder initialized to convert a byte array to a char array using the charset of the
     * dictionary.
     */
    protected CharsetDecoder decoder;
    /**
     * File which holds the dictionary index.
     */
    protected File indexfile;
    /**
     * Channel used to access the index.
     */
    protected FileChannel indexchannel;
    /**
     * View of the index file as int buffer.
     */
    protected IntBuffer index;
    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    protected String name;
    /**
     * Random number generator. Used for randomized quicksort in index creation.
     */
    protected static Random random = new Random();
    
    /**
     * Initializes the dictionary by mapping the dictionary and file into memory and
     * possibly creatig the indexfile if neccessary.
     *
     * @dicfile File which holds the dictionary.
     * @param createindex Flag if the index should be automatically created and written to disk
     *                    if no index file exists. A write will not be attempted if the directory
     *                    is not writable. If no index file exists and createindex is
     *                    false, you have to call {@link #buildIndex(File) buildIndex} to create the
     *                    index manually.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    protected FileBasedDictionary( File dicfile, boolean createindex) 
        throws IOException, IndexCreationException {
        this.dicfile = dicfile;
        name = dicfile.getName();
        System.err.println( MessageFormat.format( messages.getString( "dictionary.load"),
                                                  new String[] { name }));

        try {
            decoder = Charset.forName( getEncoding()).newDecoder();
        } catch (UnsupportedCharsetException ex) {
            // bah...
            // leave decoder==null and use String constructor for byte->char conversion
        }

        // load the dictionary
        dicchannel = new FileInputStream( dicfile).getChannel();
        dictionarySize = (int) dicchannel.size();
        dictionary = dicchannel.map( FileChannel.MapMode.READ_ONLY, 0, dictionarySize);

        // prepare the index
        indexfile = new File( dicfile.getAbsolutePath() + INDEX_EXTENSION);
        // rebuild the index file if it does not exist or the dictionary file was modified
        // after the index was created
        if (!indexfile.exists() || dicfile.lastModified()>indexfile.lastModified()) {
            if (createindex) try {
                buildIndex( indexfile); // this also initializes the index member variables
            } catch (IOException ex) {
                throw new IndexCreationException();
            }
        }
        else try {
            indexchannel = new RandomAccessFile( indexfile, "r").getChannel();
            MappedByteBuffer indexbuf = indexchannel.map
                ( FileChannel.MapMode.READ_ONLY, 0, indexchannel.size());
            // read index header
            int version = indexbuf.getInt();
            if (version > JJDX_VERSION) {
                System.err.println( MessageFormat.format( messages.getString
                                                          ( "edict.warning.jjdxversion"),
                                                          new String[] { getName() }));
            }
            int offset = indexbuf.getInt();
            int size = indexbuf.getInt();
            
            // sanity check the index file size
            if (offset + size*4 != indexfile.length()) {
                System.err.println( MessageFormat.format( messages.getString
                                                          ( "edict.warning.jjdxsize"),
                                                          new String[] { getName() }));
                // throw an exception which will be caught by the enclosing block and
                // trigger a rebuild of the index file
                throw new Exception();
            }
            
            ByteOrder order = ByteOrder.BIG_ENDIAN;
            if (version >= JJDX_VERSION) { // byte order is introduced in JJDX 2001
                if (indexbuf.getInt() == JJDX_LITTLE_ENDIAN)
                    order = ByteOrder.LITTLE_ENDIAN;
            }
            
            // move to index start
            indexbuf.position( offset);
            indexbuf.order( order);
            index = indexbuf.asIntBuffer();
        } catch (Exception ex) {
            // the index file is broken, try to rebuild it
            if (indexchannel != null)
                indexchannel.close();
            if (createindex) try {
                buildIndex( indexfile);
                return;
            } catch (IOException ex2) {
                dicchannel.close();
                ex2.printStackTrace();
            }
            throw new IndexCreationException();
        }
    }

    /**
     * Returns the name of the dictionary file encoding used by this dictionary.
     */
    public abstract String getEncoding();

    public List search( String expression, short searchmode, short resultmode) throws SearchException {
        List result = new ArrayList( 10);
        expression = escape( expression);

        // do a binary search through the index file
        try {
            ByteBuffer exprbuf = ByteBuffer.wrap( expression.getBytes( getEncoding()));
            byte[] entry = new byte[4096];
            int match;
            match = findMatch( exprbuf);

            if (match != -1) {
                int firstmatch;
                int lastmatch;
                firstmatch = findMatch( exprbuf, match, true);
                lastmatch = findMatch( exprbuf, match, false);
                
                // Several index entries can point to the same entry line. For example
                // if a word contains the same kanji twice, and words with this kanji are looked up,
                // one index entry is found for the first occurrence and one for the second. To prevent
                // adding the entry multiple times, entries are stored in a set.
                Set seenEntries = new HashSet( 51);

                // read all matching entries
                for ( match=firstmatch; match<=lastmatch; match++) {
                    int matchstart = index.get( match);
                    int start = matchstart; // start of entry (inclusive)
                    int end = matchstart+1; // end of entry (exclusive)

                    // test if entry matches search mode
                    if (searchmode == SEARCH_EXACT_MATCHES ||
                        searchmode == SEARCH_STARTS_WITH) {
                        // test if preceeding character marks beginning of word entry
                        if (start>0 && !isWordStart( start))
                            continue;
                    }
                    if (searchmode == SEARCH_EXACT_MATCHES ||
                        searchmode == SEARCH_ENDS_WITH) {
                        // test if following character marks end of word entry
                        if (start+exprbuf.capacity()+1<dictionarySize &&
                            !isWordEnd( start+exprbuf.capacity()))
                            continue;
                    }

                    // Find beginning of entry line by searching backwards for the entry separator.
                    // Read bytes are stored back to front in entry array.
                    byte b;
                    try {
                        int curr = entry.length-1;
                        while (!isEntrySeparator( b=dictionary.get( start-1))) {
                            try {
                                entry[curr--] = b;
                                start--;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                // array too small for entry; grow it
                                byte[] entry2 = new byte[entry.length*2];
                                System.arraycopy( entry, 0, entry2, entry.length, entry.length);
                                entry = entry2;
                                curr = entry.length-1;
                                // since start is not decremented, the loop will be repeated at the
                                // same position, with loop body succeeding this time
                            }
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        // beginning of dictionary
                    }

                    // test if entry was already found through another index word
                    Integer starti = new Integer( start);
                    if (seenEntries.contains( starti))
                        continue;
                    else
                        seenEntries.add( starti);
                    
                    // move read bytes to beginning of entry array
                    try {
                        System.arraycopy( entry, entry.length-(matchstart-start), entry, 0,
                                          matchstart-start);
                    } catch (IndexOutOfBoundsException ex) {
                        // matchstart==start, no copying neccessary
                    }
                    // read match start char
                    entry[matchstart - start] = dictionary.get( matchstart);

                    // find end of entry line
                    int entrylength = matchstart - start + 1;
                    try {
                        while (!isEntrySeparator( b=dictionary.get( end))) try {
                            entry[entrylength++] = b;
                            end++;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            // array too small for entry; grow it
                            byte[] entry2 = new byte[entry.length*2];
                            System.arraycopy( entry, 0, entry2, 0, entrylength);
                            entry = entry2;
                        }
                    } catch (BufferUnderflowException ex) {
                        // end of dictionary->end of entry
                    }

                    String entrystring;
                    // NIO decoder is faster than new String(), but NIO character encoding support is limited
                    if (decoder != null) {
                        decoder.reset();
                        entrystring = decoder.decode( ByteBuffer.wrap( entry, 0, entrylength)).toString();
                    }
                    else { // NIO does not support the required encoding
                        entrystring = unescape( new String( entry, 0, entrylength, getEncoding()));
                    }
                    parseEntry( result, entrystring, start, matchstart, expression, exprbuf,
                                searchmode, resultmode);
                }
            }
        } catch (IOException ex) {
            throw new SearchException( ex);
        }

        return result;
    }

    /**
     * Test if the character is the first character of an entry in the dictionary.
     *
     * @param offset Offset in the dictionary to the first byte of the entry. Guaranteed to be &gt;0 
     */
    protected abstract boolean isWordStart( int offset);

    /**
     * Test if the character is the last character of an entry in the dictionary.
     *
     * @param offset Offset in the dictionary to the first byte after the entry.
     *               Guaranteed to be &lt; dictionary size-1.
     */
    protected abstract boolean isWordEnd( int offset);

    /**
     * Test if the byte is the separator mark for two entries. This implementation uses
     * ASCII lf (10) and cr (13) bytes as entry separator.
     *
     * @param <CODE>true</CODE> if the byte separates two entries.
     */
    protected boolean isEntrySeparator( byte c) {
        return (c==10 || c==13);
    }

    /**
     * Escape all characters in the string not representable in the dictionary byte array.
     * This can be either chars not representable through the character encoding used by the
     * dictionary, or special characters used as field and entry separators.
     * <p>
     * The method is called to escape search expressions before the search in the dictionary
     * array is begun. The case where different escaping rules are needed for different
     * fields of a dictionary entry is not covered by this escaping scheme.
     * </p><p>
     * This implementation calls {@link #escapeChar(char) escapeChar} for every character
     * in the string and uses a {@link StringTools.unicodeEscape(char) unicode escape sequence}
     * if the method returns <code>true</code>.
     * </p>
     */
    protected String escape( String str) {
        StringBuffer buf = null; // initialize only if needed
        for ( int i=str.length()-1; i>=0; i--) {
            if (escapeChar( str.charAt( i))) {
                if (buf == null)
                    buf = new StringBuffer( str);
                buf.replace( i, i+1, StringTools.unicodeEscape( str.charAt( i)));
            }
        }

        if (buf == null) // no changes
            return str;
        else
            return buf.toString();
    }

    /**
     * Test if a character must be escaped if it is to be used in a dictionary entry.
     *
     * @param c The character to test.
     * @return <code>true</code>, if the character must be escaped.
     */
    protected abstract boolean escapeChar( char c);

    /**
     * Replace any escape sequences in the string by the character represented.
     * This is the inverse method to {@link #escape(String) escape}. This implementation
     * calls {@link StringTools.unicodeUnescape(String) StringTools.unicodeUnescape}.
     */
    protected String unescape( String str) {
        return StringTools.unicodeUnescape( str);
    }
    
    /**
     * Create <CODE>DictionaryEntries</CODE> from the entry string and add them to the
     * result list. The parsing may create more than one dictionary entry. The method is
     * passed information about what was matched and where in the dictionary the match was
     * found so it can decide which entries to add.
     *
     * @param result List to which new dictionary entries should be added.
     * @param entry String from the dictionary file from which the dictionary entries should be
     *              created. The entry is not unescaped, the {@link #unescape(String) unescape} method
     *              must be called for each field if the dictionary needs escaping for special characters.
     * @param entrystart Position in the dictionary of the first byte of the entry.
     * @param where Position in the dictionary where the expression was found.
     * @param expression The expression searched.
     * @param exprbuf Byte Buffer wrapping the expression encoded using the dictionary's character
     *                encoding.
     * @param searchmode The search mode.
     * @param resultmode Determines the wanted type of results.
     */
    protected abstract void parseEntry( List result, String entry, int entrystart, int where,
                                        String expression, ByteBuffer exprbuf, short searchmode,
                                        short resultmode);
    
    /**
     * Returns the index of an index entry which matches a word. If there is more than one match,
     * it is not defined which match is returned. If no match is found, <code>-1</code>
     * is returned.
     */
    protected int findMatch( ByteBuffer expression) {
        // do a binary search
        int from = 0;
        int to = index.limit()-1;
        int match = -1;
        int curr;

        // create an independent instance of dictionary to ensure thread safety
        ByteBuffer dictionary = this.dictionary.duplicate();
        // search matching entry
        do {
            curr = (to-from)/2 + from;

            int c = compare( expression, 0, expression.capacity(), dictionary, index.get( curr));
            if (c > 0)
                from = curr+1;
            else if (c < 0)
                to = curr-1;
            else
                match = curr;
        } while (from<=to && match==-1);

        return match;
    }

    /**
     * Searches the index backwards/forwards from a matching entry to the first/last match of an expression.
     *
     * @param expression Expression to match.
     * @param match Offset in the index to a matching entry.
     * @param first <CODE>true</CODE> if the first matching entry should be returned,
     *              <CODE>false</CODE> if the last matching entry is returned.
     * @return Offset in the index to the first/last matching entry.
     */
    protected int findMatch( ByteBuffer expression, int match, boolean first) {
        int direction = first ? -1 : 1;
        
        // create an independent instance of dictionary to ensure thread safety
        ByteBuffer dictionary = this.dictionary.duplicate();
        try {
            while (compare( expression, 0, expression.capacity(), dictionary, 
                            index.get( match+direction)) == 0)
                match += direction;
        } catch (IndexOutOfBoundsException ex) {
            // match is now either 0 or index.size - 1
        }

        return match;
    }

    public void dispose() {
        try {
            dicchannel.close();
            if (indexchannel != null)
                indexchannel.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create the index file for the dictionary.
     *
     * @param indexfile File to which the index should be saved.
     */
    public void buildIndex( File indexfile) throws IOException {
        synchronized (dictionary) {
            System.err.println( MessageFormat.format( messages.getString( "edict.buildindex"), 
                                                      new String[] { getName() }));

            this.indexfile = indexfile;
            // delete old (invalid) index file if any
            indexfile.delete();

            DataOutputStream indexstream
                = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( indexfile)));
            
            // write index header (big endian format)
            indexstream.writeInt( JJDX_VERSION);
            indexstream.writeInt( INDEX_OFFSET); // offset to first index entry
            indexstream.writeInt( 0); // number of index entries (currently unknown)
            // use platform's native byte order
            indexstream.writeInt( (ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN) ? 
                                  JJDX_BIG_ENDIAN : JJDX_LITTLE_ENDIAN);
            
            // add index entries
            int entries = addIndexRange( indexstream, 0, (int) dictionarySize);

            // reopen the index file for random access and map it into memory
            indexstream.close();
            RandomAccessFile indexra = new RandomAccessFile( indexfile, "rw");
            // write number of entries
            indexra.seek( 4*2);
            indexra.writeInt( entries);
            indexchannel = indexra.getChannel();
            index = indexchannel.map( FileChannel.MapMode.READ_WRITE, INDEX_OFFSET,
                                      indexchannel.size()-INDEX_OFFSET).order( ByteOrder.nativeOrder())
                .asIntBuffer();

            // sort index
            quicksortIndex( 0, entries-1, dictionary, dictionary.duplicate());
        }
    }

    /**
     * Returns the name of this dictionary. This implemenation uses the filename of the dictionary file.
     *
     * @return The name of this dictionary.
     */
    public String getName() {
        return name;
    }

    /**
     * Parses a subset of the dictionary file for index creation.
     * <p>
     * The method iterates over all characters in the range <CODE>start-end</CODE> by repeatedly
     * calling {@link #readNextCharacter(boolean) readNextCharacter}, which decides if the character
     * at the current position is part of an indexable word or not and how long the word has
     * to be in order to be added to the index. Since the position of {@link #dictionary dictionary}
     * is only modified in {@link #readNextCharacter(boolean) readNextCharacter}, the method may skip
     * over an arbitrary number of bytes, e. g. to leave out non-indexable fields.
     * At the end of an indexable word it is added to the index by a call to
     * {@link #addIndexEntry(int) addIndexEntry}.
     * </p>
     *
     * @param out Output stream to which index entries will be appended.
     * @param start Byte offset in the dictionary where the parsing should start (inclusive).
     * @param end Byte offset in the dictionary where the parsing should end (exclusive).
     * @return The number of index entries created.
     */
    protected int addIndexRange( DataOutputStream out, int start, int end) throws IOException {
        int indexsize = 0;
        dictionary.position( start);
        boolean inWord = false;
        int wordstart = start;
        int wordlength = 0;
        int wantedlength = 2;
        try {
            int character = 0;
            skipEntries( dictionary, 0);
            while (dictionary.position() < dictionarySize) {
                int position = dictionary.position();
                character = readCharacter( dictionary);
                int type = isWordCharacter( character, inWord); // changes position
                skipEntries( dictionary, character);
                if (inWord) {
                    if (type < 0) {
                        // end of word; add index entry the current word is long enough
                        if (wordlength >= wantedlength) {
                            addIndexEntry( out, wordstart);
                            indexsize++;
                        }
                        inWord = false;
                    }
                    else if (type == 0) {
                        // since we are inWord, this is guaranteed not to be the first character
                        // of an index word, the index entry won't be added once for type==0 and once
                        // for type==-1 at the end of the word
                        addIndexEntry( out, position);
                        indexsize++;
                        wordlength++;
                    }
                    else { // in normal word
                        // use the minimum of all wanted lengths to decide if the word should be added
                        if (type < wantedlength)
                            type = wantedlength;
                        wordlength++;
                    }
                }
                else { // not in word
                    if (type >= 0) {
                        // start of word
                        inWord = true;
                        wordstart = position;
                        wordlength = 1;
                        wantedlength = type;
                        // if type==0 (add index entry immediately) the short wantedlength guarantees
                        // that an entry will be added at the end of the word
                    }
                }
            }
        } catch (BufferUnderflowException ex) {
            // end of the index reached in readNextCharacter
        }

        return indexsize;
    }

    /**
     * Reads a character from the byte buffer, possibly modifying it. Modifications may be
     * uppercase->lowercase or katakana->hiragana.
     *
     * @param buf Byte buffer from which the character should be read.
     * @return The character read. The character does not have to be in unicode (Java character)
     *         representation, as long as a comparison of two values returned by
     *         the method equals the lexicographic comparison of the represented characters.
     * @exception BufferUnderflowException When the end of the buffer is reached.
     */
    protected abstract int readCharacter( ByteBuffer buf) throws BufferUnderflowException;

    /**
     * Decide how a character should be treated for index creation.
     *
     * @param character The character to decide. This is a character returned by 
     *                  {@link #readCharacter( ByteBuffer) readCharacter}, so it is not neccessary in
     *                  unicode representation.
     * @param inWord <CODE>true</CODE> if the current character follows a character in a indexable
     *               word. This can be used to change the meaning of characters depending on their position.
     * @return -1, if the character is not part of an indexable word; 0 if an index entry should
     *         be created immediately at the character position; n (&gt;0): add an index entry
     *         for the word containing this character if it has more than n characters.
     */
    protected abstract int isWordCharacter( int character, boolean inWord);

    /**
     * Skip an arbitrary number of characters during index creation. This method is called at index
     * creation time before any character is read from the dictionary and after each read character.
     * It can be used to skip entry fields which should not be indexed. This default implementation
     * does nothing.
     *
     * @param buf Skip entries in this buffer by moving the current position of the buffer.
     * @param character The last character read from the buffer, or 0 at the first invocation. The
     *                  character format is dependent on {@link #readCharacter(ByteBuffer) readCharacter}
     *                  and not neccessary unicode.
     */
    protected void skipEntries( ByteBuffer buf, int character) {}

    /**
     * Add a word entry to the index. This method will be called by 
     * {@link #addIndexRange(int,int) addIndexRange} during index creation.
     *
     * @param out Output stream to which the index entry will be written. The integer values will 
     *            be written in the operating system's byte order.
     * @param offset Offset in the dictionary file where the word starts.
     */
    protected void addIndexEntry( DataOutputStream out, int offset) throws IOException {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
            out.writeInt( offset);
        else { // shuffle bytes
            out.write( offset); // writes the low eight bits of offset
            offset >>>= 8;
            out.write( offset);
            offset >>>= 8;
            out.write( offset);
            offset >>>= 8;
            out.write( offset);
        }
    }

    /**
     * Sorts a part of the index array using randomized quicksort. Call this with
     * (0, index lenght-1) to sort the whole index.
     */
    protected void quicksortIndex( int left, int right, ByteBuffer buf1, ByteBuffer buf2) 
        throws IOException {
        if (left >= right)
            return;

        int middle = left + random.nextInt( right-left+1);
        int mv = index.get( middle);
        index.put( middle, index.get( left));

        int l = left + 1; // l is the first index which compares greater mv
        for ( int i=l; i<=right; i++) {
            if (compare( buf1, mv, Integer.MAX_VALUE, buf2, index.get( i)) > 0) {
                if (i > l) {
                    int t = index.get( i);
                    index.put( i, index.get( l));
                    index.put( l, t);
                }
                l++;
            }
        }
        l--;
        index.put( left, index.get( l));
        index.put( l, mv);
        
        // sorting the smaller subset first will keep the stack depth small
        if (l < (left+right)/2) {
            quicksortIndex( left, l-1, buf1, buf2);
            quicksortIndex( l+1, right, buf1, buf2);
        }
        else {
            quicksortIndex( l+1, right, buf1, buf2);
            quicksortIndex( left, l-1, buf1, buf2);
        }
    }


    /**
     * Lexicographic comparison of to byte buffers.
     *
     * @param buf1 First buffer to compare.
     * @param i1 Offset in the first buffer to the first string.
     * @param length maximum number of bytes to compare.
     * @param buf2 Second buffer to compare. Must be different from <code>buf1</code> for the comparison
     *             to succeed. If two positions in the same buffer should be compared, use
     *             <code>buf1.duplicate()</code> to create a second independent instance.
     * @param i2 Offset in the second buffer to the second string.
     * @return -1 if i1&lt;i2, 1 if i1&gt;i2, 0 for equality.
     */
    protected int compare( ByteBuffer buf1, int i1, int length, ByteBuffer buf2, int i2) {
        buf1.position( i1);
        buf2.position( i2);
        int end = (int) Math.min( Integer.MAX_VALUE, (long) i1 + (long) length);
        try {
            while (buf1.position() < end) {
                int b1 = readCharacter( buf1);
                int b2 = readCharacter( buf2);
                if (b1 < b2)
                    return -1;
                else if (b1 > b2)
                    return 1;
            }
        } catch (BufferUnderflowException ex) {
            if (buf1.hasRemaining()) // buf2 is prefix of buf1
                return 1;
            else if (buf2.hasRemaining()) // buf1 is prefix of buf2
                return -1;
            // else equality
        }
        return 0; // equality
    }

    /**
     * Converts the byte value to an int with the value of the 8 bits
     * interpreted as an unsigned byte.
     *
     * @param b The byte value to convert.
     * @return The unsigned byte value of b.
     */
    protected final static int byteToUnsignedByte( byte b) {
        return b & 0xff;
    }
} // class FileBasedDictionary
