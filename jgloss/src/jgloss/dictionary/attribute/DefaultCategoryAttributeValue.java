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

package jgloss.dictionary.attribute;

import java.util.ResourceBundle;

abstract class DefaultCategoryAttributeValue implements CategoryAttributeValue {
    protected final static String SHORT_NAME_SUFFIX = ".short";
    protected final static String LONG_NAME_SUFFIX = ".long";

    protected String id;
    protected String shortName;
    protected String longName;

    protected DefaultCategoryAttributeValue( String _id) {
        this.id = _id;
        setNames();
    }

    protected void setNames() {
        this.shortName = getNames().getString( id + SHORT_NAME_SUFFIX);
        this.longName = getNames().getString( id + LONG_NAME_SUFFIX);
    }
    
    public String getId() { return id; }
    public String toString() { return '(' + id + ')'; }

    public String getShortName() { return shortName; }
    public String getLongName() { return longName; }

    protected abstract ResourceBundle getNames();
} // class DefaultCategoryAttributeValue
