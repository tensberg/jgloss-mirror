/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.gloss;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jgloss.JGloss;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationEvent;
import jgloss.ui.annotation.AnnotationListener;

public class AnnotationEditorPanel extends JPanel implements ActionListener, AnnotationListener {
    private static final long serialVersionUID = 1L;

	private Annotation annotation;

    private final JLabel title;
    private final JTextField translation;
    private final JTextField dictionaryForm;
    private final JTextField dictionaryFormReading;
    private final JLabel grammaticalType;
    private final ReadingEditor readingEditor;

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
        GridBagConstraints c3 = new GridBagConstraints();
        c3.anchor = GridBagConstraints.NORTHWEST;
        c3.gridx = 1;
        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.weightx = 1;

        add( new JLabel( JGloss.messages.getString( "annotationeditor.grammaticaltype")), c2);
        add( grammaticalType, c3);
        
        add( new JLabel( JGloss.messages.getString( "annotationeditor.dictionaryform")), c);
        GridBagConstraints c4 = (GridBagConstraints) c2.clone();
        c4.anchor = GridBagConstraints.NORTHWEST;
        c4.insets = new Insets( 0, 10, 0, 0);
        add( new JLabel( JGloss.messages.getString( "annotationeditor.dictionaryform.word")), c4);
        add( dictionaryForm, c3);
        add( new JLabel( JGloss.messages.getString( "annotationeditor.dictionaryform.reading")),
             c4);
        add( dictionaryFormReading, c3);

        add( new JLabel( JGloss.messages.getString( "annotationeditor.translation")), c2);
        add( translation, c3);

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
        if (annotation == _annotation) {
	        return;
        }

        annotation = _annotation;
        updateDisplay();
        readingEditor.setAnnotation( _annotation);
    }

    @Override
	public void actionPerformed( ActionEvent e) {
        if (annotation == null) {
	        return;
        }

        JTextField source = (JTextField) e.getSource();
        String text = source.getText();
        if (source == translation) {
            if (!text.equals( annotation.getTranslation())) {
	            annotation.setTranslation( text);
            }
        }
        else if (source == dictionaryForm) {
            if (!text.equals( annotation.getDictionaryForm())) {
	            annotation.setDictionaryForm( text);
            }
        }
        else if (source == dictionaryFormReading) {
            if (!text.equals( annotation.getDictionaryFormReading())) {
	            annotation.setDictionaryFormReading( text);
            }
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

    @Override
	public void setEnabled( boolean _enabled) {
        super.setEnabled( _enabled);
        enabled = _enabled;
        
        translation.setEnabled( enabled);
        dictionaryForm.setEnabled( enabled);
        dictionaryFormReading.setEnabled( enabled);
        grammaticalType.setEnabled( enabled);
    }

    @Override
	public boolean isEnabled() { return enabled; }

    @Override
	public void annotationInserted( AnnotationEvent ae) {}
    @Override
	public void annotationRemoved( AnnotationEvent ae) {}

    @Override
	public void annotationChanged( AnnotationEvent ae) {
        if (ae.getAnnotation() == annotation) {
            updateDisplay();
        }
    }

    @Override
	public void readingChanged( AnnotationEvent ae) {
       if (ae.getAnnotation() == annotation) {
            readingEditor.updateDisplay();
       } 
    }

    private static class ReadingEditor extends JPanel implements ActionListener {
        private static final long serialVersionUID = 1L;

		private Annotation annotation;
        private final JLabel multiReadingLabel = new JLabel
            ( JGloss.messages.getString( "annotationeditor.readings"));
        private final List<JLabel> readingLabels = new ArrayList<JLabel>( 5);
        private final List<JTextField> readings = new ArrayList<JTextField>( 5);
        private final GridBagConstraints multiReadingLabelC;
        private final GridBagConstraints multiReadingBaseC;
        private final GridBagConstraints multiReadingTextC;
        private final JLabel title;

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
            if (annotation == _annotation) {
	            return;
            }

            if (annotation==null || _annotation==null ||
                annotation.getReadingCount()!=_annotation.getReadingCount()) {
                removeAll();
                if (_annotation != null) {

                    while (readingLabels.size() < _annotation.getReadingCount()) {
	                    readingLabels.add( new JLabel());
                    }
                    while (readings.size() < _annotation.getReadingCount()) {
                        JTextField reading = new JTextField( "");
                        reading.addActionListener( this);
                        readings.add( reading);
                    }

                    if (_annotation.getReadingCount() > 0) {
                        add( multiReadingLabel, multiReadingLabelC);
                        for ( int i=0; i<_annotation.getReadingCount(); i++) {
                            add( readingLabels.get( i), multiReadingBaseC);
                            add( readings.get( i), multiReadingTextC);
                        }
                    }
                }
            }

            annotation = _annotation;
            updateDisplay();
        }

        private void updateDisplay() {
            if (annotation == null) {
	            return;
            }

            String[] base = new String[1];
            for ( int i=0; i<annotation.getReadingCount(); i++) {
                base[0] = annotation.getReadingBase( i);
                readingLabels.get( i).setText
                    ( JGloss.messages.getString( "annotationeditor.reading.base", base));
                readings.get( i).setText
                    ( annotation.getReading( i));
            }
        }

        @Override
		public void actionPerformed( ActionEvent e) {
            if (annotation == null) {
	            return;
            }

            JTextField source = (JTextField) e.getSource();
            String reading = source.getText();
            int index = readings.indexOf( source);
            if (!reading.equals( annotation.getReading( index))) {
	            annotation.setReading( index, reading);
            }
            title.setText( annotation.toString());
        }
    } // class ReadingEditor
} // class AnnotationEditorPanel
