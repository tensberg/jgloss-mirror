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
 * <p>
 * The class tries to provide a framework to implement dictionaries which are stored in a file.
 * It is assumed that the dictionary file is a sequence of dictionary entries which are separated by
 * byte-sized entry sepator markers. Each entry is again subdivided into several fields like word,
 * reading or translation. Field division markers can be more complex. Common tasks like index
 * management, searching and search type management are implemented in this class. Derived classes
 * which implement specific dictionary formats must implement the abstract methods which deal
 * with the differences in file formats, especially the parsing of dictionary entries to
 * {@link DictionaryEntry DictionaryEntry} instances.
 * </p>
 *
 * @author Michael Koch
 */
public abstract class FileBasedDictionary implements IndexedDictionary, Indexable {
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
     * File which holds the dictionary.
     */
    protected File dicfile;
    /**
     * Channel used to access the dictionary.
     */
    protected FileChannel dicchannel;
    /**
     * Size of the dictionary in bytes. This equals dicchannel.size(), which is slow.
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
     * Stores the character handler created by a call to 
     * {@link #createCharacterHandler() createCharacterHandler} and used thorough this class.
     */
    protected EncodedCharacterHandler characterHandler;
    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    protected String name;
    /**
     * Container which stores the index data for this dictionary.
     */
    protected IndexContainer indexContainer;
    /**
     * Binary search index which is used for expression searches.
     */
    protected Index binarySearchIndex;
    /**
     * Stores the supported search modes of this dictionary. Initialized in 
     * {@link #initSearchModes() initSearchModes}.
     */
    protected Map supportedSearchModes;
    
    /**
     * Initializes the dictionary. The dictionary file is opened and mapped into memory.
     * The index file is not loaded from the constructor. Before the dictionary can be used,
     * {@link #loadIndex() loadIndex} must be successfully called.
     *
     * @dicfile File which holds the dictionary.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    protected FileBasedDictionary( File _dicfile) 
        throws IOException {
        this.dicfile = _dicfile;
        name = dicfile.getName();

        characterHandler = getEncodedCharacterHandler();
        try {
            decoder = Charset.forName( characterHandler.getEncodingName()).newDecoder();
        } catch (UnsupportedCharsetException ex) {
            // bah...
            // leave decoder==null and use String constructor for byte->char conversion
        }

        // load the dictionary
        dicchannel = new FileInputStream( dicfile).getChannel();
        dictionarySize = (int) dicchannel.size();
        dictionary = dicchannel.map( FileChannel.MapMode.READ_ONLY, 0, dictionarySize);

        binarySearchIndex = new BinarySearchIndex();

        initSearchModes();
    }

    /**
     * Initialize the map of search modes supported by this dictionary implementation.
     */
    protected void initSearchModes() {
        // For each search mode which is stored as key in the map, supports() will return true.
        // For search modes which use a SearchFieldSelection parameter, the SearchFieldSelection
        // is stored as value of the search mode key and getSupportedFields() will return this
        // selection.

        supportedSearchModes = new HashMap( 11);
        
        SearchFieldSelection fields = new SearchFieldSelection();
        fields.select( DictionaryEntryField.WORD, true);
        fields.select( DictionaryEntryField.READING, true);
        fields.select( DictionaryEntryField.TRANSLATION, true);
        fields.select( MatchMode.FIELD, true);
        fields.select( MatchMode.WORD, true);

        supportedSearchModes.put( ExpressionSearchModes.EXACT, fields);
        supportedSearchModes.put( ExpressionSearchModes.PREFIX, fields);
        supportedSearchModes.put( ExpressionSearchModes.SUFFIX, fields);
        supportedSearchModes.put( ExpressionSearchModes.ANY, fields);
    }

    public boolean supports( SearchMode mode, boolean fully) {
        return supportedSearchModes.containsKey( mode);
    }
    
    public SearchFieldSelection getSupportedFields( SearchMode mode) {
        SearchFieldSelection fields = (SearchFieldSelection) supportedSearchModes.get( mode);
        if (fields != null)
            return fields;
        else
            throw new IllegalArgumentException();
    }

