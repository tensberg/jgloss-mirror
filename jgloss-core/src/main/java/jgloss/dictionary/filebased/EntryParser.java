package jgloss.dictionary.filebased;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Convert a line of text in a dictionary into a {@link DictionaryEntry}. Provided by concrete implementations
 * of {@link FileBasedDictionary}.
 */
interface EntryParser {

	/**
	 * Sets the dictionary for which the entry parser is created. Called from the 
	 * {@link FileBasedDictionary#FileBasedDictionary(FileBasedDictionaryStructure, EntryParser, java.io.File, String) constructor of FileBasedDictionary}.
	 * 
	 * @param dictionary {@link FileBasedDictionary} instance for which this entry parser is created.
	 */
	void setDictionary(Dictionary dictionary);
	
    /**
     * Create a {@link DictionaryEntry DictionaryEntry} object from the entry string from the dictionary.
     *
     * @param entry Entry string as found in the dictionary.
     * @param startOffset Start offset of the entry in the dictionary file.
     * @exception SearchException if the dictionary entry is malformed.
     */
    DictionaryEntry parseEntry( String entry, int startOffset) throws SearchException;

}
