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

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

/**
 * Dictionary implementation for dictionaries in EDICT format with
 * associated xjdx or jjdx index. For a documentation of the format see
 * <a href="http://www.csse.monash.edu.au/~jwb/edict_doc.html">
 * http://www.csse.monash.edu.au/~jwb/edict_doc.html</a>.
 *
 * @author Michael Koch
 */
public class EDict implements Dictionary {
    /**
     * Filename extension of an XJDX-format index. Will be added to the filename of the
     * dictionary.
     */
    public final static String XJDX_EXTENSION = ".xjdx";
    /**
     * Filename extension of a JJDX-format index. Will be added to the filename of the
     * dictionary.
     */
    public final static String JJDX_EXTENSION = ".jjdx";
    /**
     * Current version of the JJDX format.
     */
    public final static int JJDX_VERSION = 1001;

    /**
     * Localizable message resource.
     */
    protected final static ResourceBundle messages = 
        ResourceBundle.getBundle( "resources/messages-dictionary");

    /**
     * Path to the dictionary file.
     */
    protected String dicfile;
    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    protected String name;
    /**
     * Array containing the content of the dictionary file.
     */
    protected byte[] dictionary;
    /**
     * Length of the dictionary in bytes. This can be smaller than <CODE>dictionary.length</CODE>
     * if part of the array is unused.
     */
    protected int dictionaryLength;
    /**
     * Array containing the content of the xjdx/jjdx file associated with the dictionary file.
     */
    protected int[] index;
    /**
     * Length of the index in number of entries. This can be smaller than <CODE>index.length</CODE>
     * if part of the array is unused.
     */
    protected int indexLength;
    /**
     * Flag if index is in XJDX or JJDX format.
     */
    protected boolean xjdxIndex;
    /**
     * Random number generator. Used for randomized quicksort in index creation.
     */
    protected static Random random = new Random();

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
                        String l;
                        int lines = 0;
                        do {
                            l = r.readLine();
                            lines++;
                            // skip empty lines and comments
                        } while (l!=null && (l.length()==0 || l.charAt( 0)<128) && lines<100);
                        r.close();
                        if (l!=null && lines<100) {
                            int i = l.indexOf( ' ');
                            // An entry in EDICT has the form
                            // word [reading] /translation1/translation2/.../
                            // ([reading] is optional).
                            // An entry in the SKK dictionary has the form
                            // reading /word1/word2/.../
                            // To distinguish between the two formats I test if the
                            // first character after the '/' is ISO-8859-1 or not.
                            if (i!=-1 && i<l.length()-2 && 
                                (l.charAt( i+1)=='[' ||
                                 l.charAt( i+1)=='/' && l.charAt( i+2)<256))
                                return getMaxConfidence();
                        }
                    } catch (Exception ex) {}
                    return ZERO_CONFIDENCE;
                }
                
                public float getMaxConfidence() { return 1.0f; }
                
                public Dictionary createInstance( String descriptor) 
                    throws DictionaryFactory.InstantiationException {
                    try {
                        return new EDict( descriptor, true);
                    } catch (IOException ex) {
                        throw new DictionaryFactory.InstantiationException( ex.getLocalizedMessage(), ex);
                    }
                }

                public String getName() { return "EDICT"; }
            };

    /**
     * Creates a new dictionary from a dictionary file in EDICT format and the associated
     * xjdx or jjdx index file.
     *
     * @param dicfile Path to a file in EDICT format.
     * @param createindex Flag if the index should be automatically created and written to disk
     *                    if no index file exists. A write will not be attempted if the directory
     *                    is not writable. If no index file exists and createindex is
     *                    false, you have to call {@link #buildIndex() buildIndex} to create the
     *                    index manually.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    public EDict( String dicfile, boolean createindex) throws IOException {
        this.dicfile = dicfile;

        File df = new File( dicfile);
        name = df.getName();
        System.err.println( MessageFormat.format( messages.getString( "dictionary.load"),
                                                  new String[] { name }));
        dictionary = new byte[(int) df.length()];
        InputStream is = new BufferedInputStream( new FileInputStream( df));
        int off = 0;
        int len = dictionary.length;
        int read = 0;
        while (read!=-1 && len>0) {
            read = is.read( dictionary, off, len); // this call might not read all data at once
            off += read;
            len -= read;
        }
        is.close();
        dictionaryLength = dictionary.length - len; // == dictionary.length if the file was read fully

        File jindex = new File( dicfile + JJDX_EXTENSION);
        File xindex = new File( dicfile + XJDX_EXTENSION);
        if (jindex.canRead() && jindex.lastModified()>=df.lastModified()) {
            loadJJDX( jindex);
            xjdxIndex = false;
        }
        else if (xindex.canRead()) {
            // no test for file modification date because the user should have greater manual
            // control over XJDX files since they are not created by JGloss
            loadXJDX( xindex);
            xjdxIndex = true;
        }
        else if (createindex) {
            buildIndex( true);
            try {
                saveJJDX( jindex);
            } catch (IOException ex) {
                System.err.println( MessageFormat.format( messages.getString( "edict.error.writejjdx"),
                                                          new String[] { ex.getClass().getName(),
                                                                         ex.getLocalizedMessage() }));
            }
        }
    }

    /**
     * Searches for entries in the dictionary.
     *
     * @param expression The string to search for.
     * @param mode The search mode. One of <CODE>SEARCH_EXACT_MATCHES, SEARCH_STARTS_WITH,
     *             SEARCH_ENDS_WITH</CODE> or <CODE>SEARCH_ANY_MATCHES</CODE>.
     * @return A list of dictionary entries which match the expression given the search modes.
     *         Items in the list are instances of 
     *         <CODE>DictionaryEntry</CODE>. If no match is found, the empty list will be returned.
     * @exception SearchException if there was an error during the search.
     * @see Dictionary
     * @see DictionaryEntry
     */
    public List search( String expression, short mode) throws SearchException {
        List result = new LinkedList();

        // do a binary search through the index file
        try {
            byte[] expr_euc = expression.getBytes( "EUC-JP");
            int match = findMatch( expr_euc);

            if (match != -1) {
                int firstmatch = findFirstMatch( expr_euc, match);
                int lastmatch = findLastMatch( expr_euc, match);

                // Several index entries can point to the same entry line. For example
                // if a word contains the same kanji twice, and words with this kanji are looked up,
                // one index entry is found for the first occurrence and one for the second. To prevent
                // adding the entry multiple times, entries are stored in a set.
                Set seenEntries = new HashSet( 50);

                // read all matching entries
                for ( match=firstmatch; match<=lastmatch; match++) {
                    int start = index[match];

                    // test if entry matches search mode
                    if (mode == SEARCH_EXACT_MATCHES ||
                        mode == SEARCH_STARTS_WITH) {
                        // test if preceeding character marks beginning of word entry
                        if (start > 0) {
                            switch (dictionary[start-1]) {
                            case 0x0a: // start of word
                            case 0x0d: // start of word
                            case ((byte) '['): // start of reading
                            case ((byte) '/'): // start of translation
                                break;
                            default: // entry does not match
                                continue;
                            }
                        }
                    }
                    if (mode == SEARCH_EXACT_MATCHES ||
                        mode == SEARCH_ENDS_WITH) {
                        // test if preceeding character marks beginning of word entry
                        if (start+expr_euc.length < dictionaryLength) { 
                            switch (dictionary[start+expr_euc.length]) {
                            case ((byte) ' '): // end of word or space in translation
                                if (dictionary[start+expr_euc.length-1] >= 0)
                                    // translation; bytes are signed
                                    continue;
                                break;
                            case ((byte) ']'): // end of reading
                            case ((byte) '/'): // end of translation
                                break;
                            default: // entry does not match
                                continue;
                            }
                        }
                    }

                    // find beginning of entry line
                    while (start>0 && dictionary[start-1]!=0x0a)
                        start--;

                    Integer starti = new Integer( start);
                    if (seenEntries.contains( starti)) {
                        match++;
                        continue;
                    }
                    else
                        seenEntries.add( starti);
                    
                    int end = index[match];
                    while (end<dictionaryLength && dictionary[end]!=0x0a)
                        end++;

                    String entry = new String( dictionary, start, end-start, "EUC-JP");

                    int j, k;
                    // word:
                    String word;
                    int i = entry.indexOf( ' ');
                    if (i == -1) {
                        System.err.println( "WARNING: " + dicfile +
                                            "\nMalformed dictionary entry: " + entry);
                        continue;
                    }
                    else {
                        word = entry.substring( 0, i);
                    }
                    // reading:
                    String reading = null;
                    i = entry.indexOf( '[');
                    if (i != -1) {
                        j = entry.indexOf( ']', i+1);
                        if (j == -1) {
                            System.err.println( "WARNING: " + dicfile +
                                                "\nMalformed dictionary entry: " + entry);
                            continue;
                        }
                        else
                            reading = entry.substring( i+1, j);
                    } // else: no reading

                    // translations
                    i = entry.indexOf( '/', i);
                    if (i == -1) {
                        System.err.println( "WARNING: " + dicfile +
                                            "\nMalformed dictionary entry: " + entry);
                        continue;
                    }
                    // count number of translations
                    int slashes = 1;
                    for ( int x=i+1; x<entry.length(); x++)
                        if (entry.charAt( x)=='/')
                            slashes++;
                    String[] translation = new String[slashes-1];
                    slashes = 0;
                    j = entry.lastIndexOf( '/');
                    while (i < j) {
                        k = entry.indexOf( '/', i+1);
                        translation[slashes++] = entry.substring( i+1, k);
                        i = k;
                    }
                    result.add( new DictionaryEntry( word, reading, translation, this));
                }
            }
        } catch (IOException ex) {
            throw new SearchException( "IOException: " + ex.getMessage());
        }

        return result;
    }

    /**
     * Returns the index of an index entry which matches a word. If there is more than one match,
     * it is not defined which match is returned. If no match is found, <code>-1</code>
     * is returned.
     */
    protected int findMatch( byte[] word) {
        // do a binary search
        int from = 0;
        int to = indexLength-1;
        int match = -1;
        int curr;
        // search matching entry
        do {
            curr = (to-from)/2 + from;
            
            // read entry
            int i = 0;
            while (i<word.length && index[curr]+i<dictionaryLength)
                i++;
            
            // A simple compareToIgnoreCase does not work, because Kstrcmp, which was used
            // by the index creation program behaves somewhat differently.
            // It also does not have the overhead of creating a new string for the search.
            int c = Kstrcmp( word, 0, word.length, dictionary, index[curr], i, xjdxIndex);
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
     */
    protected int findFirstMatch( byte[] word, int match) {
        int firstmatch = match - 1;
        // search backwards for the first matching entry
        while (firstmatch >= 0) {
            // read entry
            int c = -1;
            if (word.length <= dictionaryLength-index[firstmatch])
                c = Kstrcmp( word, 0, word.length, dictionary, index[firstmatch],
                             word.length, xjdxIndex);
            if (c != 0) { // First non-matching entry. End search.
                break;
            }
            else {
                firstmatch--;
            }
        }
        firstmatch++;
        return firstmatch;
    }

    /**
     * Searches forward in the index from an already determined match to the last
     * matching entry.
     */
    protected int findLastMatch( byte[] word, int match) {
        int lastmatch = match + 1;
        while (lastmatch < indexLength) {
            // read entry
            int c = -1;
            if (word.length <= dictionaryLength-index[lastmatch])
                c = Kstrcmp( word, 0, word.length, dictionary, index[lastmatch],
                             word.length, xjdxIndex);
            if (c != 0) { // First non-matching entry. End search.
                break;
            }
            else {
                lastmatch++;
            }
        }
        lastmatch--;
        return lastmatch;
    }

    /**
     * Loads an XJDX-format index for the dictionary. 
     */
    protected void loadXJDX( File indexfile) throws IOException {
        /* The xjdx files are created by a platform-dependent C program. They
           consist of an array of unsigned longs with indexes into the EDICT file.
           On an ix86 box, the format of an array element is 4-byte
           least significant byte first. The following code tries to detect
           endianness by assuming that the MSB is always zero. This works as
           long as the dictionary size does not exceed 2^24 bytes.
           The first entry in the xjdx file contains the size of the dictionary
           file plus the version number of the index format.
        */
        // Test the first 4 entries. On a LSB first architecture it is very impropable that all 4 entries
        // are multiples of 256 and thus have a first byte of 0.
        DataInputStream is = new DataInputStream( new BufferedInputStream( new FileInputStream( indexfile)));
        byte b[] = new byte[16];
        is.readFully( b);
        boolean msbFirst = (b[0]==0) && (b[4]==0) && (b[8]==0) && (b[12]==0);
        is.close();

        // read the index
        index = new int[(int) indexfile.length()/4-1];
        indexLength = index.length;
        InputStream fis = new BufferedInputStream( new FileInputStream( indexfile));
        byte[] ib = new byte[4];
        int[] ii = new int[4];
        for ( int i=0; i<indexLength; i++) {
            fis.read( ib);
            for ( int j=0; j<4; j++)
                ii[j] = (ib[j]>=0) ? ib[j] : 256 + ib[j];
            
            if (msbFirst)
                index[i] = ii[0]<<24 | ii[1]<<16 | ii[2]<<8 | ii[3];
            else
                index[i] = ii[3]<<24 | ii[2]<<16 | ii[1]<<8 | ii[0];
            index[i]--; // XJDX uses 1-based offsets, whereas this class uses 0-based offsets
        }
        fis.close();
    }

    /**
     * Loads a JJDX-format index for the dictionary. The JJDX format is similar to
     * XJDX, but without the endianness-problems.
     *
     * @param indexfile The file which contains the index.
     */
    protected void loadJJDX( File indexfile) throws IOException {
        // JJDX consists of 4 byte long signed integers, with most significant byte first.
        // The first integer is the version of the index format.
        // The second integer is the offset in bytes from the start of the file
        // to the first index entry (12 for version 1001).
        // The third integer is the number of index entries.
        // All other entries are byte offsets into the EUC-encoded EDICT dictionary,
        // ordered alphabetically.

        // read the index
        DataInputStream is = new DataInputStream( new BufferedInputStream( new FileInputStream( indexfile)));
        int version = is.readInt();
        if (version > JJDX_VERSION) {
            System.err.println( MessageFormat.format( messages.getString( "edict.warning.jjdxversion"),
                                                      new String[] { getName() }));
        }
        int offset = is.readInt();
        int size = is.readInt();
        is.skip( offset - 4*3); // should be 0 for version 1001 index
        index = new int[size];
        indexLength = size;
        for ( int i=0; i<size; i++)
            index[i] = is.readInt();
        is.close();
    }

    /**
     * Saves the index to a file in JJDX format.
     *
     * @param indexfile File to write to.
     * @exception IOException when the file cannot be written.
     */
    public void saveJJDX( File indexfile) throws IOException {
        if (indexLength == 0)
            return;

        System.err.println( MessageFormat.format( messages.getString( "edict.writejjdx"),
                                                  new String[] { getName() }));

        DataOutputStream os = new DataOutputStream( new BufferedOutputStream
            ( new FileOutputStream( indexfile)));
        os.writeInt( JJDX_VERSION);
        os.writeInt( 12);
        os.writeInt( indexLength);

        for ( int i=0; i<indexLength; i++)
            os.writeInt( index[i]);
        os.close();
    }

    /**
     * Creates an index for the EDICT dictionary. The dictionary file must have been already loaded.
     * The method will call {@link #preBuildIndex() preBuildIndex}, 
     * {@link #addIndexRange(int,int) addIndexRange( 0, dictionaryLength)} and
     * {@link #postBuildIndex() postBuildIndex()}.
     *
     * @param printMessage Flag if an informational message should be printed to <CODE>System.err</CODE>.
     */
    public void buildIndex( boolean printMessage) {
        if (printMessage)
            System.err.println( MessageFormat.format( messages.getString( "edict.buildindex"), 
                                                      new String[] { getName() }));
      
        xjdxIndex = false;

        preBuildIndex();
        addIndexRange( 0, dictionaryLength);
        postBuildIndex();
    }

    /**
     * First step in index creation.
     */
    protected void preBuildIndex() {
        if (index == null)
            index = new int[50000];
        indexLength = 0;
    }

    /**
     * Parses a subset of the dictionary file for index creation. For each word which should
     * get an index entry, {@link #addIndexEntry(int) addIndexEntry} is called.
     *
     * @param start Byte offset in the dictionary where the parsing should start (inclusive).
     * @param end Byte offset in the dictionary where the parsing should end (exclusive).
     */
    protected void addIndexRange( int start, int end) {
        // Search over the whole dictionary and add an index entry for every kanji/kana string and
        // every alphabetic string with length >= 3.
        // This creates an index similar to one created by xjdxgen.
        boolean inword = false;
        int entry = 0;
        for ( int i=start; i<end; i++) {
            int c = byteToUnsignedByte( dictionary[i]);
            if (inword) {
                if (!(alphaoreuc( dictionary[i]) || c=='-' || c=='.' || (c>='0' && c<='9'))) {
                    inword = false;
                    int len = i - entry;
                    // save all entries with length >= 3 or kanji/kana entries of length 2 (bytes).
                    if (byteToUnsignedByte(dictionary[entry])>=127 || len>2) {
                        addIndexEntry( entry);

                        if (byteToUnsignedByte( dictionary[entry]) > 127) {
                            // add index entry for every kanji in word.
                            if (byteToUnsignedByte( dictionary[entry]) == 0x8f) // JIS X 0212 3-Byte Kanji
                                entry++;
                            for ( int j=entry+2; j<i; j+=2) {
                                int cj = byteToUnsignedByte( dictionary[j]);
                                if (cj >= 0xb0 || cj == 0x8f) {
                                    addIndexEntry( j);
                                    
                                    if (cj == 0x8f) // JIS X 0212 3-Byte Kanji
                                        j++;
                                }
                            }
                        }
                    }
                }
            }
            else {
                if (alphaoreuc( dictionary[i])) {
                    inword = true;
                    entry = i;
                }
            }
        }
    }

    /**
     * Add a word entry to the index. This method will be called by 
     * {@link addIndexRange(int,int) addIndexRange} during index creation.
     *
     * @param offset Offset in the dictionary file where the word starts.
     */
    protected void addIndexEntry( int offset) {
        if (indexLength == index.length) {
            // allocate more storage space
            int[] tindex = new int[Math.max( Math.min( index.length*2, 10), index.length + 500*1024)];
            System.arraycopy( index, 0, tindex, 0, index.length);
            index = tindex;
        }
        index[indexLength++] = offset;
    }

    /**
     * Called as the last step in index file creation. This method will quicksort
     * the index array.
     */
    protected void postBuildIndex() {
        quicksortIndex( 0, indexLength-1);
    }

    /**
     * Determines if b is an alphanumerical ASCII character or an EUC kana or kanji character.
     * Adapted from Jim Breens xjdic.
     *
     * @param b The character to test.
     * @param return <CODE>true</CODE>, if the character is alphanumeric or EUC.
     */
    protected final static boolean alphaoreuc( byte b) {
        int c = byteToUnsignedByte( b);

        if (c>=65 && c<= 90 || c>=97 && c<=122)
            return true;
        if (c>='0' && c<='9')
            return true;
        if ((c & 0x80) > 0)
            return true;

        return false;
    }

    /**
     * Lexicographic comparison of two strings encoded as EUC-JP byte-arrays. 
     * Adapted from Jim Breen's xjdic.
     * Effectively does a equalsIgnoreCase on two "strings" within the dictionary,
     * except it will make katakana and hirgana match (EUC A4 & A5).
     *
     * @param str1 A string encoded as EUC-JP.
     * @param off1 Offset to where to start the comparison in str1.
     * @param len1 Length in bytes of the string.
     * @param str2 A string encoded as EUC-JP.
     * @param off2 Offset to where to start the comparison in str2.
     * @param len2 Length in bytes of the string.
     * @param backwardsCompatible Flag if the comparison should be backwards compatible to
     *        an index generated by xjdxgen from xjdic 2.3.
     */
    protected final int Kstrcmp( byte[] str1, int off1, int len1, 
                                 byte[] str2, int off2, int len2, boolean backwardsCompatible) {
        int c1 = 0, c2 = 0;

        int len = Math.min( len1, len2);

        int b = 1; // byte in multibyte character
        int ONE_BYTE = 1;
        int TWO_BYTES = 2;
        int THREE_BYTES = 3;
        int type = ONE_BYTE;

        for ( int i=0; i<len; i++) {
            c1 = byteToUnsignedByte( str1[i+off1]);
            c2 = byteToUnsignedByte( str2[i+off2]);

            if (backwardsCompatible) {
                if ((i % 2) == 0) {
                    // Convert katakana to hiragana
                    // The i%2 test for first byte in a two-byte character can fail because
                    // a previous character may have been a 3-byte JIS X 0212 Kanji, but I keep
                    // it for XJDX compatibility.
                    if (c1 == 0xA5) {
                        c1 = 0xA4;
                    }
                    if (c2 == 0xA5) {
                        c2 = 0xA4;
                    }
                }
            }
            else {
                if (b == 1) {
                    // convert katakana to hiragana
                    if (c1 == 0xA5) {
                        c1 = 0xA4;
                    }
                    if (c2 == 0xA5) {
                        c2 = 0xA4;
                    }
                    // We only need to test the type of c1, because if c1 and c2 are different,
                    // the equality test ends.
                    if (c1 < 127)
                        type = ONE_BYTE;
                    else {
                        if (c1 == 0x8f) // JIS X 0212 3-Byte Kanji
                            type = THREE_BYTES;
                        else
                            type = TWO_BYTES;
                    }
                    b++;
                }
                else if (b == type) // last byte in char, reset counter
                    b = 1;
            }

            if ((c1 >= 'A') && (c1 <= 'Z')) c1 |= 0x20;
            if ((c2 >= 'A') && (c2 <= 'Z')) c2 |= 0x20;
            if (c1 != c2) break;
        }

        if (c1 == c2)
            return (len2 - len1);
        
        return(c1-c2);
    }

    /**
     * Lexicographic comparison of strings in the dictionary. This is a special comparison
     * which is used when creating the dictionary index with quicksort. Instead of stopping
     * the comparison and returning equality at word boundaries, the method will compare at
     * most 20 bytes starting from the index positions. This increases the chance of
     * inequality even if the indexed words would compare equal. It is done this way because quicksort
     * behaves lousy if there are many equalities.
     *
     * @param i1 Index in the dictionary to the first string.
     * @param i2 Index in the dictionary to the second string.
     */
    protected final int Kstrcmp( int i1, int i2) {
        int c1 = 0, c2 = 0;

        int b = 1; // byte in multibyte character
        int ONE_BYTE = 1;
        int TWO_BYTES = 2;
        int THREE_BYTES = 3;
        int type = ONE_BYTE;

        int i = 0;
        while (i1<dictionaryLength && i2<dictionaryLength && i<20) {
            c1 = byteToUnsignedByte( dictionary[i1]);
            c2 = byteToUnsignedByte( dictionary[i2]);

            if (b == 1) {
                // convert katakana to hiragana
                if (c1 == 0xA5) {
                    c1 = 0xA4;
                }
                if (c2 == 0xA5) {
                    c2 = 0xA4;
                }
                // We only need to test the type of c1, because if c1 and c2 are different,
                // the equality test ends.
                if (c1 < 127)
                    type = ONE_BYTE;
                else {
                    if (c1 == 0x8f) // JIS X 0212 3-Byte Kanji
                        type = THREE_BYTES;
                    else
                        type = TWO_BYTES;
                }
                b++;
            }
            else if (b == type) // last byte in char, reset counter
                b = 1;

            // convert katakana to hiragana
            if ((c1 >= 'A') && (c1 <= 'Z')) c1 |= 0x20;
            if ((c2 >= 'A') && (c2 <= 'Z')) c2 |= 0x20;
            if (c1 != c2) break;

            i1++;
            i2++;
            i++;
        }
        if (i == 20)
            return 0;

        if (c1 == c2) { // end of dictionary for one of the strings
            if (i1 < dictionaryLength)
                return 1; // string 1 > string 2
            else
                return -1;
        }

        return (c1-c2) > 0 ? 1 : -1;
    }

    /**
     * Sorts a part of the index array using randomized quicksort. Call this with
     * (0, indexLenght-1) to sort the whole index.
     */
    protected void quicksortIndex( int left, int right) {
        if (left >= right)
            return;

        int middle = left + random.nextInt( right-left+1);
        int mv = index[middle];
        index[middle] = index[left];

        int l = left + 1; // l is the first index which compares greater mv
        for ( int i=l; i<=right; i++) {
            if (Kstrcmp( mv, index[i]) > 0) {
                if (i > l) {
                    int t = index[i];
                    index[i] = index[l];
                    index[l] = t;
                }
                l++;
            }
        }
        l--;
        index[left] = index[l];
        index[l] = mv;
        
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
        return "EDICT " + name;
    }

    public void dispose() {
        // Free the arrays for garbage collection. This is not really needed
        // because the method should be called just before the object is destroyed, but
        // what the hell...
        dictionary = null;
        index = null;
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
} // class EDict
