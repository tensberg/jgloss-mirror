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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import jgloss.dictionary.attribute.Attribute;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SynchronizedDictionaryTest {

    @Mock
    private Dictionary dictionary;

    private SynchronizedDictionary synchronizedDictionary;

    @Before
    public void createSynchronizedDictionary() {
        synchronizedDictionary = new SynchronizedDictionary(dictionary);
    }

    @Test
    public void testGetName() {
        when(dictionary.getName()).thenReturn("foo");
        assertThat(synchronizedDictionary.getName()).isEqualTo("foo");
    }

    @Test
    public void testSearch() {
        SearchMode searchmode = mock(SearchMode.class);
        Object[] parameters = new Object[0];
        @SuppressWarnings("unchecked")
        Iterator<DictionaryEntry> iterator = mock(Iterator.class);

        when(dictionary.search(searchmode, parameters)).thenReturn(iterator);

        Iterator<DictionaryEntry> search = synchronizedDictionary.search(searchmode, parameters);
        assertThat((Object) search).isSameAs(iterator); // TODO: investigate why test fails without object cast
    }

    @Test
    public void testSupports() {
        SearchMode searchmode = mock(SearchMode.class);
        when(dictionary.supports(searchmode, true)).thenReturn(true);

        assertThat(synchronizedDictionary.supports(searchmode, true)).isTrue();
    }

    @Test
    public void testGetSupportedAttributes() {
        Set<Attribute<?>> set = Collections.emptySet();
        when(dictionary.getSupportedAttributes()).thenReturn(set);

        assertThat(synchronizedDictionary.getSupportedAttributes()).isSameAs(set);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testGetAttributeValues() {
        Attribute<?> attribute = mock(Attribute.class);
        Set<Object> set = Collections.emptySet();
        when(dictionary.getAttributeValues(attribute)).thenReturn((Set) set);

        assertThat(synchronizedDictionary.getAttributeValues(attribute)).isSameAs(set);
    }

    @Test
    public void testGetSupportedFields() {
        SearchMode searchmode = mock(SearchMode.class);
        SearchFieldSelection fields = mock(SearchFieldSelection.class);
        when(dictionary.getSupportedFields(searchmode)).thenReturn(fields);

        assertThat(synchronizedDictionary.getSupportedFields(searchmode)).isSameAs(fields);
    }

    @Test
    public void testDispose() {
        synchronizedDictionary.dispose();
        verify(dictionary).dispose();
    }

}
