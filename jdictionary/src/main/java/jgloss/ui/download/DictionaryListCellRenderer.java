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

package jgloss.ui.download;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jgloss.ui.download.schema.Dictionary;

/**
 * Show dictionary details in the list.
 */
class DictionaryListCellRenderer extends DictionaryPanel implements ListCellRenderer<Dictionary> {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(JList<? extends Dictionary> list, Dictionary value, int index, boolean isSelected, boolean cellHasFocus) {
        setDictionary(value);
        return this;
    }

}
