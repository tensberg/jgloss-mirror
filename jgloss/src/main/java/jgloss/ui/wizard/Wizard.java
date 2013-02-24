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

package jgloss.ui.wizard;

import static jgloss.JGloss.MESSAGES;
import static jgloss.ui.wizard.WizardNavigation.BACK;
import static jgloss.ui.wizard.WizardNavigation.FORWARD;
import static jgloss.ui.wizard.WizardPage.FORWARD_ENABLED_PROPERTY;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;

/**
 * Controls the navigation between wizard pages.
 */
public class Wizard {

    private final class Navigation implements WizardNavigationListener {
        @Override
        public void navigate(WizardNavigation navigation) {
            switch (navigation) {
            case BACK:
                goBack();
                break;

            case FORWARD:
                goForward();
                break;

            case CANCEL:
                cancelWizard();
                break;
            }
        }
    }

    private final WizardPage[] pages;

    private int currentPageIndex;

    private WizardPage currentPage;

    private final ButtonBar buttons = new ButtonBar();

    private final WizardPageContainer container = new WizardPageContainer(buttons);

    private final List<WizardListener> listeners = new CopyOnWriteArrayList<>();

    private final PropertyChangeListener forwardEnabledChangeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            forwardEnabledChanged(((Boolean) evt.getNewValue()).booleanValue());
        }
    };

    public Wizard(WizardPage[] pages) {
        this.pages = pages;
        buttons.addNavigationListener(new Navigation());
        showCurrentPage();
    }

    public void addWizardListener(WizardListener listener) {
        listeners.add(listener);
    }

    public void removeWizardListener(WizardListener listener) {
        listeners.remove(listener);
    }

    public JPanel getWizardPanel() {
        return container;
    }

    private void goBack() {
        currentPageIndex--;
        showCurrentPage();
    }

    private void goForward() {
        if (isLastPage(currentPageIndex)) {
            closeWizard();
        } else {
            currentPageIndex++;
            showCurrentPage();
        }
    }

    private void cancelWizard() {
        for (WizardListener listener : listeners) {
            listener.wizardCancelled();
        }
    }

    private void closeWizard() {
        for (WizardListener listener : listeners) {
            listener.wizardClosed();
        }
    }

    private void showCurrentPage() {
        if (currentPage != null) {
            currentPage.removePropertyChangeListener(FORWARD_ENABLED_PROPERTY, forwardEnabledChangeListener);
            buttons.removeNavigationListener(currentPage);
        }

        currentPage = pages[currentPageIndex];
        container.setPage(currentPage);
        buttons.setNavigationEnabled(FORWARD, currentPage.isForwardEnabled());
        buttons.setNavigationText(FORWARD, MESSAGES.getString(isLastPage(currentPageIndex) ? "wizard.action.close" : "wizard.action.forward"));
        buttons.setNavigationVisible(BACK, !isFirstPage(currentPageIndex));
        currentPage.addPropertyChangeListener(FORWARD_ENABLED_PROPERTY, forwardEnabledChangeListener);
        buttons.addNavigationListener(currentPage);
    }

    private boolean isLastPage(int pageIndex) {
        return pageIndex == pages.length - 1;
    }

    private boolean isFirstPage(int pageIndex) {
        return pageIndex == 0;
    }

    private void forwardEnabledChanged(boolean enabled) {
        buttons.setNavigationEnabled(FORWARD, enabled);
    }

}
