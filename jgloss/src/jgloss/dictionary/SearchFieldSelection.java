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

/**
 * Container storing information about search field and match mode selection states.
 *
 * @author Michael Koch
 */
public class SearchFieldSelection {
    private boolean wordSelected = false;
    private boolean readingSelected = false;
    private boolean translationSelected = false;

    private boolean matchField;
    private boolean matchWord;

    /**
     * Creates a new instance in which none of the fields is selected.
     */
    public SearchFieldSelection() {}

    /**
     * Toggle the selection value of a field.
     */
    public void select( DictionaryEntryField field, boolean selected) {
        if (field == DictionaryEntryField.WORD)
            wordSelected = selected;
        else if (field == DictionaryEntryField.READING)
            readingSelected = selected;
        else if (field == DictionaryEntryField.TRANSLATION) {
            translationSelected = selected;
        }
        else
            throw new IllegalArgumentException();
    }

    /**
     * Test if a field is selected.
     */
    public boolean isSelected( DictionaryEntryField field) {
        if (field == DictionaryEntryField.WORD)
            return wordSelected;
        else if (field == DictionaryEntryField.READING)
            return readingSelected;
        else if (field == DictionaryEntryField.TRANSLATION)
            return translationSelected;
        else
            throw new IllegalArgumentException();        
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
        if (mode == MatchMode.FIELD)
            matchField = selected;
        else if (mode == MatchMode.WORD)
            matchWord = selected;
        else
            throw new IllegalArgumentException();
    }

    public boolean isSelected( MatchMode mode) {
        if (mode == MatchMode.FIELD)
            return matchField;
        else if (mode == MatchMode.WORD)
            return matchWord;
        else
            throw new IllegalArgumentException();
    }
} // class SearchFieldSelection
