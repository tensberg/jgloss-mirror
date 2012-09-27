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
 *
 */

package jgloss.ui.export;

import java.net.URL;

import jgloss.ui.gloss.JGlossFrameModel;

/**
 * Export parameter passed to the export XSLT style sheet.
 * The parameter can be accessed in the XSLT style sheet by defining a global
 * &lt;xsl:param&gt; section with the parameter name as name attribute.
 */
interface Parameter {
    /**
     * Name of the parameter. Set this in the name attribute of the xsl:param element
     * of the style sheet.
     */
    String getName();
    /**
     * Value of the parameter. The <code>Object</code> value is converted internally
     * to the expected type by the style sheet engine. To pass primitive types like
     * <code>boolean</code>, wrap them in their object counterparts.
     */
    Object getValue( JGlossFrameModel source, URL systemId);
} // interface Parameter
