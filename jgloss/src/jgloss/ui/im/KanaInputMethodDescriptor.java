/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.im;

import java.awt.Image;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodDescriptor;
import java.util.Locale;

public class KanaInputMethodDescriptor implements InputMethodDescriptor {
    private static final Locale[] LOCALES = new Locale[] { new Locale( "ja", "JA", "KanaIM") };

    public KanaInputMethodDescriptor() {
        System.err.println( "instantiation");
    }

    public InputMethod createInputMethod() {
        System.err.println( "creating kana input");
        return new KanaInputMethod();
    }

    public Locale[] getAvailableLocales() {
        System.err.println( "getting locales");
        return LOCALES;
    }

    public String getInputMethodDisplayName( Locale inputLocale, Locale displayLanguage) {
        return "simple kana input";
    }

    public Image getInputMethodIcon( Locale inputLocale) {
        return null;
    }

    public boolean hasDynamicLocaleList() { return false; }
} // class KanaInputMethodDescriptor