    public boolean loadIndex() throws IndexException {
        File indexFile = new File( name + FileIndexContainer.EXTENSION);

        // rebuild the index if the dictionary was changed after the index was created
        if (indexFile.lastModified() < dicfile.lastModified())
            return false;

        try {
            indexContainer = new FileIndexContainer( indexFile, false);
            // insert tests for existence of additional index types here
            if (indexContainer.hasIndex( binarySearchIndex.getType()))
                return true;
        } catch (FileNotFoundException ex) {
            // no index file, create it
        } catch (IndexException ex) {
            // index file damaged, rebuild it
            indexFile.delete();
        } catch (IOException ex) {
            // IOExceptions signal an error when accessing the index file and should not be caught here
            throw new IndexException( ex);
        }

        return false;
    }

    public void buildIndex() throws IndexException {
        File indexFile = new File( name + FileIndexContainer.EXTENSION);
        try {
            indexContainer = new FileIndexContainer( indexFile, true);
            if (!indexContainer.hasIndex( binarySearchIndex.getType())) {
                IndexBuilder builder = new BinarySearchIndexBuilder();
                builder.startBuildIndex( indexContainer, this);
                boolean commit = false;
                try {
                    addIndexTerms( builder);
                    commit = true;
                } finally {
                    builder.endBuildIndex( commit);
                }
            }
            // put creation of additional index types here
        } catch (IOException ex) {
            throw new IndexException( ex);
        } finally {
            indexContainer.endEditing();
        }
    }

    /**
     * Create a character handler which understands the character encoding format used by this
     * dictionary.
     */
    protected abstract EncodedCharacterHandler createCharacterHandler();

    /**
     * Return a character handler which understands the character encoding format used by this
     * dictionary.
     */
    public EncodedCharacterHandler getCharacterHandler() {
        return characterHandler;
    }

    public Iterator search( SearchMode searchmode, Object[] parameters) throws SearchException {
        if (searchmode == ExpressionSearchModes.EXACT ||
            searchmode == ExpressionSearchModes.PREFIX ||
            searchmode == ExpressionSearchModes.SUFFIX ||
            searchmode == ExpressionSearchModes.ANY) {
            return searchExpression( searchmode, (String) parameters[0], 
                                     (SearchFieldSelection) parameters[1]);
        }

        throw new UnsupportedSearchModeException( searchmode);
    }

    /**
     * Implements search for expression search modes.
     */
    protected Iterator searchExpression( SearchMode searchmode, String expression,
                                         SearchFieldSelection searchFields) 
        throws SearchException {
        expression = escape( expression);

        try {
            ByteBuffer exprbuf = ByteBuffer.wrap( expression.getBytes
                                                  ( characterHandler.getEncodingName()));
            return new ExpressionSearchIterator( searchmode, searchFields, exprbuf.limit(),
                                                 binarySearchIndex.getEntryPositions
                                                 ( this, exprbuf, null));
        } catch (UnsupportedEncodingException ex) {
            throw new SearchException( ex);
        }
    }
    
