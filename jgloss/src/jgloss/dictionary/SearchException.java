/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

/**
 * SearchException signals a fatal error during a dictionary lookup.
 *
 * @author Michael Koch
 */
public class SearchException extends Exception {
    /**
     * Constructs a SearchException without a detail message.
     */
    public SearchException() {}

    /**
     * Constructs a SearchException with the given detail message.
     *
     * @param message Message describing the reason for this exception.
     */
    public SearchException( String message) {
        super( message);
    }
} // class SearchException
