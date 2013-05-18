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

package jgloss.dictionary;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import jgloss.util.UTF8ResourceBundleControl;

/**
 * Thrown when dictionary entry is of a format unparseable by the dictionary
 * implementation. If this exception is throws while iterating over a dictionary
 * search result, it usually indicates a failure of just this iteration step, and
 * that more dictionary entries may be available. The exception should only be thrown
 * if the entry format is completely unrecognizable; if most of the data from the entry
 * can be read it is more appropriate to print a warning on the standard error stream
 * and create the dictionary entry object.
 *
 * @author Michael Koch
 */
public class MalformedEntryException extends SearchException {
    private static final long serialVersionUID = 1L;

    protected final Dictionary dictionary;
    protected final String dictionaryEntry;

    public MalformedEntryException( Dictionary _dictionary, String _dictionaryEntry) {
        super( MessageFormat.format( ResourceBundle.getBundle( "messages-dictionary", new UTF8ResourceBundleControl())
                                     .getString( "exception.malformedentry.message"), 
                                     new Object[] { _dictionary.getName(), _dictionaryEntry }));
        dictionary = _dictionary;
        dictionaryEntry = _dictionaryEntry;
    }

    public MalformedEntryException( Dictionary _dictionary, String _dictionaryEntry,
                                    Throwable _rootCause) {
        super( MessageFormat.format( ResourceBundle.getBundle( "messages-dictionary", new UTF8ResourceBundleControl())
                                     .getString( "exception.malformedentry.message"), 
                                     new Object[] { _dictionary.getName(), _dictionaryEntry }),
               _rootCause);
        dictionary = _dictionary;
        dictionaryEntry = _dictionaryEntry;
    }
} // class MalformedEntryException
