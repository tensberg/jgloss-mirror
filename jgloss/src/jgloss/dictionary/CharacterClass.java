/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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
 * Typesafe enumeration of character classes used for index creation. The character classes
 * are used as return parameters of {@link EncodedCharacterHandler#getCharacterClass(int,boolean)
 * getCharacterClass} to decide how a character is to be treated during index creation.
 *
 * @author Michael Koch
 */
public class CharacterClass {
    /**
     * The character is a kanji character.
     */
    public static final CharacterClass KANJI = new CharacterClass( "KANJI");
    /**
     * The character is a hiragana character.
     */
    public static final CharacterClass HIRAGANA = new CharacterClass( "HIRAGANA");
    /**
     * The character is a katakana character. This includes the katakana dash.
     */
    public static final CharacterClass KATAKANA = new CharacterClass( "KATAKANA");
    /**
     * The character is part of a roman word. This includes all characters of the western
     * alphabet, may include numbers and other characters like dashes in the middle of a word.
     */
    public static final CharacterClass ROMAN_WORD = new CharacterClass( "ROMAN WORD");
    /**
     * The character is not in any of the other character classes.
     */
    public static final CharacterClass OTHER = new CharacterClass( "OTHER");

    private String name;

    private CharacterClass( String _name) {
        this.name = _name;
    }

    public String toString() {
        return "Character class: " + name;
    }
} // class CharacterClass
