package jgloss.ui.export;

import org.w3c.dom.Element;

class ParameterFactory {
    public static interface Elements {
        String ENCODING = "encoding";
        String STRING = "string";
        String BOOLEAN = "boolean";
        String COMBOBOX = "combobox";
        String DOCNAME = "docname";
        String DATETIME = "datetime";
    }

    private ParameterFactory() {}

    public static Parameter createParameter( Element elem) {
        String name = elem.getTagName();
        if (name.equals( Elements.DOCNAME))
            return new DocnameParameter( elem);
        else if (name.equals( Elements.DATETIME))
            return new DateTimeParameter( elem);
        else if (name.equals( Elements.ENCODING))
            return new EncodingParameter( elem);
        else if (name.equals( Elements.STRING))
            return new StringParameter( elem);
        else if (name.equals( Elements.BOOLEAN))
            return new BooleanParameter( elem);
        else if (name.equals( Elements.COMBOBOX))
            return new ComboBoxParameter( elem);
        else
            throw new IllegalArgumentException( elem.getTagName());
    }
} // class ParameterFactory
