package jgloss.ui;

import jgloss.util.ListFormatter;

import java.util.Map;
import java.util.regex.Pattern;

class HyperlinkListFormatter implements ListFormatter {
    protected String protocol;
    protected Map references;
    protected ListFormatter parent;
    protected StringBuffer tempBuffer = new StringBuffer();

    public HyperlinkListFormatter( String _protocol, Map _references, 
                                   ListFormatter _parent) {
        protocol = _protocol;
        references = _references;
        parent = _parent;
    }

    public ListFormatter newList( StringBuffer _buffer, int _length) {
        parent.newList( _buffer, _length);
        return this;
    }

    public ListFormatter addItem( Object item) {
        String itemString = item.toString();

        tempBuffer.setLength( 0);
        String refKey = createLinkStart( tempBuffer);
        references.put( refKey, itemString);
        tempBuffer.append( itemString);
        createLinkEnd( tempBuffer);

        parent.addItem( tempBuffer);

        return this;
    }

    public StringBuffer endList() {
        return parent.endList();
    }

    public StringBuffer getBuffer() {
        return parent.getBuffer();
    }

    public Pattern getPattern() {
        return parent.getPattern();
    }

    protected String createLinkStart( StringBuffer buf) {
        String refKey = Integer.toString( references.size()+1);
        buf.append( "<a href=\"");
        buf.append( protocol);
        buf.append( ':');
        buf.append( refKey);
        buf.append( "\" class=\"");
        buf.append( protocol);
        buf.append( "\">");

        return refKey;
    }

    protected void createLinkEnd( StringBuffer buf) {
        buf.append( "</a>");
    }
} // class HyperlinkListFormatter
