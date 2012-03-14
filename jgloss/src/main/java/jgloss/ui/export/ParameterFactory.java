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

package jgloss.ui.export;

import org.w3c.dom.Element;

/**
 * Factory for {@link Parameter Parameter} instances. The instances are chosen and initialized
 * from XML elements, which appear as children of a <code>parameters</code> element in an
 * export configuration XML file.
 */
class ParameterFactory {
    /**
     * Parameter element names. Each of the listed elements can appear as child element
     * of the <code>parameters</code> element of the export configuration.
     */
    static interface Elements {
        String ENCODING = "encoding";
        String STRING = "string";
        String BOOLEAN = "boolean";
        String LIST = "list";
        String DOCNAME = "docname";
        String DATETIME = "datetime";
        String LONGEST_ANNOTATION = "longest-annotation";
    }

    /**
     * Names of attributes specific to parameter elements.
     */
    static interface Attributes {
        String TYPE = "type";
    }

    /**
     * Known values of attributes of parameter elements.
     */
    static interface AttributeValues {
        String WORD = "word";
        String READING = "reading";
        String DICTIONARY_WORD = "dictionary-word";
        String DICTIONARY_READING = "dictionary-reading";
        String TRANSLATION = "translation";
    }

    private ParameterFactory() {}

    /**
     * Create a {@link Parameter Parameter} instance from the parameter configuration stored
     * in a XML element. The element tag name determines the instantiated parameter class.
     *
     * @exception IllegalArgumentException if the element tag name does not equal the name
     *            of a known parameter type.
     */
    public static Parameter createParameter( Element elem) {
        String name = elem.getTagName();
        if (name.equals( Elements.DOCNAME)) {
	        return new DocnameParameter( elem);
        } else if (name.equals( Elements.DATETIME)) {
	        return new DateTimeParameter( elem);
        } else if (name.equals( Elements.ENCODING)) {
	        return new EncodingParameter( elem);
        } else if (name.equals( Elements.STRING)) {
	        return new StringParameter( elem);
        } else if (name.equals( Elements.BOOLEAN)) {
	        return new BooleanParameter( elem);
        } else if (name.equals( Elements.LIST)) {
	        return new ListParameter( elem);
        } else if (name.equals( Elements.LONGEST_ANNOTATION)) {
	        return new LongestAnnotationParameter( elem);
        } else {
	        throw new IllegalArgumentException( elem.getTagName());
        }
    }
} // class ParameterFactory
