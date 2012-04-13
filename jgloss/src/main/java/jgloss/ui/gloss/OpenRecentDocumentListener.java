/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.gloss;

import java.io.File;

import jgloss.ui.OpenRecentMenu;

/**
 * Open the document selected from the open recent menu.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class OpenRecentDocumentListener implements OpenRecentMenu.FileSelectedListener {
    private final JGlossFrame target;

    /**
     * @param target frame to open the document into. If <code>null</code>, creates a new frame.
     */
   OpenRecentDocumentListener(JGlossFrame target) {
        this.target = target;
    }

    @Override
    public void fileSelected(File file) {
        // test if the file is already open
        JGlossFrame frame = getFrameForFile(file);
        if (frame != null) {
            frame.frame.setVisible(true);
        } else {
            loadFile(file);
        }
    }

    private JGlossFrame getFrameForFile(File file) {
        String path = file.getAbsolutePath();
        
        for (JGlossFrame frame : JGlossFrame.jglossFrames) {
            if (path.equals( frame.getModel().getDocumentPath())) {
                return frame;
            }
        }
        
        return null;
    }

    private void loadFile(final File file) {
        new Thread() {
                @Override
                public void run() {
                    JGlossFrame which = target==null ||
                        target.getModel().isEmpty() ? new JGlossFrame() : target;
                    which.loadDocument( file);
                }
            }.start();
    }
}