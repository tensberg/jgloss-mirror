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

import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.HAND_CURSOR;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.swing.JLabel;

/**
 * Label which shows a hyperlink which will be opened in the default browser on click.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
public class HyperlinkLabel extends JLabel {
    
    private class BrowseHyperlinkListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (hyperlink != null) {
                try {
                    Desktop.getDesktop().browse(hyperlink);
                } catch (IOException ex) {
                    LOGGER.log(SEVERE, "could not open hyperlink " + hyperlink + " for browsing");
                }
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(HyperlinkLabel.class.getPackage().getName());
    
    private URI hyperlink;

    public HyperlinkLabel() {
        addMouseListener(new BrowseHyperlinkListener());
    }
    
    public HyperlinkLabel(String hyperlink) {
        this();
        setHyperlink(hyperlink);
    }

    @Override
    public void setText(String text) {
        setHyperlink(null, text);
    }
    
    public void setHyperlink(String hyperlink) {
        setHyperlink(hyperlink, hyperlink);
    }
    
    public void setHyperlink(String hyperlinkLink, String text) {
        URI newHyperlink = null;
        String newText;
        Cursor newCursor;
        
        if (hyperlinkLink != null) {
            try {
                newHyperlink = new URI(hyperlinkLink);
            } catch (URISyntaxException ex) {
                LOGGER.log(WARNING, hyperlinkLink + " is not a valid URI", ex);
            }
        }
        
        if (newHyperlink != null) {
            newCursor = Cursor.getPredefinedCursor(HAND_CURSOR);
            newText = toHtmlLink(hyperlinkLink, text);
        } else {
            newCursor = Cursor.getPredefinedCursor(DEFAULT_CURSOR);
            newText = text;
        }

        setCursor(newCursor);
        super.setText(newText);
        this.hyperlink = newHyperlink;
    }

    private static String toHtmlLink(String link, String text) {
        return "<html><body><a href=\"" + link + "\">" + text + "</a></body></html>";
    }
}
