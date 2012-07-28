package jgloss.ui.xml;

import java.io.IOException;
import java.io.InputStream;

/**
 * Adds a closing "/" to meta tags found on the input stream. This is a primitive way to make
 * JGloss 1 documents (which are simple html) conform to the XML standard. It should work for
 * many JGloss 1 documents. Since the parsing is so simple, it may fail in multiple ways, e. g.
 * for closing brackets contained in attribute values.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class CloseMetaTagInputStream extends InputStream {
	private enum State {
		WAIT_FOR_LT,
		SEEN_LT,
		IN_OPENING_TAG_NAME,
		CLOSE_TAG_ON_GT,
		READ_GT,
	}
	
	private State state = State.WAIT_FOR_LT;
	
	private final StringBuilder tagNameBuilder = new StringBuilder();
	
	private final InputStream source;
	
	public CloseMetaTagInputStream(InputStream source) {
		this.source = source;
	}
	
	@Override
	public int read() throws IOException {
		if (state == State.READ_GT) {
			state = State.WAIT_FOR_LT;
			return '>';
		}
		
		int data = source.read();
		
		if (data != -1) {
			switch (state) {
			case WAIT_FOR_LT:
				if (data == '<') {
					state = State.SEEN_LT;
				}
				break;
				
			case SEEN_LT:
				if (Character.isLetter(data)) {
					tagNameBuilder.append((char) data);
					state = State.IN_OPENING_TAG_NAME;
				} else {
					state = State.WAIT_FOR_LT;
				}
				break;
				
			case IN_OPENING_TAG_NAME:
				if (Character.isLetter(data)) {
					tagNameBuilder.append((char) data);
				} else {
					checkCloseTagOnGt();
				}

				if (state != State.CLOSE_TAG_ON_GT) {
					break;
				} // else fallthrough: data might be '>'
				
			case CLOSE_TAG_ON_GT:
				if (data == '>') {
					data = '/';
					state = State.READ_GT;
				}
				break;
			}
		}
		
		return data;
	}
	
	@Override
	public void close() throws IOException {
	    source.close();
	}

	private void checkCloseTagOnGt() {
	    String tagName = tagNameBuilder.toString();
	    tagNameBuilder.setLength(0);
	    
	    if ("meta".equalsIgnoreCase(tagName)) {
	    	state = State.CLOSE_TAG_ON_GT;
	    } else {
	    	state = State.WAIT_FOR_LT;
	    }
    }
}
