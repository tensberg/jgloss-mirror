/*
 * Copyright (C) 2002,2003 Michael Koch (tensberg@gmx.net)
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

import jgloss.util.LaTeXEscaper;

import org.w3c.dom.Document;

/**
 * Works like XSLT exporter, but applies a {@link jgloss.util.LaTeXEscaper} to the document first.
 */
class LaTeXExporter extends XSLTExporter {
    LaTeXExporter() {}

    protected Document applyFilter(ExportConfiguration configuration, Document doc) {
        doc = (Document) doc.cloneNode(true);
        boolean escapeUmlauts = configuration.getEncoding()==null ||
            !configuration.getEncoding().toUpperCase().startsWith("UTF");
        DOMTextEscaper escaper = new DOMTextEscaper(new LaTeXEscaper(escapeUmlauts));
        escaper.escapeTextIn( doc);
        return doc;
    }
} // class LaTeXExporter
