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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.text.Element;

public class AnnotationListModel {
    private List annotations;
    private List annotationListeners = new ArrayList( 5);
    private int searchindex;

    public AnnotationListModel( List _annoElements) {
        annotations = new ArrayList( _annoElements.size()+10);
        for ( Iterator i=_annoElements.iterator(); i.hasNext(); )
            annotations.add( new Annotation( this, (Element) i.next()));
    }

    public int getAnnotationCount() { return annotations.size(); }

    public Annotation getAnnotation( int index) { 
        return (Annotation) annotations.get( index);
    }

    public int indexOf( Annotation anno) {
        return annotations.indexOf( anno);
    }

    public int findAnnotationIndex( int pos) {
        // the finishing index of the previous search is remebered. This will make the
        // search faster if the method is called for close locations. Since this method
        // is often called after mouse movement events, this is likely to be the case.
        if (searchindex<0 || searchindex>=annotations.size())
            searchindex = annotations.size() / 2;

        // do a binary search
        Annotation out;
        boolean found = false;
        int min = 0;
        int max = annotations.size()-1;
        do {
            out = (Annotation) annotations.get( searchindex);
            if (pos < out.getStartOffset()) {
                if (searchindex == min) // no annotation for this position
                    return -1;
                else if (((Annotation) annotations.get( searchindex-1))
                         .getEndOffset()-1 < pos)
                    // pos is between two annotation elements
                    return -1;
                else {
                    max = searchindex - 1;
                    searchindex = min + (searchindex-min) / 2;
                }
            }
            else if (pos > out.getEndOffset()-1) {
                if (searchindex == max) // no annotation for this position
                    return -1;
                else if (((Annotation) annotations.get( searchindex+1))
                         .getStartOffset() > pos)
                    return -1;
                else {
                    min = searchindex + 1;
                    searchindex = searchindex + (max-searchindex)/2 + 1;
                }
            }
            else
                found = true;
        } while (!found);

        return searchindex;
    }

    public void insertAnnotation( int fromOffset, int toOffset, String dictionaryForm,
                                  String dictionaryReading) {
    }

    public void removeAnnotation( Annotation anno) {
    }

    public void removeAnnotation( int index) {
    }

    public void addAnnotationListener( AnnotationListener l) {
        annotationListeners.add( l);
    }

    public void removeAnnotationListener( AnnotationListener l) {
        annotationListeners.remove( l);
    }

    private void fireAnnotationInserted( Annotation anno, int index) {
        AnnotationEvent event = new AnnotationEvent( this, anno, index);
        List listeners = new ArrayList( annotationListeners);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((AnnotationListener) i.next()).annotationInserted( event);
        }
    }

    private void fireAnnotationRemoved( Annotation anno, int index) {
        AnnotationEvent event = new AnnotationEvent( this, anno, index);
        List listeners = new ArrayList( annotationListeners);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((AnnotationListener) i.next()).annotationInserted( event);
        }
    }

    public void fireAnnotationChanged( Annotation anno) {
        AnnotationEvent event = new AnnotationEvent( this, anno, indexOf( anno));
        List listeners = new ArrayList( annotationListeners);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((AnnotationListener) i.next()).annotationInserted( event);
        }
    }

    public void fireReadingChanged( Annotation anno, int readingIndex) {
        AnnotationEvent event = new AnnotationEvent( this, anno, indexOf( anno), readingIndex);
        List listeners = new ArrayList( annotationListeners);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((AnnotationListener) i.next()).annotationInserted( event);
        }
    }
} // class AnnotationListModel
