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
