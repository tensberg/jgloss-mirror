/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

import java.util.logging.Logger;

/**
 * Lookup engine which performs lookups in its own thread.
 *
 * @author Michael Koch
 */
public class AsynchronousLookupEngine extends LookupEngine {
    private class SearchThread extends Thread {
        private final Object THREAD_LOCK = new Object();
        private final Object LOOKUP_LOCK = new Object();
        private boolean terminateThread = false;
        private boolean inLookup = false;
        
        private LookupModel model;
        private Runnable runAfterLookup;

        public SearchThread() {
            super( "lookup engine search thread");
            setDaemon( true);
        }

        @Override
		public void run() {
            synchronized (THREAD_LOCK) {
                while (!terminateThread) {
	                try {
	                    if (model == null) {
	                        THREAD_LOCK.wait();
	                    }

	                    if (terminateThread) {
	                        break;
	                    }
	                    
	                    synchronized (LOOKUP_LOCK) {
	                        inLookup = true;
	                        // clear lingering interrupted flag
	                        Thread.interrupted();
	                    }
	                    doLookupSuper( model);
	                    synchronized (LOOKUP_LOCK) {
	                        inLookup = false;
	                        model = null;
	                        // clear lingering interrupted flag
	                        Thread.interrupted();
	                    }

	                    if (runAfterLookup != null) {
	                        runAfterLookup.run();
	                    }
	                } catch (InterruptedException ex) {}
                }
            }
        }

        public void newSearch( LookupModel _model, Runnable _runAfterLookup) {
            // abort current search (if any)
            synchronized (LOOKUP_LOCK) {
                if (inLookup) {
                    SearchThread.this.interrupt();
                }
            }
            // start new search
            synchronized (THREAD_LOCK) {
                model = _model;
                runAfterLookup = _runAfterLookup;
                THREAD_LOCK.notify();
            }
        }

        public void dispose() {
            terminateThread = true;
            SearchThread.this.interrupt();
            try {
                SearchThread.this.join( 3000);
            } catch (InterruptedException ex) {}
            if (SearchThread.this.isAlive()) {
	            LOGGER.warning( "WARNING: LookupFrame search thread still alive");
            }
            model = null;
            runAfterLookup = null;
        }
    } // class SearchThread

    private static final Logger LOGGER = Logger.getLogger(AsynchronousLookupEngine.class.getPackage().getName());
    
    private SearchThread searchThread;

    public AsynchronousLookupEngine( LookupResultHandler _handler) {
        this( _handler, Integer.MAX_VALUE);
    }

    public AsynchronousLookupEngine( LookupResultHandler _handler, int _dictionaryEntryLimit) {
        super( _handler, _dictionaryEntryLimit);
        searchThread = new SearchThread();
        searchThread.start();
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
    public void doLookup( LookupModel model, Runnable runAfterLookup) {
        searchThread.newSearch( model, runAfterLookup);
    }

    private void doLookupSuper( LookupModel model) throws InterruptedException {
        super.doLookup( model);
    }
    
    public void dispose() {
        if (searchThread != null) {
	        searchThread.dispose();
        }
        searchThread = null;
    }

    @Override
	protected void finalize() {
        dispose();
    }
} // class AsynchronousLookupEngine
