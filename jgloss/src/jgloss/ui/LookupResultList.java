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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class LookupResultList extends JPanel implements LookupResultHandler {
    protected final static String STYLE = "body { color: black; background-color: white; }\n";

    /**
     * Text field used to display the result as HTML text.
     */
    protected JEditorPane resultFancy;
    protected JTextArea resultPlain;
    protected JScrollPane resultScroller;

    protected int fancyLimit;

    protected LookupModel model;
    protected boolean multipleDictionaries;
    protected List resultBuffer;
    protected StringBuffer resultTextBuffer = new StringBuffer( 8192);
    protected int entriesInTextBuffer;
    protected int dictionaryEntries;
    protected boolean previousDictionaryHasMatch;
    protected JLabel status;

    protected final static int BUFFER_LIMIT = 1000;

    public LookupResultList() {
        this( 1000);
    }

    public LookupResultList( int _fancyLimit) {
        setLayout( new BorderLayout());

        fancyLimit = _fancyLimit;
        resultFancy = new JEditorPane();
        resultFancy.setContentType( "text/html");
        resultFancy.setEditable( false);
        ((HTMLEditorKit) resultFancy.getEditorKit()).getStyleSheet().addRule( STYLE);
        ((HTMLEditorKit) resultFancy.getEditorKit()).getStyleSheet().addRule
            ( "body { font-family: '" + JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP) +
              "'; font-size: " + JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12) + 
              "pt; }\n");

        resultPlain = new JTextArea();
        resultPlain.setFont( new Font( JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP),
                                       Font.PLAIN, 
                                       JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12)));
        resultPlain.setEditable( false);

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
                        ((HTMLEditorKit) resultFancy.getEditorKit()).getStyleSheet().addRule
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
    }

    public void addToXCVManager( XCVManager manager) {
        manager.addManagedComponent( resultFancy);
        manager.addManagedComponent( resultPlain);
    }

    public void startLookup( LookupModel _model) {
        model = _model;
        multipleDictionaries = model.getSelectedDictionaries().size() > 1;
        previousDictionaryHasMatch = true;
        dictionaryEntries = 0;
        entriesInTextBuffer = 0;
        resultBuffer = new ArrayList( fancyLimit);
    }

    public void dictionary( Dictionary d) {
        if (!addToResultBuffer( d))
            formatNow( d);
    }

    public void dictionaryEntry( DictionaryEntry de) {
        if (!addToResultBuffer( de))
            formatNow( de);

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
        if (!previousDictionaryHasMatch) {
            resultTextBuffer.append( JGloss.messages.getString( "wordlookup.nomatches_dictionary"));
            if (fancy)
                resultTextBuffer.append( "<br>");
            resultTextBuffer.append( '\n');
        }

        if (multipleDictionaries) {
            if (fancy) {
                resultTextBuffer.append( "<b>");
                resultTextBuffer.append
                    ( JGloss.messages.getString( "wordlookup.matches",
                                                 new String[] { "<font color=\"green\">" +
                                                                d.getName() + "</font>" }));
                resultTextBuffer.append( "</b><br>");
            }
            else {
                resultTextBuffer.append( JGloss.messages.getString( "wordlookup.matches",
                                                                    new String[] { d.getName() }));
            }
            resultTextBuffer.append( '\n');
        }
        
        previousDictionaryHasMatch = false;
    }

    protected void format( DictionaryEntry de, boolean fancy) {
        previousDictionaryHasMatch = true;
        if (fancy) {
            DictionaryEntryFormat.getHTMLFormatter().format( de, resultTextBuffer);
            resultTextBuffer.append( "<br>");
        }
        else
            DictionaryEntryFormat.getFormatter().format( de, resultTextBuffer);

        resultTextBuffer.append( '\n');
        dictionaryEntries++;
    }

    protected void format( SearchException ex, boolean fancy) {
        ex.printStackTrace();
        
        if (fancy)
            resultTextBuffer.append( "<font color=\"red\">");
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
            resultTextBuffer.append( "</font><br>");
        resultTextBuffer.append( '\n');
    }

    protected void format( String note, boolean fancy) {
        resultTextBuffer.append( '\n');
        if (fancy)
            resultTextBuffer.append( "<br><i>");
        resultTextBuffer.append( note);
        if (fancy)
            resultTextBuffer.append( "</i>");
        resultTextBuffer.append( '\n');
    }

    public void endLookup() {
        if (resultBuffer != null)
            flushBuffer( true);
        else
            flushTextBuffer();
        updateStatusText( JGloss.messages.getString( "wordlookup.status.matches",
                                                     new Object[] { new Integer( dictionaryEntries),
                                                                    model.getSelectedSearchMode().
                                                                    getName() }));
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

        final String bufferContent = resultTextBuffer.toString();
        resultTextBuffer.setLength( 0);

        Runnable updater = new Runnable() {
                public void run() {
                    if (fancy) {
                        HTMLDocument doc = (HTMLDocument) resultFancy.getEditorKit()
                            .createDefaultDocument();
                        doc.setTokenThreshold( 150);
                        resultFancy.setDocument( doc);

                        if (resultScroller.getViewport().getView() != resultFancy) {
                            resultScroller.setViewportView( resultFancy);
                            resultPlain.setText( "");
                        }
                        
                        resultFancy.setText( bufferContent);
                        resultFancy.setCaretPosition( 0);
                    }
                    else {
                        if (resultScroller.getViewport().getView() != resultPlain) {
                            resultScroller.setViewportView( resultPlain);
                            resultFancy.setText( "");
                        }
                        resultPlain.setText( bufferContent);
                        resultPlain.setCaretPosition( 0);
                    }
                }
            };

        if (EventQueue.isDispatchThread())
            updater.run();
        else try {
            EventQueue.invokeAndWait( updater);
        } catch (Exception ex) {}
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
} // class LookupResultList
