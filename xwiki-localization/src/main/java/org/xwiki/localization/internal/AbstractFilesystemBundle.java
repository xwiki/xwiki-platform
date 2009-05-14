/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.localization.internal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.xwiki.localization.Bundle;

/**
 * Base class for {@link Bundle}s using {@link ResourceBundle} resources. Provides methods for loading properties from
 * such resources.
 * 
 * @version $Id$
 */
public abstract class AbstractFilesystemBundle extends AbstractBundle implements Bundle
{
    /**
     * <p>
     * Cached properties corresponding to resources loaded from filesystem files.
     * </p>
     * <p>
     * Map: (filename -&gt; map: (language -&gt; bundle)).
     * </p>
     */
    protected Map<String, Map<String, Properties>> fileBundles = new HashMap<String, Map<String, Properties>>();

    /**
     * Gets the translations corresponding to a given {@link ResourceBundle resource bundle}. If these properties are
     * already cached, then use them. Otherwise, first load from the <code>ResourceBundle</code> and update the cache.
     * 
     * @param fileName The name of the resource bundle. See {@link ResourceBundle} for details about which is the name
     *            identifying a bundle.
     * @param language The 2-character code of the target language.
     * @return The corresponding properties, or an empty Properties object if the resource bundle does not exist.
     */
    protected Properties getFileBundle(String fileName, String language)
    {
        synchronized (this.fileBundles) {
            if (this.fileBundles.containsKey(fileName)) {
                if (this.fileBundles.get(fileName).containsKey(language)) {
                    return this.fileBundles.get(fileName).get(language);
                }
            } else {
                this.fileBundles.put(fileName, new HashMap<String, Properties>());
            }

            Properties result = loadFileBundle(fileName, language);
            this.fileBundles.get(fileName).put(language, result);
            return result;
        }
    }

    /**
     * Loads a {@link ResourceBundle} from the filesystem and extracts the values for the current language into a
     * {@link Properties} object.
     * 
     * @param fileName The name of the bundle to load. This is the base name of the bundle, without language suffixes
     *            and the <tt>.properties</tt> extension.
     * @param language The 2-character code of the language to load.
     * @return A {@link Properties} object with the loaded resources, or an empty {@link Properties} object if an error
     *         occurs, for example if the bundle does not exist.
     */
    protected Properties loadFileBundle(String fileName, String language)
    {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(fileName, new Locale(language));
            Properties props = new Properties();
            // Copy the bundle into the Properties object.
            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                props.setProperty(key, bundle.getString(key));
            }
            // ResourceBundles already have inherited properties included.
            return props;
        } catch (MissingResourceException ex) {
            getLogger().warn("Invalid file bundle used: [{0}]", fileName);
        }
        return new Properties();
    }
}
