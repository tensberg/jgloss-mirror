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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.DictionaryEntryFormatter;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.UnsupportedSearchModeException;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.util.ListFormatter;

public class LookupResultList extends JPanel implements LookupResultHandler {
    private static class Marker implements DictionaryEntryFormat.Decorator {
        private MarkerListFormatter.Group markerGroup = 
            new MarkerListFormatter.Group( "<font color=\"blue\">", "</font>");
        private DictionaryEntryFormat.Decorator child;

        private Marker( DictionaryEntryFormat.Decorator _child) {
            child = _child;
        }

        @Override
		public ListFormatter decorateList( ListFormatter formatter, int type) {
            if (type==WORD || type==READING || type==TRANSLATION_SYN) {
                formatter = new MarkerListFormatter( markerGroup, formatter);
            }

            if (child != null)
                formatter = child.decorateList( formatter, type);

            return formatter;
        }

        @Override
		public ListFormatter decorateList( ListFormatter formatter, Attribute type,
                                           int position) {
            if (type == Attributes.EXPLANATION)
                formatter = new MarkerListFormatter( markerGroup, formatter);

            if (child != null)
                formatter = child.decorateList( formatter, type, position);
            
            return formatter;
        }

        @Override
		public AttributeFormatter decorateAttribute( AttributeFormatter formatter, Attribute type,
                                                     int position) {
            if (child != null)
                formatter = child.decorateAttribute( formatter, type, position);

            return formatter;
        }

        public void setMarkedText( String text) {
            markerGroup.setMarkedText( text);
        }
    } // class Marker

    public static class Hyperlinker extends DictionaryEntryFormat.IdentityDecorator {
        public static final String WORD_PROTOCOL = "wo";
        public static final String READING_PROTOCOL = "re";
        public static final String TRANSLATION_PROTOCOL = "tr";
        public static final String REFERENCE_PROTOCOL = "ref";
        public static final String ATTRIBUTE_BEFORE_PROTOCOL = "atb";
        public static final String ATTRIBUTE_AFTER_PROTOCOL = "ata";

        private boolean words;
        private boolean readings;
        private boolean translations;
        private boolean references;
        private boolean allAttributes;

        private Map hyperrefs;

        public Hyperlinker() {
            this( false, false, false, true, false);
        }

        public Hyperlinker( boolean _words, boolean _readings, boolean _translations,
                            boolean _references, boolean _allAttributes) {
            words = _words;
            readings = _readings;
            translations = _translations;
            references = _references | _allAttributes;
            allAttributes = _allAttributes;

            hyperrefs = new HashMap();
        }

        @Override
		public ListFormatter decorateList( ListFormatter formatter, int type) {
            if (words && type==WORD)
                formatter = new HyperlinkListFormatter( WORD_PROTOCOL, hyperrefs, formatter);
            else if (readings && type==READING)
                formatter = new HyperlinkListFormatter( READING_PROTOCOL, hyperrefs, formatter);
            else if (translations && type==TRANSLATION_SYN)
                formatter = new HyperlinkListFormatter( TRANSLATION_PROTOCOL, hyperrefs, formatter);

            return formatter;
        }

        @Override
		public AttributeFormatter decorateAttribute( AttributeFormatter formatter,
                                                     Attribute type, int position) {
            if (references && type.canHaveValue() && 
                ReferenceAttributeValue.class.isAssignableFrom
                ( type.getAttributeValueClass())) {
                formatter = new HyperlinkAttributeFormatter( REFERENCE_PROTOCOL,
                                                             true, hyperrefs, formatter);
            }
            else if (allAttributes) {
                formatter = new HyperlinkAttributeFormatter( (position==POSITION_BEFORE) ? 
                                                             ATTRIBUTE_BEFORE_PROTOCOL :
                                                             ATTRIBUTE_AFTER_PROTOCOL,
                                                             true, hyperrefs, formatter);
            }
            
            return formatter;
        }

        public Object getReference( String key) {
            return hyperrefs.get( key);
        }

        public void clearReferences() {
            hyperrefs.clear();
        }
    } // class Hyperlinker

