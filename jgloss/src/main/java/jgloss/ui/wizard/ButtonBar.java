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

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import jgloss.ui.util.UIUtilities;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * Container for the wizard navigation buttons and controlling actions.
 */
class ButtonBar extends JPanel {
    /**
     * Base class for navigating wizard pages.
     */
    class NavigationAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        private final WizardNavigation navigation;

        NavigationAction(WizardNavigation navigation, String key) {
            this.navigation = navigation;
            UIUtilities.initAction(this, key);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireNavigate(navigation);
        }

    }

    private static final long serialVersionUID = 1L;

    private final List<WizardNavigationListener> listeners = new CopyOnWriteArrayList<>();

    private final Action cancelAction = new NavigationAction(WizardNavigation.CANCEL, "wizard.action.cancel");

    private final JButton cancelButton = new JButton(cancelAction);

    private final Action forwardAction = new NavigationAction(WizardNavigation.FORWARD, "wizard.action.forward");

    private final JButton forwardButton = new JButton(forwardAction);

    private final Action backAction = new NavigationAction(WizardNavigation.BACK, "wizard.action.back");

    private final JButton backButton = new JButton(backAction);

    ButtonBar() {
        setLayout(new MigLayout(new LC().fillX()));
        add(cancelButton, "dock west");
        add(forwardButton, "dock east");
        add(backButton, "dock east");
    }

    public void addNavigationListener(WizardNavigationListener listener) {
        listeners.add(listener);
    }

    public void removeNavigationListener(WizardNavigationListener listener) {
        listeners.remove(listener);
    }

    private void fireNavigate(WizardNavigation navigation) {
        for (WizardNavigationListener listener : listeners) {
            listener.navigate(navigation);
        }
    }

    void setNavigationVisible(WizardNavigation navigation, boolean visible) {
        JButton button;

        switch (navigation) {
        case BACK:
            button = backButton;
            break;

        case FORWARD:
            button = forwardButton;
            break;

        case CANCEL:
            button = cancelButton;
            break;

        default:
            throw new IllegalArgumentException(navigation.toString());
        }

        button.setVisible(visible);
    }

    void setNavigationEnabled(WizardNavigation navigation, boolean enabled) {
        getAction(navigation).setEnabled(enabled);
    }

    void setNavigationText(WizardNavigation navigation, String text) {
        getAction(navigation).putValue(Action.NAME, text);
    }

    private Action getAction(WizardNavigation navigation) {
        switch (navigation) {
        case BACK:
            return backAction;

        case FORWARD:
            return forwardAction;

        case CANCEL:
            return cancelAction;

        default:
            throw new IllegalArgumentException(navigation.toString());
        }
    }
}
