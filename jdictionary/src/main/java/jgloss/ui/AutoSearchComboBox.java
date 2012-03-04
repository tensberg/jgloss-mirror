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

package jgloss.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import jgloss.JGloss;
import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.DictionaryEntryFormatter;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.SearchException;
import jgloss.util.StringTools;

/**
 * Combo box which automatically performs searches on the entered text using the
 * parameters from a {@link LookupModel LookupModel}. The results of the search are
 * displayed in the popup menu of the combo box.
 *
 * @author Michael Koch
 */
public class AutoSearchComboBox extends JComboBox implements LookupResultHandler,
                                                             DocumentListener {
    private static final long serialVersionUID = 1L;

    protected LookupModel model;
    protected AsynchronousLookupEngine engine;

    protected DictionaryEntryFormatter wordReadingTranslation;
    protected DictionaryEntryFormatter readingWordTranslation;
    protected DictionaryEntryFormatter translationWordReading;
    protected DictionaryEntryFormatter currentFormatter;

    protected int limit;
    protected List<Object[]> items;
    protected String searchText;
    protected StringBuilder tempBuffer = new StringBuilder( 1024);

    private boolean dontConfigureEditor;
    private boolean dontDoLookup;

    private Highlighter partialHighlighter = new PartialHighlighter();
    private Highlighter highlighter = partialHighlighter;

    public class DefinitionRenderer extends JLabel  
        implements ListCellRenderer {

        private static final long serialVersionUID = 1L;

        private Insets textInsets = new Insets(0, 0, 0, 0);
        private Rectangle textBounds = new Rectangle();

        @Override
		public Component getListCellRendererComponent(JList list, Object value, 
                                                      int index, boolean isSelected, 
                                                      boolean cellHasFocus) {

            // Colorize it
            Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
            Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();

            this.setText(String.valueOf(value));
            this.setFont(list.getFont());
      
            this.setBackground(background);
            this.setForeground(foreground);

            return this;

        }

        @Override
		public void paintComponent(Graphics g) { 

            Insets insets = this.getInsets(textInsets);
      
            int h = this.getHeight();
            int w = this.getWidth();

            textBounds.x = insets.left;
            textBounds.y = insets.top;
            textBounds.width = w - (insets.left + insets.right);
            textBounds.height = h - (insets.top + insets.bottom);
        
            g.setColor(this.getBackground());
            g.fillRect(0, 0, w, h);

            g.setColor(this.getForeground());
            highlighter.paintHighlight(g, getText(), searchText);     
      
        }
                                     
    }

    protected ListCellRenderer cellRenderer = new ListCellRenderer() {
            private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
            private DefinitionRenderer highlightRenderer = new DefinitionRenderer();
            private Font plainFont = null;
            private Font boldFont = null;
            @Override
			public Component getListCellRendererComponent( JList list, Object value,
                                                           int index,
                                                           boolean isSelected,
                                                           boolean cellHasFocus) {
                if (value instanceof String)
                    return defaultRenderer.getListCellRendererComponent
                        ( list, value, index, isSelected, cellHasFocus);

                Object[] data = (Object[]) value;
                Component out;
                if (data[0] == Dictionary.class) {
                    out = defaultRenderer.getListCellRendererComponent
                        ( list, data[1], index, false, false);
                }
                else
                    out = highlightRenderer.getListCellRendererComponent
                        ( list, data[1], index, false, false);

                if (plainFont == null) {
                    plainFont = out.getFont();
                    boldFont = plainFont.deriveFont( Font.BOLD);
                }

                if (data[0] == Dictionary.class) {
                    out.setFont( boldFont);
                }
                else {
                    out.setFont( plainFont);
                }

                return out;
            }
        };

    public AutoSearchComboBox( LookupModel _model, int _limit) {
        model = _model;
        limit = _limit;

        setPrototypeDisplayValue( "aaa");

        engine = new AsynchronousLookupEngine( this, limit);
        setModel( new ReplaceableItemsComboBoxModel());
        ((JTextComponent) getEditor().getEditorComponent()).getDocument().addDocumentListener( this);
        setRenderer( cellRenderer);

        wordReadingTranslation = new DictionaryEntryFormatter();
        wordReadingTranslation.addWordFormat( DictionaryEntryFormat.getWordFormatter());
        wordReadingTranslation.addReadingFormat( DictionaryEntryFormat.getReadingFormatter());
        wordReadingTranslation.addTranslationFormat( DictionaryEntryFormat.getTranslationRomFormatter(),
                                                     DictionaryEntryFormat.getTranslationCrmFormatter(),
                                                     DictionaryEntryFormat
                                                     .getTranslationSynonymFormatter());
        readingWordTranslation = new DictionaryEntryFormatter();
        readingWordTranslation.addReadingFormat( DictionaryEntryFormat.getReadingFormatter());
        readingWordTranslation.addWordFormat( DictionaryEntryFormat.getWordFormatter());
        readingWordTranslation.addTranslationFormat( DictionaryEntryFormat.getTranslationRomFormatter(),
                                                     DictionaryEntryFormat.getTranslationCrmFormatter(),
                                                     DictionaryEntryFormat
                                                     .getTranslationSynonymFormatter());
        translationWordReading = new DictionaryEntryFormatter();
        translationWordReading.addTranslationFormat( DictionaryEntryFormat.getTranslationRomFormatter(),
                                                     DictionaryEntryFormat.getTranslationCrmFormatter(),
                                                     DictionaryEntryFormat
                                                     .getTranslationSynonymFormatter());
        translationWordReading.addWordFormat( DictionaryEntryFormat.getWordFormatter());
        translationWordReading.addReadingFormat( DictionaryEntryFormat.getReadingFormatter());
 
        setEditable( true);
    }

    public void setLookupModel( LookupModel _model) {
        model = _model;
    }

    public LookupModel getLookupModel() { return model; }

    protected void doLookup() {
        if (dontDoLookup)
            return;

        synchronized (this) {
            String nextSearchText = ((JTextComponent) getEditor().getEditorComponent()).getText();
            if (nextSearchText.equals( searchText) || nextSearchText.length()==0)
                return;
            
            searchText = nextSearchText;
        }

        LookupModel tempModel = model.clone();
        if (tempModel.getSelectedSearchMode() == ExpressionSearchModes.EXACT) try {
            tempModel.selectSearchMode( ExpressionSearchModes.PREFIX);
        } catch (IllegalArgumentException ex) {}

        tempModel.setSearchExpression( searchText);
        if (StringTools.isHiragana( searchText.charAt( 0)) || 
            StringTools.isKatakana( searchText.charAt( 0)))
            currentFormatter = readingWordTranslation;
        else if (StringTools.isKanji( searchText.charAt( 0)))
            currentFormatter = wordReadingTranslation;
        else
            currentFormatter = translationWordReading;
        engine.doLookup( tempModel);
    }

    @Override
	public void startLookup( String description) {
        startLookup();
    }

    @Override
	public void startLookup( LookupModel model) {
        startLookup();
    }

    protected void startLookup() {
        items = new ArrayList<Object[]>( limit + 20);
    }

    @Override
	public void dictionary( Dictionary d) {
        items.add( new Object[] { Dictionary.class, JGloss.messages.getString
                                  ( "wordlookup.matches",
                                    new String[] { d.getName() }) });
    }

    @Override
	public void dictionaryEntry( DictionaryEntry de) {
        tempBuffer.setLength( 0);
        currentFormatter.format( de, tempBuffer);
        items.add( new Object[] { DictionaryEntry.class, tempBuffer.toString() });
    }

    @Override
	public void exception( SearchException ex){
    }

    @Override
	public void note( String note) {
    }

    @Override
	public void endLookup() {
        final List<Object[]> newItems = items;
        items = null;

        Runnable updater = new Runnable() {
                @Override
				public void run() {
                    // Changing the combo box model has the side effect of calling
                    // configureEditor. This messes up the editor text and is unneccessary
                    // anyway, so switch it off.
                    dontConfigureEditor = true;
                    ((ReplaceableItemsComboBoxModel) getModel()).replaceItems( newItems);
                    dontConfigureEditor = false;
                    AutoSearchComboBox.this.setPopupVisible(false);
                    AutoSearchComboBox.this.setPopupVisible(true);
                }
            };

        if (EventQueue.isDispatchThread()) {
            updater.run();
        }
        else {
            EventQueue.invokeLater( updater);
        }
    }

    @Override
	public void insertUpdate( DocumentEvent e) {
        doLookup();
    }

    @Override
	public void removeUpdate( DocumentEvent e) {
        doLookup();
    }

    @Override
	public void changedUpdate( DocumentEvent e) {
        doLookup();
    }

    @Override
	public void setSelectedItem( Object o) {
        if (o instanceof String) {
            dontDoLookup = true;
            super.setSelectedItem( o);
            dontDoLookup = false;
        }
        /* Implement some intelligent dictionary entry selection later.
           Completely disable selection for now.

          if (o!=null && o instanceof Object[]) {
            Object[] data = (Object[]) o;
            if (data[0] == DictionaryEntry.class)
                super.setSelectedItem( data[1]);
                }*/
    }

    @Override
	public void configureEditor( ComboBoxEditor editor, Object anObject) {
        // Changing the combo box model in endLookup has the side effect of calling
        // configureEditor. Since this messes up the editor text and is unneccessary
        // anyway, it can be switched off by setting dontConfigureEditor
        if (dontConfigureEditor)
            return;

        super.configureEditor( editor, anObject);
    }

    @Override
	protected void finalize() {
        engine.dispose();
    }
} // class AutoSearchComboBox
