/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 * Enumeration of character classes used for index creation. The character classes
 * are used as return parameters of {@link EncodedCharacterHandler#getCharacterClass(int,boolean)
 * getCharacterClass} to decide how a character is to be treated during index creation.
 *
 * @author Michael Koch
 */
public enum CharacterClass {
    /**
     * The character is a kanji character.
     */
    KANJI,
    /**
     * The character is a hiragana character.
     */
    HIRAGANA,
    /**
     * The character is a katakana character. This includes the katakana dash.
     */
     KATAKANA,
    /**
     * The character is part of a roman word. This includes all characters of the western
     * alphabet, may include numbers and other characters like dashes in the middle of a word.
     */
     ROMAN_WORD,
    /**
     * The character is not in any of the other character classes.
     */
    OTHER;
} // class CharacterClass
