package org.xwiki.cache.jbosscache;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.tests.AbstractTestCache;
import org.xwiki.component.manager.ComponentLookupException;

public class JBossCacheCacheTest extends AbstractTestCache
{
    public JBossCacheCacheTest()
    {
        super("jbosscache");
    }

    // ///////////////////////////////////////////////////////::
    // Tests

    public void testCreateAndDestroyCacheLRUMaxEntries() throws ComponentLookupException, Exception
    {
        /*CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setMaxEntries(1);
        conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        assertNotNull(cache);

        cache.set("key", "value");

        assertEquals("value", cache.get("key"));

        cache.set("key2", 2);

        assertNull(cache.get("key"));
        assertEquals(2, cache.get("key2"));

        cache.dispose();*/
    }

    public void testCreateAndDestroyCacheLRUTimeToLive() throws ComponentLookupException, Exception
    {
        /*CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setTimeToLive(1);
        conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        assertNotNull(cache);

        cache.set("key", "value");

        assertEquals("value", cache.get("key"));

        Thread.sleep(1000);

        assertNull(cache.get("key"));

        cache.dispose();*/
    }

    public void testCreateAndDestroyCacheLRUAll() throws ComponentLookupException, Exception
    {
        /*CacheFactory factory = getCacheFactory();

        CacheConfiguration conf = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setMaxEntries(1);
        lec.setTimeToLive(1);
        conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);

        Cache<Object> cache = factory.newCache(conf);

        assertNotNull(cache);

        cache.set("key", "value");

        assertEquals("value", cache.get("key"));

        cache.set("key2", 2);

        assertNull(cache.get("key"));
        assertEquals(2, cache.get("key2"));

        Thread.sleep(1000);

        assertNull(cache.get("key"));
        assertNull(cache.get("key2"));

        cache.dispose();*/
    }
}
