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

import jgloss.util.ListFormatter;
import jgloss.util.StringTools;
import jgloss.dictionary.*;

import java.util.Vector;
import java.awt.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Combo box which automatically performs searches on the entered text using the
 * parameters from a {@link LookupModel LookupModel}. The results of the search are
 * displayed in the popup menu of the combo box.
 */
public class AutoSearchComboBox extends JComboBox implements LookupResultHandler,
                                                             DocumentListener {
    protected LookupModel model;
    protected AsynchronousLookupEngine engine;

    protected DictionaryEntryFormatter wordReadingTranslation;
    protected DictionaryEntryFormatter readingWordTranslation;
    protected DictionaryEntryFormatter translationWordReading;
    protected DictionaryEntryFormatter currentFormatter;

    protected int limit;
    protected Vector items;
    protected String searchText;
    protected StringBuffer tempBuffer = new StringBuffer( 1024);


    private Highlighter matchHighlighter = new MatchHighlighter();
    private Highlighter partialHighlighter = new PartialHighlighter();
    private Highlighter highlighter = partialHighlighter;

    protected final static String PROTOTYPE = 
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";


    public class DefinitionRenderer extends JLabel  
        implements ListCellRenderer {

        private Insets textInsets = new Insets(0, 0, 0, 0);
        private Rectangle textBounds = new Rectangle();

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

        public void paintComponent(Graphics g) { 

            FontMetrics fm = g.getFontMetrics();
            Insets insets = this.getInsets(textInsets);
      
            int h = this.getHeight();
            int w = this.getWidth();

            textBounds.x = insets.left;
            textBounds.y = insets.top;
            textBounds.width = w - (insets.left + insets.right);
            textBounds.height = h - (insets.top + insets.bottom);

            int x = textBounds.x;
            int y = textBounds.y + fm.getAscent();
        
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
        engine = new AsynchronousLookupEngine( this, limit);
        ((JTextComponent) getEditor().getEditorComponent()).getDocument().addDocumentListener( this);
        setRenderer( cellRenderer);
        setPrototypeDisplayValue( PROTOTYPE);

        wordReadingTranslation = new DictionaryEntryFormatter();
        wordReadingTranslation.addWordFormat( DictionaryEntryFormat.getWordFormatter());
        wordReadingTranslation.addReadingFormat( DictionaryEntryFormat.getReadingFormatter());
        wordReadingTranslation.addTranslationFormat( DictionaryEntryFormat.getTranslationRomFormatter(),
                                                     DictionaryEntryFormat.getTranslationCrmFormatter(),
                                                     DictionaryEntryFormat.getTranslationSynFormatter());
        readingWordTranslation = new DictionaryEntryFormatter();
        readingWordTranslation.addReadingFormat( DictionaryEntryFormat.getReadingFormatter());
        readingWordTranslation.addWordFormat( DictionaryEntryFormat.getWordFormatter());
        readingWordTranslation.addTranslationFormat( DictionaryEntryFormat.getTranslationRomFormatter(),
                                                     DictionaryEntryFormat.getTranslationCrmFormatter(),
                                                     DictionaryEntryFormat.getTranslationSynFormatter());
        translationWordReading = new DictionaryEntryFormatter();
        translationWordReading.addTranslationFormat( DictionaryEntryFormat.getTranslationRomFormatter(),
                                                     DictionaryEntryFormat.getTranslationCrmFormatter(),
                                                     DictionaryEntryFormat.getTranslationSynFormatter());
        translationWordReading.addWordFormat( DictionaryEntryFormat.getWordFormatter());
        translationWordReading.addReadingFormat( DictionaryEntryFormat.getReadingFormatter());
 
        setEditable( true);
    }

    protected void doLookup() {
        synchronized (this) {
            String nextSearchText = ((JTextComponent) getEditor().getEditorComponent()).getText();
            if (nextSearchText.equals( searchText) || nextSearchText.length()==0)
                return;
            
            searchText = nextSearchText;
        }

        LookupModel tempModel = (LookupModel) model.clone();
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

    public void startLookup( LookupModel model) {
        items = new Vector( limit + 20);
    }

    public void dictionary( Dictionary d) {
        items.add( new Object[] { Dictionary.class, JGloss.messages.getString
                                  ( "wordlookup.matches",
                                    new String[] { d.getName() }) });
    }

    public void dictionaryEntry( DictionaryEntry de) {
        tempBuffer.setLength( 0);
        currentFormatter.format( de, tempBuffer);
        if (tempBuffer.length() > PROTOTYPE.length()) {
            tempBuffer.setLength( PROTOTYPE.length()-3);
            tempBuffer.append( "...");
        }
        items.add( new Object[] { DictionaryEntry.class, tempBuffer.toString() });
    }

    public void exception( SearchException ex){
    }

    public void note( String note) {
    }

    public void endLookup() {
        final ComboBoxModel newModel = new DefaultComboBoxModel( items);
        items = null;
        Runnable updater = new Runnable() {
                public void run() {
                    JTextComponent c = (JTextComponent) getEditor().getEditorComponent();
                    int caret = c.getCaretPosition();
                    newModel.setSelectedItem
                        ( c.getText());
                    setModel( newModel);
                    c.setCaretPosition( caret);
                    AutoSearchComboBox.this.showPopup();
                }
            };
        if (EventQueue.isDispatchThread()) {
            updater.run();
        }
        else {
            EventQueue.invokeLater( updater);
        }
    }

    public void insertUpdate( DocumentEvent e) {
        doLookup();
    }

    public void removeUpdate( DocumentEvent e) {
        doLookup();
    }

    public void changedUpdate( DocumentEvent e) {
        doLookup();
    }

    public void setSelectedItem( Object o) {
        /* Implement some intelligent dictionary entry selection later.
           Completely disable selection for now.
          if (o!=null && o instanceof Object[]) {
            Object[] data = (Object[]) o;
            if (data[0] == DictionaryEntry.class)
                super.setSelectedItem( data[1]);
                }*/
    }

    protected void finalize() {
        engine.dispose();
    }
} // class AutoSearchComboBox
