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

import jgloss.*;
import jgloss.dictionary.*;
import jgloss.ui.*;
import jgloss.ui.doc.*;

import java.util.*;

import javax.swing.text.*;
import javax.swing.tree.*;

/**
 * Node which represents the word part of an annotation. The node has the readings of the
 * annotated word as children. Parent is an {@link AnnotationNode AnnotationNode}, children
 * are {@link ReadingTextNode ReadingTextNodes}.
 *
 * @author Michael Koch
 */
public class WordNode extends InnerNode {
    private Element word;
    private String wordText;
    private String readingText;
    private ReadingTextNode[] readings;

    /**
     * Text the node displays in the tree.
     */
    private static String READINGS_TEXT = JGloss.messages.getString( "annotationeditor.readings");

    public WordNode( InnerNode parent, Element word) {
        super( parent, null);
        this.word = word;

        StringBuffer wordTextBuffer = new StringBuffer( word.getEndOffset() -
                                                        word.getStartOffset());
        int readingCount = 0;
        int firstReadingIndex = -1;
        for ( int i=0; i<word.getElementCount(); i++) {
            Element child = word.getElement( i);
            // child is either a READING_KANJI element, which has 2 children, or a BASE
            // node which has no children
            if (child.getElementCount() == 1) {
                ((AbstractDocument.AbstractElement) child).dump( System.out, 0);
                ((AbstractDocument.AbstractElement) child.getParentElement()).dump( System.out, 0);
            }
            if (child.getElementCount() > 0) {
                readingCount++;
                // append the kanji part
                wordTextBuffer.append( AnnotationNode.getElementText( child.getElement( 1)));
                if (firstReadingIndex == -1)
                    firstReadingIndex = i;
            }
            else {
                // append content
                wordTextBuffer.append( AnnotationNode.getElementText( child));
            }
        }
        wordText = wordTextBuffer.toString();
        updateReadingText();

        children = new Vector( readingCount);
        readings = new ReadingTextNode[readingCount];
        if (readingCount > 1) {
            int readingIndex = 0;
            for ( int i=firstReadingIndex; i<word.getElementCount(); i++) {
                Element child = word.getElement( i);
                if (word.getElement( i).getElementCount() > 0) { // READING_KANJI node
                    readings[readingIndex] = new ReadingTextNode( this, (byte) i, false) {
                            public void setText( String text) {
                                super.setText( text);
                                updateReadingText();
                            }
                        };
                    children.add( readings[readingIndex]);
                    readingIndex++;
                }
            }
        }
        else {
            readings[0] = new ReadingTextNode( this, (byte) firstReadingIndex, true) {
                    public void setText( String text) {
                        super.setText( text);
                        updateReadingText();
                    }
                };
            children.add( readings[0]);
        }
    }

    public int getReadingCount() {
        return readings.length;
    }

    public ReadingTextNode[] getReadings() {
        return readings;
    }

    public String getWord() { return wordText; }

    public String getReading() { return readingText; }

    public void setReading( String reading) {
        setReading( wordText, reading);
    }

    public void setReading( String baseWord, String reading) {
        if (word.getElementCount() == 1) {
            // The annotated word is either all kanji or all kana. In either case, there is
            // no need to split the reading/base.
            readings[0].setText( reading);
        }
        else {
            // extract the reading of the kanji part(s) from the reading string and use them
            // for the new annotation.
            String[][] wr = StringTools.splitWordReading( baseWord, baseWord, reading);
            int readingIndex = 0;
            // iterate over all base/reading pairs
            for ( int i=0; i<wr.length; i++) {
                if (wr[i].length == 2) {
                    if (readingIndex < readings.length)
                        // set text of next reading node
                        readings[readingIndex++].setText( wr[i][1]);
                    else
                        // append surplus readings for baseWord to the last reading node
                        readings[readings.length-1].setText( readings[readings.length-1].getText() +
                                                             wr[i][1]);
                }
            }
            // If baseWord didn't split into enough readings to fill all reading nodes, delete
            // text of remaining nodes.
            for ( int i=readingIndex; i<readings.length; i++) {
                readings[i].setText( "");
            }
        }
    }

    protected void updateReadingText() {
        StringBuffer readingTextBuffer = new StringBuffer( word.getEndOffset() - word.getStartOffset());
        for ( byte i=0; i<word.getElementCount(); i++) {
            Element child = word.getElement( i);
            if (child.getElementCount() > 0) {
                // append the kanji part
                String reading = AnnotationNode.getElementText( child.getElement( 0));
                if (!" ".equals( reading))
                    readingTextBuffer.append( reading);
            }
            else {
                // append content
                readingTextBuffer.append( AnnotationNode.getElementText( child));
            }
        }
        readingText = readingTextBuffer.toString();
    }

    public String toString() {
        return READINGS_TEXT;
    }
} // class WordNode
