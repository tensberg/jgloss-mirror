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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DictionaryFactoryTest {
    @Mock
    private IndexedDictionary indexedDictionary;

    @Mock
    private Dictionary dictionary;

    @Test
    public void testSynchronizedDictionary() {
        Dictionary synchronizedDictionary = DictionaryFactory.synchronizedDictionary(dictionary);
        assertThat(synchronizedDictionary).isNotNull();
        assertThat(synchronizedDictionary).isNotSameAs(dictionary);
        assertThat(synchronizedDictionary).isInstanceOf(SynchronizedDictionary.class);
    }

    @Test
    public void testSynchronizedDictionaryReturnsIndexedDictionary() {
        Dictionary synchronizedDictionary = DictionaryFactory.synchronizedDictionary(indexedDictionary);
        assertThat(synchronizedDictionary).isNotNull();
        assertThat(synchronizedDictionary).isNotSameAs(indexedDictionary);
        assertThat(synchronizedDictionary).isInstanceOf(SynchronizedIndexedDictionary.class);
    }

    @Test
    public void testSynchronizedIndexedDictionary() {
        IndexedDictionary synchronizedDictionary = DictionaryFactory.synchronizedIndexedDictionary(indexedDictionary);
        assertThat(synchronizedDictionary).isNotNull();
        assertThat(synchronizedDictionary).isNotSameAs(indexedDictionary);
        assertThat(synchronizedDictionary).isInstanceOf(SynchronizedIndexedDictionary.class);
    }
}
