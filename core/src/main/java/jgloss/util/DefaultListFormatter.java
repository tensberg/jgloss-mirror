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

package jgloss.util;

import java.util.regex.Pattern;

/**
 * Configurable formatting of a list of strings as one string.
 * Start, end and in-between markers can be arbitrarily defined. The formatter allows
 * distinct formatting for empty, single-item and multi-item lists.
 *
 * @author Michael Koch
 */
public class DefaultListFormatter implements ListFormatter {
    public static final char ITEMNO_MARKER = 'n';

    protected StringBuilder buffer;
    protected int length;
    protected int itemNo;

    private String emptyList;
    private String[] singleListBefore;
    private String[] singleListAfter;
    private String[] multiListBefore;
    private String[] multiListBetween;
    private String[] multiListAfter;

    public DefaultListFormatter( DefaultListFormatter _formatter) {
        emptyList = _formatter.emptyList;
        singleListBefore = _formatter.singleListBefore;
        singleListAfter = _formatter.singleListAfter;
        multiListBefore = _formatter.multiListBefore;
        multiListBetween = _formatter.multiListBetween;
        multiListAfter = _formatter.multiListAfter;
    }

    public DefaultListFormatter( String _listBetween) {
        this( "", _listBetween, "");
    }

    public DefaultListFormatter( String _listBefore, String _listAfter) {
        this( "", _listBefore, _listAfter, _listBefore, _listBefore + _listAfter, 
              _listAfter);
    }

    public DefaultListFormatter( String _listBefore, String _listBetween, String _listAfter) {
        this( "", _listBefore, _listAfter, _listBefore, _listBetween, _listAfter);
    }

    public DefaultListFormatter( String _emptyList, String _singleListBefore, String _singleListAfter,
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
        if (i == -1) {
	        return new String[] { in };
        } else {
	        return new String[] { in.substring( 0, i), in.substring( i+1) };
        }
    }

    @Override
	public ListFormatter newList( StringBuilder _buffer, int _length) {
        buffer = _buffer;
        itemNo = 0;
        length = _length;
        return this;
    }

    @Override
	public ListFormatter addItem( Object item) {
        appendText( length==1 ? singleListBefore : 
                    (itemNo==0 ? multiListBefore : multiListBetween), itemNo);
        doAppendItem( item);
        if (itemNo == length-1) {
	        appendText( length==1 ? singleListAfter :
                        multiListAfter, itemNo);
        }
        itemNo++;

        return this;
    }

    protected void doAppendItem( Object item) {
        if (item instanceof String) {
            buffer.append( (String) item);
        }
        else if (item instanceof StringBuilder) {
            buffer.append( (StringBuilder) item);
        } else {
	        buffer.append( String.valueOf( item));
        }
    }

    private void appendText( String[] text, int itemNo) {
        buffer.append( text[0]);
        if (text.length > 1) {
            buffer.append( itemNo+1);
            buffer.append( text[1]);
        }
    }

    @Override
	public StringBuilder endList() {
        if (length == 0) {
	        buffer.append( emptyList);
        }

        return buffer;
    }

    @Override
	public StringBuilder getBuffer() { return buffer; }

    @Override
	public Pattern getPattern() {
        StringBuilder pattern = new StringBuilder();

        pattern.append( "(?:(?:");
        addToRegex( multiListBefore, pattern);
        pattern.append( '|');
        addToRegex( multiListBetween, pattern);
        pattern.append( ")(.+?)(?=");
        addToRegex( multiListBetween, pattern);
        pattern.append( '|');
        addToRegex( multiListAfter, pattern);

        pattern.append( ")|(?:");
        addToRegex( singleListBefore, pattern);
        pattern.append( "(.+?)");
        addToRegex( singleListAfter, pattern);
        pattern.append( ")");

        return Pattern.compile( pattern.toString());
    }

    private void addToRegex( String[] format, StringBuilder regex) {
        StringTools.addToRegex( format[0], regex);
        if (format.length == 2) {
            // match itemno marker
            regex.append( "\\d+");
            StringTools.addToRegex( format[1], regex);
        }
    }
} // class DefaultListFormatter
