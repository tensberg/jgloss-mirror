/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.dictionary.*;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Lookup result handler which will store the lookup result in a list.
 * The cached search can be replayed an arbitrary number of times on other result handlers.
 *
 * @author Michael Koch
 */
public class LookupResultCache implements LookupResultHandler, Cloneable {
    protected Collection cache;
    private LookupResultHandler forwardTo;
    
    public LookupResultCache() {
        cache = new ArrayList( 100);
    }

    public LookupResultCache( LookupResultHandler _forwardTo) {
        this();
        forwardTo = _forwardTo;
    }

    public LookupResultCache( String description, ResultIterator dictionaryEntries) 
        throws SearchException {
        this();
        setData( description, dictionaryEntries);
    }

    public void setForwardTo( LookupResultHandler _forwardTo) {
        forwardTo = _forwardTo;
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

    public void replay() {
        replay( forwardTo);
    }

    public void replay( LookupResultHandler handler) {
        if (cache.size() == 0)
            throw new IllegalStateException( "cache is empty");

        Iterator i = cache.iterator();
        Object o = i.next();
        if (o instanceof String)
            handler.startLookup( (String) o);
        else
            handler.startLookup( (LookupModel) o);

        while (i.hasNext()) {
            o = i.next();

            if (o instanceof Dictionary)
                handler.dictionary( (Dictionary) o);
            else if (o instanceof DictionaryEntry)
                handler.dictionaryEntry( (DictionaryEntry) o);
            else if (o instanceof DictionaryEntryReference) try {
                handler.dictionaryEntry( ((DictionaryEntryReference) o).getEntry());
            } catch (SearchException ex) {
                handler.exception( ex);
            }
            else if (o instanceof SearchException)
                handler.exception( (SearchException) o);
            else
                handler.note( o.toString());
        }

        handler.endLookup();
    }

    public void startLookup( String description) {
        cache.clear();
        cache.add( description);
        if (forwardTo != null)
            forwardTo.startLookup( description);
    }

    public void startLookup( LookupModel model) {
        cache.clear();
        cache.add( model);
        if (forwardTo != null)
            forwardTo.startLookup( model);
    }

    public void dictionary( Dictionary dictionary) {
        cache.add( dictionary);
        if (forwardTo != null)
            forwardTo.dictionary( dictionary);
    }

    public void dictionaryEntry( DictionaryEntry entry) {
        cache.add( entry.getReference());
        if (forwardTo != null)
            forwardTo.dictionaryEntry( entry);
    }

    public void exception( SearchException ex) {
        cache.add( ex);
        if (forwardTo != null)
            forwardTo.exception( ex);
    }

    public void note( String note) {
        cache.add( note);
        if (forwardTo != null)
            forwardTo.note( note);
    }

    public void endLookup() {
        if (forwardTo != null)
            forwardTo.endLookup();
    }

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
