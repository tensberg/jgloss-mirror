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
 * $Id$
 *
 */

package jgloss.ui.annotation;

import java.util.EventListener;

/**
 * Listens to changes of annotations in the annotation list model.
 *
 * @author Michael Koch
 * @see AnnotationListModel
 */
public interface AnnotationListener extends EventListener {
    void annotationInserted( AnnotationEvent ae);
    void annotationRemoved( AnnotationEvent ae);
    /**
     * Triggered if the state of an annotation has changed in some way.
     * This does not include changes to the reading.
     */
    void annotationChanged( AnnotationEvent ae);
    /**
     * Triggered if a reading of an annotation has changed.
     */
    void readingChanged( AnnotationEvent ae);
} // interface AnnotationListener
