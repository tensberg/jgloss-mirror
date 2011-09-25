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

import java.awt.Component;

import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Document;

/**
 * An <code>Exporter</code> runs the export process when the user selects an export menu item.
 *
 * @author Michael Koch
 */
interface Exporter {
    /**
     * Shows the export file chooser and runs the export.
     */
    void export( ExportConfiguration configuration, 
                 JGlossFrameModel source, Document doc, Component parent);
} // interface Exporter
