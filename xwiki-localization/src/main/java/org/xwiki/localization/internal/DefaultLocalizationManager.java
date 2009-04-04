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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.phase.LogEnabled;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.WikiInformation;

/**
 * Default implementation of the {@link LocalizationManager} component.
 * 
 * @version $Id$
 */
public class DefaultLocalizationManager extends AbstractLogEnabled implements LocalizationManager, Initializable,
    LogEnabled, Composable
{
    /** Provides access to wiki localization information. Injected by the Component Manager. */
    private WikiInformation wikiInfo;

    /** Provides access to different bundles based on their hint. Needed in {@link #use(String, String)} */
    private ComponentManager componentManager;

    /** The list of {@link Bundle}s to use. Injected by the component manager. */
    private List<Bundle> bundles;

    /**
     * {@inheritDoc}
     * 
     * @see LocalizationManager#get(String)
     */
    public String get(String key)
    {
        return get(key, this.wikiInfo.getContextLanguage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see LocalizationManager#get(String, String)
     */
    public String get(String key, String language)
    {
        String result = key;
        for (Bundle bundle : this.bundles) {
            result = bundle.getTranslation(key, language);
            if (!result.equals(key)) {
                break;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see LocalizationManager#get(String, List)
     */
    public String get(String key, List< ? > params)
    {
        return get(key, params, this.wikiInfo.getContextLanguage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see LocalizationManager#get(String, List, String)
     */
    public String get(String key, List< ? > params, String language)
    {
        String translation = get(key, language);
        if (params != null) {
            translation = MessageFormat.format(translation, params.toArray());
        }
        return translation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see LocalizationManager#use(String, String)
     */
    public void use(String bundleTypeHint, String bundleLocation)
    {
        try {
            Bundle bundle = (Bundle) this.componentManager.lookup(Bundle.class, bundleTypeHint);
            bundle.use(bundleLocation);
        } catch (ComponentLookupException e) {
            getLogger().warn("Unknown bundle type: {0}", bundleTypeHint);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        Collections.sort(this.bundles);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }
}
