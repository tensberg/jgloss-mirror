/*
 * Copyright (C) 2002,2003 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

import jgloss.util.CharacterEncodingDetector;

/**
 * Wrapper around the ChaSen morphological analysis program. Each instance starts the
 * ChaSen program with customizable parameters. The output format can thus be customized.
 * Information about ChaSen can be found at
 * <a href="http://chasen.aist-nara.ac.jp/">http://chasen.aist-nara.ac.jp/</a>.
 *
 * @author Michael Koch
 */
public class Chasen {
    public static void main( String args[]) throws Exception {
        System.err.println( jgloss.util.StringTools.unicodeEscape( args[0].charAt( 0)));
        System.err.println( Character.UnicodeBlock.of( args[0].charAt( 0)));
        System.exit( 0);

        Chasen c = new Chasen( "/usr/local/bin/chasen", "", '\t');
        Result r = c.parse( args[0].toCharArray(), 0, args[0].length());
        while (r.hasNext()) {
            System.err.println( r.next());
        }
        c.dispose();
    }

    /**
     * End of input line marker.
     */
    public static final String EOS = "EOS";
    /**
     * End of path marker. This is used by Chasen if all paths are output.
     */
    public static final String EOP = "EOP";

    /**
     * Result of the parsing of some text using the Chasen instance of the class.
     * Instances of this class are returned by {@link Chasen#parse(char[],int,int) Chasen.parse}.
     */
    public class Result {
        /**
         * The next line of the chasen output. This line will be parsed by the next invocation of
         * {@link #next() next}.
         */
        private String nextLine;
        /**
         * Contains the parsed result of a line of output of chasen. Returned by a call to 
         * {@link #next() next}.
         */
        private List nextBuffer = new ArrayList( 10);
        /**
         * EOS lines expected from Chasen process for this iteration. The last EOS signals the end
         * of the Chasen result for the current parse.
         */
        private int expectedEOS;

        protected Result() {}

        /**
         * Prepares the result object for a new result iteration.
         *
         * @param expectedEOS EOS lines expected from Chasen process.
         */
        protected void init( int expectedEOS) throws IOException {
            this.expectedEOS = expectedEOS;
            prefetchNextLine();
        }

        /**
         * Returns <code>true</code> if there is an additional result line returned by the
         * chasen process. Returns <code>false</code> if there is no next result line, either
         * because all chasen output has been read, or because an IO error occurred.
         */
        public boolean hasNext() {
            return nextLine != null;
        }

        /**
         * Returns the next result line returned by the chasen process. The object is either
         * {@link #EOS EOS}, {@link #EOP EOP}, or a <code>List</code> of <code>Strings</code>
         * with the result fields. The list is reused across multiple calls to <code>next</code>;
         * if you want to keep the results, copy the list.
         *
         * @exception NoSuchElementException if there is no next result line.
         */
        public Object next() throws NoSuchElementException {
            if (nextLine == null)
                throw new NoSuchElementException();

            String currentLine = nextLine; // line returned by this call to next()
            // read line for next call to next() now
            // caching the next line now guarantees that the current call to next() will always
            // succeed, and any error when reading the next line will be visible with hasNext()
            prefetchNextLine();

            nextBuffer.clear();

            // handle special markers
            if (currentLine.equals( EOS))
                return EOS;
            else if (currentLine.equals( EOP))
                return EOP;

            if (separator != '\0') {
                int from = 0;
                do {
                    // split result line at user-defined separator
                    int to = currentLine.indexOf( separator, from);
                    if (to == -1) // last sub-segment of result line
                        to = currentLine.length();
                    nextBuffer.add( currentLine.substring( from, to));
                    from = to + 1;
                } while (from < currentLine.length());
            }
            else
                nextBuffer.add( currentLine);

            return nextBuffer;
        }

        /**
         * Store the next line of Chasen output in the variable {@link #nextLine nextLine}.
         */
        private void prefetchNextLine() {
            if (expectedEOS == 0) {
                // no more output expected
                nextLine = null;
                try {
                    if (chasenOut.ready()) {
                        System.err.println( "Chasen.java WARNING: unexpected chasen process output");
                        discard();
                    }
                } catch (IOException ex) {}
            }
            else { 
                try {
                    nextLine = chasenOut.readLine();
                    if (EOS.equals( nextLine))
                        expectedEOS--;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    nextLine = null;
                    expectedEOS = 0;
                }
            }
        }

