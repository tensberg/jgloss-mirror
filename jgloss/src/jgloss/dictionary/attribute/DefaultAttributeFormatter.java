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

/**
 * Default formatter for attributes and their values.
 *
 * @author Michael Koch
 */
public class DefaultAttributeFormatter implements AttributeFormatter {
    protected String printBefore;
    protected String printAfter;
    protected String printBeforeValue;
    protected boolean printAttributeName;
    protected ListFormatter valueFormat;

    public DefaultAttributeFormatter() {
        this( "(", ")", ":", true, new ListFormatter( "", "", "", "[", ",", "]"));
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
    }

    public StringBuffer format( Attribute att, ValueList val, StringBuffer out) {
        out.append( printBefore);

        if (printAttributeName)
            out.append( att.getName());

        if (val != null) {
            if (printAttributeName)
                out.append( printBeforeValue);

            valueFormat.newList( out, val.size());
            for ( int i=0; i<val.size(); i++) {
                valueFormat.addItem( formatValue( val.get( i)));
            }
            valueFormat.endList();
        }
        out.append( printAfter);

        return out;
    }

    protected String formatValue( AttributeValue val) {
        return String.valueOf( val);
    }
} // class DefaultAttributeFormatter
