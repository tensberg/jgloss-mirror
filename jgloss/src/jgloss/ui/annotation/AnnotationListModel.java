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
    public static final int BIAS_NONE = 0;
    public static final int BIAS_LEFT = 1;
    public static final int BIAS_RIGHT = 2;

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

    /**
     * Find the index of the annotation at a given position in the document.
     *
     * @param pos Position in the JGloss html document.
     * @param bias Controls what will be returned if the position is not in an annotation.
     *        <code>BIAS_NONE</code> will return -2, <code>BIAS_LEFT</code> will return the
     *        index of the annotation left of the position (<code>-1</code> if the position is
     *        left of the first annotation) and <code>BIAS_RIGHT</code> will return the
     *        index of the annotation right of the position 
     *        ({@link #getAnnotationCount() getAnnotationCount} if the position is
     *        right of the last annotation).
     */
    public int findAnnotationIndex( int pos, int bias) {
        // the finishing index of the previous search is remebered. This will make the
        // search faster if the method is called for close locations. Since this method
        // is often called after mouse movement events, this is likely to be the case.
        if (searchindex<0 || searchindex>=annotations.size())
            searchindex = annotations.size() / 2;

        // do a binary search
        Annotation anno;
        boolean found = false;
        int min = 0;
        int max = annotations.size()-1;
        do {
            anno = (Annotation) annotations.get( searchindex);
            if (pos < anno.getStartOffset()) {
                if (searchindex == min || // no annotation for this position
                    ((Annotation) annotations.get( searchindex-1))
                    .getEndOffset()-1 < pos) { // pos is between two annotation elements
                    switch (bias) {
                    case BIAS_NONE:
                        return -2;
                    case BIAS_LEFT:
                        return searchindex-1;
                    case BIAS_RIGHT:
                        return searchindex;
                    default:
                        throw new IllegalArgumentException( "bias invalid");
                    }
                }
                else {
                    max = searchindex - 1;
                    searchindex = min + (searchindex-min) / 2;
                }
            }
            else if (pos > anno.getEndOffset()-1) {
                if (searchindex == max || // no annotation for this position
                    ((Annotation) annotations.get( searchindex+1))
                    .getStartOffset() > pos) {
                    switch (bias) {
                    case BIAS_NONE:
                        return -2;
                    case BIAS_LEFT:
                        return searchindex;
                    case BIAS_RIGHT:
                        return searchindex+1;
                    default:
                        throw new IllegalArgumentException( "bias invalid");
                    }
                }
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
            ((AnnotationListener) i.next()).annotationRemoved( event);
        }
    }

    public void fireAnnotationChanged( Annotation anno) {
        AnnotationEvent event = new AnnotationEvent( this, anno, indexOf( anno));
        List listeners = new ArrayList( annotationListeners);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((AnnotationListener) i.next()).annotationChanged( event);
        }
    }

    public void fireReadingChanged( Annotation anno, int readingIndex) {
        AnnotationEvent event = new AnnotationEvent( this, anno, indexOf( anno), readingIndex);
        List listeners = new ArrayList( annotationListeners);
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((AnnotationListener) i.next()).readingChanged( event);
        }
    }
} // class AnnotationListModel
