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
import jgloss.ui.*;
import jgloss.ui.doc.*;

import javax.swing.text.*;

/**
 * Node for editing the translation of an annotation element.
 *
 * @author Michael Koch
 */
public class TranslationTextNode extends ReadingTranslationNode {
    public TranslationTextNode( InnerNode parent) {
        super( parent, JGloss.messages.getString( "annotationeditor.translation"), "");
        text = AnnotationNode.getElementText( getElement());
    }

    protected Element getElement() {
        return ((AnnotationNode) parent).getAnnotationElement().getElement( 1);
    }
} // class TranslationTextNode
