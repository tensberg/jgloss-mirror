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

/**
 * thread-safe dictionary wrapper which synchonizes each call on an internal
 * mutex.
 *
 * @see DictionaryFactory#synchronizedIndexedDictionary(IndexedDictionary)
 * @author Michael Koch <tensberg@gmx.net>
 */
class SynchronizedIndexedDictionary extends SynchronizedDictionary implements IndexedDictionary {

    SynchronizedIndexedDictionary(IndexedDictionary dictionary) {
        super(dictionary);
    }

    @Override
    public IndexedDictionary getWrappedDictionary() {
        return (IndexedDictionary) dictionary;
    }

    @Override
    public boolean loadIndex() throws IndexException {
        synchronized (mutex) {
            return getWrappedDictionary().loadIndex();
        }
    }

    @Override
    public void buildIndex() throws IndexException {
        synchronized (mutex) {
            getWrappedDictionary().buildIndex();
        }
    }
}
