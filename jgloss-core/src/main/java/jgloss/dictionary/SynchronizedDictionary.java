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
 * $Id$
 *
 */

package jgloss.dictionary;

import java.util.Iterator;
import java.util.Set;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeValue;

/**
 * thread-safe dictionary wrapper which synchonizes each call on an internal
 * mutex.
 *
 * @see DictionaryFactory#synchronizedDictionary(Dictionary)
 * @author Michael Koch <tensberg@gmx.net>
 */
class SynchronizedDictionary implements Dictionary, DictionaryWrapper {
    protected final Dictionary dictionary;

    protected final Object mutex = new Object();

    SynchronizedDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public String getName() {
        synchronized (mutex) {
            return dictionary.getName();
        }
    }

    @Override
    public Iterator<DictionaryEntry> search(SearchMode searchmode, Object[] parameters) throws SearchException {
        synchronized (mutex) {
            return dictionary.search(searchmode, parameters);
        }
    }

    @Override
    public boolean supports(SearchMode searchmode, boolean fully) {
        synchronized (mutex) {
            return dictionary.supports(searchmode, fully);
        }
    }

    @Override
    public Set<Attribute<?>> getSupportedAttributes() {
        synchronized (mutex) {
            return dictionary.getSupportedAttributes();
        }
    }

    @Override
    public <T extends AttributeValue> Set<T> getAttributeValues(Attribute<T> att) {
        synchronized (mutex) {
            return dictionary.getAttributeValues(att);
        }
    }

    @Override
    public SearchFieldSelection getSupportedFields(SearchMode searchmode) {
        synchronized (mutex) {
            return dictionary.getSupportedFields(searchmode);
        }
    }

    @Override
    public void dispose() {
        synchronized (mutex) {
            dictionary.dispose();
        }
    }
    
    @Override
    public String toString() {
    	synchronized (mutex) {
    		return dictionary.toString();
    	}
    }

    @Override
    public Dictionary getWrappedDictionary() {
	    return dictionary;
    }
}
