package jgloss.ui.doc;

public interface AnnotationList {
    int size();
    Annotation getAnnotation( int index);
    void addAnnotationListener( AnnotationListener l);
    void removeAnnotationListener( AnnotationListener l);
} // interface AnnotationList
