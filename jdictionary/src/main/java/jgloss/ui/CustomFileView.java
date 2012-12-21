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

package jgloss.ui;

import java.io.File;

import javax.swing.Icon;
import javax.swing.filechooser.FileView;

import jgloss.JGloss;
import jgloss.ui.util.Icons;

/**
 * File view which adds icons and descriptions for filetypes used by JGloss.
 *
 * @author Michael Koch
 */
public class CustomFileView extends FileView {
    protected static CustomFileView FILE_VIEW;

    /**
     * Returns the unique instance of the custom file view which can be
     * used with JFileChooser instances.
     */
    public static FileView getFileView() {
        if (FILE_VIEW == null) {
	        FILE_VIEW = new CustomFileView();
        }
        return FILE_VIEW;
    }

    protected static String JGLOSS_DESCRIPTION;
    protected static String XML_DESCRIPTION;
    protected static String HTML_DESCRIPTION;
    protected static String TEX_DESCRIPTION;
    protected static String TEXT_DESCRIPTION;
    protected static String TEMPLATE_DESCRIPTION;

    protected static Icon JGLOSS_ICON;
    protected static Icon XML_ICON;
    protected static Icon HTML_ICON;
    protected static Icon TEX_ICON;
    protected static Icon TEXT_ICON;
    protected static Icon TEMPLATE_ICON;

    protected CustomFileView() {
        if (JGLOSS_DESCRIPTION == null) {
            // initialize static members
            JGLOSS_DESCRIPTION = JGloss.MESSAGES.getString( "fileview.description.jgloss");
            XML_DESCRIPTION = JGloss.MESSAGES.getString( "fileview.description.xml");
            HTML_DESCRIPTION = JGloss.MESSAGES.getString( "fileview.description.html");
            TEX_DESCRIPTION = JGloss.MESSAGES.getString( "fileview.description.tex");
            TEXT_DESCRIPTION = JGloss.MESSAGES.getString( "fileview.description.text");
            TEMPLATE_DESCRIPTION = JGloss.MESSAGES.getString( "fileview.description.template");

            JGLOSS_ICON = Icons.getIcon("jgloss.png");
            HTML_ICON = Icons.getIcon("html.png");
            XML_ICON = HTML_ICON; // no individual icon for now
            TEX_ICON = Icons.getIcon("tex.png");
            TEXT_ICON = Icons.getIcon("txt.png");
            TEMPLATE_ICON = Icons.getIcon("template.png");
        }
    }

    @Override
	public String getTypeDescription( File f) {
        String name = f.getName().toLowerCase();
        String desc = null;
        if (name.endsWith( ".jgloss")) {
	        desc = JGLOSS_DESCRIPTION;
        } else if (name.endsWith( ".xml")) {
	        desc = XML_DESCRIPTION;
        } else if (name.endsWith( ".htm") || name.endsWith( ".html")) {
	        desc = HTML_DESCRIPTION;
        } else if (name.endsWith( ".tex")) {
	        desc = TEX_DESCRIPTION;
        } else if (name.endsWith( ".txt")) {
	        desc = TEXT_DESCRIPTION;
        } else if (name.endsWith( ".tmpl")) {
	        desc = TEMPLATE_DESCRIPTION;
        }
        
        // see getIcon() for discussion of f.isFile()
        if (desc!=null && f.isFile()) {
	        return desc;
        }

        return null; // let L&F file view determine the description
    }

    @Override
	public Icon getIcon( File f) {
        String name = f.getName().toLowerCase();
        Icon icon = null;
        if (name.endsWith( ".jgloss")) {
	        icon = JGLOSS_ICON;
        } else if (name.endsWith( ".xml")) {
	        icon = XML_ICON;
        } else if (name.endsWith( ".htm") || name.endsWith( ".html")) {
	        icon = HTML_ICON;
        } else if (name.endsWith( ".tex")) {
	        icon = TEX_ICON;
        } else if (name.endsWith( ".txt")) {
	        icon = TEXT_ICON;
        } else if (name.endsWith( ".tmpl")) {
	        icon = TEMPLATE_ICON;
        }
        
        // bug #581152
        // Only show icons for files, not directories. The f.isFile() test should be done first
        // into the method, but it has the side effect of triggering warning dialogs for missing
        // disks in drive a: when used with JRE1.3.1 and Windows, so only do the tests for filesystem
        // items with known extensions.
        if (icon!=null && f.isFile()) {
	        return icon;
        }

        return null; // let L&F file view determine the icon
    }
} // class CustomFileView
