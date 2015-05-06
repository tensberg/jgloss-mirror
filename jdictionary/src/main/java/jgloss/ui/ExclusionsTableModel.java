/*
 * Copyright (C) 2002-2015 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import javax.swing.table.AbstractTableModel;

/**
 *
 */
public class ExclusionsTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private static final int COLUMNS = 2;

    private static final int IDX_ENABLED = 0;

    private static final int IDX_NAME = 1;

    private final Exclusions exclusions = new Exclusions();

    private boolean modified = false;

    @Override
    public int getRowCount() {
        return exclusions.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ExclusionResource resource = exclusions.getExclusionResource(rowIndex);
        switch (columnIndex) {
        case IDX_ENABLED:
            return resource.isEnabled();

        case IDX_NAME:
            return resource.getName();

        default:
            throw new IllegalArgumentException("invalid column " + columnIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return exclusions.getExclusionResource(rowIndex).isEditable();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        ExclusionResource resource = exclusions.getExclusionResource(rowIndex);
        switch (columnIndex) {
        case IDX_ENABLED:
            resource.setEnabled((Boolean) value);
            break;

        case IDX_NAME:
            resource.setName(String.valueOf(value));
            break;

        default:
            throw new IllegalArgumentException("invalid column " + columnIndex);
        }

        modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case IDX_ENABLED:
            return Boolean.class;

        case IDX_NAME:
            return String.class;

        default:
            throw new IllegalArgumentException("invalid column " + columnIndex);
        }
    }
}
