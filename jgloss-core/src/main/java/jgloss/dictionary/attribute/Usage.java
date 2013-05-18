/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss.dictionary.attribute;

import java.util.ResourceBundle;

import jgloss.util.UTF8ResourceBundleControl;

/**
 * Attribute values for usage attribute. The values cannot be instantiated directly.
 * Instead, they are fetched by calling the static {@link #get(String) get} method.
 *
 * @author Michael Koch
 */
public class Usage extends DefaultCategoryAttributeValue {
    private static final IDAttributeValueFactory factory =
        new IDAttributeValueFactory() {
            @Override
			protected CategoryAttributeValue createValue( String id) {
                return new Usage( id);
            }
        };
    
    /**
     * Get the part of speech attribute value with the given id.
     *
     * @exception MissingResourceException if the id is not defined.
     */
    public static Usage get( String id) {
        return (Usage) factory.getValueFor( id);
    }

    protected final static ResourceBundle names = ResourceBundle.getBundle
        ( "attributes", new UTF8ResourceBundleControl());

    private Usage( String id) {
        super( id);
    }

    @Override
	protected ResourceBundle getNames() { return names; }
    @Override
	protected String getResourcePrefix() { return "usage."; }
} // class Usage
