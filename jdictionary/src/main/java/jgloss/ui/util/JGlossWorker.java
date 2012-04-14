/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.util;

import javax.swing.SwingWorker;

/**
 * SwingWorker with utility methods for giving feedback.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public abstract class JGlossWorker<T, V> extends SwingWorker<T, V> {
    private static final String MESSAGE_PROPERTY = "message";

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        String oldMessage = this.message;
        this.message = message;
        firePropertyChange(MESSAGE_PROPERTY, oldMessage, message);
    }
}
