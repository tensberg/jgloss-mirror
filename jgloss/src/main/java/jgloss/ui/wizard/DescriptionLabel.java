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

import javax.swing.JLabel;

/**
 * Label for HTML text with a standardized style. Typically used in wizard
 * pages.
 */
public class DescriptionLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final String TEXT_HEADER = "<html><head><style>p { padding-bottom: 10pt; }</style></head><body>";

    private static final String TEXT_FOOTER = "</body></html>";

    public DescriptionLabel() {
        this(null);
    }

    public DescriptionLabel(String text) {
        setBorder(WizardPage.EMPTY_BORDER);
        setStyledText(text);
    }

    /**
     * Adds HTML style information to the text and sets it on the label.
     *
     * @param text
     *            Text to show on the label. Must only contain the body part of
     *            a HTML document.
     */
    @Override
    public void setText(String text) {
        setStyledText(text);
    }

    private void setStyledText(String text) {
        super.setText(addStyle(text));
    }

    private String addStyle(String text) {
        String styledText;

        if (text == null) {
            styledText = null;
        } else {
            styledText = TEXT_HEADER + text + TEXT_FOOTER;
        }

        return styledText;
    }
}
