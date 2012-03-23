/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.html;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Keymap;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.UIUtilities;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.gloss.AnnotationList;

/**
 * JGlossEditor is a JTextPane with extensions specific to the manipulation on
 * text with translation annotations.
 *
 * @author Michael Koch
 */
public class JGlossEditor extends JTextPane {
	private static final Logger LOGGER = Logger.getLogger(JGlossEditor.class.getPackage().getName());
	
    private static final long serialVersionUID = 1L;

	private static DefaultHighlighter.DefaultHighlightPainter highlightPainter = 
    new DefaultHighlighter.DefaultHighlightPainter( new Color
        ( Math.max( 0, JGloss.prefs.getInt( Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xcccccc))));
    
    /**
     * Manages the automatic display of anntotation tooltips and annotation editor scrolling
     * when the mouse hovers over an annotation.
     *
     * @author Michael Koch
     */
    private class MouseFollowerThread extends Thread implements MouseMotionListener {
        /**
         * Mouse position of the last mouse event.
         */
        private Point pos = new Point();
        private boolean halt = false;
        
        public MouseFollowerThread() {
            super( "Mouse Follower Thread");
            setDaemon( true);
        }
        
        @Override
		public void run() {
            boolean updated = false;
            while (!halt) {
                try {
                    if (!updated) {
	                    sleep( 1000000000);
                    }
                    
                    while (updated) {
                        try {
                            // we don't want to react to every mouse movement event, so
                            // wait for 500ms with no action until the last mouse event is
                            // processed.
                            sleep( 500);
                            synchronized (this) {
                                if (tooltips) {
                                    showToolTip( "FIXME", pos);
                                }
                                updated = false;
                            }
                        } catch (InterruptedException ex) {}
                    }
                } catch (InterruptedException ex) {
                    updated = true;
                }
            }
        }
        
        @Override
		public void mouseMoved( MouseEvent e) {
            hideToolTip();
            
            if (tooltips) {
                synchronized (this) {
                    pos = e.getPoint();
                }
                this.interrupt();
            }
        }
        
        @Override
		public void mouseDragged( MouseEvent e) {}
        
        public void haltThread() {
            halt = true;
            this.interrupt();
            try {
                this.join();
            } catch (InterruptedException ex) {}
        }
    }
    
    /**
     * Key object of the keymap which contains the "pressed TAB" key action.
     */
    private final static String KEYMAP_TAB = "tab map";
    
    private AnnotationList annotationList;
    /**
     * Object identifying the currently highlighted annotation in the <CODE>HighlightPainter</CODE>.
     */
    private Object highlightTag = null;
    /**
     * Used to update the display according to the mouse position.
     */
    private MouseFollowerThread mouseFollower;
    /**
     * Displays annotation tooltips or a context menu as reaction to mouse clicks.
     */
    private MouseListener mouseListener;
    /**
     * <CODE>true</CODE>, if annotation tooltips will be shown automatically if the mouse
     * moves over an annotation.
     */
    private boolean tooltips;
    /**
     * <CODE>true</CODE>, if the mouse cursor is currently over the JGlossEditor component.
     */
    private boolean inComponent = true;
    /**
     * Window used to display the annotation tooltip.
     */
    private JWindow tooltipWindow;
    /**
     * Widget which holds the annotation tooltip text.
     */
    private JTextArea tooltip;
    
    /**
     * Searches a string in the document.
     */
    private Action findAction;
    /**
     * Repeats the last search.
     */
    private Action findAgainAction;
    /**
     * Last search string. Used with find again action.
     */
    private String lastFindText;
    /**
     * Last find position. Used with find again action.
     */
    private int lastFindPosition;
    
    /**
     * Update the tooltip font in response to default ui changes.
     */
    private PropertyChangeListener fontChangeListener;
    
    /**
     * Sets the highlight color to a new value. Used if the preferences changed.
     *
     * @param c The new annotation highlight color.
     */
    public static void setHighlightColor( Color c) {
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter( c);
    }
    
