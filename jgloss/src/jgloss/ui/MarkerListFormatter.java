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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * List formatter which will add some additional text to the list items, if they
 * contain a certain chooseable string.
 *
 * @author Michael Koch
 */
public class MarkerListFormatter extends ListFormatter {
    /**
     * Group of marker list formatters which share a common configuration.
     */
    public static class Group {
        private List formatters = new ArrayList( 5);
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
            if (markedText != null)
                markedText = normalize( _markedText);
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
            for ( Iterator i=formatters.iterator(); i.hasNext(); )
                ((MarkerListFormatter) i.next()).setMarkedText( _markedText);
        }

        public void setTextBefore( String _textBefore) {
            textBefore = _textBefore;
            for ( Iterator i=formatters.iterator(); i.hasNext(); )
                ((MarkerListFormatter) i.next()).setTextBefore( _textBefore);
        }

        public void setTextAfter( String _textAfter) {
            textAfter = _textAfter;
            for ( Iterator i=formatters.iterator(); i.hasNext(); )
                ((MarkerListFormatter) i.next()).setTextBefore( _textAfter);
        }

        public String getMarkedText() { return markedText; }
        public String getTextBefore() { return textBefore; }
        public String getTextAfter() { return textAfter; }
    } // class Group

    protected String markedText;
    protected String textBefore;
    protected String textAfter;

    public MarkerListFormatter( Group _group, ListFormatter _formatter) {
        super( _formatter);
        _group.add( this);
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

    protected void doAppendItem( Object item) {
        if (markedText == null) {
            super.doAppendItem( item);
            return;
        }

        String s = String.valueOf( item);
        StringBuffer tempBuffer = new StringBuffer( s);        
        String sN = normalize( s);

        int from = sN.length()-1;
        while ((from=sN.lastIndexOf( markedText, from)) != -1) {
            tempBuffer.insert( from+markedText.length(), textAfter);
            tempBuffer.insert( from, textBefore);
            from--;
        }

        buffer.append( tempBuffer);
    }

    protected static final String normalize( String in) {
        return in != null ? StringTools.toHiragana( in.toLowerCase()) : null;
    }
} // class MarkerListFormatter
