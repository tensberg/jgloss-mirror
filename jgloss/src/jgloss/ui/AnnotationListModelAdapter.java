package jgloss.ui;

import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.annotation.AnnotationListener;
import jgloss.ui.annotation.AnnotationEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

public class AnnotationListModelAdapter implements ListModel, AnnotationListener {
    private AnnotationListModel annotations;
    private List listeners = new ArrayList( 0);

    public AnnotationListModelAdapter( AnnotationListModel _annotations) {
        annotations = _annotations;
        annotations.addAnnotationListener( this);
    }

    public int getSize() { return annotations.getAnnotationCount(); }
    
    public Object getElementAt( int index) { return annotations.getAnnotation( index); }

    public void addListDataListener( ListDataListener listener) {
        List tempListeners = new ArrayList( listeners.size()+1);
        tempListeners.addAll( listeners);
        tempListeners.add( listener);
        listeners = tempListeners;
    }

    public void removeListDataListener( ListDataListener listener) {
        List tempListeners = new ArrayList( listeners);
        tempListeners.remove( listener);
        listeners = tempListeners;
    }

    public void annotationInserted( AnnotationEvent ae) {
        fireIntervalAdded( ae.getIndex(), ae.getIndex());
    }

    public void annotationRemoved( AnnotationEvent ae) {
        fireIntervalRemoved( ae.getIndex(), ae.getIndex());
    }

    public void annotationChanged( AnnotationEvent ae) {
        fireContentsChanged( ae.getIndex(), ae.getIndex());
    }

    public void readingChanged( AnnotationEvent ae) {
        fireContentsChanged( ae.getIndex(), ae.getIndex());
    }

    private void fireIntervalAdded( int startIndex, int endIndex) {
        ListDataEvent e = new ListDataEvent( this, ListDataEvent.INTERVAL_ADDED, 
                                             startIndex, endIndex);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((ListDataListener) i.next()).intervalAdded( e);
        }
    }

    private void fireIntervalRemoved( int startIndex, int endIndex) {
        ListDataEvent e = new ListDataEvent( this, ListDataEvent.INTERVAL_REMOVED, 
                                             startIndex, endIndex);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((ListDataListener) i.next()).intervalRemoved( e);
        }
    }

    private void fireContentsChanged( int startIndex, int endIndex) {
        ListDataEvent e = new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED, 
                                             startIndex, endIndex);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((ListDataListener) i.next()).contentsChanged( e);
        }
    }
} // class AnnotationListModelAdapter
