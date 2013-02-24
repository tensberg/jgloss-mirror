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

package jgloss.ui.download;

import static jgloss.JGloss.MESSAGES;

import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;

import jgloss.ui.util.UIUtilities;

/**
 * Dialog where you can select dictionaries which are downloaded and installed.
 */
public class DictionaryDownloadDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Action closeAction = new AbstractAction() {

        private static final long serialVersionUID = 1L;

        {
            UIUtilities.initAction(this, "dictionarydownloaddialog.close");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DictionaryDownloadDialog.this.setVisible(false);
        }
    };

    public DictionaryDownloadDialog(Window parent, URL dictionariesUrl) {
        super(parent);

        setTitle(MESSAGES.getString("dictionarydownload.title"));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLayout(new GridLayout(1, 1));

        DictionaryDownloadPanel downloads = new DictionaryDownloadPanel(dictionariesUrl);
        add(downloads);
        downloads.addButton(new JButton(closeAction));
    }

}
