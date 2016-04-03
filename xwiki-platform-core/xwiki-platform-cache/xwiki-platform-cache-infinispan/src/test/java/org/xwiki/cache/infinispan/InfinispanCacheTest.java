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
package org.xwiki.cache.infinispan;

import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.infinispan.internal.InfinispanCacheFactory;
import org.xwiki.cache.infinispan.internal.InfinispanConfigurationLoader;
import org.xwiki.cache.internal.DefaultCacheFactory;
import org.xwiki.cache.internal.DefaultCacheManager;
import org.xwiki.cache.internal.DefaultCacheManagerConfiguration;
import org.xwiki.cache.tests.AbstractEvictionGenericTestCache;
import org.xwiki.test.annotation.ComponentList;

/**
 * Unit tests for {@link org.xwiki.cache.infinispan.internal.InfinispanCache}.
 *
 * @version $Id$
 */
@ComponentList({
    InfinispanCacheFactory.class,
    DefaultCacheManager.class,
    DefaultCacheFactory.class,
    DefaultCacheManagerConfiguration.class
})
public class InfinispanCacheTest extends AbstractEvictionGenericTestCache
{
    public InfinispanCacheTest()
    {
        super("infinispan", true);
    }

    @Override
    protected void customizeEviction(EntryEvictionConfiguration eviction)
    {
        // Force expiration thread to wakeup often
        eviction.put(InfinispanConfigurationLoader.CONFX_EXPIRATION_WAKEUPINTERVAL, 100);
    }
}
