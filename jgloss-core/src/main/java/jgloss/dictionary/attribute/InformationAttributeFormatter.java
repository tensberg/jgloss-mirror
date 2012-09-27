/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.dictionary.attribute;

import jgloss.util.ListFormatter;

/**
 * Format information attributes. The information string is used to display the value.
 *
 * @author Michael Koch
 */
public class InformationAttributeFormatter extends DefaultAttributeFormatter {
    public InformationAttributeFormatter() {
        super();
    }

    public InformationAttributeFormatter( String _printBefore, String _printAfter,
                                          String _printBeforeValue,
                                          boolean _printAttributeName,
                                          ListFormatter _valueFormat) {
        super( _printBefore, _printAfter, _printBeforeValue, _printAttributeName,
               _valueFormat);
    }

    @Override
	public StringBuilder format( Attribute<?> att, AttributeValue val, StringBuilder buf) {
        buf.append( ((InformationAttributeValue) val).getInformation());
        return buf;
    }
} // class InformationAttributeFormatter
