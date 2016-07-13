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

import org.junit.Test;
import org.xwiki.cache.infinispan.internal.InfinispanCacheFactory;
import org.xwiki.cache.internal.DefaultCacheFactory;
import org.xwiki.cache.internal.DefaultCacheManager;
import org.xwiki.cache.internal.DefaultCacheManagerConfiguration;
import org.xwiki.cache.tests.AbstractTestCache;
import org.xwiki.environment.Environment;
import org.xwiki.test.annotation.ComponentList;

import static org.mockito.Mockito.verify;

/**
 * Verify that defining an Infinispan config file is taken into account.
 *
 * @version $Id$
 * @since 3.4M1
 */
@ComponentList({
    InfinispanCacheFactory.class,
    DefaultCacheManager.class,
    DefaultCacheFactory.class,
    DefaultCacheManagerConfiguration.class
})
public class InfinispanConfigTest extends AbstractTestCache
{
    public InfinispanConfigTest()
    {
        super("infinispan");
    }

    @Test
    public void testConfig() throws Exception
    {
        // We register a mock Container to verify that getCacheFactory() below will call
        // Environment#getResourceAsStream() which will mean that the configuration file is read.
        final Environment environment = this.componentManager.registerMockComponent(Environment.class);

        getCacheFactory();

        verify(environment).getResourceAsStream("/WEB-INF/cache/infinispan/config.xml");
    }
}
