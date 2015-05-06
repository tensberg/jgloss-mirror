/*
 * Copyright (C) 2002-2015 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class ExclusionResourceLoader {
    private static final Logger LOGGER = Logger.getLogger(ExclusionResourceLoader.class.getPackage().getName());

    private final ExclusionResource exclusionResource;

    private final Set<String> exclusions = new HashSet<String>();

    private long lastLoaded = 0;

    ExclusionResourceLoader(ExclusionResource exclusionResource) {
        this.exclusionResource = exclusionResource;
    }

    public Set<String> loadExclusions() {
        try {
            URLConnection connection = exclusionResource.getLocation().openConnection();
            if (needsReload(connection)) {
                loadExclusions(connection);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "failed to load exclusions from " + exclusionResource.getLocation(), e);
        }

        return Collections.unmodifiableSet(exclusions);
    }

    private boolean needsReload(URLConnection connection) {
        if (lastLoaded == 0) {
            return true;
        }

        long lastModified = connection.getLastModified();
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }

        return lastModified == 0 || lastModified > lastLoaded;
    }

    private void loadExclusions(URLConnection connection) {
        LOGGER.log(INFO, "loading exclusions from {0}", connection.getURL());

        lastLoaded = System.currentTimeMillis();
        String encoding = connection.getContentEncoding();
        if (encoding == null) {
            // TODO: detect charset?
            encoding = Charset.defaultCharset().name();
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding))) {
            exclusions.clear();
            String word;
            while ((word = in.readLine()) != null) {
                exclusions.add(word.trim());
            }
        } catch (IOException e) {
            LOGGER.log(WARNING, "failed to load exclusions from " + exclusionResource.getLocation(), e);
        }
        LOGGER.log(INFO, "loaded {0} exclusions from {1}", new Object[] { exclusions.size(), connection.getURL() });
    }

    public ExclusionResource getExclusionResource() {
        return exclusionResource;
    }
}