    /**
     * Copy the data of a single dictionary entry from the dictionary buffer to a newly created
     * buffer.
     *
     * @param matchstart Offset into the entry which is to be copied. This can point anywhere in the
     *        entry, the method will search the start and end of the entry.
     * @param entrybuf Buffer into which the entry data is copied. The returned byte buffer will
     *        wrap this array. If <code>null</code>, a new byte array will be allocated.
     * @param seenEntries Set of integers with start positions of entries which have already been copied.
     *        If this is not <code>null</code>, and the set contains the start offset of the entry
     *        pointed to by <code>matchstart</code>, <code>null</code> is returned instead of the
     *        entry data. This can be used
     *        by the caller of <code>copyEntry</code> to filter out duplicate entries.
     * @param outOffsets Start and end offset of the entry in the dictionary buffer. If this is
     *        an int array of size 2, the start offset (inclusive) will be written to index 0,
     *        the end offset (exclusive) to index 1. May be <code>null</code>.
     * @return A byte buffer containing the dictionary entry data. The buffer will wrap the
     *         <code>entrybuf</code> buffer, its limit will be the length of the entry data and its
     *         position will be the byte pointed to by <code>matchstart</code>.
     */
    protected ByteBuffer copyEntry( int matchstart, byte[] entrybuf, EntrySet seenEntries,
                                    int[] outOffsets) {
        if (entrybuf == null)
            entrybuf = new byte[8192];

        int start = matchstart; // start of entry (inclusive)
        int end = matchstart+1; // end of entry (exclusive)
        
        // Find beginning of entry line by searching backwards for the entry separator.
        // Read bytes are stored back to front in entry array.
        byte b;
        try {
            int curr = entrybuf.length-1;
            while (!isEntrySeparator( b=dictionary.get( start-1))) {
                try {
                    entrybuf[curr--] = b;
                    start--;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    // array too small for entry; grow it
                    byte[] entrybuf2 = new byte[entrybuf.length*2];
                    System.arraycopy( entrybuf, 0, entrybuf2, entrybuf.length, entrybuf.length);
                    entrybuf = entrybuf2;
                    curr = entrybuf.length-1;
                    // since start is not decremented, the loop will be repeated at the
                    // same position, with loop body succeeding this time
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            // beginning of dictionary
        }
        
        // test if entry was already found through another index word
        // Several index entries can point to the same entry line. For example
        // if a word contains the same kanji twice, and words with this kanji are looked up,
        // one index entry is found for the first occurrence and one for the second. To prevent
        // adding the entry multiple times, entries are stored in a set.
        if (seenEntries!=null && seenEntries.contains( start)) {
            return null;
        }
        
        // move read bytes to beginning of entry array
        try {
            System.arraycopy( entrybuf, entrybuf.length-(matchstart-start), entrybuf, 0,
                              matchstart-start);
        } catch (IndexOutOfBoundsException ex) {
            // matchstart==start, no copying neccessary
        }
        // read match start char
        entrybuf[matchstart - start] = dictionary.get( matchstart);
        
        // find end of entry line
        int entrylength = matchstart - start + 1;
        try {
            while (!isEntrySeparator( b=dictionary.get( end))) try {
                entrybuf[entrylength++] = b;
                end++;
            } catch (ArrayIndexOutOfBoundsException ex) {
                // array too small for entry; grow it
                byte[] entrybuf2 = new byte[entrybuf.length*2];
                System.arraycopy( entrybuf, 0, entrybuf2, 0, entrylength);
                entrybuf = entrybuf2;
            }
        } catch (BufferUnderflowException ex) {
            // end of dictionary->end of entry
        }

        ByteBuffer out = ByteBuffer.wrap( entrybuf, 0, entrylength);
        out.position( matchstart-start);

        if (outOffsets != null) {
            outOffsets[0] = start;
            outOffsets[1] = end;
        }

        return out;
    }

    /**
     * Create a {@link DictionaryEntry DictionaryEntry} object from the data stored in the byte
     * buffer. The method converts the byte buffer data to a string and invokes
     * {@link #parseEntry(String) parseEntry}.
     */
    protected DictionaryEntry createEntryFrom( ByteBuffer entry) 
        throws SearchException {
        
        String entrystring;
        // NIO decoder is faster than new String(), but NIO character encoding support is limited
        if (decoder != null) {
            try {
                entrystring = decoder.decode( entry).toString();
            } catch (CharacterCodingException ex) {
                throw new SearchException( ex);
            }
        }
        else { // NIO does not support the required encoding
            try {
                entrystring = unescape( new String( entry.array(), entry.arrayOffset(), entry.limit(), 
                                                    characterHandler.getEncodingName()));
            } catch (UnsupportedEncodingException ex) {
                throw new SearchException( ex);
            }
        }
        
        return parseEntry( entrystring);
    }
    
    /**
     * Test if the character at the given location is the first in an entry field.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected abstract boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field);
    /**
     * Test if the character at the given location is the last in an entry field.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected abstract boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field);
    /**
     * Test if the character at the given location is the first in a word.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected abstract boolean isWordStart( ByteBuffer entry, int location, DictionaryEntryField field);
    /**
     * Test if the character at the given location is the last in a word.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected abstract boolean isWordEnd( ByteBuffer entry, int location, DictionaryEntryField field);

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
     * Create a {@link DictionaryEntry DictionaryEntry} object from the entry string from the dictionary.
     *
     * @param entry Entry string as found in the dictionary.
     */
    protected abstract DictionaryEntry parseEntry( String entry);
    
    public void dispose() {
        try {
            dicchannel.close();
            if (indexContainer != null)
                indexContainer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
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
     *
     * @return The number of index entries created.
     */
    protected int addIndexTerms( IndexBuilder builder) throws IOException, IndexException {
        int indexsize = 0;
        dictionary.position( 0);
        ArrayList entryStarts = new ArrayList( 25);
        
        DictionaryEntryField field = moveToNextField( dictionary, 0);
        while (dictionary.position() < dictionarySize) {
            boolean inWord = false;
            int c;
            CharacterClass clazz;
            int entryStart;
            DictionaryEntryField currentField = field;
            do {
                entryStart = dictionary.position();
                c = characterHandler.convertCharacter( characterHandler.readCharacter( dictionary));
                clazz = characterHandler.getCharacterClass( c, inWord);
                field = moveToNextField( dictionary, c);
            } while (clazz == CharacterClass.OTHER);
            entryStarts.clear();
            entryStarts.add( new Integer( entryStart));
            if (clazz == CharacterClass.ROMAN_WORD)
                inWord = true;

            int entryLength = 1; // entry length in number of characters
            int entryEnd;
            CharacterClass clazz2;
            do {
                entryLength++;
                entryEnd = dictionary.position();
                c = characterHandler.convertCharacter( characterHandler.readCharacter( dictionary));
                clazz2 = characterHandler.getCharacterClass( c, inWord);

                if (clazz == CharacterClass.KANJI && clazz2==clazz)
                    entryStarts.add( new Integer( dictionary.position()));
            } while (clazz2 == clazz);
            
            if (clazz==CharacterClass.KANJI || 
                clazz==CharacterClass.HIRAGANA ||
                clazz==CharacterClass.KATAKANA ||
                clazz==CharacterClass.ROMAN_WORD && entryLength >= 3) {
                for ( int i=0; i<entryStarts.size(); i++) {
                    entryStart = ((Integer) entryStarts.get( i)).intValue();
                    if (builder.addEntry( entryStart, entryEnd-entryStart, currentField))
                        indexsize++;
                }
            }
            
            field = moveToNextField( dictionary, c);
        }
        
        return indexsize;
    }
    
    /**
     * Skip to the next indexable field. This method is called at index
     * creation time before any character is read from the dictionary and after each read character.
     * It can be used to skip entry fields which should not be indexed.
     *
     * @param buf Skip entries in this buffer by moving the current position of the buffer.
     * @param character The last character read from the buffer, or 0 at the first invocation. The
     *                  character format is dependent on 
     *                  {@link EncodedCharacterHandler#readCharacter(ByteBuffer) readCharacter}
     *                  and not neccessary unicode.
     * @return The type of the field the method moved to.
     */
    protected abstract DictionaryEntryField moveToNextField( ByteBuffer buf, int character);

    /**
     * Return the type of the entry field at the given location.
     */
    protected abstract DictionaryEntryField getFieldType( ByteBuffer buf, int location);

    public int compare( int pos1, int pos2) throws IndexException {
        try {
            return compare( dictionary, pos1, Integer.MAX_VALUE, dictionary.duplicate(), pos2);
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IndexException( ex);
        }
    }

    public int compare( ByteBuffer data, int position) throws IndexException {
        try {
            return compare( data, 0, data.limit(), dictionary, position);
        } catch (CharacterCodingException ex) {
            throw new IndexException( ex);
        }
    }

    /**
     * Lexicographic comparison of two byte buffers.
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
    protected int compare( ByteBuffer buf1, int i1, int length, ByteBuffer buf2, int i2) 
        throws CharacterCodingException {
        buf1.position( i1);
        buf2.position( i2);
        int end = (int) Math.min( Integer.MAX_VALUE, (long) i1 + (long) length);
        try {
            while (buf1.position() < end) {
                int b1 = characterHandler.readCharacter( buf1);
                int b2 = characterHandler.readCharacter( buf2);
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

    public Indexable.CharData getChar( int position, CharData result) throws IndexException {
        if (result == null)
            result = new Indexable.CharData();
        dictionary.position( position);
        try {
            result.character = characterHandler.readCharacter( dictionary);
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IndexException( ex);
        }
        result.position = dictionary.position();

        return result;
    }

    /**
     * Iterator returning results from an expression search.
     */
    protected class ExpressionSearchIterator implements Iterator {
        protected SearchMode searchmode;
        protected SearchFieldSelection fields;
        protected int expressionLength;
        protected Index.Iterator matchingIndexEntries;
        protected byte[] entrybuf = new byte[8192];
        protected EntrySet seenEntries = new EntrySet();
        protected int[] entryOffsets = new int[2];
        protected DictionaryEntry nextEntry = null;

        public ExpressionSearchIterator( SearchMode _searchmode, SearchFieldSelection _fields,
                                         int _expressionLength,
                                         Index.Iterator _matchingIndexEntries) {
            this.searchmode = _searchmode;
            this.fields = _fields;
            this.expressionLength = _expressionLength;
            this.matchingIndexEntries = _matchingIndexEntries;
            generateNextEntry();
        }

        public boolean hasNext() { return nextEntry != null; }
        public Object next() throws NoSuchElementException {
            if (!hasNext())
                throw new NoSuchElementException();
            DictionaryEntry current = nextEntry;
            generateNextEntry(); // changes nextEntry
            return current;
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Set the {@link #nextEntry nextEntry} variable to the next dictionary entry matching
         * the search. If there are no more entries, it will be set to <code>null</code>.
         */
        protected void generateNextEntry() {
            nextEntry = null;
            while (nextEntry==null && matchingIndexEntries.hasNext()) {
                int match = matchingIndexEntries.next();

                ByteBuffer entry = copyEntry( match, entrybuf, seenEntries, entryOffsets);
                if (entry == null) // this entry has already been found
                    continue;

                match = entry.position(); // location of match in entry buffer
                DictionaryEntryField field = getFieldType( entry, match);
                try {
                    if (!fields.isSelected( field))
                        continue; // field is not selected by the user
                } catch (IllegalArgumentException ex) {
                    // field not WORD, READING or TRANSLATION
                    continue;
                }

                // test if entry matches search mode
                if (searchmode == ExpressionSearchModes.EXACT ||
                    searchmode == ExpressionSearchModes.PREFIX) {
                    // test if preceeding character marks beginning of word entry
                    if (match>0 && 
                        !(fields.isSelected( DictionaryEntryField.WORD) ? 
                          isWordStart( entry, match, field) :
                          isFieldStart( entry, match, field)))
                        continue;
                }
                if (searchmode == ExpressionSearchModes.EXACT ||
                    searchmode == ExpressionSearchModes.SUFFIX) {
                    // test if following character marks end of word entry
                    if (match+expressionLength+1<entry.limit() &&
                        !(fields.isSelected( DictionaryEntryField.WORD) ? 
                          isWordEnd( entry, match, field) :
                          isFieldEnd( entry, match, field)))
                        continue;
                }
                    
                try {
                    nextEntry = createEntryFrom( entry);
                    seenEntries.add( entryOffsets[0]); // start offset of entry
                } catch (SearchException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Set of integers which represent entry start indexes in the dictionary.
     */
    private static class EntrySet {
        private HashSet entries;

        public EntrySet() {
            entries = new HashSet( 51);
        }

        public boolean contains( int entry) {
            return (entries.contains( new Integer( entry)));
        }

        public void add( int entry) {
            entries.add( new Integer( entry));
        }
    } // class EntrySet
} // class FileBasedDictionary
