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
import jgloss.Preferences;
import jgloss.dictionary.*;
import jgloss.dictionary.attribute.ReferenceAttributeValue;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class LookupResultList extends JPanel implements LookupResultHandler {
    protected final static String DEFAULT_STYLE_SHEET = "/data/lookup.css";

    /**
     * Text field used to display the result as HTML text.
     */
    protected JEditorPane resultFancy;
    protected JTextArea resultPlain;
    protected JScrollPane resultScroller;

    protected int fancyLimit;
    protected boolean showAllDictionaries;

    protected DictionaryEntryFormatter htmlFormatter;
    protected MarkerListFormatter.Group markerGroup;
    protected DictionaryEntryFormatter plainFormatter;
    
    protected boolean multipleDictionaries;
    protected List resultBuffer;
    protected StringBuffer resultTextBuffer = new StringBuffer( 8192);
    protected int entriesInTextBuffer;
    protected int dictionaryEntries;
    protected String previousDictionaryName;
    protected boolean previousDictionaryHasMatch;
    protected JLabel status;
    protected String searchExpression;
    protected Map references;
    protected int entryCount;

    protected final static int BUFFER_LIMIT = 500;

    public LookupResultList() {
        this( 300);
    }

    public LookupResultList( int _fancyLimit) {
        this( _fancyLimit, null, true);
    }

    public LookupResultList( int _fancyLimit, URL _styleSheet, boolean _showAllDictionaries) {
        setLayout( new BorderLayout());

        fancyLimit = _fancyLimit;
        showAllDictionaries = _showAllDictionaries;
        if (_styleSheet == null)
            _styleSheet = LookupResultList.class.getResource( DEFAULT_STYLE_SHEET);

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

        resultScroller = new JScrollPane( resultFancy, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( resultScroller, BorderLayout.CENTER);
        status = new JLabel();
        this.add( status, BorderLayout.SOUTH);

        references = new HashMap( fancyLimit*4+1);

        markerGroup = new MarkerListFormatter.Group( "<font color=\"blue\">", "</font>");
        htmlFormatter = DictionaryEntryFormat.createHTMLFormatter( markerGroup, references);
        plainFormatter = DictionaryEntryFormat.createFormatter();
    }

    public ReferenceAttributeValue getReference( String key) {
        return (ReferenceAttributeValue) references.get( key);
    }

    public void addHyperlinkListener( HyperlinkListener listener) {
        resultFancy.addHyperlinkListener( listener);
    }

    public void addToXCVManager( XCVManager manager) {
        manager.addManagedComponent( resultFancy);
        manager.addManagedComponent( resultPlain);
    }

    public int getEntryCount() { return entryCount; }

    public void startLookup( String description) {
        searchExpression = null;
        multipleDictionaries = true;
        startLookup();
    }

    public void startLookup( LookupModel model) {
        multipleDictionaries = model.getSelectedDictionaries().size() > 1;
        searchExpression = model.isSearchExpressionEnabled() ? model.getSearchExpression() : null;
        startLookup();
    }

    private void startLookup() {
        markerGroup.setMarkedText( searchExpression);
        previousDictionaryHasMatch = true;
        dictionaryEntries = 0;
        entriesInTextBuffer = 0;
        resultBuffer = new ArrayList( fancyLimit);
        entryCount = 0;
    }

    public void dictionary( Dictionary d) {
        if (!addToResultBuffer( d))
            formatNow( d);
    }

    public void dictionaryEntry( DictionaryEntry de) {
        if (!addToResultBuffer( de))
            formatNow( de);

        entryCount++;
    }

    public void exception( SearchException ex) {
        if (!addToResultBuffer( ex))
            formatNow( ex);
    }

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
} // class LookupResultList
