package org.xwiki.cache.jbosscache;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.jbosscache.internal.JBossCacheCacheConfiguration;
import org.xwiki.cache.tests.AbstractTestCache;
import org.xwiki.cache.tests.CacheEntryListenerTest;
import org.xwiki.cache.tests.CacheEntryListenerTest.EventType;

public class JBossCacheLocalCacheTest extends AbstractTestCache
{
    public JBossCacheLocalCacheTest()
    {
        this("jbosscache/local");
    }

    protected JBossCacheLocalCacheTest(String roleHint)
    {
        super(roleHint);
    }

    // ///////////////////////////////////////////////////////::
    // Tests

    /**
     * Validate the maximum time to live constraint.
     * 
     * @throws Exception error
     */
    public void testCreateAndDestroyCacheLRUTimeToLive() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setTimeToLive(1);
        // Force JBoss eviction interval to the minimum
        lec.put(JBossCacheCacheConfiguration.CONFX_EVICTION_WAKEUPINTERVAL, 1);
        conf.put(EntryEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();
        cache.addCacheEntryListener(eventListener);

        assertNotNull(cache);

        cache.set(KEY, VALUE);

        assertEquals(VALUE, cache.get(KEY));

        // Wait for the JBoss Eviction policy to be called
        assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));

        assertNull(cache.get(KEY));

        cache.dispose();
    }

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

        assertNotNull(cache);

        cache.set("key", "value");

        assertEquals("value", cache.get("key"));

        cache.set("key2", 2);

        // Wait for the JBoss Eviction policy to be called
        assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));

        assertNull(cache.get("key"));
        assertEquals(2, cache.get("key2"));

        cache.dispose();
    }

    public void testCreateAndDestroyCacheLRUAll() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setMaxEntries(1);
        lec.setTimeToLive(2);
        conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();
        cache.addCacheEntryListener(eventListener);

        assertNotNull(cache);

        cache.set("key", "value");

        assertEquals("value", cache.get("key"));

        cache.set("key2", 2);

        // Wait for the JBoss Eviction policy to be called
        assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));
        eventListener.reinitRemovedEvent();

        assertNull(cache.get("key"));
        assertEquals(2, cache.get("key2"));

        // Wait for the JBoss Eviction policy to be called
        assertTrue("No value has been evicted from the cache", eventListener.waitForEntryEvent(EventType.REMOVE));

        assertNull(cache.get("key"));
        assertNull(cache.get("key2"));

        cache.dispose();
    }
}
