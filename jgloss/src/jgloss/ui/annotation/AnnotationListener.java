package jgloss.ui.annotation;

import java.util.EventListener;

public interface AnnotationListener extends EventListener {
    void annotationInserted( AnnotationEvent ae);
    void annotationRemoved( AnnotationEvent ae);
    void annotationChanged( AnnotationEvent ae);
    void readingChanged( AnnotationEvent ae);
} // interface AnnotationListener
