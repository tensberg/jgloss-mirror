package jgloss.ui.export;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.util.XMLTools;
import jgloss.ui.JGlossFrame;

import java.util.Vector;
import java.awt.Component;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.JComboBox;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

class ComboBoxParameter extends UIParameter {
    protected static class Value {
        private String label;
        private String value;

        public Value( String _label, String _value) {
            label = _label;
            value = _value;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String toString() { return label; }
    } // class Value

    private Box box;
    protected JComboBox combobox;

    ComboBoxParameter( Element elem) {
        super( elem);

        box = Box.createHorizontalBox();
        box.add( new JLabel( label));
        box.add( Box.createHorizontalStrut( 3));
        combobox = new JComboBox( getItems( elem));
        combobox.setEditable( "true".equals( elem.getAttribute( Exporter.Attributes.EDITABLE)));
        box.add( combobox);
    }

    public Component getComponent() { return box; }

    public String getValue( JGlossFrame source, URL systemId) { 
        return getValue();
    }
    
    public String getValue() {
        Object item = combobox.getSelectedItem();
        if (item instanceof Value)
            return ((Value) item).getValue();
        else
            return item.toString();
    }

    public void loadFromPrefs() {
        combobox.setSelectedItem( JGloss.prefs.getString( prefsKey, defaultValue));
    }

    private Vector getItems( Element elem) {
        Vector out = new Vector();
        StringBuffer buf = new StringBuffer();

        Node item = elem.getFirstChild();
        while (item != null) {
            Node labelElem = item.getFirstChild();
            buf.setLength( 0);
            String label = XMLTools.getText( labelElem, buf).toString();
            if (labelElem.getNodeName().equals( Exporter.Elements.LABEL_KEY))
                label = JGloss.messages.getString( label);

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
} // class ComboBoxParameter
