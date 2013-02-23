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

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.PAGE_END;
import static java.awt.BorderLayout.PAGE_START;
import static java.awt.Font.BOLD;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Container for wizard pages which shows the page in the middle, a title on top
 * and a button bar below.
 */
class WizardPageContainer extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel = new JLabel();

    private WizardPage page;

    WizardPageContainer(ButtonBar buttons) {
        setLayout(new BorderLayout());

        titleLabel.setFont(titleLabel.getFont().deriveFont(BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(titleLabel, PAGE_START);

        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buttons, PAGE_END);
    }

    void setPage(WizardPage page) {
        if (this.page != null) {
            remove(this.page);
        }

        this.page = page;
        setTitle(page.getTitle());
        add(page, CENTER);
        revalidate();
    }

    private void setTitle(String title) {
        titleLabel.setText(title);
    }
}
