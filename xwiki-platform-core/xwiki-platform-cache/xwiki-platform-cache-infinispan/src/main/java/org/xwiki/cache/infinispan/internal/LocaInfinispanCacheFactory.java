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
package org.xwiki.cache.infinispan.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;

/**
 * Implements {@link org.xwiki.cache.CacheFactory} based on Infinispan.
 * 
 * @version $Id$
 * @since 3.3M1
 */
@Component
@Named("infinispan/local")
@Singleton
@Deprecated
public class LocaInfinispanCacheFactory implements CacheFactory
{
    /**
     * Don't do anything special and redirect to standard Infinispan implementation instead.
     */
    @Inject
    @Named("infinispan")
    private CacheFactory infinispanCacheFactory;

    @Override
    public <T> Cache<T> newCache(CacheConfiguration config) throws CacheException
    {
        return this.infinispanCacheFactory.newCache(config);
    }
}
