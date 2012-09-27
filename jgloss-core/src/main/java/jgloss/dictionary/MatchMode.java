/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

/**
 * Typesafe enumeration of match modes.
 *
 * @author Michael Koch
 */
public class MatchMode {
    private final String name;

    /**
     * Match against the whole entry field.
     */
    public static final MatchMode FIELD = new MatchMode( "FIELD");
    /**
     * Match against words in the entry field.
     */
    public static final MatchMode WORD = new MatchMode( "WORD");

    protected MatchMode( String _name) {
        this.name = _name;
    }

    @Override
	public String toString() { return name; }
} // class MatchMode
