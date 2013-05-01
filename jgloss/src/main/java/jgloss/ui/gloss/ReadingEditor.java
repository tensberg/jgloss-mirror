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
 */

package jgloss.ui.gloss;

import java.awt.AWTEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jgloss.JGloss;
import jgloss.ui.annotation.Annotation;

/**
 * Editor for the list of readings of an annotation.
 */
class ReadingEditor extends JPanel implements ActionListener, FocusListener {
    private static final long serialVersionUID = 1L;

	private Annotation annotation;
    private final JLabel multiReadingLabel = new JLabel
        ( JGloss.MESSAGES.getString( "annotationeditor.readings"));
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
                    reading.addFocusListener(this);
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

    void updateDisplay() {
        if (annotation == null) {
            return;
        }

        for ( int i=0; i<annotation.getReadingCount(); i++) {
        	String readingBase = annotation.getReadingBase( i);
            readingLabels.get( i).setText
                ( JGloss.MESSAGES.getString( "annotationeditor.reading.base", readingBase));
            readings.get( i).setText
                ( annotation.getReading( i));
        }
    }

    @Override
	public void actionPerformed( ActionEvent e) {
        updateReading(e);
    }

    private void updateReading(AWTEvent e) {
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

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        updateReading(e);
    }
} // class ReadingEditor