/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

import jgloss.util.DefaultListFormatter;
import jgloss.util.ListFormatter;

/**
 * Default formatter for attributes and their values.
 *
 * @author Michael Koch
 */
public class DefaultAttributeFormatter extends AttributeFormatter {
    protected String printBefore;
    protected String printAfter;
    protected String printBeforeValue;
    protected boolean printAttributeName;
    protected ListFormatter valueFormat;
    protected StringBuffer tempBuffer;

    public DefaultAttributeFormatter() {
        this( "(", ")", ":", true, new DefaultListFormatter( "", "", "", "[", ",", "]"));
    }

    public DefaultAttributeFormatter( ListFormatter _valueFormat) {
        this( "", "", null, false, _valueFormat);
    }

    public DefaultAttributeFormatter( String _printBefore, String _printAfter,
                                      String _printBeforeValue,
                                      boolean _printAttributeName,
                                      ListFormatter _valueFormat) {
        printBefore = _printBefore;
        printAfter = _printAfter;
        printBeforeValue = _printBeforeValue;
        printAttributeName = _printAttributeName;
        valueFormat = _valueFormat;

        tempBuffer = new StringBuffer();
    }

    @Override
	public StringBuffer format( AttributeValueFormatter valueFormatter, Attribute att, 
                                ValueList val, StringBuffer out) {
        out.append( printBefore);

        if (printAttributeName)
            out.append( att.getName());

        if (val != null) {
            if (printAttributeName)
                out.append( printBeforeValue);

            valueFormat.newList( out, val.size());
            for ( int i=0; i<val.size(); i++) {
                tempBuffer.setLength( 0);
                valueFormat.addItem( valueFormatter.format( att, val.get( i), tempBuffer));
            }
            valueFormat.endList();
        }
        out.append( printAfter);

        return out;
    }

    @Override
	public StringBuffer format( Attribute att, AttributeValue val, StringBuffer out) {
        out.append( String.valueOf( val));
        return out;
    }
} // class DefaultAttributeFormatter
