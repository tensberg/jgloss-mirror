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

package jgloss.util;

/**
 * Configurable formatting of a list of strings as one string.
 * Start, end and in-between markers can be arbitrarily defined. The formatter allows
 * distinct formatting for empty, single-item and multi-item lists.
 *
 * @author Michael Koch
 */
public class ListFormatter {
    public static final char ITEMNO_MARKER = 'n';

    private StringBuffer buf;
    private int length;
    private int itemNo;

    private String emptyList;
    private String[] singleListBefore;
    private String[] singleListAfter;
    private String[] multiListBefore;
    private String[] multiListBetween;
    private String[] multiListAfter;

    public ListFormatter( String _listBetween) {
        this( "", _listBetween, "");
    }

    public ListFormatter( String _listBefore, String _listAfter) {
        this( "", _listBefore, _listAfter, _listBefore, _listBefore + _listAfter, 
              _listAfter);
    }

    public ListFormatter( String _listBefore, String _listBetween, String _listAfter) {
        this( "", _listBefore, _listAfter, _listBefore, _listBetween, _listAfter);
    }

    public ListFormatter( String _emptyList, String _singleListBefore, String _singleListAfter,
                          String _multiListBefore,
                          String _multiListBetween, String _multiListAfter) {
        emptyList = _emptyList;
        singleListBefore = parse( _singleListBefore);
        singleListAfter = parse( _singleListAfter);
        multiListBefore = parse( _multiListBefore);
        multiListBetween = parse( _multiListBetween);
        multiListAfter = parse( _multiListAfter);
    }

    private String[] parse( String in) {
        int i = in.indexOf( ITEMNO_MARKER);
        if (i == -1)
            return new String[] { in };
        else
            return new String[] { in.substring( 0, i), in.substring( i+1) };
    }

    public ListFormatter newList( StringBuffer _buf, int _length) {
        buf = _buf;
        itemNo = 0;
        length = _length;
        return this;
    }

    public ListFormatter addItem( String item) {
        appendText( length==1 ? singleListBefore : 
                    (itemNo==0 ? multiListBefore : multiListBetween), itemNo);
        buf.append( item);
        if (itemNo == length)
            appendText( length==1 ? singleListAfter :
                        multiListAfter, itemNo);
        itemNo++;

        return this;
    }

    public ListFormatter addItem( StringBuffer item) {
        appendText( length==1 ? singleListBefore : 
                    (itemNo==0 ? multiListBefore : multiListBetween), itemNo);
        buf.append( item);
        if (itemNo == length-1)
            appendText( length==1 ? singleListAfter :
                        multiListAfter, itemNo);
        itemNo++;

        return this;
    }

    private void appendText( String[] text, int itemNo) {
        buf.append( text[0]);
        if (text.length > 1) {
            buf.append( itemNo+1);
            buf.append( text[1]);
        }
    }

    public StringBuffer endList() {
        if (length == 0)
            buf.append( emptyList);

        return buf;
    }

    public StringBuffer getBuffer() { return buf; }
} // class ListFormatter

