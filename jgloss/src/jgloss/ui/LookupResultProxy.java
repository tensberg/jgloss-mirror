/*
 * Copyright (C) 2002,2003 Michael Koch (tensberg@gmx.net)
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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

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
    protected List handlers;
    
    /**
     * Initialize the proxy with an empty list of handlers. Handlers can be added by calling
     * {@link #addHandler(LookupResultHandler) addHandler}.
     */
    public LookupResultProxy() {
        handlers = new ArrayList( 2);
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
    protected LookupResultProxy(List _handlers) {
        handlers = new ArrayList(_handlers);
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

    public void startLookup( String description) {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).startLookup( description);
    }

    public void startLookup( LookupModel model) {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).startLookup( model);
    }

    public void dictionary( Dictionary dictionary) {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).dictionary( dictionary);
    }

    public void dictionaryEntry( DictionaryEntry entry) {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).dictionaryEntry( entry);
    }

    public void exception( SearchException ex) {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).exception( ex);
    }

    public void note( String note) {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).note( note);
    }

    public void endLookup() {
        for (Iterator i=handlers.iterator(); i.hasNext(); )
            ((LookupResultHandler) i.next()).endLookup();
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            LookupResultProxy out = (LookupResultProxy) super.clone();
            out.handlers = new ArrayList(out.handlers);
            return out;
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }
} // class LookupResultProxy
