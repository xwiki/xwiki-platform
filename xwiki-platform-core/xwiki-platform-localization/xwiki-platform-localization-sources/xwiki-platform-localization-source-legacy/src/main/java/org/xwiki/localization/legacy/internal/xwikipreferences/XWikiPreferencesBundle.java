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
package org.xwiki.localization.legacy.internal.xwikipreferences;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.Translation;
import org.xwiki.localization.internal.AbstractBundle;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;

/**
 * Bundle corresponding to global (at the wiki level) localization documents.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(XWikiPreferencesBundle.ID)
@Singleton
public class XWikiPreferencesBundle extends AbstractBundle implements Initializable
{
    /**
     * The identifier of the bundle.
     */
    protected static final String ID = "XWikiPreferences";

    /**
     * Used to access current wiki.
     */
    @Inject
    private ModelContext modelContext;

    /**
     * Used to create a cache.
     */
    @Inject
    private CacheManager cacheManager;

    /**
     * Passed to {@link XWikiPreferencesWikiBundle}.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The cache of bundles by document reference.
     */
    private Cache<XWikiPreferencesWikiBundle> bundlesCache;

    /**
     * Default constructor.
     */
    public XWikiPreferencesBundle()
    {
        super(ID, 300);
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Setup cache
        CacheConfiguration cacheConfiguration = new CacheConfiguration("localization." + getId());

        try {
            this.bundlesCache = this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            this.logger.error("Failed to create cache [{}]", cacheConfiguration.getConfigurationId(), e);
        }
    }

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        return getBundle().getTranslation(key, locale);
    }

    /**
     * @return the {@link XWikiPreferencesBundle} for the current wiki
     */
    private XWikiPreferencesWikiBundle getBundle()
    {
        String currentWiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();

        return getBundle(currentWiki);
    }

    /**
     * @param wiki the wiki
     * @return the {@link XWikiPreferencesWikiBundle} for the provided wiki
     */
    private XWikiPreferencesWikiBundle getBundle(String wiki)
    {
        XWikiPreferencesWikiBundle bundle = this.bundlesCache.get(wiki);
        if (bundle == null) {
            try {
                bundle = createWikiBundle(wiki);
                this.bundlesCache.set(wiki, bundle);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to create preferences bundle for wiki [{}]", wiki, e);
            }
        }

        return bundle;
    }

    /**
     * @param wiki the wiki
     * @return the XWikiPreferencesWikiBundle for the provided wiki
     * @throws ComponentLookupException faleid to create the bundle
     */
    private XWikiPreferencesWikiBundle createWikiBundle(String wiki) throws ComponentLookupException
    {
        return new XWikiPreferencesWikiBundle(wiki, this.componentManager);
    }
}
