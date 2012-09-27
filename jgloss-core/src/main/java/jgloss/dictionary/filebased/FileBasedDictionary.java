/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.dictionary.filebased;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgloss.dictionary.BaseEntry;
import jgloss.dictionary.BinarySearchIndex;
import jgloss.dictionary.CharacterClass;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.EUCJPCharacterHandler;
import jgloss.dictionary.EncodedCharacterHandler;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.Index;
import jgloss.dictionary.IndexContainer;
import jgloss.dictionary.IndexException;
import jgloss.dictionary.Indexable;
import jgloss.dictionary.IndexedDictionary;
import jgloss.dictionary.MalformedEntryException;
import jgloss.dictionary.MatchMode;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.dictionary.SearchMode;
import jgloss.dictionary.UTF8CharacterHandler;
import jgloss.dictionary.UnsupportedSearchModeException;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeValue;
import jgloss.util.StringTools;
import jgloss.util.UTF8ResourceBundleControl;

/**
 * Base class for dictionaries stored in a local file with separate index file.
 * <p>
 * The class provides a framework to implement dictionaries which are stored in a file.
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
public abstract class FileBasedDictionary implements IndexedDictionary, Indexable,
                                                     BaseEntry.MarkerDictionary {
	private static final Logger LOGGER = Logger.getLogger(FileBasedDictionary.class.getPackage().getName());

    /**
     * Localized messages and strings for the dictionary implementations. Stored in
     * <code>resources/messages-dictionary</code>
     */
    protected static final ResourceBundle NAMES = ResourceBundle.getBundle
        ( "messages-dictionary", new UTF8ResourceBundleControl());

    /**
     * Structure of the concrete dictionary implementation.
     */
	private final FileBasedDictionaryStructure structure;

	/**
	 * Entry parser of the concrete dictionary implementation.
	 */
	private final EntryParser entryParser;

    /**
     * File which holds the dictionary.
     */
    private final File dicfile;
    /**
     * Channel used to access the dictionary.
     */
    private final FileChannel dicchannel;
    /**
     * Size of the dictionary in bytes. This equals dicchannel.size(), which is slow.
     */
    private final int dictionarySize;
    /**
     * Dictionary file mapped into a byte buffer.
     */
    private final MappedByteBuffer dictionary;
    /**
     * Duplicate of the  {@link #dictionary dictionary} byte buffer. Created using
     * <code>dictionary.duplicate()</code> and needed for comparison.
     */
    private final ByteBuffer dictionaryDuplicate;
    /**
     * Decoder initialized to convert a byte array to a char array using the charset of the
     * dictionary.
     */
    private final CharsetDecoder decoder;
    /**
     * Stores the character handler created by a call to
     * {@link #createCharacterHandler(String) createCharacterHandler} and used thorough this class.
     */
    protected final EncodedCharacterHandler characterHandler;
    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    private final String name;

    private final File indexFile;
    /**
     * Container which stores the index data for this dictionary.
     */
    private IndexContainer indexContainer;
    /**
     * Binary search index which is used for expression searches.
     */
    private final Index binarySearchIndex;
    /**
     * Stores the supported search modes of this dictionary. Initialized in
     * {@link #initSearchModes() initSearchModes}.
     */
    protected final Map<SearchMode, SearchFieldSelection> supportedSearchModes = new HashMap<SearchMode, SearchFieldSelection>( 11);
    /**
     * Set of attributes supported by this dictionary implementation. Initialized in
     * {@link #initSupportedAttributes() initSupportedAttributes}.
     */
    protected final Map<Attribute<?>, Set<AttributeValue>> supportedAttributes = new HashMap<Attribute<?>, Set<AttributeValue>>( 11);

    /**
     * Initializes the dictionary. The dictionary file is opened and mapped into memory.
     * The index file is not loaded from the constructor. Before the dictionary can be used,
     * {@link #loadIndex() loadIndex} must be successfully called.
     *
     * @param _dicfile File which holds the dictionary.
     * @param _encoding Character encoding of the dictionary file.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    protected FileBasedDictionary( FileBasedDictionaryStructure structure, EntryParser entryParser, File _dicfile, String _encoding) throws IOException {
        this.structure = structure;
		this.entryParser = entryParser;
		this.dicfile = _dicfile;
        this.name = _dicfile.getName();
        indexFile = new File( dicfile.getCanonicalPath() + FileIndexContainer.EXTENSION);

        characterHandler = createCharacterHandler(_encoding);

        CharsetDecoder decoder;
        try {
            decoder = Charset.forName(_encoding).newDecoder();
        } catch (UnsupportedCharsetException ex) {
            LOGGER.log(Level.WARNING, "unsupported charset " + _encoding, ex);
            // leave decoder==null and use String constructor for byte->char conversion
            decoder = null;
        }
        this.decoder = decoder;

        // load the dictionary
        dicchannel = new FileInputStream( dicfile).getChannel();
        dictionarySize = (int) dicchannel.size();
        dictionary = dicchannel.map( FileChannel.MapMode.READ_ONLY, 0, dictionarySize);
        dictionaryDuplicate = dictionary.duplicate();

        binarySearchIndex = new BinarySearchIndex( BinarySearchIndex.TYPE);

        entryParser.setDictionary(this);
        
        initSearchModes();
        initSupportedAttributes();
    }

    /**
     * Initialize the map of search modes supported by this dictionary implementation.
     */
    protected void initSearchModes() {
        // For each search mode which is stored as key in the map, supports() will return true.
        // For search modes which use a SearchFieldSelection parameter, the SearchFieldSelection
        // is stored as value of the search mode key and getSupportedFields() will return this
        // selection.

        SearchFieldSelection fields = new SearchFieldSelection( true, true, true, true, true);

        supportedSearchModes.put( ExpressionSearchModes.EXACT, fields);
        supportedSearchModes.put( ExpressionSearchModes.PREFIX, fields);
        supportedSearchModes.put( ExpressionSearchModes.SUFFIX, fields);
        supportedSearchModes.put( ExpressionSearchModes.ANY, fields);
    }

    /**
     * Initialize the set of supported attributes. Since entry parsing, and thus attribute
     * usage is entirely the responsibility of derived classes, this method only creates
     * an empty set. Derived classes should add their supported attributes.
     */
    protected void initSupportedAttributes() {
    }

    @Override
	public boolean supports( SearchMode mode, boolean fully) {
        return supportedSearchModes.containsKey( mode);
    }

    @Override
	public Set<Attribute<?>> getSupportedAttributes() {
        return supportedAttributes.keySet();
    }

    @Override
	public <T extends AttributeValue> Set<T> getAttributeValues( Attribute<T> att) {
        if (!supportedAttributes.containsKey( att)) {
	        return null;
        }

        @SuppressWarnings("unchecked")
        Set<T> out = (Set<T>) supportedAttributes.get( att);
        if (out == null) {
	        return Collections.emptySet();
        } else {
	        return out;
        }
    }

    @Override
	public SearchFieldSelection getSupportedFields( SearchMode mode) {
        SearchFieldSelection fields = supportedSearchModes.get( mode);
        if (fields != null) {
	        return fields;
        } else {
	        throw new IllegalArgumentException();
        }
    }

    @Override
	public boolean loadIndex() throws IndexException {
        // rebuild the index if the dictionary was changed after the index was created
        if (indexFile.lastModified() < dicfile.lastModified()) {
            indexFile.delete();
            return false;
        }

        try {
            indexContainer = new FileIndexContainer( indexFile, false);
            // insert tests for existence of additional index types here
            if (!indexContainer.hasIndex( binarySearchIndex.getType())) {
	            return false;
            }

            initIndexes();

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

    @Override
	public void buildIndex() throws IndexException {
        try {
            indexContainer = new FileIndexContainer( indexFile, true);

            indexContainer.deleteIndex(binarySearchIndex.getType()); // rebuild if exists
            
            new FileBasedDictionaryIndexer(this, structure, dictionary, characterHandler).buildIndex(indexContainer, binarySearchIndex);

            // put creation of additional index types here
        } catch (IOException ex) {
            throw new IndexException( ex);
        } finally {
            if (indexContainer != null) {
	            indexContainer.endEditing();
            }
        }

        initIndexes();
    }

    private void initIndexes() throws IndexException {
        binarySearchIndex.setContainer( indexContainer);
    }

    /**
     * Create a character handler for the given character encoding.
     * Creates a {@link EUCJPCharacterHandler EUCJPCharacterHandler} for encoding
     * EUC-JP, or a {@link UTF8CharacterHandler UTF8CharacterHandler} for encoding "UTF-8".
     * Throws an <code>IllegalArgumentException</code> otherwise.
     * Subclasses may override this method to create custom character handlers.
     */
    private EncodedCharacterHandler createCharacterHandler(String encoding) {
        if ("EUC-JP".equals(encoding)) {
	        return new EUCJPCharacterHandler();
        } else if ("UTF-8".equals(encoding)) {
	        return new UTF8CharacterHandler();
        } else {
	        throw new IllegalArgumentException(encoding);
        }
    }

    /**
     * Return a character handler which understands the character encoding format used by this
     * dictionary.
     */
    @Override
	public EncodedCharacterHandler getEncodedCharacterHandler() {
        return characterHandler;
    }

    @Override
	public Iterator<DictionaryEntry> search( SearchMode searchmode, Object[] parameters) throws SearchException {
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
    private Iterator<DictionaryEntry> searchExpression( SearchMode searchmode, String expression,
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
    private ByteBuffer copyEntry( int matchstart, byte[] entrybuf, Set<Integer> seenEntries,
                                    int[] outOffsets) {
        if (entrybuf == null) {
	        entrybuf = new byte[8192];
        }

        int start = matchstart; // start of entry (inclusive)
        int end = matchstart+1; // end of entry (exclusive)

        // Find beginning of entry line by searching backwards for the entry separator.
        // Read bytes are stored back to front in entry array.
        byte b;
        try {
            int curr = entrybuf.length-1;
            while (!structure.isEntrySeparator( b=dictionary.get( start-1))) {
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
            while (!structure.isEntrySeparator( b=dictionary.get( end))) {
	            try {
	                entrybuf[entrylength++] = b;
	                end++;
	            } catch (ArrayIndexOutOfBoundsException ex) {
	                // array too small for entry; grow it
	                byte[] entrybuf2 = new byte[entrybuf.length*2];
	                System.arraycopy( entrybuf, 0, entrybuf2, 0, entrylength);
	                entrybuf = entrybuf2;
	            }
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
     * Create a dictionary entry from a marker, which is the start offset of the entry.
     * Used from {@link BaseEntry.BaseEntryRef BaseEntryRef} to recreate a dictionary entry.
     */
    @Override
	public DictionaryEntry createEntryFromMarker( int marker) throws SearchException {
        dictionary.position( marker);
        ByteBuffer entry = dictionary.slice();
        while (!structure.isEntrySeparator( entry.get())) {
	        ; // entry.get() advances the loop
        }
        entry.limit( entry.position()-1);
        return createEntryFrom( entry, marker);
    }

    /**
     * Create a {@link DictionaryEntry DictionaryEntry} object from the data stored in the byte
     * buffer. The method converts the byte buffer data to a string and invokes
     * {@link #parseEntry(String,int) parseEntry}.
     */
    private DictionaryEntry createEntryFrom( ByteBuffer entry, int startOffset)
        throws SearchException {

        String entrystring;
        // NIO decoder is faster than new String(), but NIO character encoding support is limited
        if (decoder != null) {
            try {
                entrystring = unescape( decoder.decode( (ByteBuffer) entry.rewind()).toString());
            } catch (CharacterCodingException ex) {
                throw new SearchException( ex);
            }
        }
        else { // NIO does not support the required encoding
            try {
                if (entry.hasArray()) {
                    entrystring = unescape( new String( entry.array(), entry.arrayOffset(), entry.limit(),
                                                        characterHandler.getEncodingName()));
                } else {
                    entry.rewind();
                    byte[] bytes = new byte[entry.limit()];
                    entry.get( bytes);
                    entrystring = unescape( new String( bytes, 0, bytes.length,
                                                        characterHandler.getEncodingName()));
                }
            } catch (UnsupportedEncodingException ex) {
                throw new SearchException( ex);
            }
        }

        return entryParser.parseEntry( entrystring, startOffset);
    }

    /**
     * Test if the character at the given location is the first in a word. The method first tests
     * if the location is at the start of a field by calling
     * {@link #isFieldStart(ByteBuffer,int,DictionaryEntryField) isFieldStart}.
     * If it is not at the start of a field,
     * it calls {@link #isWordBoundary(ByteBuffer,int,DictionaryEntryField) isWordBoundary}.
     *
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    private boolean isWordStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        if (structure.isFieldStart( entry, location, field)) {
	        return true;
        }

        return isWordBoundary( entry, location, field);
    }

    /**
     * Test if the character at the given location is the last in a word. The method first tests
     * if the location is at the start of a field by calling
     * {@link #isFieldEnd(ByteBuffer,int,DictionaryEntryField) isFieldEnd}.
     * If it is not at the start of a field,
     * it calls {@link #isWordBoundary(ByteBuffer,int,DictionaryEntryField) isWordBoundary}.
     *
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    private boolean isWordEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        if (structure.isFieldEnd( entry, location, field)) {
	        return true;
        }

        return isWordBoundary( entry, location, field);
    }

    /**
     * Test if the characters before and at the given location form a word boundary.
     */
    private boolean isWordBoundary( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            entry.position( location);
            int c1 = characterHandler.readPreviousCharacter( entry);
            entry.position( location);
            int c2 = characterHandler.readCharacter( entry);
            CharacterClass cc1 = characterHandler.getCharacterClass( c1, false);
            boolean inWord = (cc1 == CharacterClass.ROMAN_WORD);
            return (cc1 !=
                    characterHandler.getCharacterClass( c2, inWord));
        } catch (BufferOverflowException ex) {
            return true;
        } catch (CharacterCodingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
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
     * in the string and uses a {@link StringTools#unicodeEscape(char) unicode escape sequence}
     * if the method returns <code>true</code>.
     * </p>
     */
    private String escape( String str) {
        StringBuilder buf = null; // initialize only if needed
        for ( int i=str.length()-1; i>=0; i--) {
            if (escapeChar( str.charAt( i))) {
                if (buf == null) {
	                buf = new StringBuilder( str);
                }
                buf.replace( i, i+1, StringTools.unicodeEscape( str.charAt( i)));
            }
        }

        if (buf == null) {
	        return str;
        } else {
	        return buf.toString();
        }
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
     * calls {@link StringTools#unicodeUnescape(String) StringTools.unicodeUnescape}.
     */
    protected String unescape( String str) {
        return StringTools.unicodeUnescape( str);
    }

    @Override
	public void dispose() {
        try {
            dicchannel.close();
            if (indexContainer != null) {
	            indexContainer.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Returns the name of this dictionary. This implemenation uses the filename of the dictionary file.
     *
     * @return The name of this dictionary.
     */
    @Override
	public String getName() {
        return name;
    }

    @Override
	public int compare( int pos1, int pos2) throws IndexException {
        try {
            return compare( dictionary, pos1, Integer.MAX_VALUE, dictionaryDuplicate, pos2);
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IndexException( ex);
        }
    }

    @Override
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
    private int compare( ByteBuffer buf1, int i1, int length, ByteBuffer buf2, int i2)
        throws CharacterCodingException {
        if (i1 == i2) {
            return 0;
        }

        buf1.position( i1);
        buf2.position( i2);
        int end = (int) Math.min( Integer.MAX_VALUE, (long) i1 + (long) length);
        try {
            while (buf1.position() < end) {
                int b1 = characterHandler.convertCharacter
                    ( characterHandler.readCharacter( buf1));
                int b2 = characterHandler.convertCharacter
                    ( characterHandler.readCharacter( buf2));
                if (b1 < b2) {
	                return -1;
                } else if (b1 > b2) {
	                return 1;
                }
            }
        } catch (BufferUnderflowException ex) {
            if (buf1.hasRemaining()) {
	            return 1;
            } else if (buf2.hasRemaining())
			 {
	            return -1;
            // else equality
            }
        }

        return 0; // equality
    }

    @Override
	public Indexable.CharData getChar( int position, CharData result) throws IndexException {
        if (result == null) {
	        result = new Indexable.CharData();
        }
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
    private class ExpressionSearchIterator implements Iterator<DictionaryEntry> {
        private final SearchMode searchmode;
        private final SearchFieldSelection fields;
        private final int expressionLength;
        private final Index.Iterator matchingIndexEntries;
        private final byte[] entrybuf = new byte[8192];
        private final Set<Integer> seenEntries = new HashSet<Integer>();
        private final int[] entryOffsets = new int[2];
        private DictionaryEntry nextEntry = null;
        private SearchException deferredException = null;

        public ExpressionSearchIterator( SearchMode _searchmode, SearchFieldSelection _fields,
                                         int _expressionLength,
                                         Index.Iterator _matchingIndexEntries) throws SearchException {
            this.searchmode = _searchmode;
            this.fields = _fields;
            this.expressionLength = _expressionLength;
            this.matchingIndexEntries = _matchingIndexEntries;
            generateNextEntry();
        }

        @Override
		public boolean hasNext() { return nextEntry!=null || deferredException!=null; }

        @Override
		public DictionaryEntry next() {
            if (!hasNext()) {
	            throw new NoSuchElementException();
            }

            if (deferredException == null) {
                DictionaryEntry current = nextEntry;
                generateNextEntry(); // changes nextEntry
                return current;
            }
            else {
                SearchException out = new SearchException( deferredException);
                deferredException = null;

                if (out instanceof MalformedEntryException) {
                    // perhaps the next entry will work
                    generateNextEntry();
                }

                throw out;
            }
        }

        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Set the {@link #nextEntry nextEntry} variable to the next dictionary entry matching
         * the search. If there are no more entries, it will be set to <code>null</code>.
         */
        private void generateNextEntry() {
            nextEntry = null;
            try {
                while (nextEntry==null && matchingIndexEntries.hasNext()) {
                    int match = matchingIndexEntries.next();

                    ByteBuffer entry = copyEntry( match, entrybuf, seenEntries, entryOffsets);
                    if (entry == null) {
	                    continue;
                    }

                    match = entry.position(); // location of match in entry buffer
                    DictionaryEntryField field = structure.getFieldType( entry, 0, entry.limit(), match);
                    try {
                        if (!fields.isSelected( field))
						 {
	                        continue; // field is not selected by the user
                        }
                    } catch (IllegalArgumentException ex) {
                        // field not WORD, READING or TRANSLATION
                        continue;
                    }

                    // test if entry matches search mode
                    if (searchmode == ExpressionSearchModes.EXACT ||
                        searchmode == ExpressionSearchModes.PREFIX) {
                        // test if the index entry location is at the beginning of a word or field
                        // depending on search parameter.
                        if (match>0 &&
                            !(fields.isSelected( MatchMode.WORD) ?
                              isWordStart( entry, match, field) :
                            	  structure.isFieldStart( entry, match, field))) {
	                        continue;
                        }
                    }
                    if (searchmode == ExpressionSearchModes.EXACT ||
                        searchmode == ExpressionSearchModes.SUFFIX) {
                        int matchend = match+expressionLength;
                        if (matchend<entry.limit() &&
                            !(fields.isSelected( MatchMode.WORD) ?
                              isWordEnd( entry, matchend, field) :
                            	  structure.isFieldEnd( entry, matchend, field))) {
	                        continue;
                        }
                    }

                    nextEntry = createEntryFrom( entry, entryOffsets[0]);
                    seenEntries.add( entryOffsets[0]); // start offset of entry
                }
            } catch (SearchException ex) {
                // the exception will be thrown at the next call to next()
                deferredException = ex;
            }
        }
    }
} // class FileBasedDictionary
