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

package jgloss.ui.welcome;

import java.awt.BorderLayout;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;

import jgloss.JGloss;
import jgloss.ui.Dictionaries;
import jgloss.ui.DictionaryListChangeListener;
import jgloss.ui.download.DictionaryDownloadPanel;
import jgloss.ui.wizard.WizardPage;

class DictionaryDownloadPage extends WizardPage {

    private static final long serialVersionUID = -8621062357645917541L;

    private static final Logger LOGGER = Logger.getLogger(DictionaryDownloadPage.class.getPackage().getName());

    public DictionaryDownloadPage() {
        setLayout(new BorderLayout());
        try {
            JLabel description = new JLabel(JGloss.MESSAGES.getString("welcome.dictionarydownload.description"));
            description.setBorder(EMPTY_BORDER);
            add(description, BorderLayout.PAGE_START);
            add(new DictionaryDownloadPanel(Dictionaries.getDictionariesUrl()), BorderLayout.CENTER);
            updateForwardEnabled();
            Dictionaries.getInstance().addDictionaryListChangeListener(new DictionaryListChangeListener() {

                @Override
                public void dictionaryListChanged() {
                    updateForwardEnabled();
                }
            });
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, "failed to parse dictionaries url configuration", ex);
        }
    }

    private void updateForwardEnabled() {
        setForwardEnabled(Dictionaries.getInstance().getDictionaries().length > 0);
    }

    @Override
    public String getTitle() {
        return JGloss.MESSAGES.getString("welcome.dictionarydownload.title");
    }

}
