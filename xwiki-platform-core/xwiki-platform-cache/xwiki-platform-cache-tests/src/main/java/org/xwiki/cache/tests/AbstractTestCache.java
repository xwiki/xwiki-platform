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
package org.xwiki.cache.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

/**
 * Base class for testing cache component implementation.
 *
 * @version $Id$
 */
public abstract class AbstractTestCache
{
    /**
     * The first key.
     */
    protected static final String KEY = "key";

    /**
     * The second key.
     */
    protected static final String KEY2 = "key2";

    /**
     * The value of the first key.
     */
    protected static final String VALUE = "value";

    /**
     * The value of the second key.
     */
    protected static final int VALUE2 = 2;

    /**
     * The Mockito tool.
     */
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    /**
     * The role hint of the cache component implementation to test.
     */
    protected String roleHint;

    /**
     * @param roleHint the role hint of the cache component implementation to test.
     */
    protected AbstractTestCache(String roleHint)
    {
        this.roleHint = roleHint;
    }

    /**
     * Before.
     * 
     * @throws Exception when initialization fail
     */
    @Before
    public void before() throws Exception
    {
        MemoryConfigurationSource configuration = this.componentManager.registerMemoryConfigurationSource();

        configuration.setProperty("cache.defaultCache", this.roleHint);
    }

    /**
     * @return a instance of the cache factory.
     * @throws Exception error when searching for cache factory component.
     */
    public CacheFactory getCacheFactory() throws Exception
    {
        CacheManager cacheManager = this.componentManager.getInstance(CacheManager.class);

        CacheFactory factory = cacheManager.getCacheFactory();

        Assert.assertNotNull(factory);

        return factory;
    }
}
