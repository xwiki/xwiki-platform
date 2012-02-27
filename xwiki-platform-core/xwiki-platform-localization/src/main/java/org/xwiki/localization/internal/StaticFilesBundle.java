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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.Bundle;

/**
 * Bundle corresponding to global (at the farm level) localization resources. A Properties object is constructed for
 * each language, containing all the translations from the configured resource bundles. This saves the extra calls that
 * would be needed if each resource would be stored in its own Properties object.
 * 
 * @version $Id$
 */
@Component("staticResources")
public class StaticFilesBundle extends AbstractFilesystemBundle implements Bundle, Initializable
{
    /**
     * <p>
     * Cached bundles corresponding to all the resources globally registered in the module preferences. For each
     * language, only one {@link Properties} object is created, since this needs less memory than keeping one object per
     * resource, and saves the extra calls when searching for a translation, as this list is not supposed to change
     * after the startup. This trades an increased initial overhead for a better performance afterwards.
     * </p>
     * <p>
     * Map: (language -&gt; bundle).
     * </p>
     */
    private Map<String, Properties> bundles = new HashMap<String, Properties>();

    /** The list of global resource bundles to use. */
    private String[] staticBundles = new String[] {"ApplicationResources", "CoreResources"};

    /**
     * {@inheritDoc}
     * <p>
     * In case there is no static bundle configured by the component manager, then use the default bundle for this
     * purpose.
     * </p>
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Set the Bundle priority
        setPriority(400);
    }

    @Override
    public String getTranslation(String key, String language)
    {
        Properties props = getStaticBundle(language);
        if (props.containsKey(key)) {
            return props.getProperty(key);
        }
        return key;
    }

    /**
     * Retrieves the translations for a given language. If these properties are already cached, then use them.
     * Otherwise, first load from the filesystem resources and update the cache. The returned Properties object contain
     * the translations for all resources registered as localization bundles.
     * 
     * @param language The 2-character code of the requested language.
     * @return A {@link Properties} object with the static translations, in the requested language.
     */
    protected Properties getStaticBundle(String language)
    {
        synchronized (this.bundles) {
            if (this.bundles.containsKey(language)) {
                return this.bundles.get(language);
            }
            Properties props = loadStaticBundle(language);
            this.bundles.put(language, props);
            return props;
        }
    }

    /**
     * Constructs the {@link Properties} object corresponding to a given language, from all the individual resource
     * bundles registered as global localization bundles.
     * 
     * @param language The 2-character code of the requested language.
     * @return A {@link Properties} object with the static translations of the wiki, in the requested language.
     */
    protected Properties loadStaticBundle(String language)
    {
        Properties props = new Properties();
        for (String bundleName : this.staticBundles) {
            props.putAll(getFileBundle(bundleName, language));
        }
        return props;
    }
}
