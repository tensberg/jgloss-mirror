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
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.MatchMode;

import java.lang.ref.WeakReference;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.EventQueue;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Arrays;
import java.util.Collections;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

class SimpleLookup extends JPanel implements ActionListener {
    private static final String STYLE_SHEET = "/data/lookup-minimal.css";

    private AutoSearchComboBox expression;
    private LookupModel model;

    private AsynchronousLookupEngine engine;
    private LookupResultList list;

    /**
     * A dictionary change listener which will update a lookup model if the dictionary list
     * changes, but will not prevent the model from being garbage collected. The model is
     * stored in a weak reference. When the model is garbage collected, the listener is
     * removed from the {@link Dictionaries Dictionaries} listener list.
     */
    private static class WeakDictionaryChangeListener 
        implements Dictionaries.DictionaryListChangeListener {
        private WeakReference modelRef;

        WeakDictionaryChangeListener( LookupModel model) {
            modelRef = new WeakReference( model) {
                    public boolean enqueue() {
                        boolean enqueued = super.enqueue();
                        if (enqueued)
                            Dictionaries.removeDictionaryListChangeListener
                                ( WeakDictionaryChangeListener.this);
                        return enqueued;
                    }
                };
        }

        public void dictionaryListChanged() {
            LookupModel model = (LookupModel) modelRef.get();
            if (model != null)
                model.setDictionaries
                    ( Arrays.asList( Dictionaries.getDictionaries( false)));
        }
    } // class WeakDictionaryChangeListener

    SimpleLookup( Component[] additionalControls) {
        model = new LookupModel
            ( Arrays.asList( new Object[] { ExpressionSearchModes.EXACT,
                                            ExpressionSearchModes.PREFIX,
                                            ExpressionSearchModes.ANY }),
              Arrays.asList( Dictionaries.getDictionaries( false)),
              Collections.EMPTY_LIST);
        Dictionaries.addDictionaryListChangeListener
            ( new WeakDictionaryChangeListener( model));
        model.selectAllDictionaries( true);
        model.selectSearchField( DictionaryEntryField.WORD, true);
        model.selectSearchField( DictionaryEntryField.READING, true);
        model.selectSearchField( DictionaryEntryField.TRANSLATION, false);
        model.selectMatchMode( MatchMode.FIELD, true);

        setLayout( new BorderLayout());

        JPanel controls = new JPanel();
        controls.setLayout( new GridBagLayout());
        GridBagConstraints fixedC = new GridBagConstraints();
        fixedC.anchor = GridBagConstraints.WEST;
        fixedC.gridy = 0;
        fixedC.fill = GridBagConstraints.NONE;
        GridBagConstraints expandableC = new GridBagConstraints();
        expandableC.anchor = GridBagConstraints.WEST;
        expandableC.gridy = 0;
        expandableC.fill = GridBagConstraints.HORIZONTAL;
        expandableC.weightx = 1.0f;

        expression = new AutoSearchComboBox( model, 50);
        JLabel expressionDescription = 
            new JLabel( JGloss.messages.getString( "wordlookup.enterexpression"));
        expressionDescription.setDisplayedMnemonic
            ( JGloss.messages.getString( "wordlookup.enterexpression.mk").charAt( 0));
        expressionDescription.setLabelFor( expression);
        controls.add( expressionDescription, fixedC);
        controls.add( expression, expandableC);

        JButton search = new JButton();
        UIUtilities.initButton( search, "wordlookup.search");
        search.addActionListener( this);
        controls.add( search, fixedC);

        if (additionalControls != null) {
            for ( int i=0; i<additionalControls.length; i++)
                controls.add( additionalControls[i], fixedC);
        }

        this.add( controls, BorderLayout.NORTH);

        list = new LookupResultList( 100, SimpleLookup.class.getResource( STYLE_SHEET), false);
        this.add( list, BorderLayout.CENTER);
        engine = new AsynchronousLookupEngine( list);
    }

    public void search( String text) {
        if (text == null || text.length()==0)
            return;

        expression.setSelectedItem( text);

        final LookupModel modelClone = (LookupModel) model.clone();
        modelClone.setSearchExpression( text);

        // Try a lookup with each search mode until at least one entry is found.
        // To do this in asynchronous search mode, a runnable is created which is executed
        // after a search ends. If the search did not find any results, the runnable selects
        // the next search mode and repeats the search.
        modelClone.selectSearchMode( 0);
        engine.doLookup( modelClone, new Runnable() {
                public void run() {
                    if (list.getEntryCount() == 0) try {
                        model.selectSearchMode( model.getSelectedSearchModeIndex() + 1);
                        engine.doLookup( modelClone, this);
                    } catch (IndexOutOfBoundsException ex) {
                        // All search modes tried. End search.
                    }
                }
            });
    }

    public void actionPerformed( ActionEvent e) {
        search( expression.getSelectedItem().toString());
    }
} // class SimpleLookup