        /**
         * Discard all remaining result lines.
         */
        public void discard() {
            char[] discardBuf = new char[4096];
            try {
                // read all available output from chasen process and discard it
                while (chasenOut.read( discardBuf) > 0)
                    ;
            } catch (IOException ex) {
                // ignore errors when discarding
                ex.printStackTrace();
            }
            expectedEOS = 0;
            nextLine = null;
        }

    }

    /**
     * Path to the default chasen executable. This will be used if the path is not supplied in
     * the constructor.
     */
    private static String defaultChasenExecutable = "/usr/local/bin/chasen";

    /**
     * Cache used by {@link #isChasenExecutable(String) isChasenExecutable} to store the
     * name of the last succesfully tested chasen executable. 
     */
    private static String lastChasenExecutable = null;

    /**
     * Name of the character encoding ChaSen uses on this computer.
     */
    private static String platformEncoding;
    /**
     * Separator for fields in the chasen output.
     */
    private char separator;
    /**
     * Chasen process used to parse the text.
     */
    private Process chasen;
    /**
     * Reader for stdout of chasen process.
     */
    private BufferedReader chasenOut;
    /**
     * Reader for stdin of chasen process.
     */
    private BufferedWriter chasenIn;
    /**
     * Result iterator for parsing of some text.
     */
    private Result result;

    /**
     * Sets the path to the default ChaSen executable. This executable will be used when the
     * executable path is not passed in with the constructor.
     */
    public static void setDefaultExecutable( String chasenExecutable) {
        defaultChasenExecutable = chasenExecutable;
    }

    /**
     * Returns the path to the default ChaSen executable.
     */
    public static String getDefaultExecutable() {
        return defaultChasenExecutable;
    }

    /**
     * Test if the chasen program is available at the specified path. If the path to the executable
     * is the same as in the previous test, and this test was successfull, the test will not be
     * repeated.
     *
     * @param chasenExecutable Full path to the chasen executable.
     */
    public static boolean isChasenExecutable( String chasenExecutable) {
        // If the last call to isChasenExecutable was successful, the name of the
        // executable is stored in "lastChasenExecutable".
        if (lastChasenExecutable != null &&
            lastChasenExecutable.equals( chasenExecutable))
            return true;

        // The test is done by
        // calling the program with the "-V" (for version) option.
        try {
            Process p = Runtime.getRuntime().exec( chasenExecutable + " -V");

            try {
                waitForProcess( p, 3000l);
                // analyze process output
                if (p.exitValue() != 0)
                    return false;
                BufferedReader out = new BufferedReader
                    ( new InputStreamReader( p.getInputStream()));
                String line = out.readLine();
                out.close();
                if (line==null || !line.startsWith( "ChaSen"))
                    return false;

                lastChasenExecutable = chasenExecutable;
                return true;
            } catch (InterruptedException ex) {
                // process didn't terminate normally in time, abort
                System.err.println( "isChasenExecutable: abnormal process termination");
                p.destroy();
                return false;
            }
        } catch (IOException ex) {
            // specified program probably doesn't exist
            return false;
        }
    }

    /**
     * Starts a new chasen process using the default executable, no arguments and '\t' as field
     * separator.
     */
    public Chasen() throws IOException {
        this( defaultChasenExecutable, "", '\t');
    }

    /**
     * Starts a new chasen process with the specified parameters, using the default executable.
     */
    public Chasen( String args, char separator) throws IOException {
        this( defaultChasenExecutable, args, separator);
    }

    /**
     * Starts a new chasen process with the specified parameters.
     *
     * @param executable Path to the chasen executable program.
     * @param args Parameters passed to chasen. This can be used to customize the output format. Currently,
     *             this implementation does not work with the "-j" (japanese sentence mode) flag.
     * @param separator Separator char used in the format string to separate entry fields. If the
     *                  separator char is set to '\0', the list returned by 
     *                  {@link Chasen.Result#next() next} will contain the complete result line as
     *                  only entry.
     * @exception IOException If the chasen process can't be started.
     */
    public Chasen( String chasenExecutable, String args, char separator) throws IOException {
        // Initialize platform encoding if not done already. This avoids running chasen
        // twice at the same time.
        this.separator = separator;
        String encoding = getChasenPlatformEncoding( chasenExecutable);
        chasen = Runtime.getRuntime().exec( chasenExecutable + " " + args,
                                            new String[] { "LANG=ja", "LC_ALL=ja_JP" });
        chasenOut = new BufferedReader( new InputStreamReader
            ( chasen.getInputStream(), encoding));
        chasenIn = new BufferedWriter( new OutputStreamWriter
            ( chasen.getOutputStream(), encoding));
    }

