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

import org.jmock.Mock;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Base class for testing cache component implementation.
 * 
 * @version $Id$
 */
public abstract class AbstractTestCache extends AbstractXWikiComponentTestCase
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

        Mock mockConfigurationSource = mock(ConfigurationSource.class);
        mockConfigurationSource.stubs().method("getProperty").with(eq("cache.defaultCache"), ANYTHING).will(
            returnValue(this.roleHint));
//        mockConfigurationSource.stubs().method("getProperty").will(returnValue(this.roleHint));
        getComponentManager().registerComponent(ConfigurationSource.class, mockConfigurationSource.proxy());
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
        CacheManager cacheManager = (CacheManager) getComponentManager().lookup(CacheManager.class);

        CacheFactory factory = cacheManager.getCacheFactory();

        assertNotNull(factory);

        return factory;
    }
}