    /**
     * Creates a new editor for JGloss annotated documents with an associated
     * annotation editor. The annotation editor will be updated based on
     * user actions in the JGlossEditor.
     */
    public JGlossEditor( AnnotationList _annotationList) {
        setAutoscrolls( true);
        setEditable( false);
        
        annotationList = _annotationList;
        
        // A JTextComponent normally does not do TAB focus traversal since TAB is a valid input
        // character. Because the JGlossEditor is not used for normal text input, I override
        // this behavior for convenience.
        Keymap map = addKeymap( KEYMAP_TAB, getKeymap());
        map.addActionForKeyStroke( KeyStroke.getKeyStroke( "pressed TAB"),
            new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    transferFocus();
                }
            });
        setKeymap( map);
        
        mouseFollower = new MouseFollowerThread();
        mouseFollower.start();
        this.addMouseMotionListener( mouseFollower);
        
        mouseListener = new MouseListener() {
            @Override
			public void mousePressed( MouseEvent e) {
                checkPopupTrigger( e);
            }
            
            @Override
			public void mouseReleased( MouseEvent e) {
                checkPopupTrigger( e);
            }
            
            @Override
			public void mouseClicked( MouseEvent e) 
            {
                if (!(checkPopupTrigger( e) || annotationList.getContextMenu().isVisible())) 
                {
                    int tooltipButtonMask;
                    int selectButtonMask;
                    if (JGloss.prefs.getBoolean( Preferences.LEFTCLICK_TOOLTIP, false)) 
                    {
                        // left mouse button shows annotation tooltip, 
                        // all other select the annotation
                        tooltipButtonMask = InputEvent.BUTTON1_MASK;
                        selectButtonMask = InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK;
                    }
                    else 
                    {
                        // left mouse button selects annotation, 
                        // all other show annotation tooltip
                        tooltipButtonMask = InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK;
                        selectButtonMask = InputEvent.BUTTON1_MASK;
                    }
                    
                    if ((e.getModifiers() & selectButtonMask) != 0) 
                    {
                        int pos = viewToModel( e.getPoint());
                        int annoIndex = annotationList.getAnnotationListModel().findAnnotationIndex( pos, AnnotationListModel.BIAS_NONE);

                        if (annoIndex >= 0) 
                        {
                            // Selecting the annotation in the AnnotationList also selects it in the Editor
                            
                            annotationList.setSelectedIndex( annoIndex);
                            // transfer input focus to annotation list to enable keyboard
                            // navigation
                            annotationList.requestFocus();
                        }
                        else
                        {
                            // Deselect any selected annotation 
                            //annotationList.setSelectedIndex(0);

                            // FIXME: does not work!
                            annotationList.clearSelection();
                        }
                    }
                    else if (!tooltips && (e.getModifiers() & tooltipButtonMask)!=0) 
                    {
                        //    showToolTip( ((AnnotationModel) annotationEditor.getModel())
                        //                 .getAnnotationText( viewToModel( e.getPoint())),
                        //                 e.getPoint());
                    }
                }
            }
            
            private boolean checkPopupTrigger( MouseEvent e) {
                if (e.isPopupTrigger()) {
                    //annotationList.setSelectedItem( doc.getAnnotationAt( int pos));
                    annotationList.showContextMenu( JGlossEditor.this, e.getX(), e.getY());
                    return true;
                }
                return false;
            }
            
            @Override
			public void mouseEntered( MouseEvent e) {
                inComponent = true;
            }
            
            @Override
			public void mouseExited( MouseEvent e) {
                inComponent = false;
                hideToolTip();
            }
        };
        addMouseListener( mouseListener);
        
        tooltip = new JTextArea();
        JToolTip tt = createToolTip();
        tooltip.setEditable( false);
        tooltip.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2));
        tooltip.setBackground( tt.getBackground());
        tooltip.setForeground( tt.getForeground());
        tt.setComponent( null); // remove reference to this (which prevents garbage collection)
        
        // update display if user changed font
        fontChangeListener = new PropertyChangeListener() {
            @Override
			public void propertyChange( java.beans.PropertyChangeEvent e) { 
                if (e.getPropertyName().equals( "ToolTip.font")) {
                    tooltip.setFont( UIManager.getFont( "ToolTip.font"));
                }
            }
        };
        UIManager.getDefaults().addPropertyChangeListener( fontChangeListener);
        
        findAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( ActionEvent e) {
                Object result = JOptionPane.showInputDialog
                ( SwingUtilities.getRoot( JGlossEditor.this), 
                    JGloss.messages.getString( "editor.dialog.find"),
                    JGloss.messages.getString( "editor.dialog.find.title"),
                    JOptionPane.PLAIN_MESSAGE, null, null, getSelectedText());
                if (result != null) {
                    String text = result.toString();
                    if (text.length()>0) {
                        try {
                            int where = find( text, 0);
                            lastFindText = text;
                            if (where != -1) {
                                lastFindPosition = where;
                                findAgainAction.setEnabled( true);
                            }
                        } catch (BadLocationException ex) {}
                    }
                }
            }
        };
        findAction.setEnabled( false);
        UIUtilities.initAction( findAction, "editor.menu.find");
        findAgainAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( ActionEvent e) {
                try {
                    int where = find( lastFindText, lastFindPosition + 1);
                    if (where != -1) {
	                    lastFindPosition = where;
                    }
					else {
	                    lastFindPosition = 0; // wrap search
                    }
                } catch (BadLocationException ex) {
                    lastFindPosition = 0;
                }
            }
        };
        UIUtilities.initAction( findAgainAction, "editor.menu.findagain");
        findAgainAction.setEnabled( false); // will be enabled after first find
    }
    
    /**
     * Scrolls the document such that the text between <CODE>start</CODE> and
     * <CODE>end</CODE> will be visible.
     *
     * @param start Start offset in the document.
     * @param end End offset in the document.
     */
    public void makeVisible( int start, int end) {
        UIUtilities.makeVisible(this, start, end);
    }
    
    /**
     * Highlights the document text between <CODE>start</CODE> and <CODE>end</CODE>
     * with a highlight color set in the preferences. A call to this will remove the
     * previous highlight.
     *
     * @param start Start offset in the document.
     * @param end End offset in the document.
     */
    public void highlightText( int start, int end) {
        if (highlightTag != null) {
	        getHighlighter().removeHighlight( highlightTag);
        }
        
        try {
            highlightTag = getHighlighter().addHighlight( start, end, highlightPainter);
        } catch (BadLocationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    /**
     * Remove a previously set highlight.
     */
    public void removeHighlight() {
        if (highlightTag != null) {
	        getHighlighter().removeHighlight( highlightTag);
        }
        highlightTag = null;
    }
    
    /**
     * Sets the actions to do if the mouse moves over an annotation. Annotation tooltips
     * can be shown, or the annotation editor can be made to scroll to the annotation the
     * mouse is over. Independently of this setting the user can invoke these actions
     * by clicking one of the mouse buttons.
     *
     * @param tooltips <CODE>true</CODE>, if annotation tooltips should be automatically shown.
     */
    public void followMouse( boolean tooltips) {
        this.tooltips = tooltips;
        
        if (!tooltips) {
	        hideToolTip();
        }
    }
    
    /**
     * Shows a tooltip with the specified tip at the given location. If the mouse pointer
     * is not over the JGlossEditor component, the tooltip will not be shown.
     *
     * @param text The text do display in the tooltip.
     * @param where Coordinate for the tooltip in the JGlossEditor coordinate system. The
     *              location will be adapted if the tooltip does not fit on the screen.
     */
    private void showToolTip( String text, Point where) {
        if (!inComponent || text==null || text.length()==0) {
            hideToolTip();
            return;
        }
        
        if (tooltipWindow==null && getTopLevelAncestor()!=null) {
            tooltipWindow = new JWindow( (Frame) getTopLevelAncestor()) {
                private static final long serialVersionUID = 1L;

				@Override
				public void setVisible(boolean visible) {
                    super.setVisible(visible);
                    // work around bug in interaction with the KDE2 window manager:
                    this.removeNotify();
                }
            };
            tooltipWindow.getContentPane().setLayout( new GridLayout( 1, 1));
            tooltipWindow.getContentPane().add( tooltip);
        }
        
        if (tooltipWindow != null) {
            Point screen = getLocationOnScreen();
            where.x += screen.x + 15;
            where.y += screen.y + 15;
            
            tooltip.setText( text);
            Dimension windowsize = tooltip.getPreferredSize();
            Dimension screensize = getToolkit().getScreenSize();
            
            if (where.x+windowsize.width > screensize.width) {
                // move window left of mouse pointer
                where.x -= 30 + windowsize.width;
                if (where.x < 0) {
	                where.x = 0;
                }
            }
            
            if (where.y+windowsize.height > screensize.height) {
                // move window above mouse pointer
                where.y -= 30 + windowsize.height;
                if (where.y < 0) {
	                where.y = 0;
                }
            }
            
            tooltipWindow.setSize( windowsize);
            tooltipWindow.setLocation( where);
            tooltipWindow.setVisible( true);
        }
    }
    
    /**
     * Hides the tooltip window. Does nothing if it is already hidden.
     */
    private void hideToolTip() {
        if (tooltipWindow != null) {
	        tooltipWindow.setVisible(false);
        }
    }
    
    /**
     * Search for some text in the document. If the text is found as part of an annotation, the
     * annotation will be selected.
     *
     * @param text Text to find.
     * @param from Index in the document from where search should start.
     * @return Index in the document where the text was found, or <CODE>-1</CODE> if the text was
     *         not found.
     * @exception BadLocationException if the from search index is not valid.
     */
    private int find( String text, int from) throws BadLocationException {
        int where = getText( from, getDocument().getLength()-from).indexOf( text);
        if (where == -1) { 
            // text not found
            if (from == 0) { 
                // search in whole document
                JOptionPane.showMessageDialog
                ( SwingUtilities.getRoot( this), 
                    JGloss.messages.getString( "editor.dialog.find.notfound",
                        new Object[] { text }));
            }
            else {
                // search in part of the document
                JOptionPane.showMessageDialog
                ( SwingUtilities.getRoot( this),
                    JGloss.messages.getString( "editor.dialog.findagain.notfound",
                        new Object[] { text }));
            }
            return -1;
        }
        where += from; // start offset must be taken into account
        
        requestFocus(); // selection will only be visible if editor has the focus
        // select found text
        setCaretPosition( where);
        moveCaretPosition( where + text.length());
        
        return where;
    }
    
    /**
     * Dispose of resources associated with the editor.
     */
    public void dispose() {
        UIManager.getDefaults().removePropertyChangeListener( fontChangeListener);
        mouseFollower.haltThread();
        removeKeymap( KEYMAP_TAB);
    }
} // class JEditorPane
