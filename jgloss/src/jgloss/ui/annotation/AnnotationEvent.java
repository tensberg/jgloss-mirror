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
