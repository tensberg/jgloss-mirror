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

import java.util.Iterator;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.SearchMode;
import jgloss.dictionary.SearchParameter;
import jgloss.dictionary.StandardSearchParameter;

/**
 * Perform a dictionary lookup. The lookup configuration is taken from a {@link LookupModel LookupModel}.
 * The lookup results are forwarded to a {@link LookupResultHandler LookupResultHandler}.
 *
 * @author Michael Koch
 */
public class LookupEngine {
    protected LookupResultHandler handler;
    protected int dictionaryEntryLimit;

    public LookupEngine( LookupResultHandler _handler) {
        this( _handler, Integer.MAX_VALUE);
    }

    public LookupEngine( LookupResultHandler _handler, int _dictionaryEntryLimit) {
        handler = _handler;
        dictionaryEntryLimit = _dictionaryEntryLimit;
    }

    public LookupResultHandler getHandler() { return handler; }
    public void setHandler( LookupResultHandler _handler) { handler = _handler; }

    public void doLookup( LookupModel model) throws InterruptedException {
        handler.startLookup( model);

        SearchMode mode = model.getSelectedSearchMode();
        Object[] parameters = new Object[mode.getParameters().size()];
        for ( int i=0; i<parameters.length; i++) {
            SearchParameter param = mode.getParameters().get( i);
            if (param == StandardSearchParameter.EXPRESSION) {
	            parameters[i] = model.getSearchExpression();
            } else if (param == StandardSearchParameter.SEARCH_FIELDS) {
	            parameters[i] = model.getSearchFields();
            } else if (param == StandardSearchParameter.DISTANCE) {
	            parameters[i] = Integer.valueOf( model.getDistance());
            } else {
	            throw new IllegalArgumentException( "Unimplemented search parameter " + param);
            }
        }
        LookupResultFilter[] filters = model.getSelectedFilters()
            .toArray( new LookupResultFilter[0]);

        int dictionaryEntries = 0;

        try {
            for ( Iterator<Dictionary> i=model.getSelectedDictionaries().iterator(); i.hasNext() &&
                      dictionaryEntries<dictionaryEntryLimit; ) {
                Dictionary d = i.next();
                handler.dictionary( d);
                try {
                    Iterator<DictionaryEntry> results = d.search( mode, parameters);
                    results:
                    while (dictionaryEntries<dictionaryEntryLimit &&
                           results.hasNext()) {
	                    try {
                               if (Thread.interrupted()) {
	                            throw new InterruptedException();
                            }
                               
                               DictionaryEntry de = results.next();
                               for ( int f=0; f<filters.length; f++) {
                                   if (!filters[f].accept( de)) {
                                       continue results;
                                   }
                               }
                               dictionaryEntries++;
                               handler.dictionaryEntry( de);
                           } catch (SearchException ex) {
                               handler.exception( ex);
                           }
                    }
                } catch (SearchException ex) {
                    handler.exception( ex);
                    continue;
                }
            }
        } finally {
            // do even if interrupted
            handler.endLookup();
        }
    }
} // class LookupEngine
