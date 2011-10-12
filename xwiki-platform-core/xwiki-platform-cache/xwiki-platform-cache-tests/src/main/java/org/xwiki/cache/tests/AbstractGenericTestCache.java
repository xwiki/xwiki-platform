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
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;

/**
 * Base class for testing cache component implementation.
 * 
 * @version $Id$
 */
public abstract class AbstractGenericTestCache extends AbstractTestCache
{
    /**
     * @param roleHint the role hint of the cache component implementation to test.
     */
    protected AbstractGenericTestCache(String roleHint)
    {
        super(roleHint);
    }

    // ///////////////////////////////////////////////////////::
    // Tests

    /**
     * Validate factory initialization.
     * 
     * @throws Exception error.
     */
    @Test
    public void testGetFactory() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheFactory factory2 = getCacheFactory();

        Assert.assertSame(factory, factory2);
    }

    /**
     * Validate some basic cache use case without any constraints.
     * 
     * @throws Exception error.
     */
    @Test
    public void testCreateAndDestroyCacheSimple() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        Assert.assertNotNull(cache);

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        Assert.assertEquals(VALUE, cache.get(KEY));
        Assert.assertEquals(VALUE2, cache.get(KEY2));

        cache.dispose();
    }

    /**
     * Validate {@link Cache#remove(String)}.
     * 
     * @throws Exception error.
     */
    @Test
    public void testRemove() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        cache.remove(KEY);

        Assert.assertNull(cache.get(KEY));
        Assert.assertEquals(VALUE2, cache.get(KEY2));
    }

    /**
     * Validate {@link Cache#removeAll()}.
     * 
     * @throws Exception error.
     */
    @Test
    public void testRemoveAll() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        cache.removeAll();

        Assert.assertNull(cache.get(KEY));
        Assert.assertNull(cache.get(KEY2));
    }

    /**
     * Validate event management.
     * 
     * @throws Exception error.
     */
    @Test
    public void testEvents() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();

        cache.addCacheEntryListener(eventListener);

        cache.set(KEY, VALUE);

        Assert.assertNotNull(eventListener.getAddedEvent());
        Assert.assertSame(cache, eventListener.getAddedEvent().getCache());
        Assert.assertEquals(KEY, eventListener.getAddedEvent().getEntry().getKey());
        Assert.assertEquals(VALUE, eventListener.getAddedEvent().getEntry().getValue());

        cache.set(KEY, VALUE2);

        Assert.assertNotNull(eventListener.getModifiedEvent());
        Assert.assertSame(cache, eventListener.getModifiedEvent().getCache());
        Assert.assertEquals(KEY, eventListener.getModifiedEvent().getEntry().getKey());
        Assert.assertEquals(VALUE2, eventListener.getModifiedEvent().getEntry().getValue());

        cache.remove(KEY);
        cache.get(KEY);

        Assert.assertNotNull(eventListener.getRemovedEvent());
        Assert.assertSame(cache, eventListener.getRemovedEvent().getCache());
        Assert.assertEquals(KEY, eventListener.getRemovedEvent().getEntry().getKey());
        Assert.assertEquals(VALUE2, eventListener.getRemovedEvent().getEntry().getValue());
    }

    /**
     * Validate that two different caches are really different.
     * 
     * @throws Exception error.
     */
    @Test
    public void testSeveralCaches() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());
        Cache<Object> cache2 = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);

        Assert.assertNull(cache2.get(KEY));
    }

    /**
     * Validate that when recreating a cache with the same id the second instance is in a proper state.
     * 
     * @throws Exception error
     */
    @Test
    public void testRecreateCache() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setConfigurationId("test");

        Cache<Object> cache = factory.newCache(configuration);

        cache.set(KEY, VALUE);

        Assert.assertEquals(VALUE, cache.get(KEY));

        // dispose the first cache

        cache.dispose();

        // recreate it

        cache = factory.newCache(configuration);

        Assert.assertNull(cache.get(KEY));

        cache.set(KEY, VALUE);

        Assert.assertEquals(VALUE, cache.get(KEY));
    }
}
