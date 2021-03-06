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

import static java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE;
import static jgloss.JGloss.MESSAGES;
import static jgloss.Preferences.SHOW_WELCOME_DIALOG;

import java.awt.Window;

import javax.swing.JDialog;

import jgloss.JGloss;
import jgloss.ui.gloss.JGlossFrame;
import jgloss.ui.wizard.Wizard;
import jgloss.ui.wizard.WizardListener;
import jgloss.ui.wizard.WizardPage;

/**
 * Dialog which contains the welcome wizard shown when first starting JGloss.
 */
public class WelcomeDialog extends JDialog implements WizardListener {

    private static final long serialVersionUID = 1L;

    private final Wizard welcomeWizard;

    public static void showWelcomeDialog(JGlossFrame target, Window parentFrame) {
        WelcomeDialog welcomeDialog = new WelcomeDialog(target, parentFrame);
        welcomeDialog.pack();
        welcomeDialog.setSize(800, 500);
        welcomeDialog.setLocationRelativeTo(null);
        welcomeDialog.setVisible(true);
    }

    public WelcomeDialog(JGlossFrame targetDocument, Window parent) {
        super(parent);
        setTitle(MESSAGES.getString("welcome.title"));
        setModalExclusionType(APPLICATION_EXCLUDE);

        welcomeWizard = new Wizard(new WizardPage[] {
                        new WelcomePage(),
                        new DictionaryDownloadPage(),
                        new ImportPage(targetDocument),
                        new FinishPage()
                    });

        setContentPane(welcomeWizard.getWizardPanel());
        welcomeWizard.addWizardListener(this);
        welcomeWizard.setDontShowAgain(!JGloss.PREFS.getBoolean(SHOW_WELCOME_DIALOG, true));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Override
    public void wizardCancelled() {
        close();
    }

    @Override
    public void wizardClosed() {
        close();
    }

    private void close() {
        setVisible(false);
        dispose();
        JGloss.PREFS.set(SHOW_WELCOME_DIALOG, !welcomeWizard.isDontShowAgain());
    }
}
