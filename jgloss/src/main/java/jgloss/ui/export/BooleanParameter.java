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

import java.awt.Component;
import java.net.URL;

import javax.swing.JCheckBox;

import jgloss.JGloss;
import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Element;

class BooleanParameter extends UIParameter {
    private final JCheckBox box;

    BooleanParameter( Element elem) {
        super( elem);
        box = new JCheckBox( label);
    }

    @Override
	public Component getComponent() { return box; }

    @Override
	public Object getValue( JGlossFrameModel source, URL systemId) {
        return Boolean.valueOf( box.isSelected()); 
    }

    @Override
	public void loadFromPrefs() {
        box.setSelected( JGloss.prefs.getBoolean( prefsKey, "true".equalsIgnoreCase( defaultValue)));
    }
} // class BooleanParameter
