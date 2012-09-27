/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodContext;
import java.text.AttributedString;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


class KanaInputMethod implements InputMethod {
    protected InputMethodContext context;
    protected boolean active = false;
    protected boolean compositionEnabled = true;
    protected StringBuilder conversionBuffer = new StringBuilder( 3);
    protected Locale locale;
    protected Character.Subset[] subsets;

    protected static final Map<TextAttribute, InputMethodHighlight> SELECTED_RAW_TEXT = new TreeMap<TextAttribute, InputMethodHighlight>();
    
    static {
        SELECTED_RAW_TEXT.put( TextAttribute.INPUT_METHOD_HIGHLIGHT,
                               InputMethodHighlight.SELECTED_RAW_TEXT_HIGHLIGHT);
    }

    KanaInputMethod() {}

    @Override
	public void activate() {
        active = true;
        conversionBuffer.setLength( 0);
    }

    @Override
	public void deactivate( boolean isTemporary) {
        endComposition();
        active = false;
    }

    @Override
	public void setInputMethodContext( InputMethodContext _context) {
        context = _context;
    }

    @Override
	public boolean setLocale( Locale _locale) {
        locale = _locale;
        return true;
    }

    @Override
	public Locale getLocale() {
        return locale;
    }
    
    @Override
	public void setCharacterSubsets( Character.Subset[] _subsets) {
        subsets = _subsets;
    }

    @Override
	public void setCompositionEnabled( boolean enable) {
        compositionEnabled = enable;
    }

    @Override
	public boolean isCompositionEnabled() {
        return compositionEnabled;
    }

    @Override
	public void reconvert() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void dispatchEvent( AWTEvent _event) {
        if (!(active && compositionEnabled)) {
	        return;
        }

        if (!(_event instanceof KeyEvent)) {
	        return;
        }

        KeyEvent event = (KeyEvent) _event;
        if (event.getID() != KeyEvent.KEY_TYPED ||
            event.getModifiers() != 0) {
	        return;
        }

        char c = event.getKeyChar();

        if (c == KeyEvent.VK_BACK_SPACE) {
            if (conversionBuffer.length() > 1) {
                conversionBuffer.deleteCharAt( conversionBuffer.length()-1);
                dispatchString( conversionBuffer.toString(), false);
                event.consume();
            } else {
	            return;
            }
        }

        if (!RomajiTranslator.isApplicableChar( c)) {
	        return;
        }

        conversionBuffer.append( c);
        String out = RomajiTranslator.translate( conversionBuffer,
                                                 RomajiTranslator.HIRAGANA);

        if (out!=null && out.length()!=0) {
	        dispatchString( out, true);
        } else {
	        dispatchString( conversionBuffer.toString(), false);
        }

        event.consume();
    }

    @Override
	public void notifyClientWindowChange( Rectangle bounds) {}

    @Override
	public void hideWindows() {}
  
    @Override
	public void removeNotify() {}

    @Override
	public void endComposition() {
        if (conversionBuffer.length() > 0) {
            dispatchString( conversionBuffer.toString(), true);
            conversionBuffer.setLength( 0);
        }
    }

    @Override
	public void dispose() {}

    @Override
	public Object getControlObject() { return null; }   

    protected void dispatchString( String s, boolean committed) {
        if (committed) {
            context.dispatchInputMethodEvent
                ( InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                  new AttributedString( s).getIterator(), s.length(), null, null);
        }
        else {
            context.dispatchInputMethodEvent
                ( InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                  new AttributedString( s, SELECTED_RAW_TEXT).getIterator(),
                  0, TextHitInfo.trailing( s.length()-1),
                  TextHitInfo.trailing( s.length()-1));
        }     
    }
} // class KanaInputMethod
