package jgloss.ui;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Remembers the first dictionary entry found in a dictionary lookup. All other entries are
 * ignored.
 */
public class FirstEntryCache implements LookupResultHandler {
    private DictionaryEntry firstEntry;

    public FirstEntryCache() {}

    /**
     * Return the first dictionary entry found in the previous search. If no entry was found,
     * <code>null</code> is returned.
     */
    public DictionaryEntry getEntry() { return firstEntry; }

    @Override
	public void startLookup( String description) {
        startLookup();
    }

    @Override
	public void startLookup( LookupModel model) {
        startLookup();
    }

    private void startLookup() {
        firstEntry = null;
    }

    @Override
	public void dictionary( Dictionary d) {}

    @Override
	public void dictionaryEntry( DictionaryEntry de) {
        if (firstEntry == null) {
	        firstEntry = de;
        }
    }

    @Override
	public void exception( SearchException ex) {}

    @Override
	public void note( String note) {}

    @Override
	public void endLookup() {}
} // class FirstEntryCache
