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
 * Container storing the selection of the several searchable fields.
 * The word, reading and translation fields may be searched independently, and
 * some dictionary or search types may only support searching in some of the fields,
 * Additionally, searching in the translation field can be done in three modes. Usually
 * when specifying search parameters, the three translation search choices are mutually
 * exclusive. This class does not enforce the exclusiveness.
 *
 * @author Michael Koch
 */
public class SearchFieldSelection {
    private boolean wordSelected = false;
    private boolean readingSelected = false;
    private boolean translationExpressionSelected = false;
    private boolean translationWordsSelected = false;
    private boolean translationAnySelected = false;
    private boolean translationSelected = false;

    /**
     * Creates a new instance in which none of the fields is selected.
     */
    public SearchFieldSelection() {}

    /**
     * Toggle the selection value of a field.
     */
    public void select( SearchField field, boolean selected) {
        if (field == SearchField.WORD)
            wordSelected = selected;
        else if (field == SearchField.READING)
            readingSelected = selected;
        else if (field == TranslationSearchField.EXPRESSION) {
            translationExpressionSelected = selected;
        }
        else if (field == TranslationSearchField.WORDS) {
            translationWordsSelected = selected;
        }
        else if (field == TranslationSearchField.ANY) {
            translationAnySelected = selected;
        }
        else
            throw new IllegalArgumentException();

        translationSelected = translationExpressionSelected|
            translationWordsSelected|translationAnySelected;
    }

    /**
     * Test if a field is selected.
     */
    public boolean isSelected( SearchField field) {
        if (field == SearchField.WORD)
            return wordSelected;
        else if (field == SearchField.READING)
            return readingSelected;
        else if (field == TranslationSearchField.EXPRESSION)
            return translationExpressionSelected;
        else if (field == TranslationSearchField.WORDS)
            return translationWordsSelected;
        else if (field == TranslationSearchField.ANY)
            return translationAnySelected;
        else
            throw new IllegalArgumentException();        
    }

    /**
     * Test if one of the translation search fields is selected.
     */
    public boolean isTranslationSelected() {
        return translationSelected;
    }

    /**
     * Test if any of the search fields is selected.
     */
    public boolean hasSelection() {
        return wordSelected|readingSelected|translationSelected;
    }
} // class SearchFieldSelection
