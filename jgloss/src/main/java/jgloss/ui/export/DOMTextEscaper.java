/*
 * Copyright (C) 2003-2004 Michael Koch (tensberg@gmx.net)
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

import java.util.logging.Level;
import java.util.logging.Logger;

import jgloss.util.Escaper;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Apply an escaper to the text nodes of a DOM tree.
 * Currently, all <code>CharacterData</code> nodes and all attributes are escaped. This
 * may be changed later to escape only nodes known to contain document text data, not meta information.
 *
 * @author Michael Koch.
 */
class DOMTextEscaper {
	private static final Logger LOGGER = Logger.getLogger(DOMTextEscaper.class.getPackage().getName());
    private final Escaper escaper;

    DOMTextEscaper(Escaper _escaper) {
        this.escaper = _escaper;
    }

    public Document escapeTextIn( Document doc) {
        try {
            escapeNode( doc.getDocumentElement());
        } catch (DOMException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return doc;
    }

    protected void escapeNode( Node node) throws DOMException {
        // escape this node, if neccessary
        if (node instanceof CharacterData) {
            CharacterData text = (CharacterData) node;
           text.setData(escaper.escape(text.getData()));
        }

        // escape attribute values of this node
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for ( int i=0; i<attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                attribute.setValue(escaper.escape(attribute.getValue()));
            }
        }

        // recurse over children
        NodeList nl = node.getChildNodes();
        for ( int i=0; i<nl.getLength(); i++) {
	        escapeNode(nl.item(i));
        }
    }
} // class DOMTextEscaper
