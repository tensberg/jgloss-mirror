/*
 * Copyright (C) 2003-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.export;

import java.net.URL;

import jgloss.ui.JGlossFrameModel;

/**
 * Export parameter with a constant value.
 */
class ConstantParameter implements Parameter {
    private String name;
    private Object value;

    ConstantParameter(String _name, String _value) {
        this.name = _name;
        this.value = _value;
    }

    public String getName() { return name; }
    public Object getValue( JGlossFrameModel source, URL systemId) { return value; }
} // class ConstantParameter
