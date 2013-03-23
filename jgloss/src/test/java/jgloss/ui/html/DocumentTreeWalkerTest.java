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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentTreeWalkerTest {
    @Mock
    private JGlossHTMLDoc doc;

    @Mock
    private ElementProcessor processor;

    @Mock
    private Position startPosition;

    @Mock
    private Position endPosition;

    @Mock
    private Element root;

    @Mock
    private Element child0;

    @Mock
    private Element child1;

    @Mock
    private Element child2;

    @Mock
    private Element child2b;

    @Mock
    private Element child3;

    @InjectMocks
    private DocumentTreeWalker walker;

    @Test
    public void testWalkCompleteWriteLock() throws BadLocationException {
        when(processor.writesToDocument()).thenReturn(true);
        when(processor.processElement(eq(doc), any(Element.class))).thenReturn(true);

        mockElements();

        startWalk(0, 10);

        InOrder inOrder = inOrder(doc);
        inOrder.verify(doc).walkStarts(true);
        inOrder.verify(doc).walkEnds(true);

        inOrder = inOrder(processor);
        inOrder.verify(processor).documentProcessingStarts(startPosition, endPosition);
        inOrder.verify(processor).processElement(doc, root);
        inOrder.verify(processor).processElement(doc, child0);
        inOrder.verify(processor).processElement(doc, child1);
        inOrder.verify(processor).processElement(doc, child2);
        inOrder.verify(processor).processElement(doc, child2b);
        inOrder.verify(processor).processElement(doc, child3);
        inOrder.verify(processor).documentProcessingEnds();
    }

    @Test
    public void testIterateBackwards() throws BadLocationException {
        when(processor.writesToDocument()).thenReturn(true);
        when(processor.processElement(eq(doc), any(Element.class))).thenReturn(true);
        when(processor.iterateBackwards()).thenReturn(true);

        mockElements();

        startWalk(0, 10);

        InOrder inOrder = inOrder(doc);
        inOrder.verify(doc).walkStarts(true);
        inOrder.verify(doc).walkEnds(true);

        inOrder = inOrder(processor);
        inOrder.verify(processor).documentProcessingStarts(startPosition, endPosition);
        inOrder.verify(processor).processElement(doc, root);
        inOrder.verify(processor).processElement(doc, child3);
        inOrder.verify(processor).processElement(doc, child2);
        inOrder.verify(processor).processElement(doc, child2b);
        inOrder.verify(processor).processElement(doc, child1);
        inOrder.verify(processor).processElement(doc, child0);
        inOrder.verify(processor).documentProcessingEnds();
    }

    @Test
    public void testWalkPartialReadLock() throws BadLocationException {
        when(processor.writesToDocument()).thenReturn(false);
        when(processor.processElement(eq(doc), any(Element.class))).thenReturn(true);

        mockElements();

        startWalk(4, 5);

        InOrder inOrder = inOrder(doc);
        inOrder.verify(doc).walkStarts(false);
        inOrder.verify(doc).walkEnds(false);

        inOrder = inOrder(processor);
        inOrder.verify(processor).documentProcessingStarts(startPosition, endPosition);
        inOrder.verify(processor).processElement(doc, root);
        inOrder.verify(processor).processElement(doc, child2);
        inOrder.verify(processor).processElement(doc, child2b);
        inOrder.verify(processor).documentProcessingEnds();
    }

    @Test
    public void testWalkPartialNoRecurse() throws BadLocationException {
        when(processor.writesToDocument()).thenReturn(false);
        when(processor.processElement(eq(doc), any(Element.class))).thenReturn(true);
        when(processor.processElement(eq(doc), eq(child2))).thenReturn(false);

        mockElements();

        startWalk(4, 5);

        InOrder inOrder = inOrder(doc);
        inOrder.verify(doc).walkStarts(false);
        inOrder.verify(doc).walkEnds(false);

        inOrder = inOrder(processor);
        inOrder.verify(processor).documentProcessingStarts(startPosition, endPosition);
        inOrder.verify(processor).processElement(doc, root);
        inOrder.verify(processor).processElement(doc, child2);
        inOrder.verify(processor).documentProcessingEnds();
    }

    private void startWalk(int start, int end) throws BadLocationException {
        when(doc.createPosition(start)).thenReturn(startPosition);
        when(doc.createPosition(end)).thenReturn(endPosition);
        when(startPosition.getOffset()).thenReturn(start);
        when(endPosition.getOffset()).thenReturn(end);

        walker.startWalk(start, end, processor);
    }

    private void mockElements() throws BadLocationException {
        when(doc.getDefaultRootElement()).thenReturn(root);
        when(root.getElementCount()).thenReturn(4);
        when(root.getElement(0)).thenReturn(child0);
        when(root.getElement(1)).thenReturn(child1);
        when(root.getElement(2)).thenReturn(child2);
        when(root.getElement(3)).thenReturn(child3);
        when(child2.getElementCount()).thenReturn(1);
        when(child2.getElement(0)).thenReturn(child2b);

        when(root.getStartOffset()).thenReturn(0);
        when(root.getEndOffset()).thenReturn(10);
        when(child0.getStartOffset()).thenReturn(0);
        when(child0.getEndOffset()).thenReturn(1);
        when(child1.getStartOffset()).thenReturn(2);
        when(child1.getEndOffset()).thenReturn(3);
        when(child2.getStartOffset()).thenReturn(4);
        when(child2.getEndOffset()).thenReturn(5);
        when(child2b.getStartOffset()).thenReturn(4);
        when(child2b.getEndOffset()).thenReturn(5);
        when(child3.getStartOffset()).thenReturn(6);
        when(child3.getEndOffset()).thenReturn(10);
    }
}
