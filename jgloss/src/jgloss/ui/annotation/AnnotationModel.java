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

package jgloss.ui.annotation;

import jgloss.ui.*;
import jgloss.ui.doc.*;
import jgloss.dictionary.*;

import java.util.*;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.*;

/**
 * The annotation model contains a tree of all annotations in a document.
 * For each annotation element in a document, an
 * {@link AnnotationNode AnnotationNode} will be placed at the first level of
 * the annotation tree. The nodes appear in the order in which the annotation
 * elements appear in the document. This model allows easy manipulation of these
 * annotation nodes.
 *
 * @author Michael Koch
 */
public class AnnotationModel extends DefaultTreeModel {
    /**
     * Take some text and return annotations for this text. This is
     * used by 
     * {@link AnnotationModel#addAnnotation(int,int,AnnotationModel.LookupTranslator,HTMLEditorKit)
     * addAnnotation}
     * to allow the user to modify what will be looked up when adding a new annotation.
     */
    public static interface LookupTranslator {
        /**
         * Takes the string and returns a list of annotations.
         *
         * @param text The original string.
         * @return A list of {@link jgloss.dictionary.Parser.TextAnnotation TextAnnotations},
         *         {@link jgloss.dictionary.WordReadingPair WordReadingPairs} and
         *         {@link jgloss.dictionary.DictionaryEntry DictionaryEntries}, or <CODE>null</CODE>
         *         to cancel the operation.
         */
        List translate( String text);
    } // interface LookupTranslator

    /**
     * The root node of the tree. Added as instance of <CODE>RootNode</CODE> to
     * avoid the constant typecasting.
     */
    private RootNode root;
    /**
     * The listener which adapts the model to changes in the document.
     */
    private DocumentListener documentListener;
    /**
     * The document which contains the annotations of this model.
     */
    private JGlossDocument doc;
    /**
     * Flag if changes to the document should be tracked.
     */
    private boolean trackChanges;

    /**
     * Creates a new empty annotation model.
     */
    public AnnotationModel() {
        super( new RootNode());
        root = (RootNode) super.root;
        root.setModel( this);
        trackChanges = false; // don't set this to true until the initial model is loaded
        documentListener = new DocumentListener() {
                public void insertUpdate( DocumentEvent e) {
                    // find out if an annotation element was inserted

                    Element el = doc.getDefaultRootElement();
                    Element elp = el;
                    // find the first element which does not fully contain the
                    // changed region and store it in elp
                    while (el!=null && el.getStartOffset() <= e.getOffset() &&
                           el.getEndOffset() >= e.getOffset()+e.getLength()) {
                        DocumentEvent.ElementChange change = e.getChange( el);
                        if (change != null)
                            handleInsert( change.getChildrenAdded());

                        elp = el;
                        el = el.getElement( el.getElementIndex( e.getOffset()));
                    }
                }

                private void handleInsert( Element[] children) {
                    for ( int i=0; i<children.length; i++)
                        handleInsert( children[i]);
                }

                private void handleInsert( Element e) {
                    if (e.getAttributes().getAttribute( StyleConstants.NameAttribute)
                        .equals( AnnotationTags.ANNOTATION))
                        addAnnotationFor( e);
                    else
                        for ( int i=0; i<e.getElementCount(); i++)
                            handleInsert( e.getElement( i));
                }

                public void removeUpdate( DocumentEvent e) {
                    // find out if an annotation element was removed

                    Element el = doc.getDefaultRootElement();
                    Element elp = el;
                    // find the first element which does not fully contain the
                    // changed region and store it in elp
                    while (el != null) {
                        DocumentEvent.ElementChange change = e.getChange( el);
                        if (change != null)
                            handleRemove( change.getChildrenRemoved());
                        elp = el;
                        el = el.getElement( el.getElementIndex( e.getOffset()));
                    }
                }

                private void handleRemove( Element[] children) {
                    for ( int i=0; i<children.length; i++)
                        handleRemove( children[i]);
                }

                private void handleRemove( Element e) {
                    if (e.getAttributes().getAttribute( StyleConstants.NameAttribute)
                        .equals( AnnotationTags.ANNOTATION))
                        root.remove( e);
                    else
                        for ( int i=0; i<e.getElementCount(); i++)
                            handleRemove( e.getElement( i));
                }

                public void changedUpdate( DocumentEvent e) {}
            };
    }

