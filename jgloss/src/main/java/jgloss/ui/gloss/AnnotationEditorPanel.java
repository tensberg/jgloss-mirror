/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.gloss;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jgloss.JGloss;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationEvent;
import jgloss.ui.annotation.AnnotationListener;

public class AnnotationEditorPanel extends JPanel implements AnnotationListener {
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

        add( new JLabel( JGloss.MESSAGES.getString( "annotationeditor.grammaticaltype")), c2);
        add( grammaticalType, c3);

        add( new JLabel( JGloss.MESSAGES.getString( "annotationeditor.dictionaryform")), c);
        GridBagConstraints c4 = (GridBagConstraints) c2.clone();
        c4.anchor = GridBagConstraints.NORTHWEST;
        c4.insets = new Insets( 0, 10, 0, 0);
        add( new JLabel( JGloss.MESSAGES.getString( "annotationeditor.dictionaryform.word")), c4);
        add( dictionaryForm, c3);
        add( new JLabel( JGloss.MESSAGES.getString( "annotationeditor.dictionaryform.reading")),
             c4);
        add( dictionaryFormReading, c3);

        add( new JLabel( JGloss.MESSAGES.getString( "annotationeditor.translation")), c2);
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

        translation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateTranslation();
            }
        });
        translation.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTranslation();
            }
        });
        dictionaryForm.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateDictionaryForm();
            }
        });
        dictionaryForm.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateDictionaryForm();
            }
        });
        dictionaryFormReading.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateDictionaryFormReading();
            }
        });
        dictionaryFormReading.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateDictionaryFormReading();
            }
        });
    }

    public void setAnnotation( Annotation _annotation) {
        if (annotation == _annotation) {
	        return;
        }

        annotation = _annotation;
        updateDisplay();
        readingEditor.setAnnotation( _annotation);
    }

    private void updateTitle() {
        title.setText(annotation == null ? "" : annotation.toString());
    }

    private void updateTranslation() {
        String newTranslation = translation.getText();
        if (!newTranslation.equals(annotation.getTranslation())) {
            annotation.setTranslation(newTranslation);
            updateTitle();
        }
    }

    private void updateDictionaryForm() {
        String newDictionaryForm = dictionaryForm.getText();
        if (!newDictionaryForm.equals(annotation.getDictionaryForm())) {
            annotation.setDictionaryForm(newDictionaryForm);
            updateTitle();
        }
    }

    private void updateDictionaryFormReading() {
        String newDictionaryFormReading = dictionaryFormReading.getText();
        if (!newDictionaryFormReading.equals(annotation.getDictionaryFormReading())) {
            annotation.setDictionaryFormReading(newDictionaryFormReading);
            updateTitle();
        }
    }

    private void updateDisplay() {
        if (annotation == null) {
            translation.setText( "");
            translation.setEnabled( false);
            dictionaryForm.setText( "");
            dictionaryForm.setEnabled( false);
            dictionaryFormReading.setText( "");
            dictionaryFormReading.setEnabled( false);
            grammaticalType.setText( "");
        }
        else {
            translation.setText( annotation.getTranslation() == null ?
                                 "" : annotation.getTranslation());
            dictionaryForm.setText( annotation.getDictionaryForm());
            dictionaryFormReading.setText( annotation.getDictionaryFormReading());
            grammaticalType.setText( annotation.getGrammaticalType());
            setEnabled( enabled);
        }
        updateTitle();
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
} // class AnnotationEditorPanel
