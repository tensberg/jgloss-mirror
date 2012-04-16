/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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
 * $Id$
 *
 */

package jgloss.ui;

/**
 * Stores an object together with its selected and enabled state.
 *
 * @author Michael Koch <tensberg@gmx.net>
 * @param <T> Type of the object which is stored.
 */
class StateWrapper<T> implements Cloneable {
    private T obj;
    private boolean selected;
    private boolean enabled;

    public StateWrapper( T _obj) {
        this( _obj, false, false);
    }

    public StateWrapper( T _obj, boolean _selected, boolean _enabled) {
        obj = _obj;
        selected = _selected;
        enabled = _enabled;
    }

    public T getObject() { return obj; }
    public boolean isSelected() { return selected; }
    public boolean isEnabled() { return enabled; }

    public void setSelected( boolean _selected) { selected = _selected; }
    public void setEnabled( boolean _enabled) { enabled = _enabled; }

    @SuppressWarnings("unchecked")
    @Override
	public StateWrapper<T> clone() {
        try {
            return (StateWrapper<T>) super.clone();
        } catch (CloneNotSupportedException ex) { return null; }
    }
} // class StateWrapper
