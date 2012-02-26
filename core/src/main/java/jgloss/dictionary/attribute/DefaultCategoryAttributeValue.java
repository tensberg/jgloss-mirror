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

package jgloss.dictionary.attribute;

import java.util.ResourceBundle;

/**
 * Default implementation of a category attribute value. An ID string is used to identify resource
 * keys which store the short and long names of the attribute value. Resource keys are of the form
 * <code>getResourcePrefix() + id + ".s"</code> for the short and
 * <code>getResourcePrefix() + id + ".l"</code> for the long name.
 *
 * @author Michael Koch
 */
abstract class DefaultCategoryAttributeValue implements CategoryAttributeValue {
    protected final static String SHORT_NAME_SUFFIX = ".s";
    protected final static String LONG_NAME_SUFFIX = ".l";

    protected String id;
    protected String shortName;
    protected String longName;

    protected DefaultCategoryAttributeValue( String _id) {
        this.id = _id;
        setNames();
    }

    /**
     * Load the short and long name from the resources. This method is called from the constructor,
     * and may be called afterwards to reload the values if the resource bundle should change.
     */
    protected void setNames() {
        this.shortName = getNames().getString( getResourcePrefix() + id + SHORT_NAME_SUFFIX);
        this.longName = getNames().getString( getResourcePrefix() + id + LONG_NAME_SUFFIX);
    }
    
    public String getId() { return id; }
    @Override
	public String getShortName() { return shortName; }
    @Override
	public String getLongName() { return longName; }

    @Override
	public String toString() { return shortName; }

    /**
     * Return the resource bundle from which the attribute value names will be fetched.
     */
    protected abstract ResourceBundle getNames();

    /**
     * Return a string which will be used as prefix to the resource key.
     */
    protected abstract String getResourcePrefix();

    @Override
	public boolean equals( Object o) {
        return (o!=null && o instanceof DefaultCategoryAttributeValue &&
                ((DefaultCategoryAttributeValue) o).id.equals( id));
    }
} // class DefaultCategoryAttributeValue
