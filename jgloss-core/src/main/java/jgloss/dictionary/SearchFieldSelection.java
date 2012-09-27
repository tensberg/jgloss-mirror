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

package jgloss.dictionary;

/**
 * Container storing information about search field and match mode selection states.
 *
 * @author Michael Koch
 */
public class SearchFieldSelection implements Cloneable {
    private boolean wordSelected = false;
    private boolean readingSelected = false;
    private boolean translationSelected = false;

    private boolean matchField = false;
    private boolean matchWord = false;

    /**
     * Creates a new instance in which none of the fields is selected.
     */
    public SearchFieldSelection() {}

    public SearchFieldSelection( boolean _wordSelected, boolean _readingSelected, 
                                 boolean _translationSelected, 
                                 boolean _matchField, boolean _matchWord) {
        wordSelected = _wordSelected;
        readingSelected = _readingSelected;
        translationSelected = _translationSelected;
        matchField = _matchField;
        matchWord = _matchWord;
    }

    /**
     * Toggle the selection value of a field.
     */
    public void select( DictionaryEntryField field, boolean selected) {
        if (field == DictionaryEntryField.WORD) {
	        wordSelected = selected;
        } else if (field == DictionaryEntryField.READING) {
	        readingSelected = selected;
        } else if (field == DictionaryEntryField.TRANSLATION) {
            translationSelected = selected;
        } else {
	        throw new IllegalArgumentException();
        }
    }

    /**
     * Test if a field is selected.
     */
    public boolean isSelected( DictionaryEntryField field) {
        if (field == DictionaryEntryField.WORD) {
	        return wordSelected;
        } else if (field == DictionaryEntryField.READING) {
	        return readingSelected;
        } else if (field == DictionaryEntryField.TRANSLATION) {
	        return translationSelected;
        } else {
	        throw new IllegalArgumentException();
        }        
    }

    /**
     * Test if the configuration of the object is valid as a search parameter.
     * This is the case if at least one of the search fields is selected and
     * exactly one of the match modes is selected.
     */
    public boolean isValid() {
        return (wordSelected|readingSelected|translationSelected) &&
            (matchField ^ matchWord);
    }

    public void select( MatchMode mode, boolean selected) {
        if (mode == MatchMode.FIELD) {
	        matchField = selected;
        } else if (mode == MatchMode.WORD) {
	        matchWord = selected;
        } else {
	        throw new IllegalArgumentException();
        }
    }

    public boolean isSelected( MatchMode mode) {
        if (mode == MatchMode.FIELD) {
	        return matchField;
        } else if (mode == MatchMode.WORD) {
	        return matchWord;
        } else {
	        throw new IllegalArgumentException();
        }
    }

    public SearchFieldSelection or( SearchFieldSelection sfs) {
        wordSelected |= sfs.wordSelected;
        readingSelected |= sfs.readingSelected;
        translationSelected |= sfs.translationSelected;
        matchField |= sfs.matchField;
        matchWord |= sfs.matchWord;

        return this;
    }

    public SearchFieldSelection and( SearchFieldSelection sfs) {
        wordSelected &= sfs.wordSelected;
        readingSelected &= sfs.readingSelected;
        translationSelected &= sfs.translationSelected;
        matchField &= sfs.matchField;
        matchWord &= sfs.matchWord;

        return this;
    }

    @SuppressWarnings("PMD") // generated code
    @Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + (matchField ? 1231 : 1237);
	    result = prime * result + (matchWord ? 1231 : 1237);
	    result = prime * result + (readingSelected ? 1231 : 1237);
	    result = prime * result + (translationSelected ? 1231 : 1237);
	    result = prime * result + (wordSelected ? 1231 : 1237);
	    return result;
    }
    
    @SuppressWarnings("PMD") // generated code
	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    SearchFieldSelection other = (SearchFieldSelection) obj;
	    if (matchField != other.matchField)
		    return false;
	    if (matchWord != other.matchWord)
		    return false;
	    if (readingSelected != other.readingSelected)
		    return false;
	    if (translationSelected != other.translationSelected)
		    return false;
	    if (wordSelected != other.wordSelected)
		    return false;
	    return true;
    }

	@Override
	public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) { return null; }
    }
} // class SearchFieldSelection
