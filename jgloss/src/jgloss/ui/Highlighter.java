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
 * $Id$
 */
package jgloss.ui;

import java.awt.*;

public interface Highlighter {

  /**
   * Paint the given text, highlighting the portion that appears in
   * the match string.
   *
   * @param g Graphics object to paint on
   * @param text text to paint  
   */
  public void paintHighlight(Graphics g, String entryText, String searchText);

}