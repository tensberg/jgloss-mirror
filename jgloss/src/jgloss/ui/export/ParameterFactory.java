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

package jgloss.ui.export;

import org.w3c.dom.Element;

class ParameterFactory {
    public static interface Elements {
        String ENCODING = "encoding";
        String STRING = "string";
        String BOOLEAN = "boolean";
        String LIST = "list";
        String DOCNAME = "docname";
        String DATETIME = "datetime";
    }

    private ParameterFactory() {}

    public static Parameter createParameter( Element elem) {
        String name = elem.getTagName();
        if (name.equals( Elements.DOCNAME))
            return new DocnameParameter( elem);
        else if (name.equals( Elements.DATETIME))
            return new DateTimeParameter( elem);
        else if (name.equals( Elements.ENCODING))
            return new EncodingParameter( elem);
        else if (name.equals( Elements.STRING))
            return new StringParameter( elem);
        else if (name.equals( Elements.BOOLEAN))
            return new BooleanParameter( elem);
        else if (name.equals( Elements.LIST))
            return new ListParameter( elem);
        else
            throw new IllegalArgumentException( elem.getTagName());
    }
} // class ParameterFactory
