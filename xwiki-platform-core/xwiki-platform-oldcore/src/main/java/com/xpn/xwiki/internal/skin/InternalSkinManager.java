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
package com.xpn.xwiki.internal.skin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.classloader.NamespaceURLClassLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.skin.Skin;
import org.xwiki.url.URLConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 7.0M1
 */
@Component(roles = InternalSkinManager.class)
@Singleton
public class InternalSkinManager implements Initializable
{
    public static final String CKEY_SKIN = "skin";

    public static final String CKEY_PARENTSKIN = "baseskin";

    @Inject
    private InternalSkinConfiguration skinConfiguration;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiSkinUtils wikiSkinUtils;

    @Inject
    private Environment environment;

    @Inject
    @Named("all")
    private ConfigurationSource allConfiguration;

    @Inject
    private ObservationManager observation;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Logger logger;

    @Inject
    private Provider<URLConfiguration> urlConfigurationProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ClassLoaderManager classLoaderManager;

    private Cache<Skin> cache;

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize cache
        try {
            this.cache = this.cacheManager.createNewCache(new LRUCacheConfiguration("skins", 100, 86400));
        } catch (CacheException e) {
            throw new InitializationException("Failed to initialize cache", e);
        }

        // Initialize listener
        this.observation.addListener(new AbstractEventListener("skins", new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(), new DocumentCreatedEvent())
        {
            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                XWikiDocument document = (XWikiDocument) source;

                if (document.getXObject(WikiSkinUtils.SKINCLASS_REFERENCE) != null
                    || document.getOriginalDocument().getXObject(WikiSkinUtils.SKINCLASS_REFERENCE) != null) {
                    // TODO: lower the granularity
                    InternalSkinManager.this.cache.removeAll();
                }

            }
        }, EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY);
    }

    public Skin getSkin(String id)
    {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        Skin skin = this.cache.get(id);

        if (skin == null) {
            skin = createSkin(id);

            this.cache.set(id, skin);
        }

        return skin;
    }

    private Skin createSkin(String id)
    {
        Skin skin;

        if (this.wikiSkinUtils.isWikiSkin(id)) {
            skin = new WikiSkin(id, this, this.skinConfiguration, this.wikiSkinUtils, this.logger);
        } else {
            EnvironmentSkin environmentSkin =
                new EnvironmentSkin(id, this, this.skinConfiguration, this.logger, this.environment,
                    this.xcontextProvider, this.urlConfigurationProvider.get());
            // Check if the environment skin actually exists on the environment before returning it.
            // Other fallbacks to a classloader skin.
            if (environmentSkin.exists()) {
                skin = environmentSkin;
            } else {
                // Resolve the current wiki namespace and use it to find its classloader.
                WikiNamespace wikiNamespace = new WikiNamespace(this.wikiDescriptorManager.getCurrentWikiId());
                NamespaceURLClassLoader wikiClassLoader =
                    this.classLoaderManager.getURLClassLoader(wikiNamespace.serialize(), false);
                skin = new ClassLoaderSkin(id, this, this.skinConfiguration, this.logger, this.xcontextProvider,
                    this.urlConfigurationProvider.get(), wikiClassLoader);
            }
        }

        return skin;
    }

    public Skin getCurrentSkin(boolean testRights)
    {
        return getSkin(getCurrentSkinId(testRights));
    }

    public String getCurrentSkinId(boolean testRights)
    {
        String skin;

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            // Try to get it from context
            skin = (String) xcontext.get(CKEY_SKIN);
            if (StringUtils.isNotEmpty(skin)) {
                return skin;
            } else {
                skin = null;
            }

            // Try to get it from URL
            if (xcontext.getRequest() != null) {
                skin = xcontext.getRequest().getParameter("skin");
                if (StringUtils.isNotEmpty(skin)) {
                    return skin;
                } else {
                    skin = null;
                }
            }

            // Try to get it from preferences (user -> space -> wiki -> xwiki.properties)
            skin = this.allConfiguration.getProperty("skin");
            if (skin != null) {
                return skin;
            }
        }

        // Try to get it from xwiki.cfg
        skin = getDefaultSkinId();

        if (xcontext != null) {
            // Check if current user have enough right to access the wiki skin (if it's a wiki skin)
            // TODO: shouldn't we make sure anyone see the skin whatever right he have ?
            if (testRights) {
                XWikiDocument document = this.wikiSkinUtils.getSkinDocument(skin);
                if (document != null) {
                    if (!this.authorization.hasAccess(Right.VIEW, document.getDocumentReference())) {
                        this.logger.debug(
                            "Cannot access configured wiki skin [{}] due to access rights, using the default skin.",
                            skin);
                        skin = getDefaultSkinId();
                    }
                }
            }

            // Set found skin in the context
            xcontext.put(CKEY_SKIN, skin);
        }

        return skin;
    }

    public String getParentSkin(String skinId)
    {
        Skin skin = getSkin(skinId);
        if (skin != null) {
            ResourceRepository parent = skin.getParent();
            if (parent != null) {
                return parent.getId();
            }
        }

        return null;
    }

    public Skin getCurrentParentSkin(boolean testRights)
    {
        return getSkin(getCurrentParentSkinId(testRights));
    }

    public String getCurrentParentSkinId(boolean testRights)
    {
        // From the context
        String baseSkin = getContextParentId();

        // From the skin
        if (baseSkin == null) {
            Skin skin = getCurrentSkin(testRights);
            if (skin != null) {
                ResourceRepository parent = skin.getParent();
                if (parent != null) {
                    baseSkin = parent.getId();
                }
            }
        }

        // From the configuration
        if (baseSkin == null) {
            baseSkin = getDefaultParentSkinId();
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            // Check if current user have enough right to access the wiki skin (if it's a wiki skin)
            // TODO: shouldn't we make sure anyone see the skin whatever right he have ?
            if (testRights) {
                XWikiDocument document = this.wikiSkinUtils.getSkinDocument(baseSkin);
                if (document != null) {
                    if (!this.authorization.hasAccess(Right.VIEW, document.getDocumentReference())) {
                        this.logger.debug(
                            "Cannot access configured wiki skin [{}] due to access rights, using the default skin.",
                            baseSkin);
                        baseSkin = getDefaultParentSkinId();
                    }
                }
            }

            // Set found skin in the context
            xcontext.put(CKEY_PARENTSKIN, baseSkin);
        }

        return baseSkin;
    }

    public String getContextParentId()
    {
        String parentId = null;

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            parentId = (String) xcontext.get(CKEY_PARENTSKIN);
            if (StringUtils.isEmpty(parentId)) {
                parentId = null;
            }
        }

        return parentId;
    }

    public Skin getDefaultSkin()
    {
        return getSkin(getDefaultSkinId());
    }

    public String getDefaultSkinId()
    {
        String skin = this.skinConfiguration.getDefaultSkinId();

        // Fallback on default base skin
        if (skin == null) {
            skin = getDefaultParentSkinId();
        }

        return skin;
    }

    public Skin getDefaultParentSkin()
    {
        return getSkin(this.skinConfiguration.getDefaultParentSkinId());
    }

    public String getDefaultParentSkinId()
    {
        String skin = this.skinConfiguration.getDefaultParentSkinId();

        // Fallback on default skin
        if (skin == null) {
            skin = this.skinConfiguration.getDefaultSkinId();
        }

        return skin;
    }
}
