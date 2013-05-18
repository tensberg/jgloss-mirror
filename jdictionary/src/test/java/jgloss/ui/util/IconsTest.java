/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.ui.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import javax.swing.ImageIcon;

import org.junit.Test;

public class IconsTest {
    @Test
    public void testGetIcon() {
        ImageIcon icon = Icons.getIcon("jgloss.png");
        assertNotNull(icon);
    }
    
    @Test
    public void testGetIconTwiceReturnsCachedIcon() {
        ImageIcon icon1 = Icons.getIcon("jgloss.png");
        ImageIcon icon2 = Icons.getIcon("jgloss.png");
        assertNotNull(icon1);
        assertSame(icon1, icon2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testThrowExceptionIfIconNotFound() {
        Icons.getIcon("_doesnotexist_.png");
    }
}
