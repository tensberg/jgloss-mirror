/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id$
 *
 */

package jgloss.ui;

import jgloss.dictionary.attribute.*;
import jgloss.util.ListFormatter;

import java.util.Map;

/**
 * Generate a HTML hyperlink for a reference attribute. The <code>a href</code> hyperlinks are of the
 * form <code>protocol:refid</code>. The protocol can be chosen in the constructor; it can be
 * chosen based on the types of reference which are formatted using the instance. For each
 * encountered reference attribute value, a unique refid string is generated, which is used
 * as part of the hyperlink URL and used as key to the reference attribute value in the
 * map of references.
 *
 * @author Michael Koch
 */
public class HTMLReferenceAttributeFormatter implements AttributeFormatter {
    protected String protocol;
    protected String beforeValue;
    protected String afterValue;
    protected ListFormatter formatter;
    protected Map references;
    protected StringBuffer itemBuffer = new StringBuffer( 128);

    public HTMLReferenceAttributeFormatter( String _protocol, String _beforeValue,
                                            String _afterValue, ListFormatter _formatter,
                                            Map _references) {
        protocol = _protocol;
        beforeValue = _beforeValue;
        afterValue = _afterValue;
        formatter = _formatter;
        references = _references;
    }

    public StringBuffer format( Attribute att, ValueList val, StringBuffer buf) {
        formatter.newList( buf, val.size());
        for ( int i=0; i<val.size(); i++) {
            itemBuffer.setLength( 0);
            ReferenceAttributeValue ref = (ReferenceAttributeValue) val.get( i);
            String refKey = Integer.toString( references.size()+1);
            references.put( refKey, ref);
            itemBuffer.append( "<a href=\"");
            itemBuffer.append( protocol);
            itemBuffer.append( ':');
            itemBuffer.append( refKey);
            itemBuffer.append( "\">");
            itemBuffer.append( beforeValue);
            itemBuffer.append( ref.getReferenceTitle());
            itemBuffer.append( afterValue);
            itemBuffer.append( "</a>");
            
            formatter.addItem( itemBuffer);
        }

        return buf;
    }
} // class HTMLReferenceAttributeFormatter
