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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jgloss.JGloss;
import jgloss.ui.Dictionaries;
import jgloss.ui.ExclusionList;
import jgloss.ui.ImportDialog;
import jgloss.ui.util.UIUtilities;

/**
 * Action to import a document seleted in the shown {@link ImportDialog}.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class ImportDocumentAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final JGlossFrame target;

    /**
     * @param target frame to import the document into. If <code>null</code>, creates a new frame.
     */
    ImportDocumentAction(JGlossFrame target) {
        this.target = target;
        UIUtilities.initAction(this, "main.menu.import"); 
    }

    @Override
    public void actionPerformed( ActionEvent e) {
        new Thread( "JGloss import") {
                @Override
    			public void run() {
                    ImportDialog d = new ImportDialog( target != null ? 
                                                       target.frame :
                                                       null);
                    if (d.doDialog()) {
                        JGlossFrame which;
                        if (target==null || target.model.isEmpty()) {
                            which = new JGlossFrame();
                        } else {
                            which = target;
                        }
                        
                        if (d.selectionIsFilename()) {
                            which.importDocument
                                ( d.getSelection(), d.isDetectParagraphs(),
                                  d.createParser( Dictionaries.getInstance().getDictionaries(),
                                                  ExclusionList.getExclusions()),
                                  d.createReadingAnnotationFilter(),
                                  d.getEncoding());
                        } else {
                            which.importString
                                ( d.getSelection(), d.isDetectParagraphs(), 
                                  JGloss.MESSAGES.getString( "import.textarea"),
                                  JGloss.MESSAGES.getString( "import.textarea"),
                                  d.createParser( Dictionaries.getInstance().getDictionaries(),
                                                  ExclusionList.getExclusions()),
                                  d.createReadingAnnotationFilter(),
                                  false);
                        }
                    }
                }
            }.start();
    }
}