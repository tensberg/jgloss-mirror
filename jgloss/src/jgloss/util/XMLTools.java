package jgloss.util;

import org.w3c.dom.Node;
import org.w3c.dom.CharacterData;

public class XMLTools {
    private XMLTools() {}

    public static String getText( Node node) {
        return getText( node, new StringBuffer()).toString();
    }

    public static StringBuffer getText( Node node, StringBuffer buf) {
        if (node instanceof CharacterData) {
            buf.append( ((CharacterData) node).getData());
        }

        Node child = node.getFirstChild();
        while (child != null) {
            getText( child, buf);
            child = child.getNextSibling();
        }
        
        return buf;
    }
} // class XMLTools
