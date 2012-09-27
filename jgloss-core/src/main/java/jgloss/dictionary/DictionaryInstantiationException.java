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

package jgloss.dictionary;

/**
 * Thrown when the instantiation of a dictionary failed.
 */
public class DictionaryInstantiationException extends DictionaryFactoryException {
    private static final long serialVersionUID = 1L;

    public DictionaryInstantiationException() {
        super();
    }
    public DictionaryInstantiationException( String message) {
        super( message);
    }
    public DictionaryInstantiationException( java.lang.Exception rootCause) {
        super( rootCause);
    }
    public DictionaryInstantiationException( String message, java.lang.Exception rootCause) {
        super( message, rootCause);
    }
} // class InstantiationException