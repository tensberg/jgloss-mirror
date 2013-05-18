/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.ui;

import static java.util.logging.Level.SEVERE;
import static jgloss.JGloss.MESSAGES;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jgloss.JGloss;
import jgloss.ui.util.FontUtilities;
import jgloss.ui.util.JGlossWorker;

/**
 * Search all fonts for a font which can display Japanese characters.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class FontTester extends JGlossWorker<Font, Void> {
    private static final Logger LOGGER = Logger.getLogger(FontTester.class.getPackage().getName());

    private final StyleDialog styleDialog;

    FontTester(StyleDialog styleDialog) {
        this.styleDialog = styleDialog;
    }

    /**
     * Searches the list of all available fonts for one which can display Japanese characters.
     * Returns the first font found.
     *
     * @return First font found which can display Japanese characters, or <code>null</code>.
     */
    @Override
    protected Font doInBackground() {
        setMessage(MESSAGES.getString("style.autodetect.progress.description"));

        Font japaneseFont = null;
        Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        int i = 0;

        for (Font font : allFonts) {
            setMessage(MESSAGES.getString("style.autodetect.progress.font", font.getName()));

            if (FontUtilities.canDisplayJapanese(font)) {
                japaneseFont = font;
                break;
            }
            i++;
            setProgress(i*100/allFonts.length);
        }

        return japaneseFont;
    }


    @Override
    protected void done() {
        Font font;
        try {
            font = get();
            if (font == null) {
                JOptionPane.showMessageDialog
                (styleDialog,
                        JGloss.MESSAGES.getString( "style.autodetect.nofont"),
                        JGloss.MESSAGES.getString( "style.autodetect.title"),
                        JOptionPane.WARNING_MESSAGE);
            }
            else {
                styleDialog.selectJapaneseFont(font.getName());
            }
        } catch (Exception ex) {
            LOGGER.log(SEVERE, "failed to get Japanese font", ex);
        }
    }
}