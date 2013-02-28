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

package jgloss.ui.gloss;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationEvent;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.annotation.AnnotationListener;

public class AnnotationListModelAdapter implements ListModel<Annotation>, AnnotationListener {
    private final AnnotationListModel annotations;
    private final List<ListDataListener> listeners = new CopyOnWriteArrayList<ListDataListener>();

    public AnnotationListModelAdapter( AnnotationListModel _annotations) {
        annotations = _annotations;
        annotations.addAnnotationListener( this);
    }

    @Override
	public int getSize() { return annotations.getAnnotationCount(); }

    @Override
    public Annotation getElementAt(int index) {
        return annotations.getAnnotation(index);
    }

    @Override
	public void addListDataListener( ListDataListener listener) {
        listeners.add(listener);
    }

    @Override
	public void removeListDataListener( ListDataListener listener) {
        listeners.remove(listener);
    }

    @Override
	public void annotationInserted( AnnotationEvent ae) {
        fireIntervalAdded( ae.getIndex(), ae.getIndex());
    }

    @Override
	public void annotationRemoved( AnnotationEvent ae) {
        fireIntervalRemoved( ae.getIndex(), ae.getIndex());
    }

    @Override
	public void annotationChanged( AnnotationEvent ae) {
        fireContentsChanged( ae.getIndex(), ae.getIndex());
    }

    @Override
	public void readingChanged( AnnotationEvent ae) {
        fireContentsChanged( ae.getIndex(), ae.getIndex());
    }

    private void fireIntervalAdded( int startIndex, int endIndex) {
        ListDataEvent e = new ListDataEvent( this, ListDataEvent.INTERVAL_ADDED,
                                             startIndex, endIndex);
        for (ListDataListener listener : listeners) {
        	listener.intervalAdded( e);
        }
    }

    private void fireIntervalRemoved( int startIndex, int endIndex) {
        ListDataEvent e = new ListDataEvent( this, ListDataEvent.INTERVAL_REMOVED,
                                             startIndex, endIndex);
        for (ListDataListener listener : listeners) {
        	listener.intervalRemoved( e);
        }
    }

    private void fireContentsChanged( int startIndex, int endIndex) {
        ListDataEvent e = new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                             startIndex, endIndex);
        for (ListDataListener listener : listeners) {
        	listener.contentsChanged( e);
        }
    }
} // class AnnotationListModelAdapter
