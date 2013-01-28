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
 */

package jgloss.ui.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * Utility class for getting icons by their filename relative to the default icon resource path.
 * Icons are cached so that they are only loaded once.
 */
public class Icons {
    private static final Map<String, ImageIcon> ICONS = new HashMap<String, ImageIcon>(); 
    
    private static final String ICONS_RESOURCE_FOLDER = "/icons/";

    /**
     * Loads the icon with the given filename from the icons resource folder. The icon is cached the first time it is loaded.
     * 
     * @param name filename of the icon. May be a path relative to the icons resource folder.
     */
    public static ImageIcon getIcon(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        
        ImageIcon icon = ICONS.get(name);
        if (icon == null) {
            icon = loadIcon(name);
            ICONS.put(name, icon);
        }
        
        return icon;
    }
    
    private static ImageIcon loadIcon(String name) {
        URL iconResource = Icons.class.getResource(ICONS_RESOURCE_FOLDER + name);
        if (iconResource == null) {
            throw new IllegalArgumentException("no icon found with name " + name);
        }
        
        return new ImageIcon(iconResource);
    }

    private Icons() {
    }
}
