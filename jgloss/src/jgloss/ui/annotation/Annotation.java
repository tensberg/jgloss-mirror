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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;

import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.ui.xml.JGlossDocument;
import jgloss.util.StringTools;

public class Annotation {
    protected AnnotationListModel owner;
    protected Element anno;

    protected String annotatedText;
    protected String annotatedTextReading;

    protected TextElement translation;
    protected TextElement[] readings;
    protected String[] rbases;

    public Annotation( AnnotationListModel _owner, Element _anno) {
        owner = _owner;
        anno = _anno;

        translation = new TextElement( anno.getElement( 1));
        Element word = anno.getElement( 0);
        List readingsl = new ArrayList( word.getElementCount());
        List rbasesl = new ArrayList( word.getElementCount());
        StringBuffer text = new StringBuffer();
        StringBuffer reading = new StringBuffer();
        for ( int i=0; i<word.getElementCount(); i++) {
            Element child = word.getElement( i);
            if (child.getElementCount() == 0) { // base text
                String et = getText( child);
                text.append( et);
                reading.append( et);
            }
            else {
                // rb containing reading and basetext
                String rbase = getText( child.getElement( 1));
                rbasesl.add( rbase);
                text.append( rbase);
                TextElement r = new TextElement( child.getElement( 0));
                readingsl.add( r);
                reading.append( r.getText());
            }
        }
        annotatedText = text.toString();
        annotatedTextReading = reading.toString();
        rbases = (String[]) rbasesl.toArray( new String[rbasesl.size()]);
        readings = (TextElement[]) readingsl.toArray( new TextElement[readingsl.size()]);
    }

    public Element getAnnotationElement() { return anno; }
    
    public String getAnnotatedText() { return annotatedText; }
    public String getAnnotatedTextReading() { return annotatedTextReading; }

    public String getDictionaryForm() {
        String base = (String) anno.getAttributes().getAttribute( JGlossDocument.Attributes.BASE);
        if (base != null)
            return base;
        else
            return annotatedText; // equal to dictionary form per definition
    }

    public String getDictionaryFormReading() {
        String basere = (String) anno.getAttributes()
            .getAttribute( JGlossDocument.Attributes.BASE_READING);
        if (basere != null)
            return basere;
        else
            return annotatedTextReading; // equal to dictionary form reading per definition
    }

    public String getGrammaticalType() {
        return (String) anno.getAttributes().getAttribute( JGlossDocument.Attributes.TYPE);
    }

    public String getTranslation() {
        return translation.getText();
    }

    public int getReadingCount() {
        return readings.length;
    }

    public String getReading( int index) {
        return readings[index].getText();
    }

    public String getReadingBase( int index) {
        return rbases[index];
    }

    public void setTranslation( String _translation) {
        translation.setText( _translation);

        owner.fireAnnotationChanged( this);
    }

    public void setReading( String _reading) {
        if (readings.length == 0) // no reading; TODO: create reading?
            return;
        else {
            try {
                String[][] wordReading = StringTools.splitWordReading( getDictionaryForm(), _reading);
                int targetReading = 0;
                for ( int i=0; i<wordReading.length; i++) {
                    if (wordReading[i].length == 2) { // word substring with a reading
                        if (targetReading < readings.length)
                            readings[targetReading++].setText( wordReading[i][1]);
                        else
                            // More reading substrings in wordReading than there are readings
                            // in the annotated text. Add the additional readings to the last
                            // reading element.
                            readings[targetReading-1].setText
                                ( readings[targetReading-1].getText() + wordReading[i][1]);
                    }
                }
                // If there were less readings in wordReading than there are in the annotated
                // element, set the remaining reading elements to the empty string.
                while (targetReading < readings.length)
                    readings[targetReading++].setText( "");

                updateAnnotatedTextReading();
                owner.fireReadingChanged( this, -1);
            } catch (StringIndexOutOfBoundsException ex) {
                System.err.println( "Warning: Unparseable Word/Reading");
                setReading( 0, _reading);
            }
        }
    }

    public void setReading( int index, String _reading) {
        readings[index].setText( _reading);
        updateAnnotatedTextReading();
        owner.fireReadingChanged( this, index);
    }

    public void setDictionaryForm( String _dictionaryForm) {
        if (annotatedText.equals( _dictionaryForm))
            _dictionaryForm = null;

        setAttribute( JGlossDocument.Attributes.BASE, _dictionaryForm);

        owner.fireAnnotationChanged( this);
    }

    public void setDictionaryFormReading( String _dictionaryFormReading) {
        if (annotatedTextReading.equals( _dictionaryFormReading))
            _dictionaryFormReading = null;

        setAttribute( JGlossDocument.Attributes.BASE_READING, _dictionaryFormReading);

        owner.fireAnnotationChanged( this);
    }

    private void setAttribute( String name, String value) {
        ((JGlossHTMLDoc) anno.getDocument()).setAttribute
            ( (MutableAttributeSet) anno.getAttributes(), name, value, false);
    }

    private static String getText( Element elem) {
        try {
            return elem.getDocument().getText( elem.getStartOffset(), 
                                               elem.getEndOffset()-elem.getStartOffset());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private void updateAnnotatedTextReading() {
        StringBuffer reading = new StringBuffer();
        Element word = anno.getElement( 0);
        for ( int i=0; i<word.getElementCount(); i++) {
            Element child = word.getElement( i);
            if (child.getElementCount() > 0) {
                // rb element, get re child
                child = child.getElement( 0);
                String text = getText( child);
                if (text.equals( JGlossHTMLDoc.EMPTY_ELEMENT_PLACEHOLDER))
                    text = "";
                reading.append( text);
            }
            else // child is bt
                reading.append( getText( child));
        }
        annotatedTextReading = reading.toString();
    }

    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append( annotatedText);
        if (annotatedTextReading!=null &&
            annotatedTextReading.length() > 0) {
            out.append( " [");
            out.append( annotatedTextReading);
            out.append( ']');
        }

        String translation = getTranslation();
        if (translation!=null && translation.length()>0) {
            out.append( ' ');
            out.append( translation);
        }

        return out.toString();
    }

    public int getStartOffset() { return anno.getStartOffset(); }
    public int getEndOffset() { return anno.getEndOffset(); }
} // class Annotation
