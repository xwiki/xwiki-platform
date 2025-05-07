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
package org.xwiki.user.internal.group;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;

/**
 * Cache all the groups per wiki.
 * 
 * @version $Id$
 * @since 17.3.0RC1
 * @since 16.10.6
 */
@Component(roles = WikiGroupCache.class)
@Singleton
public class WikiGroupCache implements Initializable, Disposable
{
    private static final int DEFAULT_CAPACITY = 1000;

    protected Cache<Set<DocumentReference>> cache;

    @Inject
    private CacheManager cacheManager;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.cache = this.cacheManager.createNewCache(new LRUCacheConfiguration("user.groups", DEFAULT_CAPACITY));
        } catch (Exception e) {
            throw new InitializationException("Failed to create the group cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.cache.dispose();
    }

    /**
     * @param wiki the wiki of the groups
     * @return the groups of the wiki
     */
    public Set<DocumentReference> get(String wiki)
    {
        return this.cache.get(wiki);
    }

    /**
     * @param wiki the wiki of the groups
     * @param groups the groups of the wiki
     */
    public void set(String wiki, Set<DocumentReference> groups)
    {
        this.cache.set(wiki, groups);
    }

    /**
     * @param wiki the wiki of the groups
     */
    public void invalidate(String wiki)
    {
        this.cache.remove(wiki);
    }
}
