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

import javax.swing.text.Element;


/**
 * Remove all annotation elements intersecting a span of text in the document.
 */
class AnnotationRemover extends ElementProcessorAdapter {

    @Override
    public boolean writesToDocument() {
        return true;
    }

    @Override
    public boolean iterateBackwards() {
        return true;
    }

    @Override
    public boolean processElement(JGlossHTMLDoc doc, Element elem) {
        if (elem.getName().equals(AnnotationTags.ANNOTATION.getId())) {
            doc.removeAnnotationElement(elem);
            return false;
        }

        return true;
    }

} // class AnnotationRemover