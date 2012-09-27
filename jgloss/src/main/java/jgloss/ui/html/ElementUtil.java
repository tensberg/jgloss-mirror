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

package jgloss.ui.html;

import javax.swing.text.Element;

/**
 * Static utility methods for {@code javax.swing.text.Element}.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
public class ElementUtil {
    
	/**
	 * Dump the element and its children to System.out. Useful for debugging.
	 */
	public static void dump(Element elem) {
		if (elem.isLeaf()) {
			System.out.println(TextElement.getTextFromElement(elem));
		} else {
			System.out.println("<" + elem.getName() + ">");
			for (int i=0; i<elem.getElementCount(); i++) {
				dump(elem.getElement(i));
			}
			System.out.println("</" + elem.getName() + ">");
		}
	}
	
	private ElementUtil() {
	}
}
