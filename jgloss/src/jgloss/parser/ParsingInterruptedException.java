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

package jgloss.parser;

import jgloss.dictionary.SearchException;

/**
 * Thrown if the parsing thread is interrupted by calling its <CODE>interrupt</CODE>
 * method.
 *
 * @author Michael Koch
 * @see java.lang.Thread#interrupt()
 */
public class ParsingInterruptedException extends SearchException {
    /**
     * Creates a new exception without a description.
     */
    public ParsingInterruptedException() {
        super();
    }
    
    /**
     * Creates a new exception with the given message.
     *
     * @param message A description of this exception.
     */
    public ParsingInterruptedException( String message) {
        super( message);
    }
} // class ParsingInterruptedException
