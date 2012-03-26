/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.parser;

import static java.util.logging.Level.SEVERE;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import jgloss.util.UTF8ResourceBundleControl;

/**
 * Find verb/adjective conjugations. This is done by searching through a tree with a
 * inflection/dictionary form mapping.
 * The file "vconj", which stores the inflection/dictionary form mapping is taken from
 * Jim Breen's XJDIC.
 *
 * @author Michael Koch
 */
public class Conjugation {
	private static final Logger LOGGER = Logger.getLogger(Conjugation.class.getPackage().getName());
	
    /**
     * Location of the file with the conjugation definition.
     */
    private final static String VCONJ_RESOURCE = "/data/vconj";

    /**
     * Prefix used when constructing keys for resource lookups.
     */
    private final static String RESOURCE_PREFIX = "vconj.";

    /**
     * Localizable message resource.
     */
    private final static ResourceBundle messages = 
        ResourceBundle.getBundle( "messages-parser", new UTF8ResourceBundleControl());

    /**
     * Ending of the conjugated verb/adjective.
     */
    private final String conjugatedForm;
    /**
     * Dictionary form of the verb/adjective ending.
     */
    private final String dictionaryForm;
    /**
     * A description of the grammatical type of the conjugation.
     */
    private final String type;

    /**
     * Dumps the generated tree to System.out.
     *
     * @param args Program arguments. Not used.
     */
    public static void main( String args[]) {
        dump();
    }

    /**
     * Node in a tree of conjugations. Conjugations are stored in a tree for fast lookup.
     * Each edge in the tree is labeled with the substring of the inflecion it has in common
     * with the other siblings. The path from the root to a node thus reads the inflected form
     * of a conjugation.
     */
    private static class Node {
        /**
         * Edge label in the tree. Contains a substring of an inflection.
         */
        public String edge;
        /**
         * Conjugations which inflected form labels the edges from the root to this node.
         * May be <CODE>null</CODE> if the path to this node does not describe an inflection
         * but merely a split in the tree.
         */
        public Conjugation[] conjugations;
        /**
         * Children of this node. The labels from the root node to this node are a prefix of
         * the inflections of any child.
         */
        public Node[] children;

        /**
         * Creates a new node.
         *
         * @param edge Edge label. The labels from the root plus this label form a prefix of
         *             an inflected form.
         * @param conjugation Conjugation which this node models. May be <CODE>null</CODE> if
         *                    this node only describes a split in the tree.
         * @param children Children of this node. The labels from the root node to this node are a prefix of
         *                 the inflections of any child.
         */
        public Node( String edge, Conjugation conjugation, Node[] children) {
            this.edge = edge;
            if (conjugation != null) {
	            this.conjugations = new Conjugation[] { conjugation };
            } else {
	            this.conjugations = null;
            }
            this.children = children;
        }
    }

    /**
     * Map of all created conjugations.
     */
    private static Map<String, Conjugation> conjugations = new HashMap<String, Conjugation>( 501);

    /**
     * The root inflection node. Does not describe a conjugation by itself.
     */
    private static Node root = null;

    /**
     * Creates a new conjugation.
     *
     * @param conjugatedForm Ending of the conjugated verb/adjective.
     * @param dictionaryForm Dictionary form of the verb/adjective ending.
     * @param type A description of the grammatical type of this conjugation.
     */
    private Conjugation( String conjugatedForm, String dictionaryForm, String type) {
        this.conjugatedForm = conjugatedForm;
        this.dictionaryForm = dictionaryForm;
        this.type = type;
    }

    /**
     * Returns a conjugation with the given information. If an identical conjugation has already
     * been created, the object will be reused.
     */
    public static Conjugation getConjugation( String conjugatedForm, String dictionaryForm,
                                              String type) {
        String key = conjugatedForm + ":" + dictionaryForm + ":" + type;
        Conjugation c = conjugations.get( key);
        if (c == null) {
            c = new Conjugation( conjugatedForm, dictionaryForm, type);
            conjugations.put( key, c);
        }

        return c;
    }

