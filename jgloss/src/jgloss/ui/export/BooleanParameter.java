package jgloss.ui.export;

import jgloss.JGloss;
import jgloss.ui.JGlossFrame;

import java.awt.Component;
import java.net.URL;

import javax.swing.JCheckBox;

import org.w3c.dom.Element;

class BooleanParameter extends UIParameter {
    private JCheckBox box;

    BooleanParameter( Element elem) {
        super( elem);
        box = new JCheckBox( label);
    }

    public Component getComponent() { return box; }

    public String getValue( JGlossFrame source, URL systemId) {
        return String.valueOf( box.isSelected()); 
    }

    public void loadFromPrefs() {
        box.setSelected( JGloss.prefs.getBoolean( prefsKey, "true".equalsIgnoreCase( defaultValue)));
    }
} // class BooleanParameter
