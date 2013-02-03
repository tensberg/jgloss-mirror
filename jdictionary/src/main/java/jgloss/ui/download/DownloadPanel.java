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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;

import jgloss.ui.download.schema.Dictionary;
import jgloss.ui.util.SwingWorkerProgressFeedback;
import jgloss.ui.util.UIUtilities;

/**
 * Show the download controls for a given dictionary.
 */
class DownloadPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private class DownloadAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        private final Dictionary dictionary;

        public DownloadAction(Dictionary dictionary) {
            this.dictionary = dictionary;
            UIUtilities.initAction(this, "downloadpanel.download");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DictionaryDownloader downloader = new DictionaryDownloader(dictionary);
            SwingWorkerProgressFeedback.showProgress(downloader, DownloadPanel.this);
            downloader.execute();
        }

    }

    public DownloadPanel(Dictionary dictionary) {
        add(new JButton(new DownloadAction(dictionary)));
    }

}
