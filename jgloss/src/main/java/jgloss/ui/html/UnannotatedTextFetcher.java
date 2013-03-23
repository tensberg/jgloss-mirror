/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.ui.html;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.html.HTML;


/**
 * Fetch a span of text from the document, leaving out text with is part of an annotation.
 */
class UnannotatedTextFetcher extends ElementProcessorAdapter {
    private static final Logger LOGGER = Logger.getLogger(UnannotatedTextFetcher.class.getPackage().getName());

    private StringBuilder text;
    private final Segment textSegment;

    private Position start;

    private Position end;

    UnannotatedTextFetcher() {
        textSegment = new Segment();
        textSegment.setPartialReturn(true);
    }

    public String getText() {
        return text.toString();
    }

    @Override
    public void documentProcessingStarts(Position start, Position end) {
        this.start = start;
        this.end = end;
        text = new StringBuilder(end.getOffset() - start.getOffset());
    }

    @Override
	public boolean processElement(JGlossHTMLDoc doc, Element elem) {
        if (elem.getName().equals(HTML.Tag.HEAD.toString())) {
            return false;
        }

        if (elem.getName().equals(HTML.Tag.CONTENT.toString())) {
            processContentElement(doc, elem);
            return false;
        }

        return true; // recurse over children
    }

    private void processContentElement(JGlossHTMLDoc doc, Element elem) {
        // element spanning some text, add if the text is not part of an annotation
        AttributeSet as = elem.getAttributes();
        if (!as.isDefined( AnnotationTags.READING) &&
            !as.isDefined( AnnotationTags.TRANSLATION)) {
            // copy the part of the element text which intersects with the requested
            // text span to the string buffer
            int offset = Math.max(start.getOffset(),elem.getStartOffset());
            int length = Math.min(end.getOffset(),elem.getEndOffset())-offset;
            try {
                // partial return is activated for the text segment, so we have
                // to iterate until all segment fragments are copied
                while (length > 0) {
                    doc.getText(offset, length, textSegment);
                    text.append(textSegment.array,textSegment.offset,textSegment.count);
                    offset += textSegment.count;
                    length -= textSegment.count;
                }
            } catch (BadLocationException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
} // class UnannotatedTextFetcher