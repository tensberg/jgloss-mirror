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

import jgloss.util.ListFormatter;
import jgloss.util.StringTools;

/**
 * List formatter which will add some additional text to the list items, if they
 * contain a certain chooseable string. Since several instances of this class are
 * usually used in a group which share a single configuration, the interface
 * {@link Group Group} is used to access this shared state.
 */
public class MarkerListFormatter extends ListFormatter {
    /**
     * Shared state of a group of marker list formatters.
     */
    public interface Group {
        /**
         * Get the string to which some additional text should be added if it occurrs
         * in a list item.
         */
        String getMarkedText();
        /**
         * Get the string which should be added before each occurrence the marked text.
         */
        String getMarkBefore();
        /**
         * Get the string which should be added after each occurrence the marked text.
         */
        String getMarkAfter();
    } // interface Group

    protected Group group;

    public MarkerListFormatter( Group _group, ListFormatter _formatter) {
        super( _formatter);
        group = _group;
    }

    public MarkerListFormatter( Group _group, String _listBetween) {
        this( _group, "", _listBetween, "");
    }

    public MarkerListFormatter( Group _group, String _listBefore, String _listAfter) {
        this( _group, "", _listBefore, _listAfter, _listBefore, _listBefore + _listAfter, 
              _listAfter);
    }

    public MarkerListFormatter( Group _group, String _listBefore, String _listBetween, String _listAfter) {
        this( _group, "", _listBefore, _listAfter, _listBefore, _listBetween, _listAfter);
    }

    public MarkerListFormatter( Group _group, String _emptyList, String _singleListBefore,
                                String _singleListAfter, String _multiListBefore,
                                String _multiListBetween, String _multiListAfter) {
        super( _emptyList, _singleListBefore, _singleListAfter,
               _multiListBefore, _multiListBetween, _multiListAfter);
        group = _group;
    }

    protected void doAppendItem( Object item) {
        String markedText = group.getMarkedText();
        if (markedText==null || markedText.length()==0) {
            super.doAppendItem( item);
            return;
        }   
        markedText = StringTools.toHiragana( markedText.toLowerCase());

        String markBefore = group.getMarkBefore();
        String markAfter = group.getMarkAfter();

        String s = String.valueOf( item);
        StringBuffer tempBuffer = new StringBuffer( s);        
        String sN = StringTools.toHiragana( s.toLowerCase());

        int from = tempBuffer.length()-1;
        while ((from=sN.lastIndexOf( markedText, from)) != -1) {
            tempBuffer.insert( from+markedText.length(), markAfter);
            tempBuffer.insert( from, markBefore);
            from--;
        }

        buffer.append( tempBuffer);
    }
} // class MarkerListFormatter
