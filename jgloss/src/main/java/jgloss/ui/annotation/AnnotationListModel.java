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

package jgloss.ui.annotation;

import static jgloss.ui.annotation.Annotation.COMPARE_BY_START_OFFSET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.text.Element;

public class AnnotationListModel {
    public static final int BIAS_NONE = 0;
    public static final int BIAS_LEFT = 1;
    public static final int BIAS_RIGHT = 2;

    private List<Annotation> annotations;
    private final List<AnnotationListener> annotationListeners = new CopyOnWriteArrayList<AnnotationListener>();
    private int searchindex;

    public AnnotationListModel( List<Element> _annoElements) {
        annotations = new ArrayList<Annotation>( _annoElements.size()+10);
        for (Element element : _annoElements)
            annotations.add( new Annotation( this, element));
    }

    public int getAnnotationCount() { return annotations.size(); }

    public Annotation getAnnotation( int index) { 
        return annotations.get( index);
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
        // the finishing index of the previous search is remembered. This will make the
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
            anno = annotations.get( searchindex);
            if (pos < anno.getStartOffset()) {
                if (searchindex == min || // no annotation for this position
                    annotations.get( searchindex-1)
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
                    annotations.get( searchindex+1)
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

    /**
     * Add an annotation node for an annotation element newly inserted into the JGloss HTML
     * document.
     */
    public void addAnnotationFor(Element annoElement) {
        Annotation anno = new Annotation(this, annoElement);
        // The annotation list is in ascending order by element start offset.
        // Find the index where the new annotation has to be inserted.
		int insertionPoint = Collections.binarySearch(annotations, anno, COMPARE_BY_START_OFFSET);
        insertionPoint = -1 - insertionPoint; // Collections.binarySearch returns (-(insertion point) - 1)
        annotations.add(insertionPoint, anno);
        fireAnnotationInserted(anno, insertionPoint);
    }

    /**
     * Remove the annotation node which represents an annotation element removed from the
     * JGloss HTML document.
     */
    public void removeAnnotationFor(Element annoElement) {
        // find the index of the Annotation object representing the annoElement by doing a
        // binary search through the list of annotations, which is ordered by start offsets.
        int annoOffset = Collections.binarySearch
            (annotations, new Annotation(null, annoElement), new Comparator<Annotation>() {
                    @Override
					public int compare(Annotation o1, Annotation o2) {
                        Element e1 = (o1 instanceof Element) ?
                            (Element) o1 : o1.getAnnotationElement();
                        Element e2 = (o2 instanceof Element) ? 
                            (Element) o2 : o2.getAnnotationElement();
                        return e1.getStartOffset()-e2.getStartOffset();
                    }
                });
        try {
            // Since the annotation element has already been removed from the document,
            // the invariant that each Element in the annotation list has a distinct start offset
            // does not hold and binary search returns one of the elements.
            // Find the removed element among all elements with same
            // start offset.
            int annoOffsetStore = annoOffset;

            // search backward
            while (annoOffset >= 0 &&
                   annoElement != annotations.get(annoOffset).getAnnotationElement() &&
                   annoElement.getStartOffset() == 
                   annotations.get(annoOffset).getAnnotationElement().getStartOffset())
                annoOffset--;
            
            // if not found, search forward
            if (annoOffset < 0 ||
                annoElement != annotations.get(annoOffset).getAnnotationElement()) {
                annoOffset = annoOffsetStore;

                while (annoOffset < annotations.size() &&
                       annoElement != annotations.get(annoOffset).getAnnotationElement() &&
                       annoElement.getStartOffset() == 
                       annotations.get(annoOffset).getAnnotationElement().getStartOffset())
                    annoOffset++;
            }

            if (annoOffset < annotations.size() &&
                annotations.get(annoOffset).getAnnotationElement()==annoElement) {
                Annotation annotation = annotations.remove(annoOffset);
                fireAnnotationRemoved(annotation, annoOffset);
            }
            else {
                System.err.println( "WARNING: assertion failed, removed annotation element not found");
            }
        } catch (IndexOutOfBoundsException ex) {
            // element was not found, programming error
            ex.printStackTrace();
        }
    }

    public void addAnnotationListener( AnnotationListener l) {
        annotationListeners.add( l);
    }

    public void removeAnnotationListener( AnnotationListener l) {
        annotationListeners.remove( l);
    }

    private void fireAnnotationInserted( Annotation anno, int index) {
        AnnotationEvent event = new AnnotationEvent( this, anno, index);
        for (AnnotationListener listener : annotationListeners) {
        	listener.annotationInserted( event);
        }
    }

    private void fireAnnotationRemoved( Annotation anno, int index) {
        AnnotationEvent event = new AnnotationEvent( this, anno, index);
        for (AnnotationListener listener : annotationListeners) {
        	listener.annotationRemoved( event);
        }
    }

    public void fireAnnotationChanged( Annotation anno) {
        AnnotationEvent event = new AnnotationEvent( this, anno, indexOf( anno));
        for (AnnotationListener listener : annotationListeners) {
        	listener.annotationChanged( event);
        }
    }

    public void fireReadingChanged( Annotation anno, int readingIndex) {
        AnnotationEvent event = new AnnotationEvent( this, anno, indexOf( anno), readingIndex);
        for (AnnotationListener listener : annotationListeners) {
        	listener.readingChanged( event);
        }
    }
} // class AnnotationListModel
