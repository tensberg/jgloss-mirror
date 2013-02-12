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

package jgloss.util;

import java.io.IOException;
import java.util.Properties;

/**
 * Loader for properties defined in {@code jdictionary.properties}.
 */
public class ConfigurationProperties {
    /**
     * Singleton instance of the configuration properties.
     */
    public static ConfigurationProperties CONFIGURATION = new ConfigurationProperties();

    private static final String CONFIGURATION_NAME = "/jdictionary.properties";

    private final Properties properties = new Properties();

    private ConfigurationProperties() {
        try {
            properties.load(ConfigurationProperties.class.getResourceAsStream(CONFIGURATION_NAME));
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * @return Configuration property value for the given key.
     * @throws IllegalArgumentException
     *             if the given key does not exist in the configuration file.
     */
    public String getProperty(String key) {
        String value = properties.getProperty(key);

        if (value == null) {
            throw new IllegalArgumentException("no configuration defined for key " + key);
        }

        return value;
    }
}
