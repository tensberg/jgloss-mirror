/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import java.util.EventObject;

/**
 * Event which is triggered if the annotation list or the state of an annotation has 
 * changed in some way.
 *
 * @author Michael Koch
 * @see AnnotationListener
 * @see AnnotationListModel
 */
public class AnnotationEvent extends EventObject {
    protected Annotation annotation;
    protected int index;
    protected int readingIndex;

    AnnotationEvent( AnnotationListModel _source, Annotation _annotation, int _index) {
        this( _source, _annotation, _index, -1);
    }

    AnnotationEvent( AnnotationListModel _source, Annotation _annotation, int _index, int _readingIndex) {
        super( _source);
        annotation = _annotation;
        index = _index;
        readingIndex = _readingIndex;
    }

    /**
     * Returns the annotation which has changed.
     */
    public Annotation getAnnotation() { return annotation; }
    /**
     * Returns the index of the changed annotation in the annotation model.
     */
    public int getIndex() { return index; }
    /**
     * Index of the reading which changed, if this is a <code>readingChanged</code> event.
     * May be <code>-1</code> if several or all readings changed.
     */
    public int getReadingIndex() { return readingIndex; }
} // class AnnotationEvent
