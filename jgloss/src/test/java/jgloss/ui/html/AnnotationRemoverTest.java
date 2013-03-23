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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.swing.text.Element;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationRemoverTest {
    @Mock
    private JGlossHTMLDoc doc;

    @Mock
    private Element element;

    private final AnnotationRemover remover = new AnnotationRemover();

    @Test
    public void verifyWriteAndIterationOrder() {
        assertThat(remover.writesToDocument()).isTrue();
        assertThat(remover.iterateBackwards()).isTrue();
    }

    @Test
    public void testAnnotationElementIsRemoved() {
        when(element.getName()).thenReturn(AnnotationTags.ANNOTATION.getId());

        remover.processElement(doc, element);
        verify(doc).removeAnnotationElement(element);
    }

    @Test
    public void testOtherElementsAreIgnored() {
        when(element.getName()).thenReturn("foo");
        remover.processElement(doc, element);
        verifyZeroInteractions(doc);
    }
}
