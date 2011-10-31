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
package org.xwiki.cache.jbosscache;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.jbosscache.internal.JBossCacheCacheConfiguration;
import org.xwiki.cache.tests.AbstractTestCache;
import org.xwiki.cache.tests.CacheEntryListenerTest;
import org.xwiki.cache.tests.CacheEntryListenerTest.EventType;

public class JBossCacheLocalCacheEvictionsTest extends AbstractTestCache
{
    public JBossCacheLocalCacheEvictionsTest()
    {
        super("jbosscache/local");
    }

    // ///////////////////////////////////////////////////////::
    // Tests

    /**
     * Validate the maximum time to live constraint.
     * 
     * @throws Exception error
     */
    @Test
    public void testCreateAndDestroyCacheLRUTimeToLive() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setTimeToLive(1);
        // Force JBoss eviction interval to the minimum
        lec.put(JBossCacheCacheConfiguration.CONFX_EVICTION_WAKEUPINTERVAL, 1000);
        conf.put(EntryEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();
        cache.addCacheEntryListener(eventListener);

        Assert.assertNotNull(cache);

        cache.set(KEY, VALUE);

        Assert.assertEquals(VALUE, cache.get(KEY));

        // Wait for the JBoss Eviction policy to be called
        Assert.assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));

        Assert.assertNull(cache.get(KEY));

        cache.dispose();
    }

    @Test
    public void testCreateAndDestroyCacheLRUMaxEntries() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        conf.setConfigurationId("unit-test");
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setMaxEntries(1);
        // Force JBoss eviction interval to the minimum
        lec.put(JBossCacheCacheConfiguration.CONFX_EVICTION_WAKEUPINTERVAL, 1);
        conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();
        cache.addCacheEntryListener(eventListener);

        Assert.assertNotNull(cache);

        cache.set("key", "value");

        Assert.assertEquals("value", cache.get("key"));

        cache.set("key2", 2);

        // Wait for the JBoss Eviction policy to be called
        Assert.assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));

        Assert.assertNull(cache.get("key"));
        Assert.assertEquals(2, cache.get("key2"));

        cache.dispose();
    }

    @Test
    public void testCreateAndDestroyCacheLRUAll() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setMaxEntries(1);
        lec.setTimeToLive(2);
        // Force JBoss eviction interval to the minimum
        lec.put(JBossCacheCacheConfiguration.CONFX_EVICTION_WAKEUPINTERVAL, 1);
        conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();
        cache.addCacheEntryListener(eventListener);

        Assert.assertNotNull(cache);

        cache.set("key", "value");

        Assert.assertEquals("value", cache.get("key"));

        cache.set("key2", 2);

        // Wait for the JBoss Eviction policy to be called
        Assert.assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));
        eventListener.reinitRemovedEvent();

        Assert.assertNull(cache.get("key"));
        Assert.assertEquals(2, cache.get("key2"));

        // Wait for the JBoss Eviction policy to be called
        Assert.assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));

        Assert.assertNull(cache.get("key"));
        Assert.assertNull(cache.get("key2"));

        cache.dispose();
    }
}
