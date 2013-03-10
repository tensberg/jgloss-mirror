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
 */

package jgloss.dictionary.filebased;

import static jgloss.dictionary.DictionaryEntryField.READING;
import static jgloss.dictionary.DictionaryEntryField.TRANSLATION;
import static jgloss.dictionary.DictionaryEntryField.WORD;
import static org.fest.assertions.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Test;

public class EDictStructureTest {

    private static ByteBuffer asBuffer(String string) {
        try {
            return ByteBuffer.wrap(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final ByteBuffer simpleEntry =
                    //        0         1         2          3
                    //        01234567890123456789012345678 901234567
                    asBuffer("KANJI [READING] /translation/\nKANJI-2 /...");

    private final ByteBuffer simpleEntryWithoutReading =
                    //        0         1         2          3
                    //        01234567890123456789012345678 901234567
                    asBuffer("KANJI /translation/");

    private final ByteBuffer complexEntry =
                    //        0         1         2         3         4         5         6         7         8
                    //        012345678901234567890123456789012345678901234567890123456789012345678901234567890
                    asBuffer("KANJI-1(P);KANJI-2;KANJI-3 [READING-1;READING-2] /translation-1/(2) translation-2/");

    private final EDictStructure structure = new EDictStructure();

    @Test
    public void testIsFieldStartWordSimpleEntry() {
        assertThat(structure.isFieldStart(simpleEntry, 0, WORD)).isTrue();
        assertThat(structure.isFieldStart(simpleEntry, 30, WORD)).isTrue();
        assertThat(structure.isFieldStart(simpleEntry, 1, WORD)).isFalse();
    }

    @Test
    public void testIsFieldEndWordSimpleEntry() {
        assertThat(structure.isFieldEnd(simpleEntry, 5, WORD)).isTrue();
        assertThat(structure.isFieldEnd(simpleEntry, 6, WORD)).isFalse();
        assertThat(structure.isFieldEnd(simpleEntry, 4, WORD)).isFalse();
    }

    @Test
    public void testIsFieldStartWordSecondWord() {
        assertThat(structure.isFieldStart(complexEntry, 11, WORD)).isTrue();
        assertThat(structure.isFieldStart(complexEntry, 12, WORD)).isFalse();
        assertThat(structure.isFieldStart(complexEntry, 10, WORD)).isFalse();
    }

    @Test
    public void testIsFieldEndWordSecondWord() {
        assertThat(structure.isFieldEnd(complexEntry, 18, WORD)).isTrue();
        assertThat(structure.isFieldEnd(complexEntry, 17, WORD)).isFalse();
        assertThat(structure.isFieldEnd(complexEntry, 19, WORD)).isFalse();
    }

    @Test
    public void testIsFieldEndWordWordWithAttribute() {
        assertThat(structure.isFieldEnd(complexEntry, 7, WORD)).isTrue();
        assertThat(structure.isFieldEnd(complexEntry, 6, WORD)).isFalse();
    }

    @Test
    public void testIsFieldStartReading() {
        assertThat(structure.isFieldStart(simpleEntry, 7, READING)).isTrue();
        assertThat(structure.isFieldStart(simpleEntry, 8, READING)).isFalse();
        assertThat(structure.isFieldStart(simpleEntry, 5, READING)).isFalse();
    }

    @Test
    public void testIsFieldEndReading() {
        assertThat(structure.isFieldEnd(simpleEntry, 14, READING)).isTrue();
        assertThat(structure.isFieldEnd(simpleEntry, 15, READING)).isFalse();
        assertThat(structure.isFieldEnd(simpleEntry, 13, READING)).isFalse();
    }

    @Test
    public void testIsFieldStartReadingSecondReading() {
        assertThat(structure.isFieldStart(complexEntry, 38, READING)).isTrue();
        assertThat(structure.isFieldStart(complexEntry, 37, READING)).isFalse();
        assertThat(structure.isFieldStart(complexEntry, 39, READING)).isFalse();
    }

    @Test
    public void testIsFieldEndReadingComplexReading() {
        assertThat(structure.isFieldEnd(complexEntry, 37, READING)).isTrue();
        assertThat(structure.isFieldEnd(complexEntry, 38, READING)).isFalse();
        assertThat(structure.isFieldEnd(complexEntry, 36, READING)).isFalse();
    }

    @Test
    public void testIsFieldStartTranslation() {
        assertThat(structure.isFieldStart(simpleEntry, 17, TRANSLATION)).isTrue();
        assertThat(structure.isFieldStart(simpleEntry, 18, TRANSLATION)).isFalse();
        assertThat(structure.isFieldStart(simpleEntry, 16, TRANSLATION)).isFalse();
    }

    @Test
    public void testIsFieldEndTranslation() {
        assertThat(structure.isFieldEnd(simpleEntry, 28, TRANSLATION)).isTrue();
        assertThat(structure.isFieldEnd(simpleEntry, 27, TRANSLATION)).isFalse();
    }

    @Test
    public void testIsFieldEndAnyFieldOnLineBreak() {
        assertThat(structure.isFieldEnd(simpleEntry, 29, WORD)).isTrue();
        assertThat(structure.isFieldEnd(simpleEntry, 29, READING)).isTrue();
        assertThat(structure.isFieldEnd(simpleEntry, 29, TRANSLATION)).isTrue();
    }

    @Test
    public void testIsFieldStartSecondTranslation() {
        assertThat(structure.isFieldStart(complexEntry, 63, TRANSLATION)).isFalse();
        assertThat(structure.isFieldStart(complexEntry, 64, TRANSLATION)).isTrue();
        assertThat(structure.isFieldStart(complexEntry, 65, TRANSLATION)).isFalse();

        assertThat(structure.isFieldStart(complexEntry, 68, TRANSLATION)).isTrue();
        assertThat(structure.isFieldStart(complexEntry, 67, TRANSLATION)).isFalse();
        assertThat(structure.isFieldStart(complexEntry, 69, TRANSLATION)).isFalse();
    }

    @Test
    public void testGetFieldTypeWord() {
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 0)).isEqualTo(WORD);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 4)).isEqualTo(WORD);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 5)).isEqualTo(WORD);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 30)).isEqualTo(WORD);
        assertThat(structure.getFieldType(complexEntry, -1, -1, 10)).isEqualTo(WORD);
    }

    @Test
    public void testGetFieldTypeReading() {
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 6)).isEqualTo(READING);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 7)).isEqualTo(READING);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 13)).isEqualTo(READING);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 14)).isEqualTo(READING);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 15)).isEqualTo(READING);
        assertThat(structure.getFieldType(complexEntry, -1, -1, 37)).isEqualTo(READING);
    }

    @Test
    public void testGetFieldTypeTranslation() {
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 16)).isEqualTo(TRANSLATION);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 17)).isEqualTo(TRANSLATION);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 27)).isEqualTo(TRANSLATION);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 28)).isEqualTo(TRANSLATION);
        assertThat(structure.getFieldType(simpleEntry, -1, -1, 29)).isEqualTo(TRANSLATION);
        assertThat(structure.getFieldType(complexEntry, -1, -1, 63)).isEqualTo(TRANSLATION);
    }

    @Test
    public void testMoveToNextFieldFirstField() {
        assertThat(structure.moveToNextField(simpleEntry, 0, null)).isEqualTo(WORD);
        assertThat(simpleEntry.position()).isEqualTo(0);
    }

    @Test
    public void testMoveToNextFieldWordToWord() {
        complexEntry.position(1);
        assertThat(structure.moveToNextField(complexEntry, 'K', WORD)).isEqualTo(WORD);
        assertThat(complexEntry.position()).isEqualTo(1);
    }

    @Test
    public void testMoveToNextFieldWordToWordAtEnd() {
        complexEntry.position(25);
        assertThat(structure.moveToNextField(complexEntry, '3', WORD)).isEqualTo(WORD);
        assertThat(complexEntry.position()).isEqualTo(25);
    }

    @Test
    public void testMoveToNextFieldWordToReading() {
        complexEntry.position(27);
        assertThat(structure.moveToNextField(complexEntry, ' ', WORD)).isEqualTo(READING);
        assertThat(complexEntry.position()).isEqualTo(28);
    }

    @Test
    public void testMoveToNextFieldWordToTranslation() {
        simpleEntryWithoutReading.position(6);
        assertThat(structure.moveToNextField(simpleEntryWithoutReading, ' ', WORD)).isEqualTo(TRANSLATION);
        assertThat(simpleEntryWithoutReading.position()).isEqualTo(7);
    }

    @Test
    public void testMoveToNextFieldReadingToReadingAtEnd() {
        complexEntry.position(47);
        assertThat(structure.moveToNextField(complexEntry, '2', READING)).isEqualTo(READING);
        assertThat(complexEntry.position()).isEqualTo(47);
    }

    @Test
    public void testMoveToNextFieldReadingToTranslation() {
        complexEntry.position(48);
        assertThat(structure.moveToNextField(complexEntry, ']', READING)).isEqualTo(TRANSLATION);
        assertThat(complexEntry.position()).isEqualTo(50);
    }

    @Test
    public void testMoveToNextFieldTranslationToTranslation() {
        complexEntry.position(64);
        assertThat(structure.moveToNextField(complexEntry, '/', TRANSLATION)).isEqualTo(TRANSLATION);
        assertThat(complexEntry.position()).isEqualTo(64);
    }

    @Test
    public void testMoveToNextFieldTranslationToWord() {
        simpleEntry.position(29);
        assertThat(structure.moveToNextField(simpleEntry, '/', TRANSLATION)).isEqualTo(WORD);
        assertThat(simpleEntry.position()).isEqualTo(30);
    }
}
