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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.PlexusContainerLocator;
import org.jmock.MockObjectTestCase;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.plexus.manager.PlexusComponentManager;

/**
 * Base class for testing cache component implementation.
 * 
 * @version $Id: $
 */
public abstract class AbstractTestCache extends MockObjectTestCase implements ApplicationContext
{
    /**
     * The first key.
     */
    private static final String KEY = "key";

    /**
     * The second key.
     */
    private static final String KEY2 = "key2";

    /**
     * The value of the first key.
     */
    private static final String VALUE = "value";

    /**
     * The value of the second key.
     */
    private static final int VALUE2 = 2;

    /**
     * The role hint of the cache component implementation to test.
     */
    protected String roleHint;

    /**
     * The component manager to get a cache component.
     */
    private ComponentManager componentManager;

    /**
     * @param roleHint the role hint of the cache component implementation to test.
     */
    protected AbstractTestCache(String roleHint)
    {
        this.roleHint = roleHint;
    }

    /**
     * @return the component manager to get a cache component.
     * @throws Exception error when initializing component manager.
     */
    protected ComponentManager getComponentManager() throws Exception
    {
        if (this.componentManager == null) {
            DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();
            configuration.setContainerConfiguration("/plexus.xml");
            DefaultPlexusContainer container = new DefaultPlexusContainer(configuration);
            PlexusContainerLocator locator = new PlexusContainerLocator(container);
            this.componentManager = new PlexusComponentManager(locator);

            // Initialize the Container
            Container c = (Container) getComponentManager().lookup(Container.ROLE);
            c.setApplicationContext(this);
        }

        return this.componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String resourceName)
    {
        return getClass().getResourceAsStream(resourceName);
    }
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResource(java.lang.String)
     */
    public URL getResource(String resourceName) throws MalformedURLException
    {
        return getClass().getResource(resourceName);
    }

    /**
     * @return a instance of the cache factory.
     * @throws Exception error when searching for cache factory component.
     */
    public CacheFactory getCacheFactory() throws Exception
    {
        return (CacheFactory) getComponentManager().lookup(CacheFactory.ROLE, this.roleHint);
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

        assertNotNull(factory);

        CacheFactory factory2 = getCacheFactory();

        assertNotNull(factory2);

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
}
