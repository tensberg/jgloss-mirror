package jgloss.ui;

import jgloss.JGloss;
import jgloss.ui.annotation.Annotation;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;

import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class AnnotationEditorPanel extends JPanel {
    private Annotation annotation;

    private JLabel title;
    private JTextField translation;
    private JTextField dictionaryForm;
    private JTextField dictionaryFormReading;
    private JLabel grammaticalType;
    private ReadingEditor readingEditor;

    private boolean enabled = true;

    public AnnotationEditorPanel() {
        setLayout( new GridBagLayout());
        setBackground( Color.white);

        title = new JLabel( " ");
        title.setFont( title.getFont().deriveFont( Font.BOLD, title.getFont().getSize()*1.2f));
        title.setBackground( Color.lightGray);
        title.setOpaque( true);
        translation = new JTextField();
        dictionaryForm = new JTextField();
        dictionaryFormReading = new JTextField();
        grammaticalType = new JLabel();
        readingEditor = new ReadingEditor();

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        add( title, c);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.NORTHWEST;
        c2.gridx = 0;
        c2.fill = GridBagConstraints.NONE;
        add( new JLabel( JGloss.messages.getString( "annotationeditor.translation")), c2);
        GridBagConstraints c3 = new GridBagConstraints();
        c3.anchor = GridBagConstraints.NORTHWEST;
        c3.gridx = 1;
        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.weightx = 1;
        add( translation, c3);
        
        add( new JLabel( JGloss.messages.getString( "annotationeditor.dictionaryform")), c);
        GridBagConstraints c4 = (GridBagConstraints) c2.clone();
        c4.anchor = GridBagConstraints.NORTHWEST;
        c4.insets = new Insets( 0, 10, 0, 0);
        add( new JLabel( JGloss.messages.getString( "annotationeditor.dictionaryform.word")), c4);
        add( dictionaryForm, c3);
        add( new JLabel( JGloss.messages.getString( "annotationeditor.dictionaryform.reading")),
             c4);
        add( dictionaryFormReading, c3);

        add( new JLabel( JGloss.messages.getString( "annotationeditor.grammaticaltype")), c2);
        add( grammaticalType, c3);

        add( readingEditor, c);

        GridBagConstraints c5 = new GridBagConstraints();
        c5.anchor = GridBagConstraints.NORTHWEST;
        c5.fill = GridBagConstraints.BOTH;
        c5.weightx = 10;
        c5.weighty = 10;
        c5.gridx = 0;
        c5.gridwidth = 2;
        JPanel p = new JPanel();
        p.setOpaque( false);
        add( p, c5);
    }

    public void setAnnotation( Annotation _annotation) {
        if (annotation == _annotation)
            return;

        annotation = _annotation;
        updateDisplay();
        readingEditor.setAnnotation( _annotation);
    }

    private void updateDisplay() {
        if (annotation == null) {
            title.setText( "");
            translation.setText( "");
            translation.setEnabled( false);
            dictionaryForm.setText( "");
            dictionaryForm.setEnabled( false);
            dictionaryFormReading.setText( "");
            dictionaryFormReading.setEnabled( false);
            grammaticalType.setText( "");
        }
        else {
            title.setText( annotation.toString());
            translation.setText( annotation.getTranslation() == null ?
                                 "" : annotation.getTranslation());
            dictionaryForm.setText( annotation.getDictionaryForm());
            dictionaryFormReading.setText( annotation.getDictionaryFormReading());
            grammaticalType.setText( annotation.getGrammaticalType());
            setEnabled( enabled);
        }
    }

    public void setEnabled( boolean _enabled) {
        super.setEnabled( _enabled);
        enabled = _enabled;
        
        translation.setEnabled( enabled);
        dictionaryForm.setEnabled( enabled);
        dictionaryFormReading.setEnabled( enabled);
        grammaticalType.setEnabled( enabled);
    }

    public boolean isEnabled() { return enabled; }

    private static class ReadingEditor extends JPanel {
        private Annotation annotation;
        private JLabel multiReadingLabel = new JLabel
            ( JGloss.messages.getString( "annotationeditor.readings"));
        private List readingLabels = new ArrayList( 5);
        private List readings = new ArrayList( 5);
        private GridBagConstraints multiReadingLabelC;
        private GridBagConstraints multiReadingBaseC;
        private GridBagConstraints multiReadingTextC;

        ReadingEditor() {
            setLayout( new GridBagLayout());
            setOpaque( false);

            multiReadingLabelC = new GridBagConstraints();
            multiReadingLabelC.anchor = GridBagConstraints.WEST;
            multiReadingLabelC.gridwidth = 2;
            multiReadingLabelC.fill = GridBagConstraints.HORIZONTAL;
            multiReadingLabelC.weightx = 1;
            multiReadingLabelC.gridx = 0;
            multiReadingLabelC.gridy = 0;
            multiReadingBaseC = new GridBagConstraints();
            multiReadingBaseC.anchor = GridBagConstraints.WEST;
            multiReadingBaseC.insets = new Insets( 0, 10, 0, 0);
            multiReadingBaseC.gridx = 0;
            multiReadingTextC = new GridBagConstraints();
            multiReadingTextC.anchor = GridBagConstraints.WEST;
            multiReadingTextC.gridx = 1;
            multiReadingTextC.fill = GridBagConstraints.HORIZONTAL;
            multiReadingTextC.weightx = 1;
        }
        
        public void setAnnotation( Annotation _annotation) {
            if (annotation == _annotation)
                return;

            if (annotation==null || _annotation==null ||
                annotation.getReadingCount()!=_annotation.getReadingCount()) {
                removeAll();
                if (_annotation != null) {

                    while (readingLabels.size() < _annotation.getReadingCount())
                        readingLabels.add( new JLabel());
                    while (readings.size() < _annotation.getReadingCount()) {
                        JTextField reading = new JTextField( "");
                        readings.add( reading);
                    }

                    if (_annotation.getReadingCount() > 0) {
                        add( multiReadingLabel, multiReadingLabelC);
                        for ( int i=0; i<_annotation.getReadingCount(); i++) {
                            add( (JLabel) readingLabels.get( i), multiReadingBaseC);
                            add( (JTextField) readings.get( i), multiReadingTextC);
                        }
                    }
                }
            }

            annotation = _annotation;
            updateText();
        }

        private void updateText() {
            if (annotation == null)
                return;

            String[] base = new String[1];
            for ( int i=0; i<annotation.getReadingCount(); i++) {
                base[0] = annotation.getReadingBase( i);
                ((JLabel) readingLabels.get( i)).setText
                    ( JGloss.messages.getString( "annotationeditor.reading.base", base));
                ((JTextField) readings.get( i)).setText
                    ( annotation.getReading( i));
            }
        }
    } // class ReadingEditor
} // class AnnotationEditorPanel
