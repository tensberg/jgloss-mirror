/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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

import java.awt.Component;
import java.net.URL;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Element;

class EncodingParameter extends UIParameter {
    private final Box box;
    private final JComboBox encodings;

    EncodingParameter( Element elem) {
        super( elem);

        box = Box.createHorizontalBox();
        box.add( new JLabel( label));
        box.add( Box.createHorizontalStrut( 3));
        encodings = new JComboBox( JGloss.PREFS.getList( Preferences.ENCODINGS, ','));
        encodings.setEditable( true);
        box.add( encodings);
    }

    @Override
	public Component getComponent() { return box; }

    @Override
	public Object getValue( JGlossFrameModel source, URL systemId) {
        return getValue();
    }

    public Object getValue() {
        return encodings.getSelectedItem().toString();
    }

    @Override
	public void loadFromPrefs() {
        encodings.setSelectedItem( JGloss.PREFS.getString( prefsKey, defaultValue));
    }
} // class EncodingParameter
