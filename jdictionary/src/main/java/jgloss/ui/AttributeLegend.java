/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import jgloss.dictionary.attribute.AttributeValue;
import jgloss.dictionary.attribute.CategoryAttributeValue;
import jgloss.ui.util.UIUtilities;

/**
 * Displays a legend to the attributes used by a dictionary.
 *
 * @author Michael Koch
 */
public class AttributeLegend extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final Comparator<Attribute<?>> attributeComparator = new Comparator<Attribute<?>>() {
            @Override
			public int compare(Attribute<?> o1, Attribute<?> o2) {
                return o1.getDescription().compareToIgnoreCase
                    (o2.getDescription());
            }
        };

    private static final Comparator<CategoryAttributeValue> categoryComparator = new Comparator<CategoryAttributeValue>() {
            @Override
			public int compare(CategoryAttributeValue o1, CategoryAttributeValue o2) {
                return o1.getShortName().
                    compareToIgnoreCase( o2.getShortName());
            }
        };

    private static final class DictionaryItem {
        private final String dictionaryName;
        private final String text;

        public DictionaryItem( String _dictionaryName, String _text) { 
            dictionaryName = _dictionaryName;
            text = _text;
        }
        
        @Override
		public String toString() { return dictionaryName; }
        public String getText() { return text; }
    } // class DictionaryItem

    private final JComboBox dictionaryChoice;
    private final JEditorPane legend;

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
        p.add( new JLabel( JGloss.MESSAGES.getString( "legend.dictionarychoice")));
        p.add( dictionaryChoice);
        this.add( UIUtilities.createFlexiblePanel( p, true), BorderLayout.NORTH);
        
        legend = new JEditorPane();
        legend.setEditable( false);
        legend.setContentType( "text/html");
        JScrollPane legendScroller = new JScrollPane( legend, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add( legendScroller, BorderLayout.CENTER);
    }

    public void setDictionaries( Dictionary[] dictionaries) {
        dictionaryChoice.removeAllItems();
        StringBuilder text = new StringBuilder();
        for (Dictionary dictionarie : dictionaries) {
            text.setLength( 0);
            text.append( "<html><head></head><body>");
            createLegend( dictionarie, text);
            text.append( "</body></html>");
            dictionaryChoice.addItem( new DictionaryItem( dictionarie.getName(),
                                                          text.toString()));
        }
    }

	protected void createLegend( Dictionary dic, StringBuilder buf) {
        buf.append( "<h2>");
        buf.append( JGloss.MESSAGES.getString( "legend.dictionary", dic.getName()));
        buf.append( "</h2>\n");

        SortedSet<Attribute<?>> attributes = new TreeSet<Attribute<?>>( attributeComparator);
        attributes.addAll( dic.getSupportedAttributes());
        boolean noAttributes = true;

        for (Attribute<?> att : attributes) {
            AttributeFormatter formatter = DictionaryEntryFormat.getAttributeFormatter( att);
            if (formatter == null) {
	            // attribute not displayed by UI, ignore
                continue;
            }
            noAttributes = false;

            buf.append( "<p><b>");
            buf.append( JGloss.MESSAGES.getString( "legend.attribute",
                                                   att.getName(), att.getDescription() ));
            buf.append( "</b><br>\n");
            buf.append( JGloss.MESSAGES.getString( "legend.attributeexample"));
            if (att.canHaveValue()) {
                if (!att.alwaysHasValue()) {
                    DictionaryEntryFormat.getAttributeFormatter( att, true).format
                        ( att, (List<AttributeValue>) null, buf);
                    buf.append( " / ");
                }
                formatter.format( att, Collections.singletonList( att.getExampleValue()), buf);
                createLegendForValueIfSupported(dic, buf, att);
            } else {
	            formatter.format( att, (List<AttributeValue>) null, buf);
            }
            buf.append( "</p>\n");
        }

        if (noAttributes) {
            buf.append( "<p><i>");
            buf.append( JGloss.MESSAGES.getString( "legend.noattributes"));
            buf.append( "</i></p>");
        }
    }

	@SuppressWarnings("unchecked")
	private void createLegendForValueIfSupported(Dictionary dic,
			StringBuilder buf, Attribute<?> att) {
		if (CategoryAttributeValue.class.isAssignableFrom(att.getAttributeValueClass())) {
			createLegendForValue( dic, (Attribute<? extends CategoryAttributeValue>) att, buf);
		}
	}

    protected void createLegendForValue( Dictionary dic, Attribute<? extends CategoryAttributeValue> att, StringBuilder buf) {
        // currently only CategoryAttributeValues are supported
        if (!CategoryAttributeValue.class.isAssignableFrom( att.getAttributeValueClass())) {
	        return;
        }

        SortedSet<CategoryAttributeValue> values = new TreeSet<CategoryAttributeValue>( categoryComparator);
        values.addAll( dic.getAttributeValues( att));
        if (values.isEmpty()) {
	        return;
        }

        buf.append( "<br>\n");
        buf.append( JGloss.MESSAGES.getString( "legend.values"));
        buf.append( "\n<table border=\"0\">");
        for (CategoryAttributeValue value : values) {
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
        if (item == null) {
	        return;
        }

        legend.setText( item.getText());
        legend.setCaretPosition( 0);
    }
} // class AttributeLegend
