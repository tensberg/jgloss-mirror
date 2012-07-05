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

package jgloss.dictionary;

/**
 * Exception signalling an error which occurred while accessing the index.
 *
 * @author Michael Koch
 */
public class IndexException extends SearchException {
    private static final long serialVersionUID = 1L;

	public IndexException() {}

    public IndexException( String message) {
        super( message);
    }

    public IndexException( String message, Throwable rootCause) {
        super( message, rootCause);
    }

    public IndexException( Throwable rootCause) {
        super( rootCause);
    }
} // class IndexException
