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

import static java.util.logging.Level.SEVERE;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import jgloss.JGloss;

/**
 * Action which opens a hyperlink in an external browser. The hyperlink is
 * configured in the resources and can therefore be changed depending on the
 * locale.
 */
public class HyperlinkAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(HyperlinkAction.class.getPackage().getName());

    private final URI hyperlink;

    /**
     * Creates an action configured from the {@link JGloss#MESSAGES} with the
     * given key. The URL key is {@code key + ".url"}.
     *
     * @see UIUtilities#initAction(javax.swing.Action, String, Object...)
     */
    public HyperlinkAction(String key) {
        UIUtilities.initAction(this, key);
        hyperlink = URI.create(JGloss.MESSAGES.getString(key + ".url"));
        checkSetEnabled();
    }

    private void checkSetEnabled() {
        boolean browseSupported = Desktop.getDesktop().isSupported(Action.BROWSE);
        setEnabled(browseSupported);
        if (!browseSupported) {
            LOGGER.warning("browsing hyperlinks is not supported in this installation");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(hyperlink);
        } catch (IOException ex) {
            LOGGER.log(SEVERE, "could not open hyperlink " + hyperlink + " for browsing");
        }
    }

}
