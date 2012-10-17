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
package org.xwiki.localization.internal.xwikipreferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.Translation;
import org.xwiki.localization.internal.AbstractBundle;
import org.xwiki.localization.internal.AbstractWikiBundle;
import org.xwiki.localization.internal.WikiInformation;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Bundle corresponding to global (at the wiki level) localization documents.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component(XWikiPreferencesBundle.ID)
public class XWikiPreferencesBundle extends AbstractBundle implements Initializable
{
    protected final static String ID = "XWikiPreferences";
    
    @Inject
    private ModelContext modelContext;

    @Inject
    private CacheManager cacheManager;

    private Cache<Bundle> bundlesCache;

    public XWikiPreferencesBundle()
    {
        super(ID, 300);
    }
    
    @Override
    public void initialize() throws InitializationException
    {
        // Setup cache
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("localization.XWikiPreferences");

        this.bundlesCache = this.cacheManager.createNewCache(cacheConfiguration);
    }


    @Override
    public int getPriority()
    {
        return 300;
    }

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private Bundle getBundle()
    {
        String currentWiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();

        return getBundle(currentWiki);
    }

    private Bundle getBundle(String wiki)
    {
        Bundle bundle = this.bundlesCache.get(wiki);
        if (bundle == null) {
            synchronized (this.bundlesCache) {
                bundle = this.bundlesCache.get(wiki);
                if (bundle == null) {
                    bundle = createWikiBundle(wiki);
                    this.bundlesCache.set(wiki, bundle);
                }
            }
        }

        return bundle;
    }

    private Bundle createWikiBundle(String wiki)
    {
        
    }

    // OLD



    /**
     * Constructs the {@link Properties} object corresponding to a wiki in a given language, from all the individual
     * documents registered as wiki-wide localization bundles.
     * 
     * @param wiki The target wiki.
     * @param language The 2-character code of the requested language.
     * @return A {@link Properties} object with the static translations of the wiki, in the requested language.
     */
    protected Properties loadStaticBundle(String wiki, String language)
    {
        Properties properties = new Properties();
        String[] bundles = getStaticDocumentBundles(wiki);
        // Reverse the order of the bundles, so that the first entry in the list has the most priority.
        ArrayUtils.reverse(bundles);
        for (String documentName : bundles) {
            String fullDocumentName = documentName;
            try {
                if (documentName.indexOf(WikiInformation.WIKI_PREFIX_SEPARATOR) < 0) {
                    fullDocumentName = wiki + WikiInformation.WIKI_PREFIX_SEPARATOR + documentName;
                }
                properties.putAll(getDocumentBundle(fullDocumentName, language));
            } catch (Exception ex) {
                this.logger.warn("Exception loading document bundle [{0}]", fullDocumentName);
            }
        }
        // Also watch the preferences for this wiki, in case the list of documents is changed.
        watchDocument(wiki + WikiInformation.WIKI_PREFIX_SEPARATOR + WikiInformation.PREFERENCES_DOCUMENT_NAME);
        return properties;
    }

    /**
     * Return the list of document names configured for a wiki as global localization bundles. If this list is already
     * cached, then use it. Otherwise, first load from the wiki and update the cache.
     * 
     * @param wiki The target wiki.
     * @return An array of <code>String</code>, each value representing the name of a document that should be used as a
     *         global localization resource. The names do not usually contain the wiki prefix, as these are the raw
     *         values taken from the wiki settings.
     */
    protected String[] getStaticDocumentBundles(String wiki)
    {
        synchronized (this.staticBundleNames) {
            if (this.staticBundleNames.containsKey(wiki)) {
                return this.staticBundleNames.get(wiki);
            }
            String[] result = new String[0];
            try {
                String bundles =
                    this.documentAccessBridge.getProperty(wiki + WikiInformation.WIKI_PREFIX_SEPARATOR
                        + WikiInformation.PREFERENCES_DOCUMENT_NAME, WikiInformation.PREFERENCES_CLASS_NAME,
                        DOCUMENT_BUNDLE_PROPERTY);
                if (!StringUtils.isBlank(bundles)) {
                    result = bundles.split("[,|]\\s*");
                }
            } catch (Exception ex) {
                this.logger.warn("Cannot access a wiki setting", ex);
            }
            this.staticBundleNames.put(wiki, result);

            return result;
        }
    }
}
