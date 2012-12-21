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

package jgloss.ui.download;

import static jgloss.ui.download.DictionarySchemaUtils.getDescriptionForLocale;

import java.awt.Font;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jgloss.ui.download.schema.Dictionary;
import jgloss.ui.download.schema.Dictionary.Languages;
import jgloss.ui.util.HyperlinkLabel;
import jgloss.ui.util.Icons;
import net.miginfocom.swing.MigLayout;

/**
 * Shows the dictionary information of one dictionary from the dictionaries xml file.
 * 
 * @see Dictionary
 */
class DictionaryPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    
    private static final Logger LOGGER = Logger.getLogger(DictionaryPanel.class.getPackage().getName());

    DictionaryPanel(Dictionary dictionary) {
        setLayout(new MigLayout());
        
        JLabel name = new JLabel(dictionary.getName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        addFlags(dictionary.getLanguages());
        add(name, "wrap");
        add(new JLabel(getDescriptionForLocale(dictionary.getDescription(), Locale.getDefault())), "wrap");
        add(new HyperlinkLabel(dictionary.getHomepage()), "wrap");
        add(new HyperlinkLabel(dictionary.getLicense()), "wrap");
        add(new JLabel(MessageFormat.format("(c) {0} {1}", dictionary.getCopyright().getYear().getYear(), dictionary.getCopyright().getBy())));
    }

    private void addFlags(Languages languages) {
        boolean firstFlag = true;
        
        for (String language : languages.getLanguage()) {
            JLabel languageLabel = new JLabel();
            try {
                languageLabel.setIcon(Icons.getIcon("flags/" + language + ".png"));
            } catch (IllegalArgumentException ex) {
                LOGGER.fine("no icon found for language " + language);
                languageLabel.setText(language);
            }
            
            add(languageLabel, firstFlag ? "split" : null);
            firstFlag = false;
        }
    }
}
