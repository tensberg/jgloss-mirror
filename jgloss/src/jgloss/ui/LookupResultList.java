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
    protected JTextPane result;
    protected JScrollPane resultScroller;

    protected LookupModel model;
    protected boolean multipleDictionaries;
    protected StringBuffer resultBuffer = new StringBuffer( 8192);
    protected int dictionaryEntries;
    protected int dictionaryEntriesInBuffer;
    protected boolean previousDictionaryHasMatch;
    protected boolean firstBufferFlush;

    protected final static int BUFFER_LIMIT = 100;

    public LookupResultList() {
        setLayout( new BorderLayout());

        result = new JTextPane();
        result.setContentType( "text/html");
        result.setEditable( false);
        ((HTMLEditorKit) result.getEditorKit()).getStyleSheet().addRule( STYLE);
        ((HTMLEditorKit) result.getEditorKit()).getStyleSheet().addRule
            ( "body { font-family: '" + JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP) +
              "'; font-size: " + JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12) + 
              "pt; }\n");
        result.getKeymap().addActionForKeyStroke
            ( KeyStroke.getKeyStroke( "pressed TAB"),
              new AbstractAction() {
                  public void actionPerformed( ActionEvent e) {
                      transferFocus();
                  }
              });

        resultScroller = new JScrollPane( result, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( resultScroller, BorderLayout.CENTER);

        // update display if user changed font
        JGloss.prefs.addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                public void propertyChange( java.beans.PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP) ||
                        e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP_SIZE)) {
                        String fontname = JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP);
                        int size = JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12);
                        Font font = new Font( fontname, Font.PLAIN, size);
                        ((HTMLEditorKit) result.getEditorKit()).getStyleSheet().addRule
                            ( "body { font-family: '" + fontname +
                              "'; font-size: " + size +
                              "pt; }\n");
                        result.setFont( font);
                    }
                }
            });
    }

    public JTextPane getResultPane() { return result; }

    public void startLookup( LookupModel _model) {
        System.err.println( "starting lookup");
        model = _model;
        multipleDictionaries = model.getSelectedDictionaries().size() > 1;
        previousDictionaryHasMatch = true;
        dictionaryEntries = 0;
        dictionaryEntriesInBuffer = 0;
        resultBuffer.setLength( 0);
        firstBufferFlush = true;
    }

    public void dictionary( Dictionary d) {
        if (!previousDictionaryHasMatch) {
            resultBuffer.append( JGloss.messages.getString( "wordlookup.nomatches_dictionary"));
            resultBuffer.append( "<br>\n");
        }
        if (multipleDictionaries) {
            resultBuffer.append( JGloss.messages.getString( "wordlookup.matches"));
            resultBuffer.append( "<font color=\"green\">");
            resultBuffer.append( d.getName());
            resultBuffer.append( "</font>:<br>\n");
        }
        previousDictionaryHasMatch = false;
    }

    public void dictionaryEntry( DictionaryEntry de) {
        previousDictionaryHasMatch = true;
        resultBuffer.append( de.toString());
        resultBuffer.append( "<br>\n");
        dictionaryEntries++;
        dictionaryEntriesInBuffer++;
        if (dictionaryEntriesInBuffer > BUFFER_LIMIT)
            flushBuffer();
    }

    public void exception( SearchException ex) {
        ex.printStackTrace();
        if (ex instanceof UnsupportedSearchModeException) {
            resultBuffer.append( JGloss.messages.getString( "wordlookup.unsupportedsearchmode"));
            resultBuffer.append( "<br>\n");
        }
        else {
            resultBuffer.append( "<font color=\"red\">");
            resultBuffer.append( JGloss.messages.getString( "wordlookup.exception",
                                                            new Object[] { ex.getClass().getName(),
                                                                           ex.getLocalizedMessage() }));
            resultBuffer.append( "</font>:<br>\n");
        }
    }

    public void endLookup() {
        flushBuffer();
    }

    protected void flushBuffer() {
        final String bufferContent = resultBuffer.toString();
        System.err.println( "flushing buffer");
        Runnable updater = new Runnable() {
                public void run() {
                    try {
                        if (firstBufferFlush) {
                            firstBufferFlush = false;
                            HTMLDocument doc = (HTMLDocument) result.getEditorKit()
                                .createDefaultDocument();
                            doc.setTokenThreshold( 50);
                            result.setDocument( doc);
                            result.setCaretPosition( 0);
                        }
                        ((HTMLDocument) result.getDocument()).insertBeforeEnd
                            ( result.getDocument().getDefaultRootElement()
                              .getElement( 0).getElement( 0), bufferContent);
                            result.setCaretPosition( 0);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            };

        if (EventQueue.isDispatchThread())
            updater.run();
        else try {
            EventQueue.invokeAndWait( updater);
        } catch (Exception ex) {}
        
        dictionaryEntriesInBuffer = 0;
        resultBuffer.setLength( 0);
    }
} // class LookupResultList
