/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.ui.gloss;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.Dictionaries;
import jgloss.ui.ExclusionList;
import jgloss.ui.GeneralDialog;
import jgloss.util.CharacterEncodingDetector;

/**
 * Import the String clipboard content, annotating the Japanese words in the text.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class ImportClipboardStrategy extends ImportStrategy {
    
    private static final String IMPORT_CLIPBOARD = JGloss.MESSAGES.getString( "import.clipboard");
    
    private String data;

    ImportClipboardStrategy(JGlossFrame frame) {
        super(frame, IMPORT_CLIPBOARD, JGloss.PREFS.getBoolean
                ( Preferences.IMPORTCLIPBOARD_DETECTPARAGRAPHS, true), 
                GeneralDialog.getInstance().createReadingAnnotationFilter(), 
                GeneralDialog.getInstance().createImportClipboardParser
                    ( Dictionaries.getInstance().getDictionaries(), ExclusionList.getExclusions()));
    }

    @Override
    Reader createReader() throws IOException, UnsupportedFlavorException {
        Transferable t = frame.getToolkit().getSystemClipboard().getContents( this);

        data = (String) t.getTransferData( DataFlavor.stringFlavor);

        // try to autodetect the character encoding if the transfer didn't honor the 
        // charset correctly.
        boolean autodetect = true;
        for ( int i=0; i<data.length(); i++) {
            if (data.charAt( i) > 255) {
                // The string contains a character outside the ISO-8859-1 range,
                // so presumably the transfer went OK.
                autodetect = false;
                break;
            }
        }
        if (autodetect) {
            byte[] bytes = data.getBytes( "ISO-8859-1");
            String enc = CharacterEncodingDetector.guessEncodingName( bytes);
            if (!enc.equals( CharacterEncodingDetector.ENC_UTF_8)) {
                data = new String( bytes, enc);
            }
        }

        return new StringReader(data);
    }

    @Override
    void customizeModel(JGlossFrameModel model) {
        model.setDocumentName(IMPORT_CLIPBOARD);
        model.setDocumentChanged(true);
    }

    @Override
    int getLength() {
        return data.length();
    }

}
