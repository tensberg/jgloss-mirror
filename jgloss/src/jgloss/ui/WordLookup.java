/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Frontend for looking up single words in the dictionaries.
 *
 * @author Michael Koch
 */
public class WordLookup extends JFrame {
    /**
     * Action which displays the word lookup window.
     */
    public final static Action showAction;

    static {
        showAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    getFrame().show();
                }
            };
        JGlossFrame.initAction( showAction, "main.menu.wordlookup");
    }

    /**
     * Instance of the dialog used in the show action.
     */
    private static WordLookup instance;

    public static synchronized WordLookup getFrame() {
        if (instance == null)
            instance = new WordLookup();

        return instance;
    }

    protected final static String STYLE =
        "body { font-size: 12pt; color: black; background-color: white; }\n";

    protected JRadioButton exact;
    protected JRadioButton startsWith;
    protected JRadioButton endsWith;
    protected JRadioButton any;

    protected JCheckBox verbDeinflection;

    protected JRadioButton dictionary;
    protected JRadioButton allDictionaries;
    protected JComboBox dictionaryChoice;

    protected JButton search;

    protected JComboBox expression;
    protected JTextPane result;
    protected JScrollPane resultScroller;

    public WordLookup() {
        super( JGloss.messages.getString( "wordlookup.title"));

        Container content = getContentPane();
        content.setLayout( new GridBagLayout());
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
        verbDeinflection.setSelected( JGloss.prefs.getBoolean( Preferences.WORDLOOKUP_DEINFLECTION));

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
        if (JGloss.prefs.getBoolean( Preferences.WORDLOOKUP_ALLDICTIONARIES))
            allDictionaries.setSelected( true);
        else
            dictionary.setSelected( true);

        p = new JPanel( new GridLayout( 0, 2));
        p.add( dictionary);
        p.add( dictionaryChoice);
        p.add( allDictionaries);
        p = JGlossFrame.createSpaceEater( p, false);
        p.setBorder( BorderFactory.createCompoundBorder
                    ( BorderFactory.createTitledBorder
                      ( JGloss.messages.getString( "wordlookup.dictionaryselection")),
                      BorderFactory.createEmptyBorder( 2, 2, 2, 2)));        

        c2.gridwidth = GridBagConstraints.REMAINDER;
        p2.add( p, c2);
        content.add( p2, c);

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
        JGlossFrame.initAction( searchAction, "wordlookup.search");
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
        JGlossFrame.initAction( clearAction, "wordlookup.clear");
        p.add( new JButton( clearAction), c2);

        content.add( p, c);

        c.gridheight = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        
        p = new JPanel( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createCompoundBorder
                     ( BorderFactory.createTitledBorder( JGloss.messages.getString( "wordlookup.result")),
                       BorderFactory.createEmptyBorder( 2, 2, 2, 2)));
        result = new JTextPane();
        result.setEditable( false);
        result.setContentType( "text/html");
        ((HTMLEditorKit) result.getEditorKit()).getStyleSheet().addRule( STYLE);
        resultScroller = new JScrollPane( result, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p.add( resultScroller);
        content.add( p, c);

        updateDictionaryChoice();
        JGloss.prefs.addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                public void propertyChange( java.beans.PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.DICTIONARIES))
                        updateDictionaryChoice();
                }
            });

        // create actions
        Action printAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                }
            };
        printAction.setEnabled( false);
        JGlossFrame.initAction( printAction, "main.menu.print"); 
        Action closeAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    hide();
                    JGloss.exit();
                }
            };
        JGlossFrame.initAction( closeAction, "main.menu.close");

        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE);
        addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    hide();
                    JGloss.exit();
                }
            });

        // setup menu bar
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.file"));
        menu.add( JGlossFrame.createMenuItem( JGlossFrame.actions.importDocument));
        menu.add( JGlossFrame.createMenuItem( JGlossFrame.actions.importClipboard));
        menu.addMenuListener( JGlossFrame.actions.importClipboardListener);
        menu.addSeparator();
        menu.add( JGlossFrame.createMenuItem( JGlossFrame.actions.open));
        menu.addSeparator();
        menu.add( JGlossFrame.createMenuItem( printAction));
        menu.addSeparator();
        menu.add( JGlossFrame.createMenuItem( closeAction));
        bar.add( menu);

        final JMenu editMenu = new JMenu( JGloss.messages.getString( "editor.menu.edit"));

        // There are two text areas in the dialog from which text can be cut/copied/pasted.
        // The following code manages the menu items based on the currently focused text components.
        XCVManager expressionXCV = new XCVManager( expression);
        final JMenuItem cut1 = JGlossFrame.createMenuItem( expressionXCV.getCutAction());
        final JMenuItem copy1 = JGlossFrame.createMenuItem( expressionXCV.getCopyAction());
        final JMenuItem paste1 = JGlossFrame.createMenuItem( expressionXCV.getPasteAction());
        editMenu.add( cut1);
        editMenu.add( copy1);
        editMenu.add( paste1);
        FocusListener fl = new FocusListener() {
                public void focusGained( FocusEvent e) {
                    if (editMenu.getItem( 0) != cut1) {
                        editMenu.remove( 0);
                        editMenu.remove( 0);
                        editMenu.remove( 0);
                        editMenu.insert( cut1, 0);
                        editMenu.insert( copy1, 1);
                        editMenu.insert( paste1, 2);
                    }
                }
                public void focusLost( FocusEvent e) {}
            };
        expression.addFocusListener( fl);
        expression.getEditor().getEditorComponent().addFocusListener( fl);
        editMenu.addMenuListener( expressionXCV.getEditMenuListener());

        XCVManager resultXCV = new XCVManager( result);
        final JMenuItem cut2 = JGlossFrame.createMenuItem( resultXCV.getCutAction());
        final JMenuItem copy2 = JGlossFrame.createMenuItem( resultXCV.getCopyAction());
        final JMenuItem paste2 = JGlossFrame.createMenuItem( resultXCV.getPasteAction());
        result.addFocusListener( new FocusListener() {
                public void focusGained( FocusEvent e) {
                    if (editMenu.getItem( 0) != cut2) {
                        editMenu.remove( 0);
                        editMenu.remove( 0);
                        editMenu.remove( 0);
                        editMenu.insert( cut2, 0);
                        editMenu.insert( copy2, 1);
                        editMenu.insert( paste2, 2);
                    }
                }
                public void focusLost( FocusEvent e) {}
            });
        editMenu.addMenuListener( resultXCV.getEditMenuListener());
        
        editMenu.addSeparator();
        editMenu.add( JGlossFrame.createMenuItem( PreferencesFrame.showAction));
        bar.add( editMenu);           

        menu = new JMenu( JGloss.messages.getString( "main.menu.help"));
        menu.add( JGlossFrame.createMenuItem( AboutFrame.showAction));
        bar.add( menu);

        setJMenuBar( bar);

        pack();
        setSize( getPreferredSize().width, getPreferredSize().height + 100);
    }

    /**
     * Update the list of dictionaries. This method is called after the dictionary list has
     * been changed in the preferences window.
     */
    protected void updateDictionaryChoice() {
        Object selected = dictionaryChoice.getSelectedItem();
        dictionaryChoice.removeAllItems();
        Dictionary[] d = Dictionaries.getDictionaries();
        int index = -1;
        if (d.length == 0) {
            search.setEnabled( false);
            dictionaryChoice.addItem( JGloss.messages.getString( "wordlookup.nodictionary"));
        }
        else {
            for ( int i=0; i<d.length; i++) {
                dictionaryChoice.addItem( d[i]);
                if (d[i].equals( selected))
                    index = i;
            }
            search.setEnabled( true);
        }

        // restore old selection
        if (index != -1)
            dictionaryChoice.setSelectedIndex( index);
    }

    /**
     * Looks up the selected text.
     *
     * @param text The word which should be looked up.
     */
    public void search( String text) {
        expression.setSelectedItem( text);
        search();
    }

    /**
     * Looks up the current selection.
     */
    protected void search() {
        if (expression.getSelectedItem() == null)
            return;

        String ex = expression.getSelectedItem().toString();
        if (ex.length() == 0)
            return;
        StringBuffer result = new StringBuffer();

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
            if (i < ex.length()) {
                hiragana = ex.substring( i);
                conjugations = Conjugation.findConjugations( hiragana);
                if (conjugations != null) {
                    ex = ex.substring( 0, i);
                }
            }
        }
            
        if (allDictionaries.isSelected()) {
            Dictionary[] d = Dictionaries.getDictionaries();
            for ( int i=0; i<d.length; i++) {
                result.append( JGloss.messages.getString( "wordlookup.matches"));
                result.append( "<font color=\"green\">");
                result.append( d[i].getName());
                result.append( "</font>:<br>\n");
                lookupAll( d[i], ex, conjugations, hiragana, mode, result);
            }
        }
        else {
            lookupAll( (Dictionary) dictionaryChoice.getSelectedItem(),
                       ex, conjugations, hiragana, mode, result);
        }
            
        if (result.length() == 0) {
            result.append( JGloss.messages.getString( "wordlookup.nomatches",
                                                      new String[] { ex }));
        }
        else
            result.insert( 0, JGloss.messages.getString( "wordlookup.matchesfor",
                                                         new String[] { ex }) + "<br>\n");

        result.insert( 0, "<html><head></head><body>");
        result.append( "</body></html>");
        
        // setting up the new doc this way is more complicated than simply using setText
        // on the JTextPane, but avoids the scroll pane moving to the end of the generated document
        Document doc = this.result.getEditorKit().createDefaultDocument();
        try {
            this.result.getEditorKit().read( new java.io.StringReader( result.toString()),
                                        doc, 0);
        } catch (Exception exc) {}
        this.result.setDocument( doc);
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

    protected void lookupAll( Dictionary dic, String expression, Conjugation[] conjugations, 
                              String hiragana, short mode, StringBuffer result) {
        if (conjugations == null)
            lookupWord( dic, expression, null, mode, result);
        else {
            lookupWord( dic, expression + hiragana, null, mode, result);
            for ( int i=0; i<conjugations.length; i++) {
                if (!conjugations[i].getDictionaryForm().equals( hiragana))
                    lookupWord( dic, expression + conjugations[i].getDictionaryForm(),
                                conjugations[i], mode, result);
            }
        }
    }
    
    protected void lookupWord( Dictionary dic, String expression, Conjugation conjugation,
                               short mode, StringBuffer result) {
        try {
            java.util.List entries = dic.search( expression, mode);
            if (entries.size() > 0) {
                if (conjugation != null) {
                    result.append( "<i>" + JGloss.messages.getString
                                   ( "wordlookup.inflection", new String[] {
                                       conjugation.getType(), conjugation.getDictionaryForm(),
                                       conjugation.getConjugatedForm() }) +
                                   "</i><br>");
                }

                for ( Iterator i=entries.iterator(); i.hasNext(); ) {
                    StringBuffer match = new StringBuffer();
                    WordReadingPair wrp = (WordReadingPair) i.next();
                    match.append( wrp.getWord());
                    if (wrp.getReading() != null && wrp.getReading().length()>0
                        && !(wrp.getWord().equals( wrp.getReading()))) {
                        match.append( " \uff08");
                        match.append( wrp.getReading());
                        match.append( '\uff09');
                    }
                    if (wrp instanceof DictionaryEntry) {
                        match.append( " ");
                        String[] t = ((DictionaryEntry) wrp).getTranslations();
                        match.append( t[0]);
                        for ( int j=1; j<t.length; j++) {
                            match.append( " / ");
                            match.append( t[j]);
                        }
                    }

                    // highlight the expression in the result
                    String lcex = expression.toLowerCase();
                    String lcmatch = match.toString().toLowerCase();
                    int off = lcmatch.lastIndexOf( lcex);
                    while (off != -1) {
                        match.insert( off+expression.length(), "</font>");
                        match.insert( off, "<font color=\"blue\">");
                        off = lcmatch.lastIndexOf( lcex, off-1);
                    }

                    result.append( match);
                    result.append( "<br>\n");
                }
            }
        } catch (SearchException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Saves the preferences in addition to hiding the frame.
     */
    public void hide() {
        super.hide();
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
} // class WordLookup
