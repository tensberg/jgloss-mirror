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

package jgloss.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jgloss.util.ListFormatter;
import jgloss.util.StringTools;

/**
 * List formatter which will add some additional text to the list items, if they
 * contain a certain chooseable string.
 *
 * @author Michael Koch
 */
public class MarkerListFormatter implements ListFormatter {
    /**
     * Group of marker list formatters which share a common configuration.
     */
    public static class Group {
        private List<MarkerListFormatter> formatters = new ArrayList<MarkerListFormatter>( 5);
        private String markedText;
        private String textBefore;
        private String textAfter;

        public Group() {
            this( null, "", "");
        }

        public Group( String _textBefore, String _textAfter) {
            this( null, _textBefore, _textAfter);
        }

        public Group( String _markedText, String _textBefore, String _textAfter) {
            if (markedText != null) {
	            markedText = normalize( _markedText);
            }
            textBefore = _textBefore;
            textAfter = _textAfter;
        }

        public void add( MarkerListFormatter formatter) {
            formatters.add( formatter);
            formatter.setMarkedText( markedText);
            formatter.setTextBefore( textBefore);
            formatter.setTextAfter( textAfter);
        }

        public void remove( MarkerListFormatter formatter) {
            formatters.remove( formatter);
        }

        public void setMarkedText( String _markedText) {
            markedText = normalize( _markedText);
            for (MarkerListFormatter formatter : formatters) {
	            formatter.setMarkedText( _markedText);
            }
        }

        public void setTextBefore( String _textBefore) {
            textBefore = _textBefore;
            for (MarkerListFormatter formatter : formatters) {
	            formatter.setTextBefore( _textBefore);
            }
        }

        public void setTextAfter( String _textAfter) {
            textAfter = _textAfter;
            for (MarkerListFormatter formatter : formatters) {
	            formatter.setTextBefore( _textAfter);
            }
        }

        public String getMarkedText() { return markedText; }
        public String getTextBefore() { return textBefore; }
        public String getTextAfter() { return textAfter; }
    } // class Group

    protected ListFormatter parent;

    protected String markedText;
    protected String textBefore;
    protected String textAfter;

    public MarkerListFormatter( ListFormatter _parent, String _markedText,
                                String _textBefore, String _textAfter) {
        parent = _parent;
        markedText = _markedText;
        textBefore = _textBefore;
        textAfter = _textAfter;
    }

    public MarkerListFormatter( Group _group, ListFormatter _parent) {
        parent = _parent;
        _group.add( this);
    }

    public void setMarkedText( String _markedText) {
        markedText = _markedText;
    }

    public void setTextBefore( String _textBefore) {
        textBefore = _textBefore;
    }

    public void setTextAfter( String _textAfter) {
        textAfter = _textAfter;
    }

    public String getMarkedText() { return markedText; }
    public String getTextBefore() { return textBefore; }
    public String getTextAfter() { return textAfter; }

    @Override
	public ListFormatter newList( StringBuilder _buffer, int _length) {
        parent.newList( _buffer, _length);
        return this;
    }

    @Override
	public ListFormatter addItem( Object item) {
        if (markedText == null) {
            parent.addItem( item);
            return this;
        }

        String s = String.valueOf( item);
        StringBuilder tempBuffer = new StringBuilder( s);        
        String sN = normalize( s);

        int from = sN.length()-1;
        while ((from=sN.lastIndexOf( markedText, from)) != -1) {
            tempBuffer.insert( from+markedText.length(), textAfter);
            tempBuffer.insert( from, textBefore);
            from--;
        }

        parent.addItem( tempBuffer);
        return this;
    }

    @Override
	public StringBuilder endList() { return parent.endList(); }
    @Override
	public StringBuilder getBuffer() { return parent.getBuffer(); }
    @Override
	public Pattern getPattern() { return parent.getPattern(); }

    protected static final String normalize( String in) {
        return in != null ? StringTools.toHiragana( in.toLowerCase()) : null;
    }
} // class MarkerListFormatter
