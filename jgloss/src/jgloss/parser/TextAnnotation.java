package jgloss.parser;

/**
 * Describes an annotation for a specific position in the parsed text.
 * Results returned by a parser are instances of this.
 *
 * @author Michael Koch
 */
public class TextAnnotation extends ReadingAnnotation {
    protected String dictionaryForm;
    protected String dictionaryFormReading;
    protected String grammaticalType;
    protected String translation;

    public TextAnnotation( int _start, int _length, String _dictionaryForm) {
        this( _start, _length, null, _dictionaryForm, null, null, null);
    }

    public TextAnnotation( int _start, int _length, String _reading,
                           String _dictionaryForm, String _dictionaryFormReading,
                           String _grammaticalType) {
        this( _start, _length, _reading, _dictionaryForm, _dictionaryFormReading,
              _grammaticalType, null);
    }

    public TextAnnotation( int _start, int _length, String _reading, 
                           String _dictionaryForm, String _dictionaryFormReading,
                           String _grammaticalType, String _translation) {
        super( _start, _length, _reading);
        dictionaryForm = _dictionaryForm;
        dictionaryFormReading = _dictionaryFormReading;
        grammaticalType = _grammaticalType;
        translation = _translation;
    }

    /**
     * Returns the dictionary form of the annotated text. The dictionary form may be identical to
     * the annotated text.
     */
    public String getDictionaryForm() { return dictionaryForm; }
    /**
     * Returns the reading of the dictionary form of the annotated text. May be <code>null</code> if the
     * parser cannot determine the reading.
     */
    public String getDictionaryFormReading() { return dictionaryFormReading; }
    /**
     * Returns the grammatical type of the annotated text. May be <code>null</code> if the
     * parser cannot determine the grammatical type.
     */
    public String getGrammaticalType() { return grammaticalType; }

    public String getTranslation() { return translation; }

    public void setDictionaryForm( String _dictionaryForm) { dictionaryForm = _dictionaryForm; }
    public void setDictionaryFormReading( String _dictionaryFormReading) {
        dictionaryFormReading = _dictionaryFormReading;
    }
    public void setGrammaticalType( String _grammaticalType) {
        grammaticalType = _grammaticalType;
    }
    public void setTranslation( String _translation) { translation = _translation; }

    public String toString() {
        return start + "/" + length + "/" + reading + "/" + dictionaryForm + "/"
            + dictionaryFormReading + "/" + grammaticalType + "/" + translation;
    }
} // class TextAnnotation
