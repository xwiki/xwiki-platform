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
    public void testGetFactory() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheFactory factory2 = getCacheFactory();

        assertSame(factory, factory2);
    }

    /**
     * Validate some basic cache use case without any constraints.
     * 
     * @throws Exception error.
     */
    public void testCreateAndDestroyCacheSimple() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        assertNotNull(cache);

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        assertEquals(VALUE, cache.get(KEY));
        assertEquals(VALUE2, cache.get(KEY2));

        cache.dispose();
    }

    /**
     * Validate {@link Cache#remove(String)}.
     * 
     * @throws Exception error.
     */
    public void testRemove() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        cache.remove(KEY);

        assertNull(cache.get(KEY));
        assertEquals(VALUE2, cache.get(KEY2));
    }

    /**
     * Validate {@link Cache#removeAll()}.
     * 
     * @throws Exception error.
     */
    public void testRemoveAll() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        cache.removeAll();

        assertNull(cache.get(KEY));
        assertNull(cache.get(KEY2));
    }

    /**
     * Validate event management.
     * 
     * @throws Exception error.
     */
    public void testEvents() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();

        cache.addCacheEntryListener(eventListener);

        cache.set(KEY, VALUE);

        assertNotNull(eventListener.getAddedEvent());
        assertSame(cache, eventListener.getAddedEvent().getCache());
        assertEquals(KEY, eventListener.getAddedEvent().getEntry().getKey());
        assertEquals(VALUE, eventListener.getAddedEvent().getEntry().getValue());

        cache.set(KEY, VALUE2);

        assertNotNull(eventListener.getModifiedEvent());
        assertSame(cache, eventListener.getModifiedEvent().getCache());
        assertEquals(KEY, eventListener.getModifiedEvent().getEntry().getKey());
        assertEquals(VALUE2, eventListener.getModifiedEvent().getEntry().getValue());

        cache.remove(KEY);
        cache.get(KEY);

        assertNotNull(eventListener.getModifiedEvent());
        assertSame(cache, eventListener.getModifiedEvent().getCache());
        assertEquals(KEY, eventListener.getModifiedEvent().getEntry().getKey());
        assertEquals(VALUE2, eventListener.getModifiedEvent().getEntry().getValue());
    }
}
