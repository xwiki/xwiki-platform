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
package org.xwiki.user.internal.document;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.function.FailableFunction;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.WikiReference;

/**
 * Cache various user related information.
 * 
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component(roles = UserCache.class)
@Singleton
public class UserCache implements Initializable, Disposable
{
    @Inject
    private CacheManager cacheManager;

    private Cache<Boolean> hasUserCache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.hasUserCache = this.cacheManager.createNewCache(new LRUCacheConfiguration("user.hasUsers"));
        } catch (CacheException e) {
            throw new InitializationException("Failed to create the cache to store if a user exist in a given wiki", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.hasUserCache.dispose();
    }

    /**
     * @param <E> the type of the exception
     * @param wikiReference the reference of the wiki
     * @param function the function to use to check of a user exist
     * @return true if a user exist for the passed wiki, false otherwise
     * @throws E when failing to check if a user exist
     */
    public <E extends Throwable> boolean computeIfAbsent(WikiReference wikiReference,
        FailableFunction<WikiReference, Boolean, E> function) throws E
    {
        Boolean hasUser = this.hasUserCache.get(wikiReference.getName());

        if (hasUser == null) {
            synchronized (this.hasUserCache) {
                hasUser = function.apply(wikiReference);

                this.hasUserCache.set(wikiReference.getName(), hasUser);
            }
        }

        return hasUser;
    }

    /**
     * @param wikiReference the reference of the wiki
     * @param hasUsers true if a user exist for the passed wiki, false otherwise
     */
    public void set(WikiReference wikiReference, Boolean hasUsers)
    {
        if (hasUsers == null) {
            invalidate(wikiReference);
        } else {
            this.hasUserCache.set(wikiReference.getName(), hasUsers);
        }
    }

    /**
     * @param wikiReference the reference of the wiki for which to invalidate the cache
     */
    public void invalidate(WikiReference wikiReference)
    {
        this.hasUserCache.remove(wikiReference.getName());
    }
}
