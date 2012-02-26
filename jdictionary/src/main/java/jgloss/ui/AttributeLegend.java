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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jgloss.JGloss;
import jgloss.dictionary.Dictionary;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.CategoryAttributeValue;
import jgloss.dictionary.attribute.SingletonValueList;
import jgloss.dictionary.attribute.ValueList;

/**
 * Displays a legend to the attributes used by a dictionary.
 *
 * @author Michael Koch
 */
public class AttributeLegend extends JPanel {
    private static final Comparator attributeComparator = new Comparator() {
            @Override
			public int compare( Object o1, Object o2) {
                return ((Attribute) o1).getDescription().compareToIgnoreCase
                    ( ((Attribute) o2).getDescription());
            }

            @Override
			public boolean equals( Object o) { return o == this; }
        };

    private static final Comparator categoryComparator = new Comparator() {
            @Override
			public int compare( Object o1, Object o2) {
                return ((CategoryAttributeValue) o1).getShortName().
                    compareToIgnoreCase( ((CategoryAttributeValue) o2).getShortName());
            }

            @Override
			public boolean equals( Object o) { return o == this; }            
        };

    private static final class DictionaryItem {
        private String dictionaryName;
        private String text;

        public DictionaryItem( String _dictionaryName, String _text) { 
            dictionaryName = _dictionaryName;
            text = _text;
        }
        
        @Override
		public String toString() { return dictionaryName; }
        public String getText() { return text; }
    } // class DictionaryItem

    private JComboBox dictionaryChoice;
    private JEditorPane legend;
    private JScrollPane legendScroller;

    public AttributeLegend() {
        setLayout( new BorderLayout());
        dictionaryChoice = new JComboBox();
        dictionaryChoice.setEditable( false);
        dictionaryChoice.addActionListener( new ActionListener() {
                @Override
				public void actionPerformed( ActionEvent e) {
                    showSelectedDictionary();
                }
            });

        JPanel p = new JPanel();
        p.add( new JLabel( JGloss.messages.getString( "legend.dictionarychoice")));
        p.add( dictionaryChoice);
        this.add( UIUtilities.createFlexiblePanel( p, true), BorderLayout.NORTH);
        
        legend = new JEditorPane();
        legend.setEditable( false);
        legend.setContentType( "text/html");
        legendScroller = new JScrollPane( legend, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( legendScroller, BorderLayout.CENTER);
    }

    public void setDictionaries( Dictionary[] dictionaries) {
        dictionaryChoice.removeAllItems();
        StringBuilder text = new StringBuilder();
        for ( int i=0; i<dictionaries.length; i++) {
            text.setLength( 0);
            text.append( "<html><head></head><body>");
            createLegend( dictionaries[i], text);
            text.append( "</body></html>");
            dictionaryChoice.addItem( new DictionaryItem( dictionaries[i].getName(),
                                                          text.toString()));
        }
    }

    protected void createLegend( Dictionary dic, StringBuilder buf) {
        buf.append( "<h2>");
        buf.append( JGloss.messages.getString( "legend.dictionary", new String[] { dic.getName() }));
        buf.append( "</h2>\n");

        SortedSet attributes = new TreeSet( attributeComparator);
        attributes.addAll( dic.getSupportedAttributes());
        boolean noAttributes = true;

        for ( Iterator ai=attributes.iterator(); ai.hasNext(); ) {
            Attribute att = (Attribute) ai.next();
            AttributeFormatter formatter = DictionaryEntryFormat.getAttributeFormatter( att);
            if (formatter == null)
                // attribute not displayed by UI, ignore
                continue;
            noAttributes = false;

            buf.append( "<p><b>");
            buf.append( JGloss.messages.getString( "legend.attribute",
                                                   new String[] { att.getName(), 
                                                                  att.getDescription() }));
            buf.append( "</b><br>\n");
            buf.append( JGloss.messages.getString( "legend.attributeexample"));
            if (att.canHaveValue()) {
                if (!att.alwaysHasValue()) {
                    DictionaryEntryFormat.getAttributeFormatter( att, true).format
                        ( att, (ValueList) null, buf);
                    buf.append( " / ");
                }
                formatter.format( att, new SingletonValueList( att.getExampleValue()), buf);
                createLegendForValue( dic, att, buf);
            }
            else
                formatter.format( att, (ValueList) null, buf);
            buf.append( "</p>\n");
        }

        if (noAttributes) {
            buf.append( "<p><i>");
            buf.append( JGloss.messages.getString( "legend.noattributes"));
            buf.append( "</i></p>");
        }
    }

    protected void createLegendForValue( Dictionary dic, Attribute att, StringBuilder buf) {
        // currently only CategoryAttributeValues are supported
        if (!CategoryAttributeValue.class.isAssignableFrom( att.getAttributeValueClass()))
            return;

        SortedSet values = new TreeSet( categoryComparator);
        values.addAll( dic.getAttributeValues( att));
        if (values.isEmpty())
            return;

        buf.append( "<br>\n");
        buf.append( JGloss.messages.getString( "legend.values"));
        buf.append( "\n<table border=\"0\">");
        for ( Iterator vi=values.iterator(); vi.hasNext(); ) {
            CategoryAttributeValue value = (CategoryAttributeValue) vi.next();
            buf.append( "<tr><td>");
            buf.append( value.getShortName());
            buf.append( "</td><td>");
            buf.append( value.getLongName());
            buf.append( "</tr>\n");
        }
        buf.append( "</table>\n");
    }

    protected void showSelectedDictionary() {
        DictionaryItem item = (DictionaryItem) dictionaryChoice.getSelectedItem();
        if (item == null)
            return;

        legend.setText( item.getText());
        legend.setCaretPosition( 0);
    }
} // class AttributeLegend
