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
 *
 */

package jgloss.ui.util;

import static javax.swing.SwingWorker.StateValue.DONE;
import static javax.swing.SwingWorker.StateValue.STARTED;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Show progress feedback for SwingWorker background execution.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public class SwingWorkerProgressFeedback implements PropertyChangeListener {
    public static final String DEFAULT_MESSAGE_PROPERTY = "message";

    private static final String PROGRESS_PROPERTY = "progress";

    private static final String STATE_PROPERTY = "state";

    private static final int DIALOG_DELAY_MS = 500;

    private static final Cursor WAIT_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    private final SwingWorker<?, ?> worker;

    private final String messageProperty;

    private final Component feedbackTarget;

    private final Cursor originalCursor;

    private ProgressDialog progressDialog;

    private String message;

    private Timer dialogShowTimer;

    public static void showProgress(SwingWorker<?, ?> worker, Component feedbackTarget) {
        if (worker.isDone()) {
            return;
        }

        new SwingWorkerProgressFeedback(worker, feedbackTarget, DEFAULT_MESSAGE_PROPERTY);
    }

    private SwingWorkerProgressFeedback(SwingWorker<?, ?> worker, Component feedbackTarget, String messageProperty) {
        this.worker = worker;
        this.feedbackTarget = feedbackTarget;
        this.originalCursor = feedbackTarget.getCursor();
        this.messageProperty = messageProperty;

        worker.addPropertyChangeListener(this);
        if (worker instanceof JGlossWorker) {
            message = ((JGlossWorker<?, ?>) worker).getMessage();
        }

        if (worker.getState() == STARTED) {
            workerStarted();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (STATE_PROPERTY.equals(event.getPropertyName())) {
            if (STARTED == event.getNewValue()) {
                workerStarted();
            } else if (DONE == event.getNewValue()) {
                workerDone();
            }
        } else if (PROGRESS_PROPERTY.equals(event.getPropertyName())) {
            updateProgress();
        } else if (messageProperty.equals(event.getPropertyName())) {
            updateMessage(String.valueOf(event.getNewValue()));
        }
    }

    private void updateMessage(String message) {
        this.message = message;
        if (progressDialog != null) {
            progressDialog.setMessage(message);
        }
    }

    private void updateProgress() {
        if (progressDialog != null) {
            progressDialog.setProgress(worker.getProgress());
        }
    }

    private void workerStarted() {
        feedbackTarget.setCursor(WAIT_CURSOR);
        dialogShowTimer = new Timer(DIALOG_DELAY_MS, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showProgressDialog();
            }

        });

        dialogShowTimer.setRepeats(false);
        dialogShowTimer.start();
    }

    private void showProgressDialog() {
        if (worker.isDone()) {
            return;
        }

        Window feedbackWindow = SwingUtilities.windowForComponent(feedbackTarget);
        Action cancelAction;
        if (worker instanceof Cancelable) {
            cancelAction = new CancelAction((Cancelable) worker);
        } else {
            cancelAction = null;
        }
        progressDialog = new ProgressDialog(feedbackWindow, cancelAction);
        progressDialog.setMessage(message);
        progressDialog.setProgress(worker.getProgress());
        progressDialog.setVisible(true);
    }

    private void workerDone() {
        worker.removePropertyChangeListener(this);
        feedbackTarget.setCursor(originalCursor);

        if (dialogShowTimer != null) {
            dialogShowTimer.stop();
        }
        if (progressDialog != null) {
            progressDialog.setVisible(false);
        }
    }
}
