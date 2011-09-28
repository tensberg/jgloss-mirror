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

import java.net.MalformedURLException;
import java.net.URL;

import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Element;

/**
 * UI element for choosing the XSLT export template. While the element names are different,
 * the tree structure of the XML element is identical to the list parameter. The list parameter
 * can thus be used as superclass.
 */
class TemplateChooser extends ListParameter {
    TemplateChooser( Element elem) {
        super( elem);
    }

    public Object getValue( JGlossFrameModel source, URL systemId) {
        String value = getValue();

        if (systemId != null) {
            try {
                value = new URL( systemId, value).toExternalForm();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }

        return value;
    }
} // class TemplateChooser