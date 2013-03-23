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

import javax.swing.text.html.HTML;
import javax.swing.text.html.parser.TagElement;

/**
 * TagElements are used by the HTML parser to inquire properties of an
 * element. This class adds support for the annotation elements of JGloss.
 */
class JGlossTagElement extends TagElement {
    /**
     * The tag this element wraps.
     */
    private HTML.Tag htmlTag;

    /**
     * Creates a new TagElement which wraps the given element. If the HTML
     * tag represented by this element is an instance of HTML.UnknownTag it
     * will be replaced by the equivalent unque annotation tag.
     */
    public JGlossTagElement(javax.swing.text.html.parser.Element e, boolean fictional) {
        super(e, fictional);

        // Get the real annotation tag. Unfortunately the creation of the
        // HTML.UnknownTags
        // cannot be prevented, so we have to work around it.
        htmlTag = super.getHTMLTag();
        if (htmlTag instanceof HTML.UnknownTag) {
            htmlTag = AnnotationTags.getAnnotationTagEqualTo(htmlTag);
        }
    }

    @Override
    public HTML.Tag getHTMLTag() {
        return htmlTag;
    }

    @Override
    public boolean breaksFlow() {
        return htmlTag.breaksFlow();
    }

    @Override
    public boolean isPreformatted() {
        return htmlTag.isPreformatted();
    }
}