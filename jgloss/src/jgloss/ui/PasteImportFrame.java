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

package jgloss.ui;

import jgloss.JGloss;
import jgloss.dictionary.CharacterEncodingDetector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Frame with a single TextArea which will create a new JGloss document
 * from text pasted in it.
 *
 * @author Michael Koch
 */
public class PasteImportFrame extends JFrame implements TextListener {
    private TextArea pastearea;

    public PasteImportFrame() {
        super( JGloss.messages.getString( "pasteimport.title"));
        
        setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout( new BorderLayout());

        JTextArea description = 
            new JTextArea( JGloss.messages.getString( "pasteimport.description"));
        description.setEditable( false);
        description.setOpaque( false);
        getContentPane().add( description, BorderLayout.NORTH);

        // Pasting the X primary selection does only work with AWT components
        // with the Sun JDK 1.3 for Linux
        pastearea = new TextArea( "", 5, 5, TextArea.SCROLLBARS_NONE);
        pastearea.setEditable( true);
        pastearea.addTextListener( this);
        getContentPane().add( pastearea, BorderLayout.CENTER);

        pack();
        setVisible( true);
    }

    /**
     * Creates a new document from the inserted text.
     */
    public void textValueChanged( TextEvent e) {
        String text = pastearea.getText();
        if (text.length() < 5 || // ignore single key inputs
            text.equals( JGloss.messages.getString( "pasteimport.standby"))) { // avoid recursive calls
            if (text.length()>0 && text.length()<5) // clear single key strokes
                pastearea.setText( "");
            return;
        }
        pastearea.setEditable( false);
        pastearea.setText( JGloss.messages.getString( "pasteimport.standby"));

        // construct a new string the hacky way because sometimes the charset is ignored
        // when pasting japanese text from the X primary selection
        byte[] tb = new byte[text.length()];
        boolean convert = true;
        for ( int i=0; i<text.length(); i++) {
            char c = text.charAt( i);
            if (c > 255) { 
                // non-ISO-8859-1, seems that the paste operation did not ignore the charset
                convert = false;
                break;
            }
            tb[i] = (byte) c;
        }

        if (convert) {
            String enc = CharacterEncodingDetector.guessEncodingName( tb);
            if (!enc.equals( CharacterEncodingDetector.ENC_UTF_8)) { // don't trust UTF-8 detection
                try {
                    text = new String( tb, enc);
                } catch (java.io.UnsupportedEncodingException ex) {}
            }
        }

        // During the construction of the frame, the user has no visible feedback except for the
        // message in the text area. So don't do it in an own thread.
        final JGlossFrame f = new JGlossFrame();
        final String ftext = text;
        new Thread() {
                public void run() {
                    f.importString( ftext, GeneralDialog.getComponent().createImportClipboardParser
                                    ( Dictionaries.getDictionaries(), ExclusionList.getExclusions()),
                                    GeneralDialog.getComponent().createReadingAnnotationFilter(),
                                    JGloss.messages.getString( "import.clipboard"),
                                    JGloss.messages.getString( "import.clipboard"), false);
                }
            }.start();
        pastearea.setText( "");
        pastearea.setEditable( true);
    }
} // class PasteImportFrame