    protected final static String DEFAULT_STYLE_SHEET = "/data/lookup.css";

    /**
     * Text field used to display the result as HTML text.
     */
    protected JEditorPane resultFancy;
    /**
     * Text field used to display the result as plain text, if the result list becomes too
     * long for fast HTML rendering.
     */
    protected JTextArea resultPlain;
    protected JScrollPane resultScroller;

    protected int fancyLimit;
    protected boolean showAllDictionaries;

    protected DictionaryEntryFormatter plainFormatter;
    protected DictionaryEntryFormatter htmlFormatter;
    protected Marker marker;
    protected Hyperlinker hyperlinker;
    
    protected boolean multipleDictionaries;
    protected List resultBuffer;
    protected StringBuilder resultTextBuffer = new StringBuilder( 8192);
    protected int entriesInTextBuffer;
    protected int dictionaryEntries;
    protected String previousDictionaryName;
    protected boolean previousDictionaryHasMatch;
    protected JLabel status;
    protected String searchExpression;
    protected int entryCount;

    protected final static int BUFFER_LIMIT = 500;

    public LookupResultList() {
        this( 300);
    }

    public LookupResultList( int _fancyLimit) {
        this( _fancyLimit, null, true, new Hyperlinker());
    }

    public LookupResultList( int _fancyLimit, URL _styleSheet, boolean _showAllDictionaries,
                             Hyperlinker _hyperlinker) {
        setLayout( new BorderLayout());

        fancyLimit = _fancyLimit;
        showAllDictionaries = _showAllDictionaries;
        if (_styleSheet == null)
            _styleSheet = LookupResultList.class.getResource( DEFAULT_STYLE_SHEET);
        hyperlinker = _hyperlinker;

        resultFancy = new JEditorPane();
        resultFancy.setContentType( "text/html");
        resultFancy.setEditable( false);
        
        StyleSheet styleSheet = ((HTMLDocument) resultFancy.getDocument()).getStyleSheet();
        styleSheet.importStyleSheet( _styleSheet);
        styleSheet.addRule
            ( "body { font-family: '" + JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP) +
              "'; font-size: " + JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12) + 
              "pt; }\n");

        resultPlain = new JTextArea();
        resultPlain.setFont( new Font( JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP),
                                       Font.PLAIN, 
                                       JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12)));
        resultPlain.setEditable( false);
        resultPlain.setLineWrap( true);
        resultPlain.setWrapStyleWord( true);

        Action transferFocus = new AbstractAction() {
                @Override
				public void actionPerformed( ActionEvent e) {
                    transferFocus();
                }
            };
        resultFancy.getKeymap().addActionForKeyStroke
            ( KeyStroke.getKeyStroke( "pressed TAB"), transferFocus);
        resultPlain.getKeymap().addActionForKeyStroke
            ( KeyStroke.getKeyStroke( "pressed TAB"), transferFocus);

        // update display if user changed font
        JGloss.prefs.addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                @Override
				public void propertyChange( java.beans.PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP) ||
                        e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP_SIZE)) {
                        String fontname = JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP);
                        int size = JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12);
                        Font font = new Font( fontname, Font.PLAIN, size);
                        ((HTMLDocument) resultFancy.getDocument()).getStyleSheet().addRule
                            ( "body { font-family: '" + fontname +
                              "'; font-size: " + size +
                              "pt; }\n");
                        resultFancy.setFont( font);
                        resultPlain.setFont( font);
                    }
                }
            });

        resultScroller = new JScrollPane( resultFancy, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( resultScroller, BorderLayout.CENTER);
        status = new JLabel( " ");
        this.add( status, BorderLayout.SOUTH);

        marker = new Marker( hyperlinker);
        htmlFormatter = DictionaryEntryFormat.createFormatter( marker);
        plainFormatter = DictionaryEntryFormat.createFormatter();
    }

    public void addHyperlinkListener( HyperlinkListener listener) {
        resultFancy.addHyperlinkListener( listener);
    }

    public void removeHyperlinkListener( HyperlinkListener listener) {
        resultFancy.removeHyperlinkListener( listener);
    }

    public void addToXCVManager( XCVManager manager) {
        manager.addManagedComponent( resultFancy);
        manager.addManagedComponent( resultPlain);
    }

    public int getEntryCount() { return entryCount; }

    public Object getReference( String key) {
        return hyperlinker.getReference( key);
    }

    @Override
	public void startLookup( String description) {
        searchExpression = null;
        multipleDictionaries = true;
        startLookup();
    }

    @Override
	public void startLookup( LookupModel model) {
        multipleDictionaries = model.getSelectedDictionaries().size() > 1;
        searchExpression = model.isSearchExpressionEnabled() ? model.getSearchExpression() : null;
        startLookup();
    }

    private void startLookup() {
        marker.setMarkedText( searchExpression);
        hyperlinker.clearReferences();
        previousDictionaryName = null;
        previousDictionaryHasMatch = true;
        dictionaryEntries = 0;
        entriesInTextBuffer = 0;
        resultBuffer = new ArrayList( fancyLimit);
        entryCount = 0;
    }

    @Override
	public void dictionary( Dictionary d) {
        if (!addToResultBuffer( d))
            formatNow( d);
    }

    @Override
	public void dictionaryEntry( DictionaryEntry de) {
        if (!addToResultBuffer( de))
            formatNow( de);

        entryCount++;
    }

    @Override
	public void exception( SearchException ex) {
        if (!addToResultBuffer( ex))
            formatNow( ex);
    }

    @Override
	public void note( String note) {
        if (!addToResultBuffer( note))
            formatNow( note);
    }

    protected boolean addToResultBuffer( Object o) {
        if (resultBuffer == null)
            return false;

        resultBuffer.add( o);
        if (resultBuffer.size() == fancyLimit)
            flushBuffer( false);
        return true;
    }

    protected void formatNow( Object o) {
        if (o instanceof Dictionary)
            format( (Dictionary) o, false);
        else if (o instanceof DictionaryEntry)
            format( (DictionaryEntry) o, false);
        else if (o instanceof SearchException)
            format( (SearchException) o, false);
        else
            format( String.valueOf( o), false);
        if (++entriesInTextBuffer > BUFFER_LIMIT)
            flushTextBuffer();
    }   

    protected void format( Dictionary d, boolean fancy) {
        if (showAllDictionaries && !previousDictionaryHasMatch) {
            // No match in the previous dictionary. Print the dictionary name
            // and a comment.

            formatDictionaryName( previousDictionaryName, fancy);
            if (fancy)
                resultTextBuffer.append( "<p>");
            resultTextBuffer.append( JGloss.messages.getString( "wordlookup.nomatches_dictionary"));
            if (fancy)
                resultTextBuffer.append( "</p>");
            resultTextBuffer.append( "\n\n");
        }

        previousDictionaryName = d.getName();
        previousDictionaryHasMatch = false;
    }

    protected void formatDictionaryName( String name, boolean fancy) {
        if (fancy) {
            resultTextBuffer.append( "<h4>");
            resultTextBuffer.append
                ( JGloss.messages.getString( "wordlookup.matches",
                                             new String[] { "<font color=\"green\">" +
                                                            name + "</font>" }));
            resultTextBuffer.append( "</h4>");
        }
        else {
            resultTextBuffer.append( JGloss.messages.getString( "wordlookup.matches",
                                                                new String[] { name }));
        }
        resultTextBuffer.append( "\n\n");
    }

    protected void format( DictionaryEntry de, boolean fancy) {
        if (previousDictionaryName != null) {
            // First entry for this dictionary. Print the dictionary name
            // if multi-dictionary mode is active.
            if (multipleDictionaries)
                formatDictionaryName( previousDictionaryName, fancy);
            previousDictionaryName = null;
        }

        previousDictionaryHasMatch = true;
        if (fancy) {
            resultTextBuffer.append( "<p>");
            htmlFormatter.format( de, resultTextBuffer);
            resultTextBuffer.append( "</p>");
        }
        else
            plainFormatter.format( de, resultTextBuffer);

        resultTextBuffer.append( "\n\n");
        dictionaryEntries++;
    }

    protected void format( SearchException ex, boolean fancy) {
        ex.printStackTrace();
        
        if (fancy)
            resultTextBuffer.append( "<p><font color=\"red\">");
        if (ex instanceof UnsupportedSearchModeException) {
            resultTextBuffer.append( JGloss.messages.getString( "wordlookup.unsupportedsearchmode"));
        }
        else {
            resultTextBuffer.append
                ( JGloss.messages.getString( "wordlookup.exception",
                                             new Object[] { ex.getClass().getName(),
                                                            ex.getLocalizedMessage() }));
        }
        if (fancy)
            resultTextBuffer.append( "</font></p>");
        resultTextBuffer.append( "\n\n");
    }

    protected void format( String note, boolean fancy) {
        if (fancy)
            resultTextBuffer.append( "<p><i>");
        resultTextBuffer.append( note);
        if (fancy)
            resultTextBuffer.append( "</i></p>");
        resultTextBuffer.append( "\n\n");
    }

    @Override
	public void endLookup() {
        if (resultBuffer != null)
            flushBuffer( true);
        else
            flushTextBuffer();
        updateStatusText( JGloss.messages.getString( "wordlookup.status.matches",
                                                     new Object[] { new Integer( dictionaryEntries) }));
    }

    protected void flushBuffer( final boolean fancy) {
        for ( Iterator i=resultBuffer.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Dictionary)
                format( (Dictionary) o, fancy);
            else if (o instanceof DictionaryEntry)
                format( (DictionaryEntry) o, fancy);
            else if (o instanceof SearchException)
                format( (SearchException) o, fancy);
            else
                format( String.valueOf( o), fancy);
        }
        resultBuffer = null;

        if (fancy) {
            // build complete html structure
            resultTextBuffer.insert( 0, "<html><head></head><body>");
            resultTextBuffer.append( "</body></html>");
        }

        final JTextComponent target = fancy ? (JTextComponent) resultFancy : 
            (JTextComponent) resultPlain;

        if (resultScroller.getViewport().getView() != target) {
            Runnable updater = new Runnable() {
                    @Override
					public void run() {
                        resultScroller.setViewportView( target);
                    }
                };
            
            if (EventQueue.isDispatchThread())
                updater.run();
            else try {
                EventQueue.invokeAndWait( updater);
            } catch (Exception ex) {}

            // preserve memory by deleting the old text from the unused view
            // setText is thread-safe
            if (fancy)
                resultPlain.setText( "");
            else
                resultFancy.setText( "");
        }

        target.setText( resultTextBuffer.toString());
        resultTextBuffer.setLength( 0);
    }

    protected void flushTextBuffer() {
        resultPlain.append( resultTextBuffer.toString());
        resultTextBuffer.setLength( 0);
        entriesInTextBuffer = 0;
        updateStatusText( JGloss.messages.getString( "wordlookup.status.searching",
                                                     new Object[] { new Integer( dictionaryEntries) }));
    }

    protected void updateStatusText( final String text) {
        Runnable updater = new Runnable() {
                @Override
				public void run() {
                    status.setText( text);
                }
            };
        if (EventQueue.isDispatchThread())
            updater.run();
        else 
            EventQueue.invokeLater( updater);
    }

    public static class ViewState {
        private Point resultScrollerPosition;

        private ViewState( Point _resultScrollerPosition) {
            resultScrollerPosition = _resultScrollerPosition;
        }
    }

    public ViewState saveViewState() {
        return new ViewState( resultScroller.getViewport().getViewPosition());
    }

    public void restoreViewState( ViewState state) {
        resultScroller.getViewport().setViewPosition( state.resultScrollerPosition);
    }

    /**
     * Return the editor pane which is used to display the result list marked up as HTML.
     */
    public JEditorPane getFancyResultPane() {
        return resultFancy;
    }

    /**
     * Return the editor pane which is used to display the result list marked up as plain text.
     */
    public JTextArea getPlainResultPane() {
        return resultPlain;
    }
} // class LookupResultList
