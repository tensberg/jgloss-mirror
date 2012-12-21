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
 */

package jgloss.ui.download;

import static jgloss.ui.download.DictionarySchemaUtils.getDescriptionForLocale;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jgloss.ui.download.schema.Dictionary.Description;

import org.junit.Test;

public class DictionarySchemaUtilsTest {
    @Test
    public void testGetDescriptionForLocaleEmptyList() {
        assertEquals("", getDescriptionForLocale(Collections.<Description> emptyList(), Locale.GERMAN));
    }
    
    @Test
    public void testGetDescriptionForLocaleReturnsDescriptionMatchingLocale() {
        List<Description> descriptions = Arrays.asList(new Description[] { 
                        createDescription("xx", "foo"),
                        createDescription(Locale.GERMAN.getLanguage(), "bar")
        });
        
        assertEquals("bar", getDescriptionForLocale(descriptions, Locale.GERMAN));
    }
    
    @Test
    public void testGetDescriptionForLocaleReturnsFirstDescriptionIfNoMatchingLocale() {
        List<Description> descriptions = Arrays.asList(new Description[] { 
                        createDescription("xx", "foo"),
                        createDescription(Locale.GERMAN.getLanguage(), "bar")
        });
        
        assertEquals("foo", getDescriptionForLocale(descriptions, Locale.ENGLISH));
    }

    private Description createDescription(String lang, String value) {
        Description description = new Description();
        description.setLang(lang);
        description.setValue(value);
        return description;
    }
}
