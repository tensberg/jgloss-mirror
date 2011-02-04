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

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Process dictionary lookup results. Instances of this interface handle the result of a
 * dictionary search, typically performed by a {@link LookupEngine LookupEngine}.
 *
 * @author Michael Koch
 */
public interface LookupResultHandler {
    void startLookup( String description);

    void startLookup( LookupModel model);

    void dictionary( Dictionary d);

    void dictionaryEntry( DictionaryEntry de);

    void exception( SearchException ex);

    void note( String note);

    void endLookup();
} // interface LookupResultHandler
