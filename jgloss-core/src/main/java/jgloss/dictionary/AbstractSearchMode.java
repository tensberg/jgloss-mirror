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
 * $Id$
 *
 */

package jgloss.dictionary;

import java.util.ResourceBundle;

import jgloss.util.UTF8ResourceBundleControl;

public abstract class AbstractSearchMode implements SearchMode {
    protected static final ResourceBundle MESSAGES = 
        ResourceBundle.getBundle( "messages-dictionary", new UTF8ResourceBundleControl());
    protected static final String RESOURCE_PREFIX = "searchmode.";

    private final String name;
    private final String description;
    
    protected AbstractSearchMode( String id) {
        name = MESSAGES.getString( RESOURCE_PREFIX + id + ".name");
        description = MESSAGES.getString( RESOURCE_PREFIX + id + ".desc");
    }

    @Override
	public String getName() { return name; }
    @Override
	public String getDescription() { return description; }
    @Override
	public String toString() { return getName(); }
} // class AbstractSearchMode