    /**
     * Search for possible conjugation at the beginning of the string.
     *
     * @param hiragana String of hiragana characters which possibly begin with an inflected
     *                 verb form.
     * @return An array of conjugations whose inflected form is a substring of the input
     *         string, ordered from longest to shortest match. Each dictionary form will only
     *         appear once.
     */
    public static Conjugation[] findConjugations( String hiragana) {
        if (root == null) {
	        loadConjugations();
        }

        Node n = root;
        Conjugation c[] = null;
        boolean stop = false;
       
        while (!stop) {
            stop = true; // will be set false if matching child is found
            for ( int i=0; i<n.children.length; i++) {
                if (hiragana.startsWith( n.children[i].edge)) {
                    n = n.children[i];
                    hiragana = hiragana.substring( n.edge.length());
                    if (n.conjugations != null) {
	                    c = n.conjugations;
                    }
                    stop = false;
                    break;
                }
            }
        }

        return c;
    }

    /**
     * Returns the ending of the conjugated verb/adjective.
     *
     * @return The inflected form.
     */
    public String getConjugatedForm() { return conjugatedForm; }
    /**
     * Returns the dictionary form of the verb/adjective ending.
     *
     * @return The dictionary form.
     */
    public String getDictionaryForm() { return dictionaryForm; }
    /**
     * Returns a description of the grammatical type of this conjugation. The desciption
     * is localized using the default locale.
     *
     * @return a description.
     */
    public String getType() { return type; }

