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
 * Extension of the EDict implementation which allows the manipulation of entries.
 * The dictionary and index file format is identical to that of the superclass.
 * The class is synchronized to allow concurrent access to the manipulation and search
 * methods. This also means that search performance will be a little worse than
 * that of the superclass.
 *
 * @author Michael Koch
 */
public class EditableEDict extends EDict {
    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class. Since every EDICT is an editable EDICT, this
     * implementation will always return a higher confidence than the superclass implementation
     * for matching descriptors.
     *
     * @see DictionaryFactory
     */
    public final static DictionaryFactory.Implementation implementation = 
        new DictionaryFactory.Implementation() {
                public float isInstance( String descriptor) {
                    return EDict.implementation.isInstance( descriptor)*2;
                }

                public float getMaxConfidence() {
                    return EDict.implementation.getMaxConfidence()*2;
                }

                public Dictionary createInstance( String descriptor) 
                    throws DictionaryFactory.InstantiationException {
                    try {
                        return new EditableEDict( descriptor);
                    } catch (IOException ex) {
                        throw new DictionaryFactory.InstantiationException( ex.getLocalizedMessage(), ex);
                    }
                }

                public String getName() { return EDict.messages.getString( "edict.editable.name"); }

                public Class getDictionaryClass( String descriptor) { return EditableEDict.class; }
            };

    /**
     * Flag if the dictionary was changed. This also means that the index is invalid.
     */
    protected boolean dictionaryChanged;
    
    /**
     * Create a new editable EDICT with the given file name. If the dictionary file does not
     * already exist, a new file will be created.
     */
    public EditableEDict( String dicfile) throws IOException {
        super( ensureExistence( dicfile), true);
        dictionaryChanged = false;
    }

    /**
     * Make sure that a file exists. If the file does not exist, a new empty file will be
     * created.
     *
     * @param dicfile Name of the file to test.
     * @return The name of the file.
     */
    protected static String ensureExistence( String dicfile) throws IOException {
        new File( dicfile).createNewFile(); // will only create the file if it does not exist
        return dicfile;
    }

    public void addEntry( DictionaryEntry entry) {
        addEntry( entry.getWord(), entry.getReading(), entry.getTranslations());
    }

    public void addEntry( String word, String reading, String translation) {
        addEntry( word, reading, Collections.singletonList( translation));
    }

