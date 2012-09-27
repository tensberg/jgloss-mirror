/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.util;

import java.awt.Font;

/**
 * Helper methods for working with fonts.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public class FontUtilities {
    private static final String DEFAULT_FONT = "Dialog";
    /**
     * Four japanese characters used to test the fonts.
     */
    private static final String JAPANESE_CHARS_TEST = "A\u3042\u30a2\u660e\u3002";

    /**
     * Check if the font with the given name can display Japanese characters.
     * 
     * @param fontName Name of the font to test. The {@link Font.PLAIN PLAIN} variant will be tested.
     */
    public static boolean canDisplayJapanese( String fontName) {
        Font font = new Font(fontName, Font.PLAIN, 1);
        
        if (fontIsSustitutedWithDefault(fontName, font)) {
            return false;
        }
        
        return canDisplayJapanese(font);
    }

    /**
     * Test if the font with the given name exists or if it is replaced by the default
     * font as documented by the {@link Font#Font(String, int, int) Font} constructor.
     * 
     * @param fontName Name from which the font object is constructed.
     * @param font Font object which was created from the {@code fontName}.
     * @return <code>true</code> if the font with the given name is available, <code>false</code>
     *        if it was substituted with the default font.
     */
    static boolean fontIsSustitutedWithDefault(String fontName, Font font) {
        return DEFAULT_FONT.equalsIgnoreCase(font.getFamily()) && fontName.equals(font.getName()) &&
                !DEFAULT_FONT.equalsIgnoreCase(fontName);
    }
    
    /**
     * Check if the given font can display Japanese characters.
     * 
     * @param font The font to test.
     */
    public static boolean canDisplayJapanese(Font font) {
        int length = font.canDisplayUpTo( JAPANESE_CHARS_TEST);
        return (length == -1 || // all chars succeeded (according to canDisplayUpTo specification)
                length == JAPANESE_CHARS_TEST.length()); // all chars succeeded (behavior in Java 1.3)
    }

    
    private FontUtilities() {
    }
}
