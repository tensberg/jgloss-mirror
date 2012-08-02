package jgloss.ui.gloss;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import jgloss.JGlossApp;
import jgloss.ui.util.UIUtilities;

/**
 * Panel shown in an empty JGloss document, offering the most important actions.
 *
 */
class EmptyDocumentActionsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final ImageIcon BACKGROUND_IMAGE = new ImageIcon(EmptyDocumentActionsPanel.class.getResource("/images/emptydocument_background.png"));
	
	private final Action wordLookupAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        {
        	UIUtilities.initAction( this, "main.menu.wordlookup");
        }
        
		@Override
		public void actionPerformed( ActionEvent e) {
            JGlossApp.getLookupFrame().setVisible(true);
        }
    };
	
	EmptyDocumentActionsPanel(DocumentActions documentActions) {
		setBackground(Color.WHITE);
		setOpaque(true);
		setLayout(new GridBagLayout());
		add(createActionsPanel(documentActions));
	}

	@Override
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	    Image image = BACKGROUND_IMAGE.getImage();
	    
	    int x = (getWidth() - BACKGROUND_IMAGE.getIconWidth())/2;
	    int y = (getHeight() - BACKGROUND_IMAGE.getIconHeight())/2;
	    
		g.drawImage(image, x, y, null);
	}
	
	private Component createActionsPanel(DocumentActions documentActions) {
		JPanel actionsPanel = new JPanel(new GridBagLayout());
		actionsPanel.setOpaque(false);
	   	JPanel actionButtonsPanel = new JPanel(new GridLayout(0, 1, 0, 16));
	   	actionButtonsPanel.setOpaque(false);
	   	actionsPanel.add(actionButtonsPanel);
	   	
	   	actionButtonsPanel.add(new JButton(documentActions.importClipboard));
	   	actionButtonsPanel.add(new JButton(documentActions.importDocument));
	   	actionButtonsPanel.add(new JButton(wordLookupAction));
	   	
	    return actionsPanel;
    }

}
