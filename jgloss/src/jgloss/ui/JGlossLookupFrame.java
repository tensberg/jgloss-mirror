package jgloss.ui;

import javax.swing.JMenu;

public class JGlossLookupFrame extends LookupFrame {
    public JGlossLookupFrame( LookupModel _model) {
        super( _model);
    }

    protected void createFileMenuItems( JMenu menu) {
        menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.importDocument));
        menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.importClipboard));
        addWindowListener( JGlossFrame.actions.importClipboardListener);
        menu.addMenuListener( JGlossFrame.actions.importClipboardListener);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.open));
        JMenu openRecent = JGlossFrame.OPEN_RECENT.createMenu( JGlossFrame.actions.openRecentListener);
        menu.add( openRecent);
        menu.addSeparator();

        super.createFileMenuItems( menu);
    }
} // class JGlossLookupFrame