    /**
     * Initializes the model with the annotations from the document.
     *
     * @param doc The document which contains the annotations this model should model.
     */
    public void setDocument( JGlossDocument doc) {
        this.doc = doc;
        createFromElement( doc.getDefaultRootElement());
        reload();
        trackChanges( true);
    }
    
    /**
     * Finds the annotation node which encloses the position in the document.
     *
     * @param pos A position in the document.
     * @return The annotation node which encloses the position, or <CODE>null</CODE> if there
     *         is no annotation at this position.
     */
    public AnnotationNode findAnnotation( int pos) {
        return root.findAnnotation( pos, RootNode.BIAS_NONE);
    }

    /**
     * Returns the annotation text for an annotation at a specific document location. The
     * text is the current reading and translation plus all translations which were found in
     * the dictionaries when parsing the document.
     *
     * @param pos A position in the document.
     * @return The annotation text, or <CODE>null</CODE> if there is no annotation at the
     *         specified location.
     */
    public String getAnnotationText( int pos) {
        AnnotationNode node = root.findAnnotation( pos, RootNode.BIAS_NONE);
        if (node != null) {
            return node.getAnnotationText();
        }
        return null;
    }

    /**
     * Adds a new annotation node to the tree for the specified annotation element in the document.
     *
     * @param annotation The annotation element to add.
     */
    public void addAnnotationFor( Element annotation) {
        int index = root.findAnnotationIndex
            ( annotation.getStartOffset(), RootNode.BIAS_NEXT);
        root.insert( new AnnotationNode( (RootNode) root, annotation),  index);
        if (trackChanges) {
            nodesWereInserted( root, new int[] { index });
        }
    }

    /**
     * Returns the annotation node at the specified index.
     *
     * @param index Index in the array of children of the root node.
     * @return The annotation node at the index.
     */
    public AnnotationNode getAnnotationNode( int index) {
        return (AnnotationNode) root.getChildAt( index);
    }

    /**
     * Returns an iteration over all annotation nodes.
     *
     * @return An iteration over all annotation nodes.
     */
    public Iterator getAnnotationNodes() {
        return root.getChildren();
    }

    /**
     * Returns the number of annotation nodes.
     *
     * @return The number of annotation nodes.
     */
    public int getAnnotationCount() {
        return root.getChildCount();
    }

    /**
     * Flag if changes to the document should be tracked and the appropriate changes
     * to the model be made automatically. Set this to <CODE>false</CODE> if you plan
     * a lot of changes and want to repair the model by hand.
     *
     * @param trackChanges <CODE>true</CODE> if changes to the document should be tracked.
     */
    public void trackChanges( boolean trackChanges) {
        this.trackChanges = trackChanges;
        if (trackChanges)
            doc.addDocumentListener( documentListener);
        else
            doc.removeDocumentListener( documentListener);
    }

