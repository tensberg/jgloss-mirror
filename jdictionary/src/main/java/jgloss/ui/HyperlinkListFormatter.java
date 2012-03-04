package jgloss.ui;

import java.util.Map;
import java.util.regex.Pattern;

import jgloss.util.ListFormatter;

class HyperlinkListFormatter implements ListFormatter {
    protected String protocol;
    protected Map<String, Object> references;
    protected ListFormatter parent;
    protected StringBuilder tempBuffer = new StringBuilder();

    public HyperlinkListFormatter( String _protocol, Map<String, Object> _references, 
                                   ListFormatter _parent) {
        protocol = _protocol;
        references = _references;
        parent = _parent;
    }

    @Override
	public ListFormatter newList( StringBuilder _buffer, int _length) {
        parent.newList( _buffer, _length);
        return this;
    }

    @Override
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

    @Override
	public StringBuilder endList() {
        return parent.endList();
    }

    @Override
	public StringBuilder getBuffer() {
        return parent.getBuffer();
    }

    @Override
	public Pattern getPattern() {
        return parent.getPattern();
    }

    protected String createLinkStart( StringBuilder buf) {
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

    protected void createLinkEnd( StringBuilder buf) {
        buf.append( "</a>");
    }
} // class HyperlinkListFormatter
