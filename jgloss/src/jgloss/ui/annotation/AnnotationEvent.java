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

    public Annotation getAnnotation() { return annotation; }
    public int getIndex() { return index; }
    public int getReadingIndex() { return readingIndex; }
} // class AnnotationEvent