    /**
     * Adds a new annotation for a part of the document. Since it is not possible to nest
     * annotation elements, all annotations lying in that interval will be removed first. The
     * annotation also cannot span paragraphs, so the interval will be shortened as needed.
     *
     * @param start Start offset of the interval to annotate.
     * @param end End offset of the interval to annotate.
     * @param annotations List of .
     * @param kit An editor kit, which is needed to insert the new annotation.
     * @return The newly created annotation node.
     */
    public AnnotationNode addAnnotation( int start, int end, LookupTranslator trans, HTMLEditorKit kit) {
        try {
            // find smallest enclosing element of start and don't allow annotation to
            // cross it.
            Element paragraph = doc.getParagraphElement( start);
            if (paragraph.getAttributes().getAttribute( StyleConstants.NameAttribute)
                .equals( AnnotationTags.ANNOTATION)) {
                paragraph = paragraph.getParentElement();
            }
            end = Math.min( end, paragraph.getEndOffset()-1);

            // The start and end offsets will move around while we change the document.
            // So wrap them in position objects, which adapt to changes.
            Position startp = doc.createPosition( start);
            Position endp = doc.createPosition( end);
                        
            // remove existing annotations
            int first = root.findAnnotationIndex( start, RootNode.BIAS_NEXT);
            int last = root.findAnnotationIndex( end-1, RootNode.BIAS_PREVIOUS);
            for ( int i=last; i>=first; i--) {
                ((AnnotationNode) root.getChildAt( i)).removeAnnotation();
            }

            // The interval now only contains document plain text.
            start = startp.getOffset();
            end = endp.getOffset();
            String text = doc.getText( start, end-start);
            List annotations = trans.translate( text);
            if (annotations == null) // cancelled
                return null;

            // create Reading/Translation wrappers for WordReadingPair/DictionaryEntry objects
            for ( ListIterator i=annotations.listIterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o instanceof DictionaryEntry)
                    i.set( new Translation( (DictionaryEntry) o));
                else if (o instanceof WordReadingPair)
                    i.set( new Reading( (WordReadingPair) o));
            }

            boolean paragraphSpaceInserted = false;
            if (start == paragraph.getStartOffset()) {
                doc.insertAfterStart( paragraph, "&nbsp;");
                start++;
                startp = doc.createPosition( start);
                end = endp.getOffset();
                paragraphSpaceInserted = true;
            }

            // remove the old text.
            doc.remove( start, end-start);

            // If we insert new text directly after an annotation, the new annotation will 
            // be made a child of the first one, which is not what we want. So insert an
            // additional character if needed.
            AnnotationNode ann = findAnnotation( startp.getOffset()-1);
            Element sae = null;
            if (ann != null) {
                sae = ann.getAnnotationElement();
                doc.insertAfterEnd( sae, "s");
            }
            ann = findAnnotation( endp.getOffset());
            Element eae = null;
            if (ann != null) {
                eae = ann.getAnnotationElement();
                doc.insertBeforeStart( eae, "e");
                start = startp.getOffset()-1;
                startp = doc.createPosition( start);
                end = endp.getOffset()-1;
                endp = doc.createPosition( end);
            }

            // construct the new annotation and insert it
            text = "<html><body><p>"
                + "<" + AnnotationTags.ANNOTATION.getId() + " " + JGlossDocument.TEXT_ANNOTATION
                + "=\"" + TextAnnotationCodec.encode( annotations) + "\"><"
                + AnnotationTags.READING.getId() + "> </"
                + AnnotationTags.READING.getId() + "><" + AnnotationTags.KANJI.getId() + ">"  
                + text + "</" + AnnotationTags.KANJI.getId() + "><" + AnnotationTags.TRANSLATION.getId()
                + "> </" + AnnotationTags.TRANSLATION.getId() + "></" + AnnotationTags.ANNOTATION.getId()
                + "></p></body></html>";
            // The insertion will create a new annotation element and trigger a document changed
            // event. The documentListener of the model will react to this by creating a new
            // annotation node.
            // Unfortunately the JGlossDocument has no method for inserting HTML text at an
            // arbitrary location. So we will have to use an editor kit.
            kit.insertHTML( doc, startp.getOffset(), text, 0, 0, AnnotationTags.ANNOTATION);

            // remove the '\n\n' which the HTMLEditorKit insists on inserting
            doc.remove( endp.getOffset()-2, 2);

            if (eae != null) // remove the additional space which we inserted above
                doc.remove( eae.getStartOffset()-1, 1);
            if (sae != null) // remove the additional space which we inserted above
                doc.remove( sae.getEndOffset(), 1);

            if (paragraphSpaceInserted)
                doc.remove( paragraph.getStartOffset(), 1);

            return findAnnotation( start);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Adds annotation elements to the AnnotationModel. If the element is an annotation element,
     * it will be added to the annotation model, otherwise the method will recurse with the 
     * children (if any) of the element.
     *
     * @param el The element to add.
     */
    private void createFromElement( Element el) {
        Object name = el.getAttributes().getAttribute( StyleConstants.NameAttribute);
        if (name.equals( AnnotationTags.ANNOTATION)) {
            addAnnotationFor( el);
        }
        else { // search for Annotations in child elements
            for ( int i=0; i<el.getElementCount(); i++)
                createFromElement( el.getElement( i));
        }
    }
} // class AnnotationModel
