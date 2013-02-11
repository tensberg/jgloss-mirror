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

import static java.awt.BorderLayout.LINE_END;
import static java.awt.BorderLayout.LINE_START;
import static java.awt.FlowLayout.LEADING;
import static java.awt.FlowLayout.TRAILING;
import static jgloss.JGloss.MESSAGES;
import static jgloss.ui.util.SwingWorkerProgressFeedback.showProgress;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jgloss.ui.Dictionaries;
import jgloss.ui.download.schema.Dictionary;
import jgloss.ui.util.UIUtilities;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog where you can select dictionaries which are downloaded and installed.
 */
public class DictionaryDownloadDialog extends JDialog {

    private static final LC LC_FILL_AND_WRAP = new LC().fillX().wrapAfter(1).gridGap("0", "0");

    private static final CC CC_GROWX = new CC().growX();

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(DictionaryDownloadDialog.class.getPackage().getName());

    private final JPanel dictionariesPanel = new JPanel();

    private final ChooseDownloadDirectoryAction chooseDownloadDirAction = new ChooseDownloadDirectoryAction(this);

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
        setLayout(new BorderLayout());

        dictionariesPanel.setLayout(new MigLayout(LC_FILL_AND_WRAP));
        JScrollPane dictionariesScroller = new JScrollPane(dictionariesPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dictionariesScroller.getVerticalScrollBar().setUnitIncrement(40);
        dictionariesScroller.getVerticalScrollBar().setBlockIncrement(40);

        JPanel buttonsPanel = new JPanel(new BorderLayout());
        JPanel leftButtonPanel = new JPanel(new FlowLayout(LEADING));
        buttonsPanel.add(leftButtonPanel, LINE_START);
        leftButtonPanel.add(new JLabel(MESSAGES.getString("dictionarydownload.downloaddir", Dictionaries.getDictionariesDir().getAbsolutePath())));
        leftButtonPanel.add(new JButton(chooseDownloadDirAction));
        JPanel rightButtonPanel = new JPanel(new FlowLayout(TRAILING));
        rightButtonPanel.add(new JButton(closeAction));
        buttonsPanel.add(rightButtonPanel, LINE_END);

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
            JPanel dictionaryDownloadPanel = new JPanel(new MigLayout(LC_FILL_AND_WRAP));
            dictionaryDownloadPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            dictionaryDownloadPanel.add(new DictionaryPanel(dictionary));
            dictionaryDownloadPanel.add(new DownloadPanel(dictionary, Dictionaries.getInstance()), CC_GROWX);
            dictionariesPanel.add(dictionaryDownloadPanel, CC_GROWX);
        }
        dictionariesPanel.revalidate();
    }
}