    public void addEntry( String word, String reading, List translations) {
        try {
            // Replace any special characters in the input
            word = word.replace( ' ', '\u3000'); // japanese space
            word = word.replace( (char) 0x0a, '_');
            word = word.replace( (char) 0x0d, '_');
            byte[] wa = word.getBytes( "EUC-JP");
            byte[] ra = null;
            if (reading!=null && reading.length()>0) {
                reading = reading.replace( '[', '\uff3b'); // japanese [
                reading = reading.replace( ']', '\uff3d'); // japanese ]
                reading = reading.replace( (char) 0x0a, '_');
                reading = reading.replace( (char) 0x0d, '_');
                ra = reading.getBytes( "EUC-JP");
            }
            byte[][] ta = new byte[translations.size()][];
            for ( int i=0; i<translations.size(); i++) {
                String translation = ((String) translations.get( i)).replace( '/', '|');
                translation = translation.replace( '/', '|');
                translation = translation.replace( (char) 0x0a, '_');
                translation = translation.replace( (char) 0x0d, '_');
                ta[i] = translation.getBytes( "EUC-JP");
            }

            synchronized (this) {
                // merge new entry with already existing entry and delete it
                int match = findExistingEntry( wa, ra);
                if (match != -1) {
                    LinkedList translationlist = new LinkedList();

                    // iterate over all translations in the entry
                    boolean[] seen = new boolean[ta.length];
                    int newTranslations = ta.length;
                    match = index[match];
                    // start of translation (inclusive)
                    int i = match + wa.length + 1 +
                        (ra != null ? ra.length + 3 : 0) + 1; // first character after first slash
                    int j; // end of translation (exclusive)
                    do {
                        j = i;
                        while (j<dictionaryLength && dictionary[j]!='/' &&
                               dictionary[j]!=0x0a && dictionary[j]!=0x0d)
                            j++;
                        if (j==dictionaryLength || dictionary[j]!='/')
                            break; // invalid entry
                        if (j == i) // entry with length 0
                            continue;

                        // test if existing translation matches any of the new ones
                        for ( int k=0; k<ta.length; k++) {
                            boolean equal = true;
                            if (ta[k].length != j-i)
                                equal = false;
                            else {
                                for ( int l=0; l<ta[k].length; l++) {
                                    if (dictionary[i+l] != ta[k][l]) {
                                        equal = false;
                                        break;
                                    }
                                }
                            }
                            if (!equal) {
                                byte[] nt = new byte[j-i];
                                System.arraycopy( dictionary, i, nt, 0, nt.length);
                                translationlist.add( nt);
                            }
                            else {
                                // Count number of translations in both the new
                                // and old translation set. The seen array avoids
                                // counting an entry twice if the old set for some
                                // reasons has duplicate entries.
                                if (!seen[k])
                                    newTranslations--;
                                seen[k] = true;
                            }
                        }

                        i = j + 1; // next entry
                    } while (i < dictionaryLength && dictionary[j]=='/' &&
                             dictionary[i]!=0x0a && dictionary[i]!=0x0d);
                    // i is now the index of the first char after the entry

                    if (newTranslations == 0) {
                        return;
                    }

                    if (translationlist.size() > 0) {
                        // merge new and old translations; new entries come first
                        for ( int k=ta.length-1; k>=0; k--)
                            translationlist.addFirst( ta[k]);
                        ta = (byte[][]) translationlist.toArray( ta);
                    }

                    // delete the old entry
                    // make sure that the newline at the end of the entry is also deleted
                    if (i<dictionaryLength && 
                        (dictionary[i]==0x0a || dictionary[i]==0x0d))
                        i++;
                    if (i<dictionaryLength && 
                        (dictionary[i]==0x0a || dictionary[i]==0x0d))
                        i++;
                    // i is now the index of first char in the new entry
                    if (i < dictionaryLength)
                        System.arraycopy( dictionary, i, dictionary, match, dictionaryLength-i);
                    dictionaryLength -= i-match;
                }

                // calculate the size of the new entry
                int size = wa.length + 1;
                if (ra != null)
                    size += ra.length + 3;
                for ( int i=0; i<ta.length; i++)
                    size += ta[i].length + 1;
                size += 2; // trailing slash and newline

                // reserve room
                if (dictionaryLength + size > dictionary.length) {
                    byte[] tdictionary = new byte[Math.min( (dictionary.length+size)*2,
                                                            dictionary.length+size+256*1024)];
                    System.arraycopy( dictionary, 0, tdictionary, 0, dictionaryLength);
                    dictionary = tdictionary;
                }

                // copy entry
                int entryStart = dictionaryLength;
                System.arraycopy( wa, 0, dictionary, dictionaryLength, wa.length);
                dictionaryLength += wa.length;
                dictionary[dictionaryLength++] = (byte) ' ';
                if (ra != null) {
                    dictionary[dictionaryLength++] = (byte) '[';
                    System.arraycopy( ra, 0, dictionary, dictionaryLength, ra.length);
                    dictionaryLength += ra.length;
                    dictionary[dictionaryLength++] = (byte) ']';
                    dictionary[dictionaryLength++] = (byte) ' ';                 
                }
                for ( int i=0; i<ta.length; i++) {
                    dictionary[dictionaryLength++] = (byte) '/';
                    System.arraycopy( ta[i], 0, dictionary, dictionaryLength, ta[i].length);
                    dictionaryLength += ta[i].length;
                }
                dictionary[dictionaryLength++] = (byte) '/';
                dictionary[dictionaryLength++] = (byte) '\n';

                dictionaryChanged = true;
                // rebuild index
                if (match != -1) {
                    // an old entry was deleted, the index must be completely rebuilt
                    buildIndex( false);
                }
                else {
                    // add new entry to index
                    addIndexRange( entryStart, dictionaryLength);
                    postBuildIndex();
                }
            }
        } catch (UnsupportedEncodingException ex) {}
    }

