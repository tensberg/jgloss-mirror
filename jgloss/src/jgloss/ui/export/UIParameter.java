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

/**
 * Export parameter which is configured by the user through a UI component.
 *
 * @author Michael Koch
 */
abstract class UIParameter extends AbstractParameter {
    /**
     * Label of the UI component.
     */
    protected String label;
    /**
     * Key under which the parameter value is stored in the user preferences.
     */
    protected String prefsKey;

    /**
     * Creates a new UI parameter initialized from a XML element.
     */
    protected UIParameter( Element elem) {
        super( elem);
    }

    /**
     * Returns the UI component through which the user can configure the value of the
     * export parameter.
     */
    public abstract Component getComponent();

    /**
     * Loads the value of the parameter from the user preferences.
     */
    public abstract void loadFromPrefs();

    /**
     * Saves the value of the parameter to the user preferences.
     */
    public void saveToPrefs() {
        JGloss.prefs.set( prefsKey, String.valueOf(getValue( null, null)));
    }

    /**
     * Configure the UI parameter from a XML element. Initializes the name, default value,
     * preferences key and label.
     */
    protected void initFromElement( Element elem) {
        super.initFromElement( elem);
        label = JGloss.messages.getString( elem.getAttribute( Exporter.Attributes.LABEL_KEY));
        prefsKey = elem.getAttribute( Exporter.Attributes.PREFS_KEY);
    }
} // class UIParameter
