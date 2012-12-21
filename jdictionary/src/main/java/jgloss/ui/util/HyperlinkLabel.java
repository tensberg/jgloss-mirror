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

import static java.awt.Cursor.HAND_CURSOR;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.swing.JTextField;

/**
 * Label which shows a hyperlink which will be opened in the default browser on click.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
public class HyperlinkLabel extends JTextField {
    
    private class BrowseHyperlinkListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Desktop.getDesktop().browse(hyperlink);
            } catch (IOException ex) {
                LOGGER.log(SEVERE, "could not open hyperlink " + hyperlink + " for browsing");
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(HyperlinkLabel.class.getPackage().getName());
    
    private URI hyperlink;

    public HyperlinkLabel() {
        setEditable(false);
        addActionListener(new BrowseHyperlinkListener());
        setCursor(Cursor.getPredefinedCursor(HAND_CURSOR));
    }
    
    public HyperlinkLabel(String hyperlink) {
        this();
        setHyperlink(hyperlink);
    }
    
    public void setHyperlink(String hyperlink) {
        this.hyperlink = null;
        setText(null);
        setEnabled(false);
        
        if (hyperlink != null) {
            setText(toHtmlLink(hyperlink));
            try {
                this.hyperlink = new URI(hyperlink);
                setEnabled(true);
            } catch (URISyntaxException ex) {
                LOGGER.log(WARNING, hyperlink + " is not a valid URI", ex);
            }
        }
    }

    private static String toHtmlLink(String link) {
        return "<a href=\"" + link + "\">" + link + "</a>";
    }
}
