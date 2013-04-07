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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.xwiki.localization.internal.AbstractTranslationBundle;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.localization.wiki.internal.DefaultDocumentTranslationBundle;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Bundle corresponding to global (at the wiki level) localization documents.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(XWikiPreferencesTranslationBundle.ID)
@Singleton
public class XWikiPreferencesTranslationBundle extends AbstractTranslationBundle implements Initializable
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
     * Passed to {@link XWikiPreferencesWikiTranslationBundle}.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Used to parse translation messages.
     */
    @Inject
    @Named("messagetool/1.0")
    private TranslationMessageParser translationMessageParser;

    /**
     * Used to generate a document unique String identifier.
     */
    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> uidSerializer;

    /**
     * The cache of bundles by wiki.
     */
    private Map<String, XWikiPreferencesWikiTranslationBundle> wikiBundlesCache =
        new ConcurrentHashMap<String, XWikiPreferencesWikiTranslationBundle>();

    /**
     * The cache of bundles by document id.
     */
    private Cache<DefaultDocumentTranslationBundle> documentBundlesCache;

    /**
     * Default constructor.
     */
    public XWikiPreferencesTranslationBundle()
    {
        super(ID, 300);
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Setup cache
        CacheConfiguration cacheConfiguration = new CacheConfiguration("localization." + getId());

        try {
            this.documentBundlesCache = this.cacheManager.createNewCache(cacheConfiguration);
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
     * @return the {@link XWikiPreferencesTranslationBundle} for the current wiki
     */
    private XWikiPreferencesWikiTranslationBundle getBundle()
    {
        String currentWiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();

        return getBundle(currentWiki);
    }

    /**
     * @param wiki the wiki
     * @return the {@link XWikiPreferencesWikiTranslationBundle} for the provided wiki
     */
    private XWikiPreferencesWikiTranslationBundle getBundle(String wiki)
    {
        XWikiPreferencesWikiTranslationBundle bundle = this.wikiBundlesCache.get(wiki);
        if (bundle == null) {
            bundle = getBundleSynchronized(wiki);
        }

        return bundle;
    }

    /**
     * Syncronized version of {@link #getBundle()} so that we synchronize only when necessary.
     * 
     * @param wiki the wiki
     * @return the {@link XWikiPreferencesWikiTranslationBundle} for the provided wiki
     */
    private synchronized XWikiPreferencesWikiTranslationBundle getBundleSynchronized(String wiki)
    {
        XWikiPreferencesWikiTranslationBundle bundle = this.wikiBundlesCache.get(wiki);
        if (bundle == null) {
            try {
                bundle = createWikiBundle(wiki);
                this.wikiBundlesCache.put(wiki, bundle);
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
    private XWikiPreferencesWikiTranslationBundle createWikiBundle(String wiki) throws ComponentLookupException
    {
        return new XWikiPreferencesWikiTranslationBundle(wiki, this, this.componentManager);
    }

    /**
     * Get document bundle from passed reference.
     * 
     * @param document the document reference
     * @return the document bundle
     */
    protected DefaultDocumentTranslationBundle getDocumentTranslationBundle(DocumentReference document)
    {
        String uid = this.uidSerializer.serialize(document);

        DefaultDocumentTranslationBundle documentBundle = this.documentBundlesCache.get(uid);
        if (documentBundle == null) {
            try {
                documentBundle =
                    new DefaultDocumentTranslationBundle(document, this.componentManager,
                        this.translationMessageParser);
                this.documentBundlesCache.set(uid, documentBundle);
            } catch (ComponentLookupException e) {
                // Should never happen
                this.logger.error("Failed to create document bundle for document [{}]", document, e);
            }
        }

        return documentBundle;
    }
}
