/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.*;
import jgloss.dictionary.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Panel for looking up single words in the dictionaries. The panel can be integrated in
 * dialogs or windows.
 *
 * @author Michael Koch
 */
public class WordLookupPanel extends JPanel implements Dictionaries.DictionaryListChangeListener {
    protected final static String STYLE = "body { color: black; background-color: white; }\n";

    protected JRadioButton exact;
    protected JRadioButton startsWith;
    protected JRadioButton endsWith;
    protected JRadioButton any;

    protected JCheckBox verbDeinflection;

    protected JRadioButton dictionary;
    protected JRadioButton allDictionaries;
    protected JComboBox dictionaryChoice;

    protected JButton search;

    private boolean updateListEventScheduled = false;

    /**
     * Widget in which the search expression is entered.
     */
    protected JComboBox expression;
    /**
     * Text field used to display the result as plain text. This text field is used when there
     * are more than {@link #resultLimit resultLimit} results.
     */
    protected JTextArea resultPlain;
    /**
     * Text field used to display the result as HTML text. This text field is used when there
     * are less than {@link #resultLimit resultLimit} results.
     */
    protected JTextPane resultFancy;
    /**
     * Text pane which displays the lookup result.
     */
    protected JScrollPane resultScroller;
    /**
     * Limit of entry lines up to which HTML formatting of the result will be used.
     */
    protected int resultLimit;

    protected XCVManager xcvManager;

    protected Dimension preferredSize;

