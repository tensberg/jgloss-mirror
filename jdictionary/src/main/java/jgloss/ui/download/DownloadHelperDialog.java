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

import static jgloss.ui.util.SwingWorkerProgressFeedback.showProgress;

import java.awt.BorderLayout;
import java.awt.Window;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jgloss.JGloss;
import jgloss.ui.download.schema.Dictionary;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog where you can select dictionaries which are downloaded and installed.
 */
public class DownloadHelperDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    
    private static final Logger LOGGER = Logger.getLogger(DownloadHelperDialog.class.getPackage().getName());
   
    private final JPanel dictionariesPanel = new JPanel();

    public DownloadHelperDialog(Window parent, URL dictionariesUrl) {
        super(parent);
        
        setTitle(JGloss.MESSAGES.getString( "downloadhelper.title"));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        
        dictionariesPanel.setLayout(new MigLayout(new LC().wrapAfter(1)));
        JScrollPane dictionariesScroller = new JScrollPane(dictionariesPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dictionariesScroller.getVerticalScrollBar().setUnitIncrement(40);
        dictionariesScroller.getVerticalScrollBar().setBlockIncrement(40);
        add(dictionariesScroller);
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
        }
        dictionariesPanel.validate();
    }
}
