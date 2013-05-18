/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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

    @Override
	public String toString() {
        return start + "/" + length + "/" + reading + "/" + dictionaryForm + "/"
            + dictionaryFormReading + "/" + grammaticalType + "/" + translation;
    }
} // class TextAnnotation
