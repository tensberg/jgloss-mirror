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

import static jgloss.dictionary.DictionaryEntryField.TRANSLATION;
import static jgloss.dictionary.DictionaryEntryField.WORD;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.IndexBuilder;
import jgloss.dictionary.IndexContainer;
import jgloss.dictionary.Indexable;
import jgloss.dictionary.UTF8CharacterHandler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileBasedDictionaryIndexerTest {
    // x is not indexed because it is too short. It is required for field
    // recognition to work, though.

    private static final String WORD_WITH_PRIORITY_MARKER = "子供(P) [x] /x/";

    private static final String WORD_WITH_KANJI_AND_KANA = "子ども [x] /x/";
    private static final String READING = "x [こども] x";

    private static final String TRANSLATIONS = "x [x] /(n) child/children/";

    @Mock
    private Indexable indexable;

    @Mock
    private IndexContainer indexContainer;

    @Mock
    private IndexBuilder builder;

    @Test
    public void testBuildIndexWordWithPriorityMarker() throws UnsupportedEncodingException {
        buildIndex(WORD_WITH_PRIORITY_MARKER);

        InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder).startBuildIndex(indexContainer, indexable);
        // offsets are in bytes, not in chars
        inOrder.verify(builder).addEntry(0, 6, WORD);
        inOrder.verify(builder).addEntry(3, 3, WORD);
        inOrder.verify(builder).endBuildIndex(true);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBuildIndexWordWithKanjiAndKana() throws UnsupportedEncodingException {
        buildIndex(WORD_WITH_KANJI_AND_KANA);

        InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder).startBuildIndex(indexContainer, indexable);
        // offsets are in bytes, not in chars
        inOrder.verify(builder).addEntry(0, 9, WORD);
        inOrder.verify(builder).addEntry(3, 6, WORD);
        inOrder.verify(builder).endBuildIndex(true);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBuildIndexReading() throws UnsupportedEncodingException {
        buildIndex(READING);

        InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder).startBuildIndex(indexContainer, indexable);
        inOrder.verify(builder).addEntry(3, 9, DictionaryEntryField.READING);
        inOrder.verify(builder).endBuildIndex(true);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBuildIndexTranslations() throws UnsupportedEncodingException {
        buildIndex(TRANSLATIONS);

        InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder).startBuildIndex(indexContainer, indexable);
        inOrder.verify(builder).addEntry(11, 5, TRANSLATION);
        inOrder.verify(builder).addEntry(17, 8, TRANSLATION);
        inOrder.verify(builder).endBuildIndex(true);
        inOrder.verifyNoMoreInteractions();
    }

    private void buildIndex(String dictionaryText) throws UnsupportedEncodingException {
        when(builder.addEntry(anyInt(), anyInt(), any(DictionaryEntryField.class))).thenReturn(true);

        ByteBuffer dictionary = ByteBuffer.wrap(dictionaryText.getBytes("UTF-8"));
        FileBasedDictionaryIndexer indexer = new FileBasedDictionaryIndexer(indexable, new EDictStructure(),
                        dictionary, new UTF8CharacterHandler());
        indexer.buildIndex(indexContainer, builder);
    }
}
