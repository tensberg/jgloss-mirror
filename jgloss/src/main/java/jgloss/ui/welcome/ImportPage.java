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

import static jgloss.JGloss.MESSAGES;
import static jgloss.ui.wizard.WizardNavigation.FORWARD;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jgloss.JGloss;
import jgloss.parser.KanjiParser;
import jgloss.ui.Dictionaries;
import jgloss.ui.gloss.DocumentActions;
import jgloss.ui.gloss.ImportStringStrategy;
import jgloss.ui.gloss.JGlossFrame;
import jgloss.ui.wizard.DescriptionLabel;
import jgloss.ui.wizard.WizardNavigation;
import jgloss.ui.wizard.WizardPage;

class ImportPage extends WizardPage {

    private static final long serialVersionUID = 1L;

    private final JGlossFrame targetDocument;

    private final JTextArea importText = new JTextArea();

    public ImportPage(JGlossFrame targetDocument) {
        this.targetDocument = DocumentActions.getFrame(targetDocument);
        setLayout(new BorderLayout());
        add(new DescriptionLabel(MESSAGES.getString("welcome.import.description")), BorderLayout.NORTH);
        add(new JScrollPane(importText), BorderLayout.CENTER);
        importText.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateForwardEnabled();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateForwardEnabled();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateForwardEnabled();
            }
        });
        importText.setText(MESSAGES.getString("welcome.import.example"));
        importText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                importText.selectAll();
            }
        });
        updateForwardEnabled();
    }

    @Override
    public String getTitle() {
        return JGloss.MESSAGES.getString("welcome.import.title");
    }

    @Override
    public void navigate(WizardNavigation navigation) {
        if (navigation == FORWARD) {
            new ImportStringStrategy(targetDocument, importText.getText(), true, null, new KanjiParser(Dictionaries
                            .getInstance().getDictionaries(), Collections.<String> emptySet())).executeImport();
        }
    }

    private void updateForwardEnabled() {
        setForwardEnabled(!importText.getText().isEmpty());
    }
}
