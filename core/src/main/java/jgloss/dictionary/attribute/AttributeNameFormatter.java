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

import java.util.List;

/**
 * Attribute formatter which only prints the name of the attribute, not its values.
 *
 * @author Michael Koch
 */
public class AttributeNameFormatter extends AttributeFormatter {
    private String before;
    private String after;

    public AttributeNameFormatter() {
        this( null, null);
    }
    
    public AttributeNameFormatter( String _before, String _after) {
        before = _before;
        after = _after;
    }

    protected StringBuilder format( Attribute<?> att, StringBuilder buf) {
        if (before != null) {
	        buf.append( before);
        }
        buf.append( att.getName());
        if (after != null) {
	        buf.append( after);
        }
        return buf;
    }

    @Override
	public StringBuilder format( AttributeValueFormatter formatter, Attribute<?> att, 
                                List<? extends AttributeValue> val, StringBuilder buf) {
        return format( att, buf);
    }

    @Override
	public StringBuilder format( Attribute<?> att, AttributeValue val, StringBuilder buf) {
        return format( att, buf);
    }
} // class AttributeNameFormatter
