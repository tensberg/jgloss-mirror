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


import static java.util.logging.Level.SEVERE;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.DictionaryEntryFormatter;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.UnsupportedSearchModeException;
import jgloss.ui.util.XCVManager;

public class LookupResultList extends JPanel implements LookupResultHandler {
	private static final Logger LOGGER = Logger.getLogger(LookupResultList.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_STYLE_SHEET = "/data/lookup.css";

	private static final int MAX_RESULT_BUFFER_SIZE = 50;
	
    /**
     * Text field used to display the result as HTML text.
     */
    private final JEditorPane resultPane;
    private final JScrollPane resultScroller;

    private final int entryLimit;
    private final boolean showAllDictionaries;

    private final DictionaryEntryFormatter htmlFormatter;
    private final LookupResultMarker marker;
    private final LookupResultHyperlinker hyperlinker;
    
    private boolean multipleDictionaries;
    private final List<Object> resultBuffer = new ArrayList<Object>(MAX_RESULT_BUFFER_SIZE);
    private final StringBuilder resultTextBuffer = new StringBuilder( 8192);
    private String previousDictionaryName;
    private boolean previousDictionaryHasMatch;
    private final JLabel status;
    private String searchExpression;
    private int entryCount;

    public LookupResultList() {
        this( 500);
    }

    public LookupResultList( int _entryLimit) {
        this( _entryLimit, null, true, new LookupResultHyperlinker());
    }

    public LookupResultList( int _entryLimit, URL _styleSheet, boolean _showAllDictionaries,
                             LookupResultHyperlinker _hyperlinker) {
        setLayout( new BorderLayout());

        entryLimit = _entryLimit;
        showAllDictionaries = _showAllDictionaries;
        if (_styleSheet == null) {
	        _styleSheet = LookupResultList.class.getResource( DEFAULT_STYLE_SHEET);
        }
        hyperlinker = _hyperlinker;

        resultPane = new JEditorPane();
        resultPane.setContentType( "text/html");
        resultPane.setEditable( false);
        
        StyleSheet styleSheet = ((HTMLDocument) resultPane.getDocument()).getStyleSheet();
        styleSheet.importStyleSheet( _styleSheet);
        styleSheet.addRule
            ( "body { font-family: '" + JGloss.PREFS.getString( Preferences.FONT_WORDLOOKUP) +
              "'; font-size: " + JGloss.PREFS.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12) + 
              "pt; }\n");

        Action transferFocus = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    transferFocus();
                }
            };
        resultPane.getKeymap().addActionForKeyStroke
            ( KeyStroke.getKeyStroke( "pressed TAB"), transferFocus);

        // update display if user changed font
        JGloss.PREFS.addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                @Override
				public void propertyChange( java.beans.PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP) ||
                        e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP_SIZE)) {
                        String fontname = JGloss.PREFS.getString( Preferences.FONT_WORDLOOKUP);
                        int size = JGloss.PREFS.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12);
                        Font font = new Font( fontname, Font.PLAIN, size);
                        ((HTMLDocument) resultPane.getDocument()).getStyleSheet().addRule
                            ( "body { font-family: '" + fontname +
                              "'; font-size: " + size +
                              "pt; }\n");
                        resultPane.setFont( font);
                    }
                }
            });

        resultScroller = new JScrollPane( resultPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( resultScroller, BorderLayout.CENTER);
        status = new JLabel( " ");
        this.add( status, BorderLayout.SOUTH);

        marker = new LookupResultMarker( hyperlinker);
        htmlFormatter = DictionaryEntryFormat.createFormatter( marker);
    }

    public void addHyperlinkListener( HyperlinkListener listener) {
        resultPane.addHyperlinkListener( listener);
    }

    public void removeHyperlinkListener( HyperlinkListener listener) {
        resultPane.removeHyperlinkListener( listener);
    }

    public void addToXCVManager( XCVManager manager) {
        manager.addManagedComponent( resultPane);
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
    	resultPane.setText("");
        marker.setMarkedText( searchExpression);
        hyperlinker.clearReferences();
        previousDictionaryName = null;
        previousDictionaryHasMatch = true;
        entryCount = 0;
    }

    @Override
	public void dictionary( Dictionary d) {
        addToResultBuffer( d);
    }

    @Override
	public void dictionaryEntry( DictionaryEntry de) {
    	entryCount++;
    	if (entryCount <= entryLimit) {
    		addToResultBuffer( de);
    	}
    }

    @Override
	public void exception( SearchException ex) {
        addToResultBuffer( ex);
    }

    @Override
	public void note( String note) {
        addToResultBuffer( note);
    }

    private void addToResultBuffer( Object o) {
        resultBuffer.add( o);
        if (resultBuffer.size() == MAX_RESULT_BUFFER_SIZE) {
	        flushBuffer();
	        updateStatusText();
        }
    }

    private void format( Dictionary d) {
        if (showAllDictionaries && !previousDictionaryHasMatch) {
            // No match in the previous dictionary. Print the dictionary name
            // and a comment.

            formatDictionaryName( previousDictionaryName);
            resultTextBuffer.append( "<p>");
            resultTextBuffer.append( JGloss.MESSAGES.getString( "wordlookup.nomatches_dictionary"));
            resultTextBuffer.append( "</p>");
            resultTextBuffer.append( "\n\n");
        }

        previousDictionaryName = d.getName();
        previousDictionaryHasMatch = false;
    }

    private void formatDictionaryName( String name) {
    	resultTextBuffer.append( "<h4>");
    	resultTextBuffer.append( JGloss.MESSAGES.getString( "wordlookup.matches", "<font color=\"green\">" +
    					name + "</font>"));
    	resultTextBuffer.append( "</h4>");
        resultTextBuffer.append( "\n\n");
    }

    private void format( DictionaryEntry de) {
        if (previousDictionaryName != null) {
            // First entry for this dictionary. Print the dictionary name
            // if multi-dictionary mode is active.
            if (multipleDictionaries) {
	            formatDictionaryName( previousDictionaryName);
            }
            previousDictionaryName = null;
        }

        previousDictionaryHasMatch = true;

        resultTextBuffer.append( "<p>");
        htmlFormatter.format( de, resultTextBuffer);
        resultTextBuffer.append( "</p>");

        resultTextBuffer.append( "\n\n");
    }

    private void format( SearchException ex) {
        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        
        resultTextBuffer.append( "<p><font color=\"red\">");
        if (ex instanceof UnsupportedSearchModeException) {
            resultTextBuffer.append( JGloss.MESSAGES.getString( "wordlookup.unsupportedsearchmode"));
        }
        else {
            resultTextBuffer.append
                ( JGloss.MESSAGES.getString( "wordlookup.exception",
                                             new Object[] { ex.getClass().getName(),
                                                            ex.getLocalizedMessage() }));
        }
        resultTextBuffer.append( "</font></p>");
        resultTextBuffer.append( "\n\n");
    }

    private void format( String note) {
    	resultTextBuffer.append( "<p><i>");
    	resultTextBuffer.append( note);
    	resultTextBuffer.append( "</i></p>");
    	resultTextBuffer.append( "\n\n");
    }

    @Override
	public void endLookup() {
    	flushBuffer();
        updateStatusText(false);
    }

    private void updateStatusText() {
    	updateStatusText(true);
    }
    
	private void updateStatusText(boolean performingLookup) {
		String key = performingLookup ? "wordlookup.status.searching" : "wordlookup.status.matches";
	    String message = JGloss.MESSAGES.getString( key, Integer.valueOf( entryCount));
	    if (entryCount > entryLimit) {
	    	message += " " + JGloss.MESSAGES.getString("wordlookup.status.limit", Integer.valueOf(entryLimit));
	    }
		updateStatusText( message);
    }

    private void flushBuffer() {
    	assert EventQueue.isDispatchThread();
    	
        for (Object o : resultBuffer) {
            if (o instanceof Dictionary) {
	            format( (Dictionary) o);
            } else if (o instanceof DictionaryEntry) {
	            format( (DictionaryEntry) o);
            } else if (o instanceof SearchException) {
	            format( (SearchException) o);
            } else {
	            format( String.valueOf( o));
            }
        }
        resultBuffer.clear();

        // build complete html structure
        resultTextBuffer.insert( 0, "<html><head></head><body id=\"body\">");
        resultTextBuffer.append( "</body></html>");

        HTMLDocument document = (HTMLDocument) resultPane.getDocument();
        if (document.getLength() == 0) {
        	resultPane.setText(resultTextBuffer.toString());
        } else {
        	try {
        		document.insertBeforeEnd(document.getElement("body"), resultTextBuffer.toString());
        	} catch (Exception ex) {
        		LOGGER.log(SEVERE, "failed to append search result", ex);
        	}
        }
        resultTextBuffer.setLength( 0);
    }

    private void updateStatusText( final String text) {
    	assert EventQueue.isDispatchThread();
    	
    	status.setText( text);
    }

    /**
     * Stores the scroll pane position of the lookup result list.
     */
    public static class ViewState {
        private final Point resultScrollerPosition;

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
    public JEditorPane getResultPane() {
        return resultPane;
    }
} // class LookupResultList
