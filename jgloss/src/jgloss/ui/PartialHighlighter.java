/**
 *   Copyright (C) 2002  Eric Crahen <crahen@cse.buffalo.edu>
 *   Integration into JGloss and modifications (C) 2002 Michael Koch <tensberg@gmx.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id$
 */
package jgloss.ui;

import jgloss.util.StringTools;

import java.awt.*;

public class PartialHighlighter implements Highlighter {

    private char[] text;

    /**
     * Paint the given text, highlighting the portion that appears in
     * the match string.
     *
     * @param g Graphics object to paint on
     * @param text text to paint  
     */
    public void paintHighlight(Graphics g, String entryText, String searchText) {
    
        // Allocate a buffer for the entry text
        if(text == null || text.length < entryText.length())
            text = new char[entryText.length()*2];

        entryText.getChars(0, entryText.length(), text, 0);

        FontMetrics fm = g.getFontMetrics();

        Color normal = g.getColor();
        Color highlight = Color.blue;
  
        int x = 0;
        int y = fm.getAscent();

        int max = entryText.length();
        int len = searchText.length();

        for(int i = 0, j = 0; i > -1 && i < max; i=j) {
      
            // Get the extent of the non higlighted text
            String entryTextN = StringTools.toHiragana( entryText.toLowerCase());
            String searchTextN = StringTools.toHiragana( searchText.toLowerCase());
            if((j = entryTextN.indexOf(searchTextN, i)) == -1)
                j = max;

            // Paint the text that isn't highlighted
            g.setColor(normal);
            g.drawString(entryText.substring(i, j), x, y);

            x += fm.charsWidth(text, i, j - i);

            // Paint the text that is highlighted
            if(j < max) {

                i = j;
                j += len;
        
                g.setColor(highlight);
                g.drawString(entryText.substring(i, j), x, y);
                x += fm.charsWidth(text, i, len);
        
            }

  
        } /* for */
    
    }

}
