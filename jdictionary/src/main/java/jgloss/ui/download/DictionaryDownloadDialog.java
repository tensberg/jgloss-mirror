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
import static jgloss.ui.util.SwingWorkerProgressFeedback.showProgress;

import java.awt.BorderLayout;
import java.awt.Window;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jgloss.ui.Dictionaries;
import jgloss.ui.download.schema.Dictionary;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog where you can select dictionaries which are downloaded and installed.
 */
public class DictionaryDownloadDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(DictionaryDownloadDialog.class.getPackage().getName());

    private final JPanel dictionariesPanel = new JPanel();

    private final JPanel buttonsPanel = new JPanel();

    private final ChooseDownloadDirectoryAction chooseDownloadDirAction = new ChooseDownloadDirectoryAction(this);

    public DictionaryDownloadDialog(Window parent, URL dictionariesUrl) {
        super(parent);

        setTitle(MESSAGES.getString("dictionarydownload.title"));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());

        dictionariesPanel.setLayout(new MigLayout(new LC().fillX().wrapAfter(1)));
        JScrollPane dictionariesScroller = new JScrollPane(dictionariesPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dictionariesScroller.getVerticalScrollBar().setUnitIncrement(40);
        dictionariesScroller.getVerticalScrollBar().setBlockIncrement(40);

        buttonsPanel.add(new JButton(chooseDownloadDirAction));

        add(dictionariesScroller, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.PAGE_END);

        loadDictionaries(dictionariesUrl);
    }

    private void loadDictionaries(URL dictionariesUrl) {
        DictionaryListLoader dictionaryListLoader = new DictionaryListLoader(dictionariesUrl) {
            @Override
            protected void done() {
                try {
                    showDictionaries(get().getDictionary());
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "failed to download dictionaries list", ex);
                }
            }
        };
        showProgress(dictionaryListLoader, this);
        dictionaryListLoader.execute();
    }

    private void showDictionaries(List<Dictionary> dictionaries) {
        for (Dictionary dictionary : dictionaries) {
            dictionariesPanel.add(new DictionaryPanel(dictionary));
            dictionariesPanel.add(new DownloadPanel(dictionary, Dictionaries.getInstance()), new CC().growX());
        }
        dictionariesPanel.revalidate();
    }
}
