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

package jgloss.ui.export;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Element;

class DateTimeParameter extends AbstractParameter {
    private static final DateFormat DATETIME_FORMAT = 
        DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.SHORT);

    DateTimeParameter( Element elem) {
        super( elem);
    }

    @Override
	public Object getValue( JGlossFrameModel source, URL systemId) {
        return DATETIME_FORMAT.format( new Date( System.currentTimeMillis()));
    }
} // class DateTimeParameter
