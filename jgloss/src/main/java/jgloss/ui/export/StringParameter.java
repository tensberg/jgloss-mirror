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

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jgloss.JGloss;
import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Element;

class StringParameter extends UIParameter {
    private Box box;
    private JTextField text;

    StringParameter( Element elem) {
        super( elem);

        box = Box.createHorizontalBox();
        box.add( new JLabel( label));
        box.add( Box.createHorizontalStrut( 3));
        text = new JTextField();
        box.add( text);
    }

    @Override
	public Component getComponent() { return box; }

    @Override
	public Object getValue( JGlossFrameModel source, URL systemId) { return text.getText(); }

    @Override
	public void loadFromPrefs() {
        text.setText( JGloss.prefs.getString( prefsKey, defaultValue));
    }
} // class StringParameter
