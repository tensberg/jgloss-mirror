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

package jgloss.ui;

import java.awt.EventQueue;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Invoke the lookup result handler call on a delegate handler
 * in the event dispatch thread.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class EdtLookupResultHandler implements LookupResultHandler {

    private final LookupResultHandler delegate;

    EdtLookupResultHandler(LookupResultHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void startLookup(final String description) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.startLookup(description);
            }
        });
    }

    @Override
    public void startLookup(final LookupModel model) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.startLookup(model);
            }
        });
    }

    @Override
    public void dictionary(final Dictionary d) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.dictionary(d);
            }
        });
    }

    @Override
    public void dictionaryEntry(final DictionaryEntry de) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.dictionaryEntry(de);
            }
        });
    }

    @Override
    public void exception(final SearchException ex) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.exception(ex);
            }
        });
    }

    @Override
    public void note(final String note) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.note(note);
            }
        });
    }

    @Override
    public void endLookup() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegate.endLookup();
            }
        });
    }

}