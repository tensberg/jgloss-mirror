package jgloss.ui.export;

import jgloss.util.XMLTools;

import org.w3c.dom.Element;

abstract class AbstractParameter implements Parameter {
    protected String name;
    protected String defaultValue;

    protected AbstractParameter( Element elem) {
        initFromElement( elem);
    }

    public String getName() { return name; }

    protected void initFromElement( Element elem) {
        name = elem.getAttribute( Exporter.Attributes.NAME);
        defaultValue = XMLTools.getText( elem);
    }
} // class UIParameter
