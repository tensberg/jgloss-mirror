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

package jgloss.ui;

import jgloss.JGloss;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationEvent;
import jgloss.ui.annotation.AnnotationListener;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class AnnotationEditorPanel extends JPanel implements ActionListener, AnnotationListener {
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
        readingEditor = new ReadingEditor( title);

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

        translation.addActionListener( this);
        dictionaryForm.addActionListener( this);
        dictionaryFormReading.addActionListener( this);
    }

    public void setAnnotation( Annotation _annotation) {
        if (annotation == _annotation)
            return;

        annotation = _annotation;
        updateDisplay();
        readingEditor.setAnnotation( _annotation);
    }

    public void actionPerformed( ActionEvent e) {
        if (annotation == null)
            return;

        JTextField source = (JTextField) e.getSource();
        String text = source.getText();
        if (source == translation) {
            if (!text.equals( annotation.getTranslation()))
                annotation.setTranslation( text);
        }
        else if (source == dictionaryForm) {
            if (!text.equals( annotation.getDictionaryForm()))
                annotation.setDictionaryForm( text);
        }
        else if (source == dictionaryFormReading) {
            if (!text.equals( annotation.getDictionaryFormReading()))
                annotation.setDictionaryFormReading( text);
        }
        title.setText( annotation.toString());
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

    public void annotationInserted( AnnotationEvent ae) {}
    public void annotationRemoved( AnnotationEvent ae) {}
    public void annotationChanged( AnnotationEvent ae) {
        if (ae.getAnnotation() == annotation) {
            updateDisplay();
        }
    }

    public void readingChanged( AnnotationEvent ae) {
       if (ae.getAnnotation() == annotation) {
            readingEditor.updateDisplay();
       } 
    }

    private static class ReadingEditor extends JPanel implements ActionListener {
        private Annotation annotation;
        private JLabel multiReadingLabel = new JLabel
            ( JGloss.messages.getString( "annotationeditor.readings"));
        private List readingLabels = new ArrayList( 5);
        private List readings = new ArrayList( 5);
        private GridBagConstraints multiReadingLabelC;
        private GridBagConstraints multiReadingBaseC;
        private GridBagConstraints multiReadingTextC;
        private JLabel title;

        ReadingEditor( JLabel _title) {
            title = _title;

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
                        reading.addActionListener( this);
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
            updateDisplay();
        }

        private void updateDisplay() {
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

        public void actionPerformed( ActionEvent e) {
            if (annotation == null)
                return;

            JTextField source = (JTextField) e.getSource();
            String reading = source.getText();
            int index = readings.indexOf( source);
            if (!reading.equals( annotation.getReading( index)))
                annotation.setReading( index, reading);
            title.setText( annotation.toString());
        }
    } // class ReadingEditor
} // class AnnotationEditorPanel
