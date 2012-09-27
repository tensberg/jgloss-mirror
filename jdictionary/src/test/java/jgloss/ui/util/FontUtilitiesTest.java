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

import static jgloss.ui.util.FontUtilities.canDisplayJapanese;
import static jgloss.ui.util.FontUtilities.fontIsSustitutedWithDefault;
import static org.fest.assertions.Assertions.assertThat;

import java.awt.Font;

import org.junit.BeforeClass;
import org.junit.Test;

public class FontUtilitiesTest {
    /**
     * Name of an available font which can display Japanese characters. The value is
     * initialized from the system property {@code japanesefont.name}, which is set as Maven property
     * in the jgloss-parent POM. If unset, a default value is used.
     */
    private static final String JAPANESE_FONT_NAME = initFromSystemProperty("japanesefont.name", "TakaoMincho");

    /**
     * Name of an available font which cannot display Japanese characters. The value is
     * initialized to a default value but can be overridden by setting the system property
     * {@code nonjapanesefont.name} (which is not configured in the POM). 
     */
    private static final String NON_JAPANESE_FONT_NAME = initFromSystemProperty("nonjapanesefont.name", "Symbol");

    private static String initFromSystemProperty(String propertyName, String defaultValue) {
        String value = System.getProperty(propertyName);
        
        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }
        
        return value;
    }
    
    @BeforeClass
    public static void checkTestSetup() {
        // note that these tests may also fail if fontIsSubstituedWithDefault is broken
        assertFontAvailable(JAPANESE_FONT_NAME);
        assertFontAvailable(NON_JAPANESE_FONT_NAME);
    }

    private static void assertFontAvailable(String fontName) {
        assertThat(fontIsSustitutedWithDefault(fontName, new Font(fontName, Font.PLAIN, 1)))
            .overridingErrorMessage(fontName + " is not available, please configure an available font")
            .isFalse();
    }
    
    @Test
    public void testCanDisplayJapanese() {
        assertFontAvailable(JAPANESE_FONT_NAME);
        assertFontAvailable(NON_JAPANESE_FONT_NAME);
        assertThat(canDisplayJapanese(JAPANESE_FONT_NAME)).isTrue();
        assertThat(canDisplayJapanese(NON_JAPANESE_FONT_NAME)).isFalse();
    }
    
    @Test
    public void testFontIsSustitutedWithDefault() {
        assertThat(fontIsSustitutedWithDefault("Serif", new Font("Serif", Font.PLAIN, 1))).isFalse();
        assertThat(fontIsSustitutedWithDefault("Dialog", new Font("Dialog", Font.PLAIN, 1))).isFalse();
        assertThat(fontIsSustitutedWithDefault("UnavailableFont", new Font("UnavailableFont", Font.PLAIN, 1))).isTrue();
    }
}
