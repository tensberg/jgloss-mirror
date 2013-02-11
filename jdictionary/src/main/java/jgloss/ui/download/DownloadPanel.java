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
import static jgloss.JGloss.MESSAGES;
import static jgloss.ui.util.JGlossWorker.MESSAGE_PROPERTY;
import static jgloss.ui.util.SwingWorkerProgressFeedback.PROGRESS_PROPERTY;
import static jgloss.ui.util.SwingWorkerProgressFeedback.STATE_PROPERTY;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

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
            downloader = new DictionaryDownloader(dictionary);
            downloader.addPropertyChangeListener(progressUpdateListener);
            downloader.execute();
            updateControls();
        }

    }

    private final PropertyChangeListener progressUpdateListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            assert evt.getSource() == downloader;

            switch (evt.getPropertyName()) {
            case PROGRESS_PROPERTY:
                updateProgress();
                break;

            case MESSAGE_PROPERTY:
                updateMessage();
                break;

            case STATE_PROPERTY:
                updateState();
                break;
            }
        }
    };

    private final DictionaryListChangeListener dictionaryChangeListener = new DictionaryListChangeListener() {

        @Override
        public void dictionaryListChanged() {
            updateControls();
        }
    };

    private final JProgressBar progressBar = new JProgressBar();

    private final Dictionary dictionary;

    private final Dictionaries dictionaries;

    private DictionaryDownloader downloader;

    public DownloadPanel(Dictionary dictionary, final Dictionaries dictionaries) {
        setLayout(new BorderLayout());

        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);

        this.dictionary = dictionary;
        this.dictionaries = dictionaries;

        addHierarchyListener(new ShowingChangedAdapter() {
            @Override
            protected void componentShown(HierarchyEvent event) {
                dictionaries.addDictionaryListChangeListener(dictionaryChangeListener);
                updateControls();
            }

            @Override
            protected void componentHidden(HierarchyEvent event) {
                dictionaries.removeDictionaryListChangeListener(dictionaryChangeListener);
            }
        });
    }

    private void updateControls() {
        removeAll();

        if (downloader != null && downloader.getState() != DONE) {
            add(progressBar, CENTER);
            updateMessage();
        } else if (alreadyInstalled(dictionary, dictionaries)) {
            add(new JLabel(JGloss.MESSAGES.getString("downloadpanel.installed")), LINE_START);
        } else {
            add(createDownloadButton(), LINE_START);
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

    private void updateProgress() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(downloader.getProgress());
    }

    private void updateMessage() {
        progressBar.setString(downloader.getMessage());
    }

    private void updateState() {
        if (downloader.getState() == DONE) {
            downloadFinished();
        }
    }

    private void downloadFinished() {
        downloader.removePropertyChangeListener(progressUpdateListener);
        try {
            downloader.get();
        } catch (InterruptedException | ExecutionException ex) {
            showDownloadFailed(ex);
        }
    }

    private void showDownloadFailed(Exception ex) {
        removeAll();
        JPanel downloadAndMessage = new JPanel(new FlowLayout());
        downloadAndMessage.add(createDownloadButton());
        downloadAndMessage.add(new JLabel(createErrorMessage(ex)));
        add(downloadAndMessage, LINE_START);
        revalidate();
    }

    private String createErrorMessage(Exception ex) {
        String errorMessage = MESSAGES.getString("downloadpanel.error.generic", dictionary.getDownload().getUrl());

        if (ex instanceof ExecutionException) {
            Throwable cause = ex.getCause();

            if (cause instanceof ConnectException) {
                errorMessage = MESSAGES.getString("downloadpanel.error.connect", dictionary.getDownload().getUrl());
            }
        }

        return errorMessage;
    }

    private JButton createDownloadButton() {
        return new JButton(new DownloadAction(dictionary));
    }

}
