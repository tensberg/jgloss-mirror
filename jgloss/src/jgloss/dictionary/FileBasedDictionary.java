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
     * Offset in bytes to the first index entry in index version 2001.
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
            if (!indexfile.canRead()) {
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
                if (version == JJDX_VERSION) { // byte order is introduced in JJDX 2001
                    if (indexbuf.getInt() == JJDX_LITTLE_ENDIAN)
                        order = ByteOrder.LITTLE_ENDIAN;
                }
                System.err.println( "using " + order);
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
                    if (seenEntries.contains( starti)) {
                        match++;
                        continue;
                    }
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
                        // end of buffer, can be safely ignored
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
     * Test if the byte is the separator mark for two entries.
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
     * Searches backward in the index from an already determined match to the first
     * matching entry.
     *
     * @param expression Expression to match.
     * @param match Offset in the index to a matching entry.
     * @param first <CODE>true</CODE> if the first matching entry should be returned,
     *              <CODE>false</CODE> if the last matching entry is returned.
     * @return Offset in the index to the first/last matching entry.
     */
    protected int findMatch( byte[] expression, int match, boolean first) {
        int direction = first ? -1 : 1;
        
        while (match>0 && match<index.capacity() &&
               compare( expression, index.get( match+direction)) == 0)
            match += direction;

        return match;
    }

    public void dispose() {
        try {
            dicchannel.close();
            if (indexchannel != null)
                indexchannel.close();
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
                                                      0, 50004);
        
        // write index header
        System.err.println( "writing header");
        indexbuf.putInt( JJDX_VERSION);
        indexbuf.putInt( INDEX_OFFSET); // offset to first index entry
        indexbuf.putInt( 0); // number of index entries (currently unknown)
        // use platform's native byte order
        System.err.println( ByteOrder.nativeOrder());
        indexbuf.putInt( (ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN) ? 
                         JJDX_BIG_ENDIAN : JJDX_LITTLE_ENDIAN);
        
        // add index entries
        indexbuf.order( ByteOrder.nativeOrder());
        index = indexbuf.asIntBuffer();
        System.err.println( "adding entries");
        int entries = addIndexRange( 0, (int) dicchannel.size());
        System.err.println( entries + " entries");
        System.err.println( "quicksorting entries");
        quicksortIndex( 0, entries-1);
        System.err.println( "end sorting");

        // write number of entries
        indexbuf.order( ByteOrder.BIG_ENDIAN);
        indexbuf.putInt( 4*2, entries);
        indexchannel.truncate( INDEX_OFFSET + entries*4);
        indexchannel.force( true);
    }

    /**
     * Parses a subset of the dictionary file for index creation.
     * <p>
     * The method iterates over all characters in the range <CODE>start-end</CODE> by repeatedly
     * calling {@link #readNextCharacter() readNextCharacter}, which decides if the character
     * at the current position is part of an indexable word or not and how long the word has
     * to be in order to be added to the index. Since the position of {@link #dictionary dictionary}
     * is only modified in {@link #readNextCharacter() readNextCharacter}, the method may skip
     * over an arbitrary number of bytes, e. g. leave out non-indexable fields.
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
        while (dictionary.position() < end) {
            int position = dictionary.position();
            int type = readNextCharacter(); // changes position
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

        return indexsize;
    }

    /**
     * Reads a character in the dictionary file and decides how it should be treated
     * for index creation. The character must be read from the current position of
     * {@link #dictionary dictionary}. An arbitrary number of bytes may be skipped by the method
     * after the character is read in order to skip over non-indexable dictionary fields.
     *
     * @return -1, if the character is not part of an indexable word; 0 if an index entry should
     *         be created immediately for the character position; n (&gt;0): add an index entry
     *         for the word containing this character if it has more than n characters.
     */
    protected abstract int readNextCharacter();

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
            System.err.println( indexchannel);
            int position = index.position();
            index = indexchannel.map( FileChannel.MapMode.READ_WRITE, INDEX_OFFSET,
                                      indexchannel.size()*2).order( ByteOrder.nativeOrder())
                .asIntBuffer();
            index.position( position+1);
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
     * the comparison and returning equality at word boundaries, the method will compare at
     * most 30 bytes starting from the index positions. This increases the chance of
     * inequality even if the indexed words would compare equal. It is done this way because quicksort
     * behaves lousy if there are many equalities.
     * </p><p>
     * The comparison is byte-based. To implement more sophisticated comparisons, e. g. by
     * treating katakana and hiragana equal, override the method.
     * </p>
     *
     * @param i1 Offset in the dictionary to the first string.
     * @param i2 Offset in the dictionary to the second string.
     * @return -1 if i1&lt;i2, 1 if i1&gt;i2, 0 for equality.
     */
    protected int compare( int i1, int i2) {
        int len1 = (int) dictionary.capacity() - i1;
        int len2 = (int) dictionary.capacity() - i2;
        // compare at most 30 bytes
        int length = Math.min( 30, Math.min( len1, len2));
        for ( int i=0; i<length; i++) {
            int b1 = byteToUnsignedByte( dictionary.get( i1+i));
            int b2 = byteToUnsignedByte( dictionary.get( i2+i));
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
     * The comparison is byte-based. To implement more sophisticated comparisons, e. g. by
     * treating katakana and hiragana equal, override the method.
     *
     * @param str The byte array containing the word to compare.
     * @param off Offset in the dictionary file to the position to compare.
     * @return -1 if the byte array is smaller than the sequence in the dictionary; 1 if it is larger
     *         or 0 if they are equal.
     */
    protected int compare( byte[] str, int off) {
        dictionary.position( off);
        for ( int i=0; i<str.length; i++) {
            int b1 = byteToUnsignedByte( str[i]);
            int b2;
            try {
                b2 = byteToUnsignedByte( dictionary.get());
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
     * Converts the byte value to an int with the value of the 8 bits
     * interpreted as an unsigned byte.
     *
     * @param b The byte value to convert.
     * @return The unsigned byte value of b.
     */
    protected final static int byteToUnsignedByte( byte b) {
        return b & 0xff;
    }

    /**
     * Returns the name of this dictionary. This is the filename of the dictionary file.
     *
     * @return The name of this dictionary.
     */
    public String getName() {
        return name;
    }
} // class FileBasedDictionary