    /**
     * Creates the tree of conjugations.
     */
    private static synchronized void loadConjugations() {
        if (root != null) {
	        return;
        }

        try {
            root = new Node( "", null, new Node[0]);

            LineNumberReader in = new LineNumberReader( new InputStreamReader
                (Conjugation.class.getResourceAsStream( VCONJ_RESOURCE), "EUC-JP"));
            String line = in.readLine();

            // the vconj files begin with a mapping of abbreviations to longer type
            // descriptions, which will be stored in labels
            Map<String, String> labels = new HashMap<String, String>();

            int mode = 0; // read labels
            while ((line=in.readLine()) != null) {
                if (line.charAt( 0) == '#') {
	                continue;
                }
                if (line.charAt( 0) == '$') { // start of conjugation part
                    mode = 1;
                    continue;
                }
                line = line.trim();
                if (mode == 0) { // read labels
                    int i = line.indexOf( '\t');
                    if (i != -1) {
                        String label = line.substring( 0, i).trim();
                        try {
                            labels.put( label, messages.getString( RESOURCE_PREFIX + label));
                        } catch (MissingResourceException ex) {
                            LOGGER.warning( "vconj: missing resource for description " + 
                                                RESOURCE_PREFIX + label);
                            labels.put( label, line.substring( i+1).trim());
                        }
                    }
                }
                else { // read conjugations
                    int i = line.indexOf( '\t');
                    if (i == -1) {
	                    continue;
                    }
                    String c = line.substring( 0, i); // conjugated form
                    line = line.substring( i+1);
                    i = line.indexOf( '\t');
                    if (i == -1) {
	                    continue;
                    }
                    String d = line.substring( 0, i); // dictionary form
                    String l = line.substring( i+1); // label
                    addConjugation( c, d, labels.get( l.trim()));
                }
            }
            
            propagateConjugations( root, new Conjugation[0]);
        } catch (IOException ex) {
            LOGGER.log(SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Add a conjugation node to the tree. This method will incrementally build the
     * tree while loading the conjugation definition file.
     *
     * @param conjugatedForm Inflected form of this conjugation.
     * @param dictionaryForm Dictionary form of this conjugation.
     * @param type Type description of this conjugation.
     */
    private static void addConjugation( String conjugatedForm, String dictionaryForm, String type) {
        Node n = root;
        Node pn = null; // parent of n
        boolean stop = false;
        int p = 0; // position in conjugatedForm
        Conjugation c = getConjugation( conjugatedForm, dictionaryForm, type);

        while (!stop) {
            boolean childFound = false;
            for ( int i=0; i<n.children.length; i++) {
                // find a common substring between the edge of a child of n and the conjugatedForm
                if (n.children[i].edge.charAt( 0) == conjugatedForm.charAt( p)) {
                    // follow this path
                    childFound = true;
                    pn = n;
                    n = n.children[i];
                    int j = 1;
                    // find longest match between inflected forms of new node and child not
                    // already on path from root
                    while (j<n.edge.length() && j+p<conjugatedForm.length() &&
                           n.edge.charAt(j)==conjugatedForm.charAt(p+j)) {
	                    j++;
                    }

                    if (j == n.edge.length()) {
                        if (p+j == conjugatedForm.length()) {
                            // new conjugation and n have identical conjugated form
                            if (n.conjugations != null) {
                                Conjugation[] conjugations = new Conjugation[n.conjugations.length+1];
                                System.arraycopy( n.conjugations, 0, conjugations, 0,
                                                  n.conjugations.length);
                                conjugations[conjugations.length-1] = c;
                                n.conjugations = conjugations;
                            } else {
	                            n.conjugations = new Conjugation[] { c };
                            }
                            stop = true;
                        }
                        else {
                            // new conjugation will be successor of n
                            p += j;
                        }
                    }
                    else if (p+j == conjugatedForm.length()) {
                        // insert new conjugation between pn and n
                        Node nn = new Node( conjugatedForm.substring( p), c, 
                                            new Node[] { n });
                        pn.children[i] = nn;
                        n.edge = n.edge.substring( j);
                        stop = true;
                    }
                    else {
                        // n.edge and new conjugation have common prefix
                        // split path between pn and n where new conjugation and n.edge diverge
                        Node nn = new Node( conjugatedForm.substring( p+j), c, new Node[0]);
                        n.edge = n.edge.substring( j);
                        Node nn2 = new Node( conjugatedForm.substring( p, p+j), null,
                                             new Node[] { nn, n});
                        pn.children[i] = nn2;
                        stop = true;
                    }
                    break; // matching child found, stop iteration
                }
            }

            if (!childFound) {
                // add new conjugation as child to this node
                Node nn = new Node( conjugatedForm.substring( p), c, new Node[0]);
                Node[] children = new Node[n.children.length+1];
                System.arraycopy( n.children, 0, children, 0, n.children.length);
                children[children.length-1] = nn;
                n.children = children;
                stop = true;
            }
        }
    }

    /**
     * Propagate conjugations for inflections to their children. After calling this method for
     * the root node, each node will contain all conjugations which have an inflected form which is
     * a prefix of the inflected form of the node and with a different dictionary form,
     * ordered from longest to shortest length.
     *
     * @param n Node for which the conjugations should be propagated.
     * @param c Conjugations already collected on the path from root downwards.
     */
    private static void propagateConjugations( Node n, Conjugation[] c) {
        if (n.conjugations != null) {
            // build a list of conjugation in this node plus all conjugations in ancestor nodes
            // which have a different dictionary form
            List<Conjugation> conjugations = new ArrayList<Conjugation>( c.length + n.conjugations.length);
            for ( int i=0; i<n.conjugations.length; i++) {
	            conjugations.add( n.conjugations[i]);
            }
                
            for ( int i=0; i<c.length; i++) {
                boolean add = true;
                for ( int j=0; j<n.conjugations.length; j++) {
                    // conjugation of parents is only added if there is not already a child with
                    // same dictionary form and longer inflected form
                    if (n.conjugations[j].dictionaryForm.equals( c[i].dictionaryForm)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
	                conjugations.add( c[i]);
                }
            }

            // store new list in this node
            if (conjugations.size() != n.conjugations.length) {
	            n.conjugations = conjugations.toArray( n.conjugations);
            }
            c = n.conjugations; // propagate conjugations of this node
        }
        
        for ( int i=0; i<n.children.length; i++) {
	        propagateConjugations( n.children[i], c);
        }
    }

    /**
     * Dumps the generated tree on the logger.
     */
    public static void dump() {
        loadConjugations();
        
        LOGGER.info( "Printing conjugations");
        dump( root, "");
    }
    
    /**
     * Dumps the node and all of its descendants on the logger.
     *
     * @param n The node to dump.
     * @param path Edge labels from root to this child.
     * @return Set of conjugations already seen.
     */
    private static Set<String> dump( Node n, String path) {
        Set<String> out = new HashSet<String>();
        if (n.conjugations != null) {
            for ( int i=0; i<n.conjugations.length; i++) {
                LOGGER.info( "/" + n.conjugations[i].getConjugatedForm() + " " +
                                  n.conjugations[i].getDictionaryForm() + " " +
                                  n.conjugations[i].getType());
                out.add( n.conjugations[i].getDictionaryForm());
            }
        }
        for ( int i=0; i<n.children.length; i++) {
            Set<String> s = dump( n.children[i], path + n.children[i].edge);
            if (n.conjugations != null) {
                // check if any of the descendants of this node have a dictionary form
                // different from the ones in this node
                for ( int j=0; j<n.conjugations.length; j++) {
                    if (!s.contains( n.conjugations[j].getDictionaryForm())) {
	                    LOGGER.warning( "different dictionary forms");
                    }
                    out.addAll( s);
                }
            }
        }
        
        return out;
    }
} // class Conjugation
