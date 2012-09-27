/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import java.awt.EventQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;


/**
 * Lookup engine which performs lookups in its own thread.
 *
 * @author Michael Koch
 */
public class AsynchronousLookupEngine extends LookupEngine {

    private class LookupTask implements Runnable {

        private final LookupModel model;
        private final Runnable runAfterLookup;

        LookupTask(LookupModel model, Runnable runAfterLookup) {
            this.model = model;
            this.runAfterLookup = runAfterLookup;
        }

        @Override
        public void run() {
            try {
                doLookupSuper(model);
                if (runAfterLookup != null) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            runAfterLookup.run();
                        }
                    });
                }
            } catch (InterruptedException e) {
                // search task was cancelled by new task
                LOGGER.finest("search task was interrupted");
            }
        }

    }

    private static final Logger LOGGER = Logger.getLogger(AsynchronousLookupEngine.class.getPackage().getName());

    private final ExecutorService searchTaskExecutor = Executors.newSingleThreadExecutor();

    private Future<?> lookupTask;

    public AsynchronousLookupEngine( LookupResultHandler _handler) {
        this( _handler, Integer.MAX_VALUE);
    }

    public AsynchronousLookupEngine( LookupResultHandler _handler, int _dictionaryEntryLimit) {
        super(new EdtLookupResultHandler(_handler), _dictionaryEntryLimit);
    }

    @Override
	public void doLookup( LookupModel model) {
        doLookup( model, null);
    }

    /**
     * Initiate a new lookup, which will be performed in its own thread. The method will return
     * immediately. If another search is currently executing, it will be aborted.
     *
     * @param runAfterLookup if <code>!= null</code>, this runnable will be executed in the
     *        lookup thread after the search is completed. If the search is interrupted,
     *        the runnable will not be executed. It is possible to schedule a new lookup from
     *        the runnable.
     */
    public void doLookup(LookupModel model, Runnable runAfterLookup) {
        cancelLookupTask();
        lookupTask = searchTaskExecutor.submit(new LookupTask(model.clone(), runAfterLookup));
    }

    private void cancelLookupTask() {
        if (lookupTask != null) {
            lookupTask.cancel(true);
        }
    }

    private void doLookupSuper(LookupModel model) throws InterruptedException {
        super.doLookup(model);
    }

    public void dispose() {
        cancelLookupTask();
        searchTaskExecutor.shutdown();
    }

    @Override
	protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
} // class AsynchronousLookupEngine
