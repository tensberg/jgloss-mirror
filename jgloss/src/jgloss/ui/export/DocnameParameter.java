package jgloss.ui.export;

import jgloss.ui.JGlossFrame;

import java.net.URL;

import org.w3c.dom.Element;

class DocnameParameter extends AbstractParameter {
    DocnameParameter( Element elem) {
        super( elem);
    }

    public String getValue( JGlossFrame source, URL systemId) {
        return source.getDocumentName();
    }
} // class DocnameParameter
