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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.text.MessageFormat;

/**
 * Base class for dictionaries stored in a local file with separate index file.
 * The J2 1.4 NIO API is used for file access.
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
        private java.util.regex.Pattern pattern;
        private float maxConfidence;
        private int lookAtLength;
        private java.lang.reflect.Constructor dictionaryConstructor;

        /**
         * Creates a new implementation instance for some file based dictionary format.
         *
         * @param name Name of the dictionary format.
         * @param encoding Character encoding used by dictionary files.
         * @param pattern Regular expression to match against the start of a tested file.
         * @param maxConfidence The confidence which is returned when the <code>linePattern</code>
         *                      matches.
         * @param lookAtLength Number of bytes read from the tested file. If the file is shorter
         *                     than the given length, the file will be read completely.
         * @param dictionaryConstructor Constructor used to create a new dictionary instance for 
         *        a matching file. The constructor must take a single <code>File</code> as parameter.
         */
        public Implementation( String name, String encoding, java.util.regex.Pattern pattern,
                               float maxConfidence, int lookAtLength, 
                               java.lang.reflect.Constructor dictionaryConstructor) {
            this.name = name;
            this.encoding = encoding;
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
         * Creates a new dictionary instance using {@link #dictionaryConstructor dictionaryConstructor}.
         * The constructor is passed a <code>File</code> wrapping the <code>descriptor</code> as only
         * argument.
         */
        public Dictionary createInstance( String descriptor) 
            throws DictionaryFactory.InstantiationException {
            try {
                return (Dictionary) dictionaryConstructor.newInstance
                    ( new Object[] { new File( descriptor) });
            } catch (Exception ex) {
                throw new DictionaryFactory.InstantiationException( ex);
            }
        }
    } // class Implementation

    /**
     * Implementations of this carry state for the byte conversion in 
     * {@link FileBasedDictionary#convertByteInChar(int,boolean,ByteConverterState) convertByteInChar}.
     *
     * @see #newByteConverterState()
     */
    protected static interface ByteConverterState {
        /**
         * Sets the state information to it's original setting.
         */
        void reset();
    }

    /**
     * Filename extension of a JJDX-format index. Will be added to the filename of the
     * dictionary.
     */
    public final static String JJDX_EXTENSION = ".jjdx";
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
     * Index file mapped into a byte buffer.
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
     *                    false, you have to call {@link #buildIndex(boolean) buildIndex} to create the
     *                    index manually.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    protected FileBasedDictionary( File dicfile, boolean createindex) throws IOException {
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
        dictionary = dicchannel.map( FileChannel.MapMode.READ_ONLY, 0, dicchannel.size());

        // prepare the index
        indexfile = new File( dicfile.getAbsolutePath() + JJDX_EXTENSION);
        try {
            // rebuild the index file if it does not exist or the dictionary file was modified
            // after the index was created
            if (!indexfile.exists() || dicfile.lastModified()>indexfile.lastModified()) {
                if (createindex)
                    buildIndex( true); // this also initializes the index member variable
            }
            else {
                indexchannel = new FileInputStream( indexfile).getChannel();
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
                ByteOrder order = ByteOrder.BIG_ENDIAN;
                if (version >= JJDX_VERSION) { // byte order is introduced in JJDX 2001
                    if (indexbuf.getInt() == JJDX_LITTLE_ENDIAN)
                        order = ByteOrder.LITTLE_ENDIAN;
                }

                // move to index start
                indexbuf.position( offset);
                indexbuf.order( order);
                index = indexbuf.asIntBuffer();
            }
        } catch (Throwable ex) {
            dicchannel.close();
            IOException ex2 = new IOException();
            ex2.initCause( ex);
            throw ex2;
        }
    }

    /**
     * Returns the name of the dictionary file encoding used by this dictionary.
     */
    public abstract String getEncoding();

    public List search( String expression, short mode) throws SearchException {
        List result = new ArrayList( 10);

        // do a binary search through the index file
        try {
            byte[] exprBytes = expression.getBytes( getEncoding());
            int match = findMatch( exprBytes);

            if (match != -1) {
                int firstmatch = findMatch( exprBytes, match, true);
                int lastmatch = findMatch( exprBytes, match, false);
                //System.err.println( "all matches in EDictNIO: " + (lastmatch-firstmatch+1));
                //System.err.println( "EdictNIO: " + firstmatch + "/" + lastmatch);
                
                // Several index entries can point to the same entry line. For example
                // if a word contains the same kanji twice, and words with this kanji are looked up,
                // one index entry is found for the first occurrence and one for the second. To prevent
                // adding the entry multiple times, entries are stored in a set.
                Set seenEntries = new HashSet( 51);

                // read all matching entries
                for ( match=firstmatch; match<=lastmatch; match++) {
                    int start = index.get( match);

                    // test if entry matches search mode
                    if (mode == SEARCH_EXACT_MATCHES ||
                        mode == SEARCH_STARTS_WITH) {
                        // test if preceeding character marks beginning of word entry
                        if (start>0 && !isEntryStart( start))
                            continue;
                    }
                    if (mode == SEARCH_EXACT_MATCHES ||
                        mode == SEARCH_ENDS_WITH) {
                        // test if following character marks end of word entry
                        if (start+exprBytes.length+1<dicchannel.size() &&
                            !isEntryEnd( start+exprBytes.length))
                            continue;
                    }

                    // find beginning of entry line
                    while (start>0 && !isEntrySeparator( dictionary.get( start-1)))
                        start--;

                    Integer starti = new Integer( start);
                    if (seenEntries.contains( starti))
                        continue;
                    else
                        seenEntries.add( starti);
                    
                    // create a byte buffer which holds only the entry
                    dictionary.position( start);
                    ByteBuffer entrybuf = dictionary.slice();
                    entrybuf.position( index.get( match)-start);
                    // find end of entry line
                    boolean moveBack = true;
                    try {
                        while (!isEntrySeparator( entrybuf.get()))
                            ; // entrybuf.get() advances the loop
                    } catch (BufferUnderflowException ex) {
                        // end of buffer
                        moveBack = false;
                    }
                    if (moveBack)
                        entrybuf.position( entrybuf.position()-1); // move back to last byte of entry
                    entrybuf.flip(); // truncate the buffer to the end of the entry line

                    String entry;
                    if (decoder != null) {
                        decoder.reset();
                        entry = decoder.decode( entrybuf).toString();
                    }
                    else { // NIO does not support the required encoding
                        byte[] chars = new byte[entrybuf.limit()];
                        entrybuf.get( chars);
                        entry = new String( chars, getEncoding());
                    }
                    parseEntry( result, entry);
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
    protected abstract boolean isEntryStart( int offset);

    /**
     * Test if the character is the last character of an entry in the dictionary.
     *
     * @param offset Offset in the dictionary to the first byte after the entry.
     *               Guaranteed to be &lt; dictionary size-1.
     */
    protected abstract boolean isEntryEnd( int offset);

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
     * Create <CODE>DictionaryEntries</CODE> from the entry string and add them to the
     * result list.
     */
    protected abstract void parseEntry( List result, String entry);

    /**
     * Returns the index of an index entry which matches a word. If there is more than one match,
     * it is not defined which match is returned. If no match is found, <code>-1</code>
     * is returned.
     */
    protected int findMatch( byte[] expression) {
        // do a binary search
        int from = 0;
        int to = index.limit()-1;
        int match = -1;
        int curr;
        // search matching entry
        do {
            curr = (to-from)/2 + from;
            
            int c = compare( expression, index.get( curr));
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
    protected int findMatch( byte[] expression, int match, boolean first) {
        int direction = first ? -1 : 1;
        
        try {
            while (compare( expression, index.get( match+direction)) == 0)
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
     * @param printMessage <CODE>true</CODE>, if an informational message should be printed on stderr.
     */
    public void buildIndex( boolean printMessage) throws IOException {
        if (printMessage)
            System.err.println( MessageFormat.format( messages.getString( "edict.buildindex"), 
                                                      new String[] { getName() }));

        indexchannel = new RandomAccessFile( indexfile, "rw").getChannel();
        MappedByteBuffer indexbuf = indexchannel.map( FileChannel.MapMode.READ_WRITE,
                                                      0, INDEX_OFFSET + 50000*4);
        
        // write index header (big endian format)
        indexbuf.putInt( JJDX_VERSION);
        indexbuf.putInt( INDEX_OFFSET); // offset to first index entry
        indexbuf.putInt( 0); // number of index entries (currently unknown)
        // use platform's native byte order
        indexbuf.putInt( (ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN) ? 
                         JJDX_BIG_ENDIAN : JJDX_LITTLE_ENDIAN);
        indexbuf.order( ByteOrder.nativeOrder());
        
        // add index entries
        index = indexbuf.asIntBuffer();
        int entries = addIndexRange( 0, (int) dicchannel.size());
        quicksortIndex( 0, entries-1);

        // write number of entries
        indexbuf.order( ByteOrder.BIG_ENDIAN);
        indexbuf.putInt( 4*2, entries);
        indexchannel.truncate( INDEX_OFFSET + entries*4);
        indexchannel.force( true);
        // map index to new size
        index = indexchannel.map( FileChannel.MapMode.READ_WRITE, INDEX_OFFSET,
                                  indexchannel.size()).order( ByteOrder.nativeOrder())
            .asIntBuffer();
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
     * @param start Byte offset in the dictionary where the parsing should start (inclusive).
     * @param end Byte offset in the dictionary where the parsing should end (exclusive).
     * @return The number of index entries created.
     */
    protected int addIndexRange( int start, int end) throws IOException {
        int indexsize = 0;
        dictionary.position( start);
        boolean inWord = false;
        int wordstart = start;
        int wordlength = 0;
        int wantedlength = 2;
        try {
            while (dictionary.position() < dicchannel.size()) {
                int position = dictionary.position();
                int type = readNextCharacter( inWord); // changes position
                if (inWord) {
                    if (type < 0) {
                        // end of word; add index entry the current word is long enough
                        if (wordlength >= wantedlength) {
                            addIndexEntry( wordstart);
                            indexsize++;
                        }
                        inWord = false;
                    }
                    else if (type == 0) {
                        // since we are inWord, this is guaranteed not to be the first character
                        // of an index word, the index entry won't be added once for type==0 and once
                        // for type==-1 at the end of the word
                        addIndexEntry( position);
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
     * Reads a character in the dictionary file and decides how it should be treated
     * for index creation. The character must be read from the current position of
     * {@link #dictionary dictionary}. An arbitrary number of bytes may be skipped by the method
     * after the character is read in order to skip over non-indexable dictionary fields.
     *
     * @param inWord <CODE>true</CODE> if the current character follows a character in a indexable
     *               word. This can be used to change the meaning of characters depending on their position.
     * @return -1, if the character is not part of an indexable word; 0 if an index entry should
     *         be created immediately at the character position; n (&gt;0): add an index entry
     *         for the word containing this character if it has more than n characters.
     * @exception BufferUnderflowException When the end of the dictionary is reached.
     */
    protected abstract int readNextCharacter( boolean inWord) throws BufferUnderflowException;

    /**
     * Add a word entry to the index. This method will be called by 
     * {@link #addIndexRange(int,int) addIndexRange} during index creation.
     *
     * @param offset Offset in the dictionary file where the word starts.
     */
    protected void addIndexEntry( int offset) throws IOException {
        try {
            index.put( offset);
        } catch (BufferOverflowException ex) {
            // increase file size
            int position = index.position();
            index = indexchannel.map( FileChannel.MapMode.READ_WRITE, INDEX_OFFSET,
                                      indexchannel.size()*2).order( ByteOrder.nativeOrder())
                .asIntBuffer();
            index.position( position);
            index.put( offset);
        }
    }

    /**
     * Sorts a part of the index array using randomized quicksort. Call this with
     * (0, index lenght-1) to sort the whole index.
     */
    protected void quicksortIndex( int left, int right) throws IOException {
        if (left >= right)
            return;

        int middle = left + random.nextInt( right-left+1);
        int mv = index.get( middle);
        index.put( middle, index.get( left));

        int l = left + 1; // l is the first index which compares greater mv
        for ( int i=l; i<=right; i++) {
            if (compare( mv, index.get( i)) > 0) {
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
            quicksortIndex( left, l-1);
            quicksortIndex( l+1, right);
        }
        else {
            quicksortIndex( l+1, right);
            quicksortIndex( left, l-1);
        }
    }


    /**
     * Lexicographic comparison of strings in the dictionary. 
     * <p>
     * This is a special comparison
     * which is used when creating the dictionary index with quicksort. Instead of stopping
     * the comparison and returning equality at word boundaries, the method will 
     * continue the comparison until an inequality is found. This is done because this quicksort
     * implementation behaves lousy if there are many equalities.
     * </p><p>
     * {@link #convertByteInChar(int) convertByteInChar} is called for every tested byte.
     * This can be used to implement e.g. Uppercase->Lowercase or Katakana->Hiragana conversion
     * for a given file encoding.
     * </p>
     *
     * @param i1 Offset in the dictionary to the first string.
     * @param i2 Offset in the dictionary to the second string.
     * @return -1 if i1&lt;i2, 1 if i1&gt;i2, 0 for equality.
     */
    protected int compare( int i1, int i2) {
        ByteConverterState state = newByteConverterState();
        int len1 = (int) dictionary.capacity() - i1;
        int len2 = (int) dictionary.capacity() - i2;
        int length = Math.min( len1, len2);
        for ( int i=0; i<length; i++) {
            int b1 = convertByteInChar( byteToUnsignedByte( dictionary.get( i1+i)), false, state);
            int b2 = convertByteInChar( byteToUnsignedByte( dictionary.get( i2+i)), true, state);
            if (b1 < b2)
                return -1;
            else if (b1 > b2)
                return 1;
        }
        // equal along the length
        if (len1 < len2)
            return -1;
        else if (len1 > len2)
            return 1;
        else
            return 0; // equality
    }

    /**
     * Lexicographic comparison of a byte array with a sequence in the dictionary of the same length
     * as the byte array.
     * <p>
     * {@link #convertByteInChar(int) convertByteInChar} is called for every tested byte.
     * This can be used to implement e.g. Uppercase->Lowercase or Katakana->Hiragana conversion
     * for a given file encoding.
     * </p>
     *
     * @param str The byte array containing the word to compare.
     * @param off Offset in the dictionary file to the position to compare.
     * @return -1 if the byte array is smaller than the sequence in the dictionary; 1 if it is larger
     *         or 0 if they are equal.
     */
    protected int compare( byte[] str, int off) {
        ByteConverterState state = newByteConverterState();
        dictionary.position( off);
        for ( int i=0; i<str.length; i++) {
            int b1 = convertByteInChar( byteToUnsignedByte( str[i]), false, state);
            int b2;
            try {
                b2 = convertByteInChar( byteToUnsignedByte( dictionary.get()), true, state);
            } catch (BufferUnderflowException ex) {
                // end of dictionary file, dictionary string is prefix of str
                return 1;
            }
            if (b1 < b2)
                return -1;
            else if (b1 > b2)
                return 1;
        }
        return 0; // equality
    }

    /**
     * Change a byte which is part of a multibyte encoded char in some way. 
     * <p>
     * This method can be overridden
     * for example to change uppercase ASCII characters to lowercase or katakana to hiragana.
     * This implementation returns the byte unmodified.
     * </p><p>
     * The method is called from {@link #compare(byte[],int) compare( str, off)} and
     * {@link #compare(int,int) compare( i1, i2)} for every tested byte, alternating between
     * a byte in the first and second tested array.
     * </p>
     *
     * @param b The byte which should be modified.
     * @param last <code>true</code> if this is the last byte at the same byte position in a multibyte
     *             character as the previous bytes.
     * @param state Object which can be used to store converter state information. For every call
     *              to <code>compare</code>, a unique state object is created and passed to this method.
     *              Comparison is therefore threadsafe.
     */
    protected int convertByteInChar( int b, boolean last, ByteConverterState state) {
        return b;
    }

    /**
     * Creates a new state object for byte conversion. 
     * If the derived class does not need state information, the method may return <code>null</code>.
     *
     * @return <code>null</code>
     */
    protected ByteConverterState newByteConverterState() {
        return null;
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
