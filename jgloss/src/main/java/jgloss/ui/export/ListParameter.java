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
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import jgloss.JGloss;
import jgloss.ui.gloss.JGlossFrameModel;
import jgloss.util.XMLTools;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A list parameter gives the user several choices to pick from. The UI component used is a combo
 * box.
 */
class ListParameter extends UIParameter {
    /**
     * Item in the list combo box model. Each item in the combo box has a label which is displayed
     * and a value which is returned when the item is selected.
     */
    protected static class Value {
        private String label;
        private String value;

        public Value( String _label, String _value) {
            label = _label;
            value = _value;
        }

        public String getLabel() { return label; }
        public void setLabel(String _label) { this.label = _label; }
        public String getValue() { return value; }
        public void setValue(String _value) { this.value = _value; }

        @Override
		public String toString() { return label; }
    } // class Value

    private final Box box;
    protected JComboBox combobox;

    ListParameter( Element elem) {
        super( elem);

        box = Box.createHorizontalBox();
        box.add( new JLabel( label));
        box.add( Box.createHorizontalStrut( 3));
        combobox = new JComboBox(getItems(elem));
        combobox.setEditable( "true".equals( elem.getAttribute( ExportConfiguration.Attributes.EDITABLE)));
        box.add( combobox);
    }

    @Override
	public Component getComponent() { return box; }

    @Override
	public Object getValue( JGlossFrameModel source, URL systemId) { 
        return getValue();
    }
    
    public String getValue() {
        Object item = combobox.getSelectedItem();
        if (item instanceof Value) {
	        return ((Value) item).getValue();
        } else {
	        return item.toString();
        }
    }

    @Override
	public void loadFromPrefs() {
        // saveToPrefs stores the selected value, not the label
        // Find the Value object corresponding to the stored preference
        String selection = JGloss.PREFS.getString( prefsKey, defaultValue);

        ComboBoxModel model = combobox.getModel();
        int selectedIndex = -1;
        for (int i=0; i<model.getSize(); i++) {
            if (((Value) model.getElementAt(i)).getValue().equals(selection)) {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex != -1) {
            combobox.setSelectedIndex(selectedIndex);
        }
        else if (combobox.isEditable()) {
            // set the prefs value as String
            combobox.setSelectedItem(selection);
        }
    }

    protected Vector<Value> getItems( Element elem) {
        Vector<Value> out = new Vector<Value>();
        StringBuilder buf = new StringBuilder();

        Node item = elem.getFirstChild();
        while (item != null) {
            Node labelElem = item.getFirstChild();
            buf.setLength( 0);
            String label = XMLTools.getText( labelElem, buf).toString();
            if (labelElem.getNodeName().equals( ExportConfiguration.Elements.LABEL_KEY)) {
	            label = JGloss.MESSAGES.getString( label);
            }

            String value = label;
            Node valueElem = labelElem.getNextSibling();
            if (valueElem != null) {
                buf.setLength( 0);
                value = XMLTools.getText( valueElem, buf).toString();
            }

            out.add( new Value( label, value));

            item = item.getNextSibling();
        }

        return out;
    }
} // class ListParameter
