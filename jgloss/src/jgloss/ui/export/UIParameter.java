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

import jgloss.JGloss;
import jgloss.util.XMLTools;

import java.awt.Component;

import org.w3c.dom.Element;

abstract class UIParameter extends AbstractParameter {
    protected String label;
    protected String prefsKey;

    protected UIParameter( Element elem) {
        super( elem);
    }

    public abstract Component getComponent();
    public abstract void loadFromPrefs();

    public void saveToPrefs() {
        JGloss.prefs.set( prefsKey, getValue( null, null));
    }

    protected void initFromElement( Element elem) {
        super.initFromElement( elem);
        label = JGloss.messages.getString( elem.getAttribute( Exporter.Attributes.LABEL_KEY));
        prefsKey = elem.getAttribute( Exporter.Attributes.PREFS_KEY);
    }
} // class UIParameter
