package jgloss.ui.export;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.JGlossFrame;

import java.awt.Component;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.JComboBox;

import org.w3c.dom.Element;

class EncodingParameter extends UIParameter {
    private Box box;
    private JComboBox encodings;

    EncodingParameter( Element elem) {
        super( elem);

        box = Box.createHorizontalBox();
        box.add( new JLabel( label));
        box.add( Box.createHorizontalStrut( 3));
        encodings = new JComboBox( JGloss.prefs.getList( Preferences.ENCODINGS, ','));
        encodings.setEditable( true);
        box.add( encodings);
    }

    public Component getComponent() { return box; }

    public String getValue( JGlossFrame source, URL systemId) {
        return getValue();
    }

    public String getValue() {
        return encodings.getSelectedItem().toString();
    }

    public void loadFromPrefs() {
        encodings.setSelectedItem( JGloss.prefs.getString( prefsKey, defaultValue));
    }
} // class EncodingParameter
