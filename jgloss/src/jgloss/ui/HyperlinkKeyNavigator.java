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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

/**
 * Allow keyboard navigation of links in a <code>JEditorPane</code> showing HTML content.
 * <p> 
 * There is already similar functionality built into the HTML editor kit, but the default
 * keyboard keys are impractical, the implemetation does not work well and the corresponding
 * actions are not documented, so I decided to roll my own implementation.
 * </p>
 *
 * @author Michael Koch
 */
public class HyperlinkKeyNavigator implements DocumentListener, PropertyChangeListener {
    protected Element currentElement;
    protected Object highlightTag;

    protected JEditorPane editor;
    protected HTMLDocument document;
    
    protected MoveLinkAction nextLinkAction;
    protected MoveLinkAction previousLinkAction;
    protected ActivateLinkAction activateLinkAction;
    protected DefaultHighlighter.DefaultHighlightPainter highlightPainter;

    public HyperlinkKeyNavigator() {
        this(Color.CYAN);
    }

    public HyperlinkKeyNavigator(Color highlightColor) {
        createHighlightPainter(highlightColor);
        nextLinkAction = new MoveLinkAction("nextLinkAction", false, 
                                            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        previousLinkAction = new MoveLinkAction("previousLinkAction", true,
                                                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        activateLinkAction = new ActivateLinkAction("activateLinkAction",
                                                    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
    }

    public void setTargetEditor(JEditorPane _editor) {
        if (editor == _editor)
            return;

        if (editor != null) {
            editor.removePropertyChangeListener(this);
            editor.setInputMap(JComponent.WHEN_FOCUSED, editor.getInputMap().getParent());
            editor.setActionMap(editor.getActionMap().getParent());
        }
        if (document != null) {
            document.removeDocumentListener(this);
            document = null;
        }

        if (_editor != null) {
            _editor.addPropertyChangeListener(this);
            InputMap inputmap = new InputMap();
            inputmap.put((KeyStroke) nextLinkAction.getValue(Action.ACCELERATOR_KEY), 
                         nextLinkAction.getValue(Action.NAME));
            inputmap.put((KeyStroke) previousLinkAction.getValue(Action.ACCELERATOR_KEY), 
                         previousLinkAction.getValue(Action.NAME));
            inputmap.put((KeyStroke) activateLinkAction.getValue(Action.ACCELERATOR_KEY), 
                         activateLinkAction.getValue(Action.NAME));
            inputmap.setParent(_editor.getInputMap());
            _editor.setInputMap(JComponent.WHEN_FOCUSED, inputmap);
            ActionMap actionmap = new ActionMap();
            actionmap.put(nextLinkAction.getValue(Action.NAME), nextLinkAction);
            actionmap.put(previousLinkAction.getValue(Action.NAME), previousLinkAction);
            actionmap.put(activateLinkAction.getValue(Action.NAME), activateLinkAction);
            actionmap.setParent(_editor.getActionMap());
            _editor.setActionMap(actionmap);

            try {
                document = (HTMLDocument) _editor.getDocument();
                document.addDocumentListener(this);
                resetSelection();
            } catch (ClassCastException ex) {
                document = null;
            }
        }

        editor = _editor;
    }

    public void setHighlightColor(Color highlightColor) {
        createHighlightPainter(highlightColor);
    }

    public Color getHighlightColor() {
        return highlightPainter.getColor();
    }

    protected void createHighlightPainter(Color highlightColor) {
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
    }

    protected void setHighlight(int startOffset, int endOffset) {
        try {
            if (highlightTag != null) {
                editor.getHighlighter().changeHighlight(highlightTag, startOffset, endOffset);
            }
            else {
                highlightTag = editor.getHighlighter().addHighlight(startOffset, endOffset,
                                                                    highlightPainter);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    protected void removeHighlight() {
        if (highlightTag != null) {
            editor.getHighlighter().removeHighlight(highlightTag);
            highlightTag = null;
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equalsIgnoreCase("document")) {
            if (document != null)
                document.removeDocumentListener(this);
            try {
                document = (HTMLDocument) editor.getDocument();
                document.addDocumentListener(this);
            } catch (ClassCastException ex) {
                document = null;
            }
            resetSelection();
        }
    }

    public void changedUpdate(DocumentEvent e) {
        if (currentElement == null ||
            currentElement.getStartOffset()<=e.getOffset()+e.getLength() &&
            currentElement.getEndOffset()>=e.getOffset())
            // changed area intersects current element
            resetSelection();
    }

    public void insertUpdate(DocumentEvent e) {
        if (currentElement == null)
            resetSelection();
    }

    public void removeUpdate(DocumentEvent e) {
        if (currentElement!=null &&
            currentElement.getStartOffset()<=e.getOffset()+e.getLength() &&
            currentElement.getEndOffset()>=e.getOffset())
            // changed area intersects current element
            resetSelection();
    }

    protected void resetSelection() {
        removeHighlight();
        currentElement = null;
        // select the first link element
        nextLinkAction.moveLink(editor, document);
    }

    /**
     * Searches the first <code>a href</code> element after the given position.
     *
     * @return the element, or <code>null</code> if there is no such element.
     */
    protected Element getFirstLinkAfter(int position, Element elem) {
        if (elem.isLeaf()) {
            AttributeSet att = elem.getAttributes();
            if (elem.getStartOffset() >= position &&
                att.isDefined(HTML.Tag.A) &&
                ((AttributeSet) att.getAttribute(HTML.Tag.A)).isDefined
                (HTML.Attribute.HREF)) {
                return elem; // element is first link after given position
            }
        }
        else { // recurse over children
            for (int i=0; i<elem.getElementCount(); i++) {
                Element child = elem.getElement(i);
                if (child.getEndOffset() > position) {
                    Element out = getFirstLinkAfter(position, child);
                    if (out != null) // Link element found, end recursion
                        return out;
                }
            }
        }

        // No link element found in this recursion step
        return null;
    }

    /**
     * Searches the first <code>a href</code> element after the given position.
     *
     * @return the element, or <code>null</code> if there is no such element.
     */
    protected Element getFirstLinkBefore(int position, Element elem) {
        if (elem.isLeaf()) {
            AttributeSet att = elem.getAttributes();
            if (elem.getEndOffset() <= position &&
                att.isDefined(HTML.Tag.A) &&
                ((AttributeSet) att.getAttribute(HTML.Tag.A)).isDefined
                (HTML.Attribute.HREF)) {
                return elem; // element is first link after given position
            }
        }
        else { // recurse over children
            for (int i=elem.getElementCount()-1; i>=0; i--) {
                Element child = elem.getElement(i);
                if (child.getStartOffset() < position) {
                    Element out = getFirstLinkBefore(position, child);
                    if (out != null) // Link element found, end recursion
                        return out;
                }
            }
        }

        // No link element found in this recursion step
        return null;
    }

    protected class MoveLinkAction extends AbstractAction {
        protected boolean backwards;

        protected MoveLinkAction(String name, boolean _backwards, KeyStroke acceleratorKey) {
            super(name);
            this.backwards = _backwards;
            putValue(ACCELERATOR_KEY, acceleratorKey);
        }

        public void actionPerformed(ActionEvent event) {
            if (event.getSource()==editor && document!=null)
                moveLink(editor, document);
        }

        public void moveLink(JEditorPane editor, HTMLDocument doc) {
            Element nextHref = null;
            doc.readLock();
            try {
                if (backwards) {
                    if (currentElement != null)
                        nextHref = getFirstLinkBefore(currentElement.getStartOffset(), 
                                                      doc.getDefaultRootElement());
                    if (nextHref == null)
                        // no current element, or no further link after current link element
                        // finds the current link element again if it is the only link in the
                        // document
                        nextHref = getFirstLinkBefore(doc.getLength(), doc.getDefaultRootElement());
                }
                else {
                    if (currentElement != null)
                        nextHref = getFirstLinkAfter(currentElement.getEndOffset(), 
                                                     doc.getDefaultRootElement());
                    if (nextHref == null)
                        // no current element, or no further link after current link element
                        // finds the current link element again if it is the only link in the
                        // document
                        nextHref = getFirstLinkAfter(0, doc.getDefaultRootElement());
                }
            } finally {
                doc.readUnlock();
            }

            if (nextHref != null) {
                currentElement = nextHref;
                setHighlight(currentElement.getStartOffset(), currentElement.getEndOffset());
                UIUtilities.makeVisible(editor, currentElement.getStartOffset(), 
                                        currentElement.getEndOffset());
                activateLinkAction.setEnabled(true);
            }
            else {
                // no href element in document
                removeHighlight();
                currentElement = null;
                activateLinkAction.setEnabled(false);
            }
        }
    }

    protected class ActivateLinkAction extends AbstractAction {
        protected ActivateLinkAction(String name, KeyStroke acceleratorKey) {
            super(name);
            putValue(ACCELERATOR_KEY, acceleratorKey);
        }

        public void actionPerformed(ActionEvent event) {
            if (event.getSource()==editor && document!=null)
                activateLink();
        }

        public void activateLink() {
            if (currentElement == null)
                return;

            String currentURL = (String) ((AttributeSet) currentElement.getAttributes().getAttribute
                                          (HTML.Tag.A)).getAttribute(HTML.Attribute.HREF);
            URL parsedURL = null;
            try {
                parsedURL = new URL(currentURL);
            } catch (MalformedURLException ex) {}
            editor.fireHyperlinkUpdate(new HyperlinkEvent(editor, HyperlinkEvent.EventType.ACTIVATED,
                                                          parsedURL, currentURL, currentElement));
        }
    }
} // class HyperlinkKeyNavigator
