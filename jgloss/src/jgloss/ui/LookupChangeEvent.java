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

import java.util.EventObject;

class LookupChangeEvent extends EventObject {
    public static final int SEARCH_MODE_SELECTION = 0x01;
    public static final int SEARCH_MODE_AVAILABILITY = 0x02;
    public static final int DICTIONARY_SELECTION = 0x04;
    public static final int MULTI_DICTIONARY_MODE = 0x08;
    public static final int DICTIONARY_AVAILABILITY = 0x10;
    public static final int FILTER_SELECTION = 0x20;
    public static final int FILTER_AVAILABILITY = 0x40;
    public static final int SEARCH_FIELDS_SELECTION = 0x80;
    public static final int SEARCH_FIELDS_AVAILABILITY = 0x100;
    public static final int SEARCH_PARAMETERS = 0x200;
    public static final int SEARCH_PARAMETERS_AVAILABILITY = 0x400;
    public static final int DICTIONARY_ADDED = 0x800;
    public static final int DICTIONARY_REMOVED = 0x1000;
    public static final int DICTIONARY_LIST_CHANGED = 0x2000;

    private int changes;

    public LookupChangeEvent( Object _source, int _changes) {
        super( _source);
        changes = _changes;
    }

    public boolean hasChanged( int what) {
        return (changes & what) != 0;
    }
} // class LookupChangeEvent
