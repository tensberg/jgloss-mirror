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

import jgloss.dictionary.attribute.AttributeSet;

/**
 * Single entry in a dictionary which supports the {@link Dictionary Dictionary} interface.
 *
 * @author Michael Koch
 */
public interface DictionaryEntry {
    /**
     * Attribute groups dictionary entries have.
     */
    public enum AttributeGroup {
    	GENERAL, WORD, READING, TRANSLATION
    }

    /**
     * Return the set of attributes which apply to the whole entry.
     */
    AttributeSet getGeneralAttributes();
 
    /**
     * Return one spelling variant of the entry word. Each entry word may have several spelling
     * variants (okurigana/katakana/romaji/irr. spellings).
     *
     * @param alternative Index (0-based) of the spelling variant requested.
     * @exception IllegalArgumentException if the alternative parameter is out of range.
     */
    String getWord( int alternative);
    /**
     * Returns the number of alternative word spellings. Returns values >= 1.
     */
    int getWordAlternativeCount();
    /**
     * Returns the set of attributes which apply to a particular word spelling.
     *
     * @param alternative Index (0-based) of the spelling variant requested.
     * @exception IllegalArgumentException if the alternative parameter is out of range.
     */
    AttributeSet getWordAttributes( int alternative);
    /**
     * Returns the set of attributes which apply to all word alternatives.
     */
    AttributeSet getWordAttributes();

    /**
     * Returns the reading of the word. There is always at least one reading. 
     * More than one reading is only allowed if
     * (a) the meaning (translation and all attributes) of all readings are identical
     * and (b) the spelling of the reading does not conflict with any word part.
     *
     * @param alternative Index (0-based) of the spelling variant requested.
     * @exception IllegalArgumentException if the alternative parameter is out of bounds.
     */
    String getReading( int alternative);
    /**
     * Returns the number of alternative readings. Returns values >= 1.
     */
    int getReadingAlternativeCount();
    /**
     * Returns the set of attributes which apply to a particular reading.
     *
     * @param alternative Index (0-based) of the reading requested.
     * @exception IllegalArgumentException if the alternative parameter is out of bounds.
     */
    AttributeSet getReadingAttributes( int alternative);
    /**
     * Returns the set of attributes which apply to all readings.
     */
    AttributeSet getReadingAttributes();

    /**
     * Returns a translation of this entry. Translations are separated in several groups, forming
     * a tree-like structure. With the entry as root, the first level of children are the
     * <em>ranges of meaning (rom)</em> . Each range of meaning can have one or more <em>closely related
     * meanings (crm)</em>. The lowest level is the translation, 
     * where there can be several synonymous
     * words for every crm. There is always at least 1 rom, 1 crm and one definition (synonym).
     *
     * @param rom Requested range of meanings (0-based).
     * @param crm Requested closely related meaning (0-based).
     * @param synonym Requested synonym of a translation (0-based).
     * @exception IllegalArgumentException if one of the parameters is out of range.
     */
    String getTranslation( int rom, int crm, int synonym);
    /**
     * Returns the number of ranges of meanings. Returns values >= 1.
     */
    int getTranslationRomCount();
    /**
     * Returns the number of closely related meanings of one range of meanings. Returns values >= 1.
     *
     * @param rom Requested range of meanings (0-based).
     * @exception IllegalArgumentException if one of the parameters is out of range.
     */
    int getTranslationCrmCount( int rom);
    /**
     * Returns the number of synonyms for one closely related meaning, range of meanings.
     * Returns values >= 1.
     *
     * @param rom Requested range of meanings (0-based).
     * @param crm Requested closely related meaning (0-based).
     * @exception IllegalArgumentException if one of the parameters is out of range.
     */
    int getTranslationSynonymCount( int rom, int crm);
    /**
     * Returns the set of attributes which apply to a particular translation.
     *
     * @param rom Requested range of meanings (0-based).
     * @param crm Requested closely related meaning (0-based).
     * @param synonym Requested synonym of a translation (0-based).
     * @exception IllegalArgumentException if one of the parameters is out of range.
     */
    AttributeSet getTranslationAttributes( int rom, int crm, int synonym);
    /**
     * Returns the set of attributes which apply to a particular closely related meaning.
     *
     * @param rom Requested range of meanings (0-based).
     * @param crm Requested closely related meaning (0-based).
     * @exception IllegalArgumentException if one of the parameters is out of range.
     */
    AttributeSet getTranslationAttributes( int rom, int crm);
    /**
     * Returns the set of attributes which apply to a particular range of meanings.
     *
     * @param rom Requested range of meanings (0-based).
     * @param crm Requested closely related meaning (0-based).
     * @exception IllegalArgumentException if one of the parameters is out of range.
     */
    AttributeSet getTranslationAttributes( int rom);
    /**
     * Returns the set of attributes which apply to all translations.
     */
    AttributeSet getTranslationAttributes();

    /**
     * Returns the dictionary from which this entry originated.
     */
    Dictionary getDictionary();

    /**
     * Get a reference to this entry.
     */
    DictionaryEntryReference getReference();
} // interface DictionaryEntry
