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

package jgloss.dictionary.attribute;

import jgloss.util.ListFormatter;

public class ReferenceAttributeFormatter implements AttributeFormatter {
    protected String beforeValue;
    protected String afterValue;
    protected ListFormatter formatter;
    protected StringBuffer itemBuffer = new StringBuffer( 128);

    public ReferenceAttributeFormatter( String _beforeValue, String _afterValue,
                                        ListFormatter _formatter) {
        beforeValue = _beforeValue;
        afterValue = _afterValue;
        formatter = _formatter;
    }

    public StringBuffer format( Attribute att, ValueList val, StringBuffer buf) {
        formatter.newList( buf, val.size());
        for ( int i=0; i<val.size(); i++) {
            itemBuffer.setLength( 0);
            ReferenceAttributeValue ref = (ReferenceAttributeValue) val.get( i);
            itemBuffer.append( beforeValue);
            itemBuffer.append( ref.getReferenceTitle());
            itemBuffer.append( afterValue);
            
            formatter.addItem( itemBuffer);
        }

        return buf;
    }
} // class HTMLReferenceAttributeFormatter
