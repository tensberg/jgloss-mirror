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
 *
 */

package jgloss.ui;

import java.util.ArrayList;
import java.util.List;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Lookup result handler which will forward the results to one or more other lookup result handler
 * instances. The class is not threadsafe with respect to adding and removing handlers while
 * a dictionary lookup is in progress.
 *
 * @author Michael Koch
 */
public class LookupResultProxy implements LookupResultHandler, Cloneable {
    /**
     * List of {@link LookupResultHandler LookupResultHandlers} to which the events will be
     * forwarded.
     */
    protected List<LookupResultHandler> handlers;
    
    /**
     * Initialize the proxy with an empty list of handlers. Handlers can be added by calling
     * {@link #addHandler(LookupResultHandler) addHandler}.
     */
    public LookupResultProxy() {
        handlers = new ArrayList<LookupResultHandler>( 2);
    }

    /**
     * Initialize the proxy, setting a single result handler to which actions are forwarded.
     */
    public LookupResultProxy(LookupResultHandler _forwardTo) {
        this();
        addHandler(_forwardTo);
    }

    /**
     * Initialize the proxy with a list of lookup result handlers.
     */
    protected LookupResultProxy(List<LookupResultHandler> _handlers) {
        handlers = new ArrayList<LookupResultHandler>(_handlers);
    }

    public void addHandler(LookupResultHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(LookupResultHandler handler) {
        handlers.remove(handler);
    }

    public void clearHandlers() {
        handlers.clear();
    }

    @Override
	public void startLookup( String description) {
        for (LookupResultHandler handler : handlers) {
	        handler.startLookup( description);
        }
    }

    @Override
	public void startLookup( LookupModel model) {
        for (LookupResultHandler handler : handlers) {
	        handler.startLookup( model);
        }
    }

    @Override
	public void dictionary( Dictionary dictionary) {
        for (LookupResultHandler handler : handlers) {
	        handler.dictionary( dictionary);
        }
    }

    @Override
	public void dictionaryEntry( DictionaryEntry entry) {
        for (LookupResultHandler handler : handlers) {
	        handler.dictionaryEntry( entry);
        }
    }

    @Override
	public void exception( SearchException ex) {
        for (LookupResultHandler handler : handlers) {
	        handler.exception( ex);
        }
    }

    @Override
	public void note( String note) {
        for (LookupResultHandler handler : handlers) {
	        handler.note( note);
        }
    }

    @Override
	public void endLookup() {
        for (LookupResultHandler handler : handlers) {
	        handler.endLookup();
        }
    }

    @Override
	public LookupResultProxy clone() {
        try {
            LookupResultProxy out = (LookupResultProxy) super.clone();
            out.handlers = new ArrayList<LookupResultHandler>(out.handlers);
            return out;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
} // class LookupResultProxy
