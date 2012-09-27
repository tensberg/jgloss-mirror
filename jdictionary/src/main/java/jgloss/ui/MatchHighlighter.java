/**
 *   Copyright (C) 2002  Eric Crahen <crahen@cse.buffalo.edu>
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
 */
package jgloss.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class MatchHighlighter implements Highlighter {

  private char[] match;
  private char[] text;

  /**
   * Paint the given text, highlighting the portion that appears in
   * the match string.
   *
   * @param g Graphics object to paint on
   * @param text text to paint  
   */
  @Override
public void paintHighlight(Graphics g, String entryText, String searchText) {
    
    // Allocate a buffer for the entry text
    if(text == null || text.length < entryText.length()) {
	    text = new char[entryText.length()*2];
    }

    // Allocate a buffer for the search text
    if(match == null || match.length < searchText.length()) {
	    match = new char[searchText.length()*2];
    }

    entryText.getChars(0, entryText.length(), text, 0);
    searchText.getChars(0, searchText.length(), match, 0);
    
    FontMetrics fm = g.getFontMetrics();

    Color normal = Color.blue;
    Color highlight = Color.red;

    int max = Math.min(searchText.length(), entryText.length());
  
    int x = 0;
    int y = fm.getAscent();

    for(int begin = 0, end = begin; begin < max; begin = end) {
               
      // Find the start of a region that matches 
      while(begin < max && text[begin] != match[begin]) {
	    begin++;
    }
      
      // Paint the text that isn't highlighted
      if(end < begin) {

        g.setColor(normal);
        g.drawString(entryText.substring(end, begin), x, y);

        x += fm.charsWidth(text, end, begin-end);
        
      }

      // Find the end of that region
      for(end = begin; end < max && text[end] == match[end]; end++) {
	    ;
    }
      
      // Paint the text that is highlighted
      g.setColor(highlight);
      g.drawString(entryText.substring(begin, end), x, y);
      
      x += fm.charsWidth(text, begin, end-begin);
     
    }

    // Paint the trailing text that was not highlighted
    g.setColor(normal);
    g.drawString(entryText.substring(max), x, y);    

  }

}
