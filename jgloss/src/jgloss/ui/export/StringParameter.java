package jgloss.ui.export;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.JGlossFrame;

import java.awt.Component;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.JTextField;

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

    public Component getComponent() { return box; }

    public String getValue( JGlossFrame source, URL systemId) { return text.getText(); }

    public void loadFromPrefs() {
        text.setText( JGloss.prefs.getString( prefsKey, defaultValue));
    }
} // class StringParameter
