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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Base class for testing cache component implementation.
 * 
 * @version $Id: $
 */
public abstract class AbstractTestCache extends AbstractXWikiComponentTestCase implements ApplicationContext
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
     * The role hint of the cache component implementation to test.
     */
    protected String roleHint;

    /**
     * The container.
     */
    private Container container;

    /**
     * @param roleHint the role hint of the cache component implementation to test.
     */
    protected AbstractTestCache(String roleHint)
    {
        this.roleHint = roleHint;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        getComponentManager().registerComponent(ConfigurationManagerMock.getComponentDescriptor());
        getComponentManager().registerComponent(ConfigurationSourceCollectionMock.getComponentDescriptor());
        
        ConfigurationManagerMock configurationMock = 
            (ConfigurationManagerMock) getComponentManager().lookup(ConfigurationManager.class);
        configurationMock.setCacheHint(this.roleHint);
    }

    /**
     * @return the component manager to get a cache component.
     * @throws Exception error when initializing component manager.
     */
    @Override
    public ComponentManager getComponentManager() throws Exception
    {
        ComponentManager cm = super.getComponentManager();

        if (this.container == null) {
            // Initialize the Container
            Container c = (Container) cm.lookup(Container.class);
            c.setApplicationContext(this);
        }

        return cm;
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
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getTemporaryDirectory()
     */
    public File getTemporaryDirectory()
    {
        throw new UnsupportedOperationException("This method is not implemented for this test class.");
    }

    /**
     * @return a instance of the cache factory.
     * @throws Exception error when searching for cache factory component.
     */
    public CacheFactory getCacheFactory() throws Exception
    {
        CacheManager cacheManager = (CacheManager) getComponentManager().lookup(CacheManager.class, "default");

        CacheFactory factory = cacheManager.getCacheFactory();

        assertNotNull(factory);

        return factory;
    }
}
