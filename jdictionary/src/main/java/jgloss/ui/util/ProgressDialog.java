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
 * $$Id$$
 *
 */

package jgloss.ui.util;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;

import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jgloss.JGloss;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

public class ProgressDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JLabel message = new JLabel( "", SwingConstants.CENTER);
    
    private final JProgressBar progress = new JProgressBar();
    
    public ProgressDialog(Window parent) {
        super(parent);

        setModalityType(APPLICATION_MODAL);
        setTitle( JGloss.MESSAGES.getString( "dictionaries.loading.title"));

        JPanel content = new JPanel(new MigLayout(new LC().fillX().wrapAfter(1)));
        content.add(message, "grow");
        content.add(progress, "grow");
        setContentPane(content);
        
        setSize( 450, 80);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);
    }
    
    public void setMessage(String message) {
        this.message.setText(message);
    }
    
    public void setProgress(int progress) {
        this.progress.setValue(progress);
    }
}