    /**
     * Creates a new word lookup panel.
     */
    public WordLookupPanel() {
        this.resultLimit = JGloss.prefs.getInt( Preferences.WORDLOOKUP_RESULTLIMIT, 500);

        setLayout( new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 1;

        // create options part
        ButtonGroup searchType = new ButtonGroup();
        exact = new JRadioButton( JGloss.messages.getString( "wordlookup.choice.exact"));
        searchType.add( exact);
        startsWith = new JRadioButton( JGloss.messages.getString( "wordlookup.choice.startswith"));
        searchType.add( startsWith);
        endsWith = new JRadioButton( JGloss.messages.getString( "wordlookup.choice.endswith"));
        searchType.add( endsWith);
        any = new JRadioButton( JGloss.messages.getString( "wordlookup.choice.any"));
        searchType.add( any);

        switch (JGloss.prefs.getInt( Preferences.WORDLOOKUP_SEARCHTYPE, 
                                     Dictionary.SEARCH_EXACT_MATCHES)) {
        case Dictionary.SEARCH_STARTS_WITH:
            startsWith.setSelected( true);
            break;
        case Dictionary.SEARCH_ENDS_WITH:
            endsWith.setSelected( true);
            break;
        case Dictionary.SEARCH_ANY_MATCHES:
            any.setSelected( true);
            break;
        case Dictionary.SEARCH_EXACT_MATCHES:
        default:
            exact.setSelected( true);
            break;
        }

        verbDeinflection = new JCheckBox( JGloss.messages.getString
                                          ( "wordlookup.choice.verbdeinflection"));
        verbDeinflection.setSelected( JGloss.prefs.getBoolean( Preferences.WORDLOOKUP_DEINFLECTION, false));

        JPanel p = new JPanel( new GridLayout( 0, 1));
        p.setBorder( BorderFactory.createCompoundBorder 
                     ( BorderFactory.createTitledBorder
                       ( JGloss.messages.getString( "wordlookup.searchoptions")),
                       BorderFactory.createEmptyBorder( 2, 2, 2, 2)));
        p.add( exact);
        p.add( startsWith);
        p.add( endsWith);
        p.add( any);
        p.add( verbDeinflection);

        JPanel p2 = new JPanel( new GridBagLayout());
        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridy = 0;
        c2.fill = GridBagConstraints.BOTH;
        c2.weightx = 1;
        c2.weighty = 1;
        p2.add( p, c2);

        dictionaryChoice = new JComboBox();
        dictionaryChoice.setEditable( false);

        ButtonGroup dictionaries = new ButtonGroup();
        dictionary = new JRadioButton( JGloss.messages.getString
                                       ( "wordlookup.choice.dictionary"), true);
        dictionaries.add( dictionary);
        dictionary.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e) {
                    dictionaryChoice.setEnabled( dictionary.isSelected());
                }
            });
        allDictionaries = new JRadioButton
            ( JGloss.messages.getString( "wordlookup.choice.alldictionaries"));
        dictionaries.add( allDictionaries);
        if (JGloss.prefs.getBoolean( Preferences.WORDLOOKUP_ALLDICTIONARIES, true))
            allDictionaries.setSelected( true);
        else
            dictionary.setSelected( true);

        p = new JPanel( new GridLayout( 0, 2));
        p.add( dictionary);
        p.add( dictionaryChoice);
        p.add( allDictionaries);

        p = UIUtilities.createSpaceEater( p, false);
        p.setBorder( BorderFactory.createCompoundBorder
                    ( BorderFactory.createTitledBorder
                      ( JGloss.messages.getString( "wordlookup.dictionaryselection")),
                      BorderFactory.createEmptyBorder( 2, 2, 2, 2)));        

        c2.gridwidth = GridBagConstraints.REMAINDER;
        p2.add( p, c2);
        add( p2, c);

        // create word input part
        p = new JPanel();
        p.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5));
        p.setLayout( new GridBagLayout());
        c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.WEST;
        c2.gridy = 0;
        p.add( new JLabel( JGloss.messages.getString( "wordlookup.enterexpression")), c2);
        expression = new JComboBox();
        expression.setEditable( true);

        GridBagConstraints c3 = (GridBagConstraints) c2.clone();
        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.weightx = 1;
        p.add( expression, c3);
        p.add( Box.createHorizontalStrut( 4), c2);
        
        Action searchAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    search();
                }
            };
        searchAction.setEnabled( true);
        UIUtilities.initAction( searchAction, "wordlookup.search");
        search = new JButton( searchAction);
        p.add( search, c2);
        p.add( Box.createHorizontalStrut( 2), c2);

        KeyStroke enter = KeyStroke.getKeyStroke( "ENTER");
        InputMap im = search.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (im == null)
            im = new ComponentInputMap( search);
        im.put( enter, searchAction.getValue( Action.NAME));
        search.setInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW, im);
        search.getActionMap().put( searchAction.getValue( Action.NAME), searchAction);

        Action clearAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    expression.setSelectedItem( null);
                }
            };
        clearAction.setEnabled( true);
        UIUtilities.initAction( clearAction, "wordlookup.clear");
        JButton clear = new JButton( clearAction);
        p.add( clear, c2);
        clear.setNextFocusableComponent( exact);

        add( p, c);

        c.gridheight = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        
        // set up the result panes.
        resultFancy = new JTextPane();
        resultFancy.setContentType( "text/html");
        resultFancy.setEditable( false);
        ((HTMLEditorKit) resultFancy.getEditorKit()).getStyleSheet().addRule( STYLE);
        ((HTMLEditorKit) resultFancy.getEditorKit()).getStyleSheet().addRule
            ( "body { font-family: '" + JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP) +
              "'; font-size: " + JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12) + 
              "pt; }\n");
        resultFancy.getKeymap().addActionForKeyStroke
            ( KeyStroke.getKeyStroke( "pressed TAB"),
              new AbstractAction() {
                      public void actionPerformed( ActionEvent e) {
                          transferFocus();
                      }
                  });

        resultPlain = new JTextArea();
        resultPlain.setFont( new Font( JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP),
                                       Font.PLAIN, 
                                       JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12)));
        resultPlain.setEditable( false);
        resultPlain.getKeymap().addActionForKeyStroke
            ( KeyStroke.getKeyStroke( "pressed TAB"),
              new AbstractAction() {
                      public void actionPerformed( ActionEvent e) {
                          transferFocus();
                      }
                  });

        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createCompoundBorder
                     ( BorderFactory.createTitledBorder( JGloss.messages.getString( "wordlookup.result")),
                       BorderFactory.createEmptyBorder( 2, 2, 2, 2)));
        resultScroller = new JScrollPane( resultPlain, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p.add( resultScroller);
        add( p, c);

        Dictionaries.addDictionaryListChangeListener( this);

        xcvManager = new XCVManager( expression);
        xcvManager.addManagedComponent( resultFancy);
        xcvManager.addManagedComponent( resultPlain);

        preferredSize = new Dimension
            ( Math.max( super.getPreferredSize().width,
                        JGloss.prefs.getInt( Preferences.WORDLOOKUP_WIDTH, 0)),
              Math.max( super.getPreferredSize().height + 150,
                        JGloss.prefs.getInt( Preferences.WORDLOOKUP_HEIGHT, 0)));
        
        addComponentListener( new ComponentAdapter() {
                public void componentResized( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.WORDLOOKUP_WIDTH, getWidth());
                    JGloss.prefs.set( Preferences.WORDLOOKUP_HEIGHT, getHeight());
                }
            });

        // update display if user changed font
        JGloss.prefs.addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                public void propertyChange( java.beans.PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP) ||
                        e.getPropertyName().equals( Preferences.FONT_WORDLOOKUP_SIZE)) {
                        String fontname = JGloss.prefs.getString( Preferences.FONT_WORDLOOKUP);
                        int size = JGloss.prefs.getInt( Preferences.FONT_WORDLOOKUP_SIZE, 12);
                        Font font = new Font( fontname, Font.PLAIN, size);
                        resultPlain.setFont( font);
                        ((HTMLEditorKit) resultFancy.getEditorKit()).getStyleSheet().addRule
                            ( "body { font-family: '" + fontname +
                              "'; font-size: " + size +
                              "pt; }\n");
                        resultFancy.setFont( font);
                    }
                } 
            });
        UIManager.getDefaults().addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                public void propertyChange( java.beans.PropertyChangeEvent e) { 
                    if (e.getPropertyName().equals( "ComboBox.font")) {
                        expression.setFont( (Font) e.getNewValue());
                    }
                }
            });

        // initialize dictionary list
        updateDictionaryChoice();
    }

    /**
     * Update the list of dictionaries. This method is called after the dictionary list has
     * been changed in the preferences window.
     */
    protected void updateDictionaryChoice() {
        Object selected = dictionaryChoice.getSelectedItem();
        dictionaryChoice.removeAllItems();
        Dictionary[] d = Dictionaries.getDictionaries( false);
        int index = -1;
        if (d.length == 0) {
            search.setEnabled( false);
            dictionaryChoice.addItem( JGloss.messages.getString( "wordlookup.nodictionary"));
        }
        else {
            for ( int i=0; i<d.length; i++) {
                dictionaryChoice.addItem( d[i]);
                if (selected!=null && d[i].equals( selected))
                    index = i;
            }
            search.setEnabled( true);
        }
        
        // restore old selection
        if (index != -1)
            dictionaryChoice.setSelectedIndex( index);
    }

    /**
     * Looks up the selected text. The current dialog settings will be used as search parameters.
     *
     * @param text The word which should be looked up.
     */
    public void search( String text) {
        expression.setSelectedItem( text);
        search();
    }

    /**
     * Looks up the current selection using the search parameters set in the dialog.
     *
     * @param resultmode Determines the object types in the result list.
     * @return List of dictionary lookup results.
     */
    public List searchSelection( short resultmode) {
        return searchSelection( resultmode, false);
    }

    /**
     * Looks up the current selection using the search parameters set in the dialog.
     *
     * @param resultmode Determines the object types in the result list.
     * @param markDictionaries If <code>true</code> and more than one dictionary is searched,
     *        the name of each searched dictionary will be inserted in the list prior to the
     *        search results for that dictionary.
     * @return List of dictionary lookup results.
     */
    protected List searchSelection( short resultmode, boolean markDictionaries) {
        if (expression.getSelectedItem() == null)
            return Collections.EMPTY_LIST;

        String ex = expression.getSelectedItem().toString();
        if (ex.length() == 0)
            return Collections.EMPTY_LIST;

        short mode;
        if (exact.isSelected())
            mode = Dictionary.SEARCH_EXACT_MATCHES;
        else if (startsWith.isSelected())
            mode = Dictionary.SEARCH_STARTS_WITH;
        else if (endsWith.isSelected())
            mode = Dictionary.SEARCH_ENDS_WITH;
        else
            mode = Dictionary.SEARCH_ANY_MATCHES;
        
        Conjugation[] conjugations = null;
        String hiragana = null;
        if (verbDeinflection.isSelected()) {
            // build an array of the dictionary form of all possible inflections
            int i = ex.length();
            while (i>0 && Character.UnicodeBlock.of( ex.charAt( i-1)).equals
                   ( Character.UnicodeBlock.HIRAGANA))
                i--;
            // If the search expression is only hiragana, i is now == 0, and deinflection should
            // not be used.
            if (i>0 && i<ex.length()) {
                hiragana = ex.substring( i);
                conjugations = Conjugation.findConjugations( hiragana);
                if (conjugations != null) {
                    ex = ex.substring( 0, i);
                }
            }
        }
        
        java.util.List result = new ArrayList( 50);
        if (allDictionaries.isSelected()) {
            Dictionary[] d = Dictionaries.getDictionaries( true);
            for ( int i=0; i<d.length; i++) {
                if (markDictionaries)
                    result.add( d[i].getName()); // mark beginning of next dictionary in results
                lookupAll( d[i], ex, conjugations, hiragana, mode, resultmode, result, markDictionaries);
            }
        }
        else {
            lookupAll( (Dictionary) dictionaryChoice.getSelectedItem(),
                       ex, conjugations, hiragana, mode, resultmode, result, markDictionaries);
        }
        
        return result;
    }

    protected void search() {
        List result = searchSelection( Dictionary.RESULT_NATIVE, true);
        String ex = expression.getSelectedItem().toString();

        // generate result text
        StringBuffer resultText = new StringBuffer( result.size()*30);
        boolean useHTML = (result.size() < resultLimit);
        int results = 0; // number of result entries without dictionary names
        for ( Iterator i=result.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof String) { // dictionary name
                resultText.append( JGloss.messages.getString( "wordlookup.matches"));
                if (useHTML)
                    resultText.append( "<font color=\"green\">");
                resultText.append( (String) o);
                if (useHTML)
                    resultText.append( "</font>:<br>");
                else
                    resultText.append( ':');
                resultText.append( '\n');
            }
            else if (o instanceof Conjugation) {
                Conjugation conjugation = (Conjugation) o;
                if (useHTML)
                    resultText.append( "<i>");
                resultText.append( JGloss.messages.getString
                                   ( "wordlookup.inflection", new String[] {
                                       conjugation.getType(), conjugation.getDictionaryForm(),
                                       conjugation.getConjugatedForm() }));
                if (useHTML)
                    resultText.append( "</i><br>");
                resultText.append( '\n');
            }
            else { // dictionary entry
                results++;
                WordReadingPair wrp = (WordReadingPair) o;
                if (useHTML) {
                    StringBuffer match = new StringBuffer( wrp.toString());

                    // escape HTML special characters
                    for ( int j=match.length()-1; j>=0; j--) {
                        switch (match.charAt( j)) {
                        case '&':
                            match.replace( j, j+1, "&amp;");
                            break;
                        case '<':
                            match.replace( j, j+1, "&lt;");
                            break;
                        case '>':
                            match.replace( j, j+1, "&gt;");
                            break;
                        }
                    }

                    // highlight the expression in the result
                    String lcex = ex.toLowerCase();
                    String lcmatch = match.toString().toLowerCase();
                    int off = lcmatch.lastIndexOf( lcex);
                    while (off != -1) {
                        match.insert( off+ex.length(), "</font>");
                        match.insert( off, "<font color=\"blue\">");
                        off = lcmatch.lastIndexOf( lcex, off-1);
                    }

                    resultText.append( match.toString());
                    resultText.append( "<br>\n");
                }
                else {
                    // add entry as plain text
                    resultText.append( wrp.toString());
                    resultText.append('\n');
                }
            }
        }

        if (results == 0) {
            resultText = new StringBuffer( JGloss.messages.getString
                                           ( "wordlookup.nomatches",
                                             new Object[] { expression.getSelectedItem() }));
        }
        else {
            resultText.insert( 0, '\n');
            if (useHTML)
                resultText.insert( 0, "<br>");
            resultText.insert( 0, JGloss.messages.getString( "wordlookup.matchesfor",
                                                             new Object[] 
                { new Integer( results), expression.getSelectedItem() }));
        }

        // create new result display pane
        if (useHTML) {
            resultText.insert( 0, "<html><head></head><body>");
            resultText.append( "</body></html>");
            // setting up the new doc this way is more complicated than simply using setText
            // on the JTextPane, but avoids the scroll pane moving to the end of the generated document
            HTMLEditorKit kit = (HTMLEditorKit) resultFancy.getEditorKit();
            Document doc = kit.createDefaultDocument();
            try {
                kit.read( new java.io.StringReader( resultText.toString()), doc, 0);
            } catch (Exception exc) {}
            resultFancy.setDocument( doc);
            if (resultScroller.getViewport().getView() != resultFancy) {
                resultScroller.setViewportView( resultFancy);
                resultPlain.setText( ""); // clear to save memory
            }
        }
        else {
            resultPlain.setText( resultText.toString());
            resultPlain.setCaretPosition( 0);
            if (resultScroller.getViewport().getView() != resultPlain) {
                resultScroller.setViewportView( resultPlain);
                resultFancy.setDocument( ((HTMLEditorKit) resultFancy.getEditorKit())
                                         .createDefaultDocument()); // clear to save memory
                
            }
        }
        resultScroller.getViewport().setViewPosition( new Point( 0, 0));
            
        // remember the current search string
        ex = expression.getSelectedItem().toString();
        expression.insertItemAt( ex, 0);
        int i = 1;
        while (i < expression.getItemCount()) {
            if (expression.getItemAt( i).equals( ex))
                expression.removeItemAt( i);
            else
                i++;
        }
        while (expression.getItemCount() > 30) {
            expression.removeItemAt( 30);
        }
        expression.setSelectedItem( ex);
    }

    /**
     * Look up an expression with all possible conjugations in a dictionary.
     *
     * @param dic Dictionary in which to look up the expression.
     * @param expression Expression to look up.
     * @param conjugations List of conjugations which will be added to the expression. May be
     *                     <CODE>null</CODE>.
     * @param hiragana Inflection after the expression. Only conjugations where the hiragana inflection
     *                 matches the dictionary form will be used.
     * @param mode Search mode.
     * @param result List of dictionary entries matching the search expression.
     * @param addConjugations If <code>true</code>, the conjugation used to derive the search result will
     *                        be inserted in the list of results.
     * @return Number of entries found.
     */
    protected int lookupAll( Dictionary dic, String expression, Conjugation[] conjugations, 
                             String hiragana, short mode, short resultmode, java.util.List result,
                             boolean addConjugations) {
        int results; // number of entry lines found
        
        if (conjugations == null)
            results = lookupWord( dic, expression, null, mode, resultmode, result);
        else {
            results = lookupWord( dic, expression + hiragana, null, mode, resultmode, result);
            for ( int i=0; i<conjugations.length; i++) {
                if (!conjugations[i].getDictionaryForm().equals( hiragana))
                    results += lookupWord( dic, expression + conjugations[i].getDictionaryForm(),
                                           addConjugations ? conjugations[i] : null, mode, 
                                           resultmode, result);
            }
        }

        return results;
    }
    
    /**
     * Look up an expression in a dictionary.
     *
     * @param dic Dictionary in which to look up the expression.
     * @param expression Expression to look up.
     * @param conjugation Conjugation to apply to the expression. May be <CODE>null</CODE>.
     * @param mode Search mode.
     * @param result List of dictionary entries matching the search expression.
     * @return Number of entries found.
     */
    protected int lookupWord( Dictionary dic, String expression, Conjugation conjugation,
                              short searchmode, short resultmode, java.util.List result) {
        try {
            java.util.List entries = dic.search( expression, searchmode, resultmode);
            if (entries.size() > 0) {
                if (conjugation != null)
                    result.add( conjugation);
                result.addAll( entries);
            }
            return entries.size();
        } catch (SearchException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * Saves the current dialog settings.
     */
    public void savePreferences() {
        short mode;
        if (exact.isSelected())
            mode = Dictionary.SEARCH_EXACT_MATCHES;
        else if (startsWith.isSelected())
            mode = Dictionary.SEARCH_STARTS_WITH;
        else if (endsWith.isSelected())
            mode = Dictionary.SEARCH_ENDS_WITH;
        else
            mode = Dictionary.SEARCH_ANY_MATCHES;
        JGloss.prefs.set( Preferences.WORDLOOKUP_SEARCHTYPE, (int) mode);
        
        JGloss.prefs.set( Preferences.WORDLOOKUP_DEINFLECTION, verbDeinflection.isSelected());
        JGloss.prefs.set( Preferences.WORDLOOKUP_ALLDICTIONARIES, allDictionaries.isSelected());
    }

    /**
     * Returns the manager of the cut/copy/paste actions of this dialog.
     */
    public XCVManager getXCVManager() { return xcvManager; }

    public Dimension getPreferredSize() {
        if (preferredSize == null)
            return super.getPreferredSize();
        else
            return preferredSize;
    }

    /**
     * Reflect changes in the loaded dictionary list by updating the dictionary choice combo box.
     * This is done asynchronously if the current thread is not the event dispatch thread.
     */
    public void dictionaryListChanged() {
        if (updateListEventScheduled)
            // A list update is already pending, the new event can therefore be ignored.
            return;

        Runnable worker = new Runnable() {
                public void run() {
                    updateListEventScheduled = false;
                    updateDictionaryChoice();
                }
            };
        if (EventQueue.isDispatchThread()) {
            worker.run();
        }
        else {
            updateListEventScheduled = true;
            EventQueue.invokeLater( worker);
        }
    }
} // class WordLookupPanel
