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
import jgloss.ui.doc.JGlossEditorKit;
import jgloss.ui.annotation.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * JGlossEditor is a JTextPane with extensions specific to the manipulation on
 * text with translation annotations. It works together with an <CODE>AnnotationEditor</CODE>
 * to make it easy to manipulate the annotations.
 *
 * @author Michael Koch
 * @see jgloss.ui.AnnotationEditor
 */
public class JGlossEditor extends JTextPane {
    /**
     * Highlighter used to highlight an annotation which is currently selected in the
     * <CODE>AnnotationEditor</CODE>. It uses the color from the preference
     * <CODE>ANNOTATION_HIGHLIGHT_COLOR</CODE>.
     */
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
            super( "MouseFollowerThread");
            setDaemon( true);
        }

        public void run() {
            boolean updated = false;
            int i=0;
            while (!halt) {
                try {
                    if (!updated)
                        sleep( 1000000000);
                    
                    while (updated) {
                        try {
                            // we don't want to react to every mouse movement event, so
                            // wait for 500ms of no action until the last mouse event is
                            // processed.
                            sleep( 500);
                            synchronized (this) {
                                int p = viewToModel( pos);
                                if (autoscroll)
                                    annotationEditor.makeVisible( p);
                                if (tooltips) {
                                    showToolTip( ((AnnotationModel) annotationEditor.getModel())
                                                 .getAnnotationText( p), pos);
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

        public void mouseMoved( MouseEvent e) {
            hideToolTip();

            if (autoscroll || tooltips) {
                synchronized (this) {
                    pos = e.getPoint();
                }
                this.interrupt();
            }
        }

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
     * Modal dialog which lets the user look up words in the dictionary. The dialog is used
     * when the user wants to add an annotation to a word in the document.
     */
    private class WordLookupDialog extends JDialog {
        private WordLookupPanel wordlookup;
        private JButton annotateButton;
        private boolean accepted;

        public WordLookupDialog() {
            super( (Frame) SwingUtilities.getWindowAncestor( JGlossEditor.this),
                   JGloss.messages.getString( "editor.addannotation.title"), true);
            Action annotateAction = new AbstractAction() {
                    public void actionPerformed( ActionEvent e) {
                        accepted = true;
                        WordLookupDialog.this.hide();
                    }
                };
            UIUtilities.initAction( annotateAction, "button.annotate");
            Action cancelAction = new AbstractAction() {
                    public void actionPerformed( ActionEvent e) {
                        accepted = false;
                        WordLookupDialog.this.hide();
                    }
                };
            UIUtilities.initAction( cancelAction, "button.cancel");
            wordlookup = new WordLookupPanel();

            getContentPane().setLayout( new BorderLayout());
            getContentPane().add( wordlookup, BorderLayout.CENTER);
            Box buttons = Box.createHorizontalBox();
            annotateButton = new JButton( annotateAction);
            buttons.add( annotateButton);
            buttons.add( Box.createHorizontalStrut( 5));
            buttons.add( new JButton( cancelAction));

            JPanel p = new JPanel( new BorderLayout());
            JPanel p2 = new JPanel( new GridLayout( 1, 1));
            p2.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4));
            p2.add( buttons);
            p.add( p2, BorderLayout.EAST);
            getContentPane().add( p, BorderLayout.SOUTH);
            pack();
            this.setSize( this.getPreferredSize().width, this.getPreferredSize().height + 100);
        }

        /**
         * Displays the dialog and returns the lookup result when the dialog is closed.
         *
         * @param text Initial word to look up.
         * @return List of dictionary entries, or <CODE>null</CODE> if the dialog was cancelled.
         */
        public java.util.List runDialog( String text) {
            accepted = false;
            wordlookup.search( text);
            annotateButton.requestFocus();
            WordLookupDialog.this.show();
            WordLookupDialog.this.dispose(); // works around a bug in the Java/KDE window manager interaction
            if (accepted)
                return wordlookup.getLastResult();
            else
                return null;
        }
    }

    /**
     * Object identifying the currently highlighted annotation in the <CODE>HighlightPainter</CODE>.
     */
    private Object highlightTag = null;
    /**
     * The annotation editor linked to this document editor.
     */
    private AnnotationEditor annotationEditor;
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
     * <CODE>true</CODE>, if the associated annotation editor should automatically scroll
     * to make the annotation under the mouse visible.
     */
    private boolean autoscroll;
    /**
     * Window used to display the annotation tooltip.
     */
    private JWindow tooltipWindow;
    /**
     * Widget which holds the annotation tooltip text.
     */
    private JTextArea tooltip;

    /**
     * Action which annotates the current selection.
     */
    private Action addAnnotationAction;
    /**
     * The menu which contains actions specific to document editing.
     */
    private JMenu editMenu;
    /**
     * Manager for the cut/copy/past actions;
     */
    private XCVManager xcvManager;
    /**
     * Dialog used when adding annotations to a word in the document.
     */
    private WordLookupDialog wordLookupDialog;
    /**
     * Displays a dialog which allows the user to enter the word to look up.
     */
    private AnnotationModel.LookupTranslator lookupTranslator = 
        new AnnotationModel.LookupTranslator() {
                public java.util.List translate( String text) {
                    if (wordLookupDialog == null)
                        wordLookupDialog = new WordLookupDialog();
                    return wordLookupDialog.runDialog( text);
                }
            };

    /**
     * Sets the highlight color to a new value. Used if the preferences changed.
     *
     * @param c The new annotation highlight color.
     */
    public static void setHighlightColor( Color c) {
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter( c);
    }

    /**
     * Creates a new editor for JGloss annotated documents without an associated
     * annotation editor.
     *
     */
    public JGlossEditor() {
        this( null);
    }

    /**
     * Creates a new editor for JGloss annotated documents with an associated
     * annotation editor. The annotation editor will be updated based on
     * user actions in the JGlossEditor.
     *
     * @param source The annotation editor which manages the annotations of
     *               the document this editor displays.
     */
    public JGlossEditor( AnnotationEditor source) {
        super();
        setAutoscrolls( true);

        xcvManager = new XCVManager( this);
        editMenu = new JMenu( JGloss.messages.getString( "editor.menu.edit"));
        editMenu.add( UIUtilities.createMenuItem( xcvManager.getCutAction()));
        editMenu.add( UIUtilities.createMenuItem( xcvManager.getCopyAction()));
        editMenu.add( UIUtilities.createMenuItem( xcvManager.getPasteAction()));
        editMenu.addMenuListener( xcvManager.getEditMenuListener());
        if (source != null) {
            annotationEditor = source;

            mouseFollower = new MouseFollowerThread();
            mouseFollower.start();
            this.addMouseMotionListener( mouseFollower);

            mouseListener = new MouseListener() {
                    private boolean checkPopupTrigger( MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            int pos = viewToModel( e.getPoint());
                            annotationEditor.makeVisible( pos);
                            annotationEditor.selectAnnotation( pos);
                            annotationEditor.showContextMenu( JGlossEditor.this, e.getX(), e.getY());
                            return true;
                        }
                        return false;
                    }

                    public void mousePressed( MouseEvent e) {
                        checkPopupTrigger( e);
                    }
                    
                    public void mouseReleased( MouseEvent e) {
                        checkPopupTrigger( e);
                    }
                    
                    public void mouseClicked( MouseEvent e) {
                        if (!checkPopupTrigger( e)) {
                            if (!autoscroll && (e.getModifiers() & (MouseEvent.BUTTON2_MASK|
                                                                    MouseEvent.BUTTON3_MASK))!=0) {
                                int pos = viewToModel( e.getPoint());
                                annotationEditor.makeVisible( pos);
                                annotationEditor.selectAnnotation( pos);
                            }
                            if (!tooltips && (e.getModifiers() & MouseEvent.BUTTON1_MASK)!=0) {
                                showToolTip( ((AnnotationModel) annotationEditor.getModel())
                                             .getAnnotationText( viewToModel( e.getPoint())),
                                             e.getPoint());
                            }
                        }
                    }

                    public void mouseEntered( MouseEvent e) {
                        inComponent = true;
                    }

                    public void mouseExited( MouseEvent e) {
                        inComponent = false;
                        hideToolTip();
                    }
                };
            addMouseListener( mouseListener);

            tooltip = new JTextArea();
            JToolTip tt = createToolTip();
            tooltip.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2));
            tooltip.setBackground( tt.getBackground());
            tooltip.setForeground( tt.getForeground());
            tooltip.setFont( tt.getFont());

            addAnnotationAction = new AbstractAction() {
                    public void actionPerformed( ActionEvent e) {
                        AnnotationNode node = ((AnnotationModel) annotationEditor.getModel()).addAnnotation
                            (getSelectionStart(), getSelectionEnd(),
                             lookupTranslator,
                             (JGlossEditorKit) getStyledEditorKit());
                        if (node != null) {
                            annotationEditor.selectNode( node);
                            annotationEditor.expandAll( node);
                            annotationEditor.makeVisible( node);
                        }
                    }
                };
            addAnnotationAction.setEnabled( false);
            UIUtilities.initAction( addAnnotationAction, "editor.menu.addannotation");
            editMenu.add( UIUtilities.createMenuItem( addAnnotationAction));

            addCaretListener( new CaretListener() {
                    public void caretUpdate( CaretEvent e) {
                        boolean hasSelection = e.getDot() != e.getMark();
                        addAnnotationAction.setEnabled( hasSelection);
                    }
                });
        }
    }

    /**
     * Scrolls the document such that the text between <CODE>start</CODE> and
     * <CODE>end</CODE> will be visible.
     *
     * @param start Start offset in the document.
     * @param end End offset in the document.
     */
    public void makeVisible( int start, int end) {
        try {
            Rectangle r1 = modelToView( start);
            // end-1 selects the last character of the annotation element
            Rectangle r2 = modelToView( end-1);
            if (r1 != null) {
                if (r2 != null)
                    r1 = r1.createUnion( r2).getBounds();

                if (!getVisibleRect().contains( r1)) {
                    scrollRectToVisible( r1);
                }
            }
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
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
        if (highlightTag != null)
            getHighlighter().removeHighlight( highlightTag);

        try {
            highlightTag = getHighlighter().addHighlight( start, end, highlightPainter);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sets the actions to do if the mouse moves over an annotation. Annotation tooltips
     * can be shown, or the annotation editor can be made to scroll to the annotation the
     * mouse is over. Independently of this setting the user can invoke these actions
     * by clicking one of the mouse buttons.
     *
     * @param tooltips <CODE>true</CODE>, if annotation tooltips should be automatically shown.
     * @param autoscroll </CODE>true</CODE>, if the annotation editor should automatically
     *                   scroll to the annotation.
     */
    public void followMouse( boolean tooltips, boolean autoscroll) {
        if (annotationEditor == null)
            return;

        this.tooltips = tooltips;
        this.autoscroll = autoscroll;

        if (!(tooltips || autoscroll))
            hideToolTip();
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
                    public void hide() {
                        super.hide();
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
                if (where.x < 0)
                    where.x = 0;
            }

            if (where.y+windowsize.height > screensize.height) {
                // move window above mouse pointer
                where.y -= 30 + windowsize.height;
                if (where.y < 0)
                    where.y = 0;
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
        if (tooltipWindow != null)
            tooltipWindow.hide();
    }

    /**
     * Returns the editor menu with actions specific to document editing. It can be
     * integrated in the application menu bar. Since it is unique, it cannot be used
     * in more than one menu bar.
     *
     * @return The editor menu with actions specific to document editing.
     */
    public JMenu getEditMenu() {
        return editMenu;
    }

    /**
     * Sets the editor kit used to manipulate the document.
     *
     * @param kit The document to display.
     */
    public void setEditorKit( JGlossEditorKit kit) {
        super.setEditorKit( kit);
        xcvManager.updateActions( this);
    }

    public void dispose() {
        editMenu.removeAll();
        editMenu.removeMenuListener( xcvManager.getEditMenuListener());
        if (annotationEditor != null)
            mouseFollower.haltThread();
    }
} // class JEditorPane
