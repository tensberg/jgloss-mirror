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
        pastearea.setText( "");
        if (text.length() < 2) // ignore single key inputs
            return;

        // construct a new string the hacky way because AWT ignores the
        // charset when pasting japanese text from the X primary selection
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

        new JGlossFrame().importString( text, JGloss.messages.getString( "import.clipboard"),
                                        JGloss.messages.getString( "import.clipboard"), false);
    }
} // class PasteImportFrame
