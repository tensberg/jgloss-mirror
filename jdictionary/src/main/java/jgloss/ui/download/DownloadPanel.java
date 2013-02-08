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

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.LINE_START;
import static javax.swing.SwingWorker.StateValue.DONE;
import static jgloss.ui.util.SwingWorkerProgressFeedback.PROGRESS_PROPERTY;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import jgloss.JGloss;
import jgloss.ui.Dictionaries;
import jgloss.ui.DictionaryListChangeListener;
import jgloss.ui.download.schema.Dictionary;
import jgloss.ui.util.ShowingChangedAdapter;
import jgloss.ui.util.SwingWorkerProgressFeedback;
import jgloss.ui.util.UIUtilities;

/**
 * Show the download controls for a given dictionary.
 */
class DownloadPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JProgressBar progressBar = new JProgressBar();

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
            showProgress(downloader);
            downloader.execute();
        }

    }

    private final PropertyChangeListener progressUpdateListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            DictionaryDownloader source = (DictionaryDownloader) evt.getSource();
            switch (evt.getPropertyName()) {
            case PROGRESS_PROPERTY:
                updateProgress(source);
                break;

            case SwingWorkerProgressFeedback.STATE_PROPERTY:
                updateState(source);
                break;
            }
        }
    };

    private final DictionaryListChangeListener dictionaryChangeListener = new DictionaryListChangeListener() {

        @Override
        public void dictionaryListChanged() {
            updateControls(dictionary, dictionaries);
        }
    };

    private final Dictionary dictionary;

    private final Dictionaries dictionaries;

    public DownloadPanel(final Dictionary dictionary, final Dictionaries dictionaries) {
        setLayout(new BorderLayout());

        this.dictionary = dictionary;
        this.dictionaries = dictionaries;

        addHierarchyListener(new ShowingChangedAdapter() {
            @Override
            protected void componentShown(HierarchyEvent event) {
                dictionaries.addDictionaryListChangeListener(dictionaryChangeListener);
                updateControls(dictionary, dictionaries);
            }

            @Override
            protected void componentHidden(HierarchyEvent event) {
                dictionaries.removeDictionaryListChangeListener(dictionaryChangeListener);
            }
        });
    }

    private void updateControls(Dictionary dictionary, Dictionaries dictionaries) {
        removeAll();
        if (alreadyInstalled(dictionary, dictionaries)) {
            add(new JLabel(JGloss.MESSAGES.getString("downloadpanel.installed")), LINE_START);
        } else {
            add(new JButton(new DownloadAction(dictionary)), LINE_START);
        }
        revalidate();
    }

    private boolean alreadyInstalled(Dictionary dictionary, Dictionaries dictionaries) {
        String name = dictionary.getDownload().getDictionaryFile();
        for (jgloss.dictionary.Dictionary installedDictionary : dictionaries.getDictionaries()) {
            if (installedDictionary.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void showProgress(DictionaryDownloader downloader) {
        removeAll();
        add(progressBar, CENTER);
        revalidate();
        progressBar.setIndeterminate(true);

        downloader.addPropertyChangeListener(progressUpdateListener);
    }

    private void updateProgress(DictionaryDownloader downloader) {
        progressBar.setIndeterminate(false);
        progressBar.setValue(downloader.getProgress());
    }

    private void updateState(DictionaryDownloader downloader) {
        if (downloader.getState() == DONE) {
            downloadFinished(downloader);
        }
    }

    private void downloadFinished(DictionaryDownloader downloader) {
        downloader.removePropertyChangeListener(progressUpdateListener);
        // TODO: handle download errors
    }

}
