package jgloss.ui;

import java.util.Map;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.AttributeValue;
import jgloss.dictionary.attribute.AttributeValueFormatter;
import jgloss.dictionary.attribute.ValueList;

class HyperlinkAttributeFormatter extends AttributeFormatter {
    class ReferencedAttribute {
        protected Attribute attribute;
        protected ValueList values;
        protected AttributeValue value;

        protected ReferencedAttribute( Attribute _attribute, ValueList _values,
                                       AttributeValue _value) {
            attribute = _attribute;
            values = _values;
            value = _value;
        }

        Attribute getAttribute() { return attribute; }
        ValueList getValueList() { return values; }
        AttributeValue getValue() { return value; }
    } // interface ReferencedAttribute

    protected AttributeFormatter parent;
    protected Map references;
    protected String protocol;
    protected boolean linkValues;

    HyperlinkAttributeFormatter( String _protocol, boolean _linkValues, Map _references, 
                                 AttributeFormatter _parent) {
        protocol = _protocol;
        references = _references;
        linkValues = _linkValues;
        parent = _parent;
    }

    @Override
	public StringBuffer format( AttributeValueFormatter formatter, Attribute att, 
                                ValueList val, StringBuffer buf) {
        if (linkValues) {
            parent.format( formatter, att, val, buf);
        }
        else {
            String refKey = createLinkStart( buf);
            references.put( refKey, new ReferencedAttribute( att, val, null));
            parent.format( formatter, att, val, buf);
            createLinkEnd( buf);
        }

        return buf;
    }

    @Override
	public StringBuffer format( final Attribute att, final AttributeValue val, StringBuffer buf) {
        if (!linkValues) {
            parent.format( att, val, buf);
        }
        else {
            String refKey = createLinkStart( buf);
            references.put( refKey, new ReferencedAttribute( att, null, val));
            parent.format( att, val, buf);
            createLinkEnd( buf);
        }

        return buf;
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
} // class HyperlinkAttributeFormatter
