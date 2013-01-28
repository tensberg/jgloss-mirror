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
import java.awt.GridLayout;
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

    private final JLabel name = new JLabel();
    
    private final JPanel flags = new JPanel(new GridLayout(1, 0, 2, 2));

    private final JLabel description = new JLabel();

    private final HyperlinkLabel homepage = new HyperlinkLabel();
    
    private final HyperlinkLabel license = new HyperlinkLabel();

    private final JLabel copyright = new JLabel();

    private Dictionary dictionary;

    DictionaryPanel() {
        setLayout(new MigLayout());
        
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        add(flags);
        add(name, "wrap");
        add(description, "wrap");
        add(homepage, "wrap");
        add(license, "wrap");
        add(copyright);
    }
    
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
        
        setFlags(dictionary.getLanguages());
        name.setText(dictionary.getName());
        description.setText(getDescriptionForLocale(dictionary.getDescription(), Locale.getDefault()));
        homepage.setHyperlink(dictionary.getHomepage());
        license.setHyperlink(dictionary.getLicense());
        copyright.setText(MessageFormat.format("(c) {0} {1}", dictionary.getCopyright().getYear().getYear(), dictionary.getCopyright().getBy()));
    }
    
    public Dictionary getDictionary() {
        return dictionary;
    }

    private void setFlags(Languages languages) {
        flags.removeAll();

        for (String language : languages.getLanguage()) {
            JLabel languageLabel = new JLabel();
            try {
                languageLabel.setIcon(Icons.getIcon("flags/" + language + ".png"));
            } catch (IllegalArgumentException ex) {
                LOGGER.fine("no icon found for language " + language);
                languageLabel.setText(language);
            }
            
            flags.add(languageLabel);
        }
        
        flags.validate();
    }
}
