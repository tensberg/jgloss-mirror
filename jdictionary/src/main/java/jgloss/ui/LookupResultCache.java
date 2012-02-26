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

package jgloss.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.DictionaryEntryReference;
import jgloss.dictionary.ResultIterator;
import jgloss.dictionary.SearchException;

/**
 * Lookup result handler which will store the lookup result in a list.
 * The cached search can be replayed an arbitrary number of times on other result handlers.
 *
 * @author Michael Koch
 */
public class LookupResultCache extends LookupResultProxy implements Cloneable {
    protected Collection cache;
    
    public LookupResultCache() {
        cache = new ArrayList( 100);
    }

    public LookupResultCache( LookupResultHandler _forwardTo) {
        this();
        addHandler(_forwardTo);
    }

    public LookupResultCache( String description, ResultIterator dictionaryEntries) 
        throws SearchException {
        this();
        setData( description, dictionaryEntries);
    }

    public void setData( String description, ResultIterator dictionaryEntries)
        throws SearchException {
        cache.clear();
        cache.add( description);
        while (dictionaryEntries.hasNext()) try {
            cache.add( dictionaryEntries.next().getReference());
        } catch (SearchException ex) {
            cache.add( ex);
        }        
    }

    public void clear() { cache.clear(); }

    public boolean isEmpty() { return cache.isEmpty(); }

    public int size() { return cache.size(); }

    /**
     * Replays the recorded search result events on all registered handlers.
     */
    public void replay() {
        if (cache.size() == 0)
            throw new IllegalStateException( "cache is empty");

        // to prevent the events from being re-recorded, all events are forwarded directly
        // to the proxy superclass
        Iterator i = cache.iterator();
        Object o = i.next();
        if (o instanceof String)
            super.startLookup( (String) o);
        else
            super.startLookup( (LookupModel) o);

        while (i.hasNext()) {
            o = i.next();

            if (o instanceof Dictionary)
                super.dictionary( (Dictionary) o);
            else if (o instanceof DictionaryEntry)
                super.dictionaryEntry( (DictionaryEntry) o);
            else if (o instanceof DictionaryEntryReference) try {
                super.dictionaryEntry( ((DictionaryEntryReference) o).getEntry());
            } catch (SearchException ex) {
                super.exception( ex);
            }
            else if (o instanceof SearchException)
                super.exception( (SearchException) o);
            else
                super.note( o.toString());
        }

        super.endLookup();
    }

    /**
     * Temporarily adds <code>handler</code> to the list of registered handlers and playbacks
     * the stored lookup events. <code>handler</code> will be removed from the list of handlers
     * at the end of the method.
     */
    public void replay(LookupResultHandler handler) {
        addHandler(handler);
        replay();
        removeHandler(handler);
    }

    @Override
	public void startLookup( String description) {
        cache.clear();
        cache.add( description);
        super.startLookup( description);
    }

    @Override
	public void startLookup( LookupModel model) {
        cache.clear();
        cache.add( model);
        super.startLookup(model);
    }

    @Override
	public void dictionary( Dictionary dictionary) {
        cache.add( dictionary);
        super.dictionary(dictionary);
    }

    @Override
	public void dictionaryEntry( DictionaryEntry entry) {
        cache.add( entry.getReference());
        super.dictionaryEntry(entry);
    }

    @Override
	public void exception( SearchException ex) {
        cache.add( ex);
        super.exception(ex);
    }

    @Override
	public void note( String note) {
        cache.add( note);
        super.note(note);
    }

    @Override
	public void endLookup() {
        super.endLookup();
    }

    @Override
	public Object clone() {
        try {
            LookupResultCache out = (LookupResultCache) super.clone();
            out.cache = new ArrayList( out.cache);
            return out;
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }
} // class LookupResultCache
