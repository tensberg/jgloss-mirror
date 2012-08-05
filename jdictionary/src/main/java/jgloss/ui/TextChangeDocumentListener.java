package jgloss.ui;

import static java.util.logging.Level.SEVERE;

import java.util.logging.Logger;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

abstract class TextChangeDocumentListener implements DocumentListener {
	private static final Logger LOGGER = Logger.getLogger(TextChangeDocumentListener.class.getPackage().getName());
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		textChanged(getText(e));
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		textChanged(getText(e));
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		textChanged(getText(e));
	}
	
	protected abstract void textChanged(String text);
	
	private String getText(DocumentEvent e) {
		String text;
		Document document = e.getDocument();
		try {
			text = document.getText(0, document.getLength());
		} catch (BadLocationException ex) {
			LOGGER.log(SEVERE, "failed to get text from document", ex);
			text = "";
		}
		
		return text;
	}

}
