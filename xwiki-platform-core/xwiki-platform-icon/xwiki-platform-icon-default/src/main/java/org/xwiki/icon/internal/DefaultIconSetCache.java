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
package org.xwiki.icon.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default implementation of {@link org.xwiki.icon.IconSetCache}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultIconSetCache implements IconSetCache, Initializable
{
    private static final String ICON_SET_CACHE_ID = "iconset";

    private static final String NAME_SUFFIX = "NAMED:";

    private static final String DOCUMENT_SUFFIX = "DOC:";

    @Inject
    private CacheManager cacheManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache<IconSet> cache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration(ICON_SET_CACHE_ID);
            CacheFactory cacheFactory = this.cacheManager.getCacheFactory();
            this.cache = cacheFactory.newCache(configuration);
        } catch (ComponentLookupException | CacheException e) {
            throw new InitializationException("Failed to initialize the IconSet Cache.", e);
        }
    }

    private String getCacheKey(String name, String wikiId)
    {
        return wikiId.length() + wikiId + '_' + name;
    }

    @Override
    public IconSet get(String name)
    {
        return this.cache.get(getKeyFromName(name));
    }

    @Override
    public IconSet get(String name, String wikiId)
    {
        return get(getCacheKey(name, wikiId));
    }

    @Override
    public IconSet get(DocumentReference documentReference)
    {
        return this.cache.get(getKeyFromDocument(documentReference));
    }

    @Override
    public void put(String name, IconSet iconSet)
    {
        this.cache.set(getKeyFromName(name), iconSet);
    }

    @Override
    public void put(String name, String wikiId, IconSet iconSet)
    {
        put(getCacheKey(name, wikiId), iconSet);
    }

    @Override
    public void put(DocumentReference documentReference, IconSet iconSet)
    {
        this.cache.set(getKeyFromDocument(documentReference), iconSet);
    }

    @Override
    public void clear()
    {
        this.cache.removeAll();
    }

    @Override
    public void clear(DocumentReference documentReference)
    {
        this.cache.remove(getKeyFromDocument(documentReference));
    }

    @Override
    public void clear(String name)
    {
        this.cache.remove(getKeyFromName(name));
    }

    @Override
    public void clear(String name, String wikiId)
    {
        clear(getCacheKey(name, wikiId));
    }

    private String getKeyFromName(String name)
    {
        return NAME_SUFFIX  + name;
    }

    private String getKeyFromDocument(DocumentReference docRef)
    {
        return DOCUMENT_SUFFIX  + this.entityReferenceSerializer.serialize(docRef);
    }
}
