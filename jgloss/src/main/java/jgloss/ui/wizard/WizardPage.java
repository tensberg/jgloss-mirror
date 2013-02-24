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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Base class for a page which is part of a wizard.
 */
public abstract class WizardPage extends JPanel implements WizardNavigationListener {
    private static final long serialVersionUID = 1L;

    public static final String FORWARD_ENABLED_PROPERTY = "forwardEnabled";

    public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    private boolean forwardEnabled = true;

    public abstract String getTitle();

    public void setForwardEnabled(boolean forwardEnabled) {
        boolean oldForwardEnabled = this.forwardEnabled;
        this.forwardEnabled = forwardEnabled;
        firePropertyChange(FORWARD_ENABLED_PROPERTY, oldForwardEnabled, forwardEnabled);
    }

    public boolean isForwardEnabled() {
        return forwardEnabled;
    }

    /**
     * Called when the current wizard page is closed by the given navigation.
     */
    @Override
    public void navigate(WizardNavigation navigation) {
    }
}