    /**
     * Parse some text using the chasen process of this instance. A result iterator is returned, which
     * lazily parses the result lines when its {@link Chasen.Result#next() next} method is called.
     * Iterating over the result set is not thread safe, if several threads use the same Chasen instance
     * for parsing, proper synchronization must be done:
     * <code>
     *    Chasen chasen = new Chasen(...);
     *    ...
     *    synchronized (chasen) {
     *        Chasen.Result result = chasen.parse( text);
     *        // iterate over result
     *        while (result.hasNext())
     *           ...
     *    }
     * </code>
     *
     * @param text Text to parse. The array will be modified.
     * @return Result iterator.
     * @exception IOException if communication with the chasen process failed.
     */
    public Chasen.Result parse( char[] text, int start, int length) throws IOException {
        if (result == null)
            result = new Result();

        // clear previous chasen output
        if (result.hasNext())
            result.discard();

        // Chasen sends an EOS for every 0x0a encountered in the input. In order to determine when
        // all data is parsed, the EOS we expect to see are counted.
        int expectedEOS = 0;

        for ( int i=start; i<start+length; i++) {
            // replace Dos or Mac line ends with unix line ends to make sure EOS is
            // treated correctly
            if (text[i] == 0x0d)
                text[i] = 0x0a;
            // Chasen will return a EOS for every 0x0a
            if (text[i] == 0x0a)
                expectedEOS++;
        }

        chasenIn.write( text, start, length);
        chasenIn.write( (char) 0x0a); // chasen will start parsing when end of line is encountered
        expectedEOS++; // for the previous 0x0a
        chasenIn.flush();
        
        result.init( expectedEOS);

        return result;
    }

    /**
     * Terminates the chasen process.
     */
    public void dispose() {
        if (chasen != null) {
            // terminate chasen process by writing EOT on its input stream
            try {
                chasenIn.flush(); // should be empty
                chasen.getOutputStream().close(); // this should terminate the executable
                // read remaining input
                byte[] buf = new byte[512];
                while (chasen.getInputStream().available() > 0)
                    chasen.getInputStream().read( buf);
                while (chasen.getErrorStream().available() > 0)
                    chasen.getErrorStream().read( buf);

                try {
                    waitForProcess( chasen, 5000l);
                } catch (InterruptedException ex) {
                    // chasen process did not terminate automatically
                    System.err.println( "Chasen.dispose: abnormal termination of chasen");
                    chasen.destroy();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Test which character encoding ChaSen uses for its input and output streams. On
     * Linux this will probably be EUC-JP and Shift-JIS on Windows. 
     * The test is only done the first time the method is called, the result is
     * cached and reused on further calls.
     *
     * @return Canonical name of the encoding, or <CODE>null</CODE> if the test failed.
     */
    protected String getChasenPlatformEncoding( String chasenExecutable) {
        if (platformEncoding != null)
            // return cached result
            return platformEncoding;

        // The test is done by running
        // chasen with the -lf option, which makes it list the conjugation forms,
        // and checking the encoding of the output.

        try {
            Process chasen = Runtime.getRuntime().exec( chasenExecutable + " -lf");
            InputStreamReader reader = CharacterEncodingDetector.getReader( chasen.getInputStream());
            platformEncoding = reader.getEncoding();

            // skip all input lines
            char[] buf = new char[512];
            while (reader.ready())
                reader.read(buf);
            try {
                waitForProcess( chasen, 5000l);
            } catch (InterruptedException ex) {
                // chasen process did not terminate automatically
                System.err.println( "Chasen.dispose: abnormal termination of chasen");
                chasen.destroy();
            }
            reader.close();
            chasen.getInputStream().close();
            chasen.getErrorStream().close();
            return platformEncoding;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Wait for a process to terminate. When the method returns normally, the process has terminated.
     * If the process does not terminate in time, the method throws an 
     * <code>InterruptedException</code>.
     */
    private final static void waitForProcess( Process process, long time) throws InterruptedException {
        Timer timer = new Timer();
        final Thread currentThread = Thread.currentThread();
        timer.schedule( new TimerTask() {
                public void run() {
                    // if the run method is called, the started process didn't finish in time
                    currentThread.interrupt();
                }
            }, time);
        
        process.waitFor(); // will be interrupted by timer if needed
        timer.cancel(); // normal termination of process, no interruption needed
        // clear the interrupted flag if it has been set between the last two calls
        Thread.interrupted();
    }

    /**
     * Terminate Chasen process if still running.
     */
    protected void finalize() {
        dispose();
    }
} // class Chasen
