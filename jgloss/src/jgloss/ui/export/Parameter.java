package jgloss.ui.export;

import jgloss.ui.JGlossFrame;

import java.net.URL;

interface Parameter {
    String getName();
    String getValue( JGlossFrame source, URL systemId);
} // class Parameter
