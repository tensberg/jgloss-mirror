package jgloss.ui;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Remembers the first dictionary entry found in a dictionary lookup. All other entries are
 * ignored.
 */
class FirstEntryCache implements LookupResultHandler {
    private DictionaryEntry firstEntry;

    FirstEntryCache() {}

    /**
     * Return the first dictionary entry found in the previous search. If no entry was found,
     * <code>null</code> is returned.
     */
    public DictionaryEntry getEntry() { return firstEntry; }

    public void startLookup( String description) {
        startLookup();
    }

    public void startLookup( LookupModel model) {
        startLookup();
    }

    private void startLookup() {
        firstEntry = null;
    }

    public void dictionary( Dictionary d) {}

    public void dictionaryEntry( DictionaryEntry de) {
        if (firstEntry == null)
            firstEntry = de;
    }

    public void exception( SearchException ex) {}

    public void note( String note) {}

    public void endLookup() {}
} // class FirstEntryCache
