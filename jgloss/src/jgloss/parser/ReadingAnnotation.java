package jgloss.parser;

/**
 * Reading for a part of a text. Used by {@link ReadingAnnotationFilter ReadingAnnotationFilter}.
 *
 * @author Michael Koch
 */
public class ReadingAnnotation {
    protected int start;
    protected int length;

    protected String reading;

    public ReadingAnnotation( int _start, int _length, String _reading) {
        start = _start;
        length = _length;
        reading = _reading;
    }

    /**
     * Returns the start offset of this annotation in the parsed text. 
     */
    public int getStart() { return start; }
    /**
     * Returns the length of the annotated text.
     */
    public int getLength() { return length; }

    /**
     * Returns the reading (in hiragana) of the annotated text. May be <code>null</code> if the
     * parser cannot determine the reading.
     */
    public String getReading() { return reading; }

    public void setReading( String _reading) { reading = _reading; }
} // class ReadingAnnotation
