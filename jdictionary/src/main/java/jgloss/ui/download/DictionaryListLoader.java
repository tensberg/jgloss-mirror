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
 */

package jgloss.ui.download;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import jgloss.ui.download.schema.Dictionaries;
import jgloss.ui.util.JGlossWorker;

/**
 * Loads a dictionary list XML document from an URL.
 */
class DictionaryListLoader extends JGlossWorker<Dictionaries, Void> {

    private final URL dictionariesUrl;

    DictionaryListLoader(URL dictionariesUrl) {
        this.dictionariesUrl = dictionariesUrl;
    }
    
    @Override
    protected Dictionaries doInBackground() throws Exception {
        return loadDictionaries();
    }
    
    Dictionaries loadDictionaries() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Dictionaries.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Dictionaries dictionaries = (Dictionaries) unmarshaller.unmarshal(dictionariesUrl);

        return dictionaries;
    }

}