    public void deleteEntry( String word, String reading) {
        try {
            // Replace any special characters in the input
            word = word.replace( ' ', '\u3000'); // japanese space
            word = word.replace( (char) 0x0a, '_');
            word = word.replace( (char) 0x0d, '_');
            byte[] wa = word.getBytes( "EUC-JP");
            byte[] ra = null;
            if (reading!=null && reading.length()>0) {
                reading = reading.replace( '[', '\uff3b'); // japanese [
                reading = reading.replace( ']', '\uff3d'); // japanese ]
                reading = reading.replace( (char) 0x0a, '_');
                reading = reading.replace( (char) 0x0d, '_');
                ra = reading.getBytes( "EUC-JP");
            }

            synchronized (this) {
                int match = findExistingEntry( wa, ra);
                if (match != -1) {
                    int i = index[match];
                    int j = i+1;
                    // find end of entry
                    while (j < dictionaryLength && dictionary[j]!=0x0a &&
                           dictionary[j]!=0x0d)
                        j++;
                    if (j<dictionaryLength && 
                        (dictionary[j]==0x0a || dictionary[j]==0x0d))
                        j++;
                    if (j<dictionaryLength && 
                        (dictionary[j]==0x0a || dictionary[j]==0x0d))
                        j++;
                    if (j < dictionaryLength)
                        System.arraycopy( dictionary, j, dictionary, i, dictionaryLength-i);
                    dictionaryLength -= j-i;

                    dictionaryChanged = true;
                    buildIndex( false);
                }
            }
        } catch (UnsupportedEncodingException ex) {}
    }

    /**
     * Searches for entries in the dictionary. The method will rebuild and write the index if
     * it was invalidated by a previous edit. Overridden to add synchronization on <code>this</code>.
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
    public synchronized List search( String expression, short searchmode, short resultmode)
        throws SearchException {
        return super.search( expression, searchmode, resultmode);
    }

    /**
     * Searches the dictionary for an entry. Since an EDICT dictionary should not 
     * contain multiple entries with the same word and reading, the first match found
     * is returned.
     *
     * @param word The entry word encoded in EUC-JP.
     * @param reading The entry reading encoded in EUC-JP, or <CODE>null</CODE> if the word has
     *                no reading.
     * @return Index in the index array to a matching entry, or -1 if no match was found.
     */
    protected int findExistingEntry( byte[] word, byte[] reading) {
        int match = findMatch( word);
        if (match == -1)
            return -1;

        int firstmatch = findFirstMatch( word, match);
        int lastmatch = findLastMatch( word, match);
        for ( match=firstmatch; match<=lastmatch; match++) {
            int i = index[match];

            // test if entry fits in dictionary array
            if (i + word.length + 1 + 
                (reading!=null ? reading.length + 3 : 0)
                + 2 > dictionaryLength)
                continue;
            // test exact match
            if (i>0 && dictionary[i-1]!=0x0a && dictionary[i-1]!=0x0d ||
                dictionary[i+word.length]!=(byte) ' ')
                continue;

            // test reading match
            if (reading != null) {
                if (dictionary[i+word.length+1] != (byte) '[' ||
                    dictionary[i+word.length+2+reading.length] != (byte) ']')
                    continue;

                boolean isMatch = true;
                for ( int j=0; j<reading.length; j++) {
                    if (dictionary[i+word.length+2+j] != reading[j]) {
                        isMatch = false;
                        break;
                    }
                }
                if (!isMatch)
                    continue;

            }
            else if (dictionary[i+word.length+1] != (byte) '/')
                continue;

            // Word and reading match and an EDICT dictionary should not contain
            // two entries with identical word and reading.
            return match;
        }

        return -1;
    }

    /**
     * Saves the index to the file derived from the dictionary file name + ".jjdx". Errors
     * will be written to <CODE>System.err</CODE>.
     */
    protected void saveIndex() {
        try {
            if (indexLength == 0) {
                new File( dicfile + INDEX_EXTENSION).delete();
            }
            else
                saveJJDX( new File( dicfile + INDEX_EXTENSION));
        } catch (Exception ex) {
            System.err.println( MessageFormat.format( messages.getString( "edict.error.writejjdx"),
                                                      new String[] { ex.getClass().getName(),
                                                                     ex.getLocalizedMessage() }));
        }
    }

    /**
     * Writes the dictionary array.
     */
    protected void saveDictionary() {
        try {
            if (dictionaryLength == 0) {
                new File( dicfile).delete();
            }
            else {
                OutputStream o = new FileOutputStream( dicfile);
                o.write( dictionary, 0, dictionaryLength);
                o.close();
            }
        } catch (Exception ex) {
            System.err.println( MessageFormat.format( messages.getString( "edict.error.writedictionary"),
                                                      new String[] { ex.getClass().getName(),
                                                                     ex.getLocalizedMessage() }));
        }
    }

    /**
     * This will write the dictionary file and index if they were changed.
     */
    public synchronized void dispose() {
        if (dictionaryChanged) {
            saveDictionary();
            saveIndex();
        }
    }

    /**
     * Returns a string representation of this dictionary.
     *
     * @return A string representation of this dictionary.
     */
    public String toString() {
        return EDict.messages.getString( "edict.editable.name") + " " + name;
    }
} // class EditableEDict
