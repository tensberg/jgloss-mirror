/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SynchronizedIndexedDictionaryTest {
    @Mock
    private IndexedDictionary dictionary;

    private SynchronizedIndexedDictionary synchronizedDictionary;

    @Before
    public void createSynchronizedDictionary() {
        synchronizedDictionary = new SynchronizedIndexedDictionary(dictionary);
    }

    @Test
    public void testLoadIndex() {
        when(dictionary.loadIndex()).thenReturn(true);

        assertThat(synchronizedDictionary.loadIndex()).isTrue();
    }

    @Test
    public void testBuildIndex() {
        synchronizedDictionary.buildIndex();
        verify(dictionary).buildIndex();
    }
}
