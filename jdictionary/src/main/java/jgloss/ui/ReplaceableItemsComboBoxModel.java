/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * Combo box model in which all items can be replaced at once.
 *
 * @author Michael Koch
 */
class ReplaceableItemsComboBoxModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;
    
	protected List<? extends Object> items;
    protected Object selection;

    public ReplaceableItemsComboBoxModel() {
        this( Collections.emptyList());
    }

    public ReplaceableItemsComboBoxModel( List<? extends Object> _items) {
        if (!(_items instanceof RandomAccess)) {
	        _items = new ArrayList<Object>( _items);
        }
        items = _items;
    }

    @Override
	public Object getSelectedItem() { return selection; }

    @Override
	public void setSelectedItem( Object _selection) {
        if (selection!=null && !selection.equals( _selection) ||
            selection==null && _selection!=null) {
            selection = _selection;
            fireContentsChanged( this, -1, -1);
        }
    }

    public void replaceItems( List<? extends Object> newItems) {
        if (!(newItems instanceof RandomAccess)) {
	        newItems = new ArrayList<Object>( newItems);
        }

        List<? extends Object> oldItems = items;
        items = newItems;
        
        if ((newItems.size() - oldItems.size()) < 0) {
            if (newItems.size() > 0) {
	            fireContentsChanged( this, 0, newItems.size()-1);
            }
            fireIntervalRemoved( this, newItems.size(), oldItems.size()-1);
        }
        else {
            if (oldItems.size() > 0) {
	            fireContentsChanged( this, 0, oldItems.size()-1);
            }
            if (oldItems.size() < newItems.size()) {
	            fireIntervalAdded( this, oldItems.size(), newItems.size()-1);
            }
        }
    }

    @Override
	public int getSize() { return items.size(); }

    @Override
	public Object getElementAt( int i) {
        return items.get( i);
    }
} // class ReplaceableItemsComboBoxModel
