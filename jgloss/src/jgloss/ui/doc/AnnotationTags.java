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

package jgloss.ui.doc;

import javax.swing.text.html.HTML;

/**
 * Class enumerating the custom HTML tags used for modelling annotations in a document.
 * An {@link #ANNOTATION ANNOTATION} element does not contain text itself and has exactly three children:
 * <UL>
 * <LI>A {@link #READING READING} element the text of which it spans the reading annotation.</LI>
 * <LI>A {@link #KANJI KANJI} element. This element contains the text of the original document 
 *     and represents the text which is annotated.</LI>
 * <LI>A {@link #TRANSLATION TRANSLATION} element which contains the translation of the annotated text.</LI>
 * </UL>
 *
 * @author Michael Koch
 */
public class AnnotationTags extends HTML.Tag {
    /**
     * Tag which is used to model an annotation. An annotation element
     * has exactly one reading, one kanji and one translation element as children.
     */
    public final static AnnotationTags ANNOTATION = new AnnotationTags( "anno", true, true);
    /**
     * A tag which contains the reading annotation.
     */
    public final static AnnotationTags READING = new AnnotationTags( "reading", false, false);
    /**
     * A tag which contains the annotated text.
     */
    public final static AnnotationTags KANJI = new AnnotationTags( "kanji", false, false);
    /**
     * A tag which contains the translation of the annotated text.
     */
    public final static AnnotationTags TRANSLATION = new AnnotationTags( "trans", false, false);

    /**
     * Name of the tag.
     */
    private String id;

    protected AnnotationTags( String id, boolean causesBreak, boolean isBlock) {
        super( id, causesBreak, isBlock);
        this.id = id;
    }

    /**
     * Returns the id of this tag. This is the name as it appears in the HTML document.
     *
     * @return The id of this tag.
     */
    public String getId() { return id; }

    /**
     * Tests if the object is equal to this tag. It is equal if it is an instance of <CODE>HTML.Tag</CODE>
     * and has the same string representation (this is equivalent to having the same id).
     *
     * @param o The object to test for equality.
     * @return <CODE>true</CODE> if the object is equal to this tag.
     */
    public boolean equals( Object o) {
        if (o instanceof HTML.Tag)
            return toString().equals( o.toString());
        return false;
    }

    /**
     * Returns the unique instance of the annotation tag which is equal to the HTML tag.
     * Unfortunately, the set of tags used by the method <CODE>getTag</CODE> in class 
     * <CODE>javax.swing.text.html.HTML</CODE> cannot be extended for the annotation tags.
     * Some classes, like <CODE>DocumentParser</CODE> in <CODE>javax.swing.text.html.parser</CODE>
     * will create instances of <CODE>HTML.UnknownTag</CODE> for all tags not in the set.
     * This method can be used to replace such a tag with the approriate <CODE>AnnotationTag</CODE>.
     * If the tag is not equal to one of the annotation tags, it will be returned unchanged.
     *
     * @param htmlTag The HTML tag for which the equivalent annotation tag should be returned.
     * @return The unique annotation tag equivalent to the <CODE>htmlTag</CODE>, or the
     * <CODE>htmlTag</CODE> itself if it is not an annotation tag.
     */
    public static HTML.Tag getAnnotationTagEqualTo( HTML.Tag htmlTag) {
        if (ANNOTATION.equals( htmlTag))
            return ANNOTATION;
        if (READING.equals( htmlTag))
            return READING;
        if (KANJI.equals( htmlTag))
            return KANJI;
        if (TRANSLATION.equals( htmlTag))
            return TRANSLATION;
        return htmlTag;
    }
} // class AnnotationTags
