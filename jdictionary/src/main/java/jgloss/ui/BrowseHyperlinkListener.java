package jgloss.ui;

import static java.util.logging.Level.SEVERE;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

/**
 * Open an activated hyperlink in the default external application.
 * Uses the {@link Desktop#browse(java.net.URI) Desktop.browse} API.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class BrowseHyperlinkListener implements HyperlinkListener {
	private static final Logger LOGGER = Logger.getLogger(BrowseHyperlinkListener.class.getPackage().getName());
	
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
    	if (e.getEventType() == EventType.ACTIVATED) {
    		if (!Desktop.getDesktop().isSupported(Action.BROWSE)) {
    			LOGGER.warning("opening hyperlinks in external applications is not supported");
    			return;
    		}
    		
    		try {
                Desktop.getDesktop().browse(e.getURL().toURI());
            } catch (IOException ex) {
                LOGGER.log(SEVERE, "failed to open hyperlink in external application", ex);
            } catch (URISyntaxException ex) {
                LOGGER.log(SEVERE, "invalid URL " + e.getURL(), ex);
            }
    	}
    }
}