package jgloss.ui.annotation;

import jgloss.ui.xml.JGlossDocument;
import jgloss.ui.html.JGlossHTMLDoc;

import java.util.List;
import java.util.ArrayList;

import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.BadLocationException;

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

    Element getAnnotationElement() { return anno; }
    
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

    public void setReading( String _reading, int index) {
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
            reading.append( getText( word.getElement( i).getElement( 0)));
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
