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

package jgloss.dictionary;

import java.util.List;
import java.util.Collections;

/**
 * Dictionary implementation which acts as a placeholder for a dictionary name but supports no searches.
 *
 * @author Michael Koch
 */
public class NullDictionary implements Dictionary {
    /**
     * Name of the dictionary.
     */
    private String name;

    /**
     * Creates a new dictionary with the given name.
     */ 
    public NullDictionary( String name) {
        this.name = name;
    }

    /**
     * Returns the name supplied by the constructor.
     */
    public String getName() { return name; }

    /**
     * Always returns the empty list.
     */
    public List search( String expression, short searchmode, short resultmode) {
        return Collections.EMPTY_LIST;
    }

    public void dispose() {}
} // class NullDictionary
