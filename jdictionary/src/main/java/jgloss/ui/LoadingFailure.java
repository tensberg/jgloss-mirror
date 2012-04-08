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
 * $Id$
 *
 */

package jgloss.ui;

/**
 * Stores a dictionary descriptor and the cause why loading the dictionary failed.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class LoadingFailure {
    private final String descriptor;
    
    private final Throwable cause;

    LoadingFailure(String descriptor, Throwable cause) {
        this.descriptor = descriptor;
        this.cause = cause;
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public Throwable getCause() {
        return cause;
    }
}