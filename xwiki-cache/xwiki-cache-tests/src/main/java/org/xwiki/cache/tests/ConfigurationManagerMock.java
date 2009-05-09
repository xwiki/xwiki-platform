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

import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.xwiki.cache.CacheManagerConfiguration;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSource;

/**
 * @version $Id$
 * @since 1.7M1
 */
public class ConfigurationManagerMock extends AbstractLogEnabled implements ConfigurationManager
{
    /**
     * The cache hint.
     */
    private String cache;

    /**
     * The local cache hint.
     */
    private String localCache;

    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor getComponentDescriptor()
    {
        DefaultComponentDescriptor componentDescriptor = new DefaultComponentDescriptor();

        componentDescriptor.setRole(ConfigurationManager.class);
        componentDescriptor.setImplementation(ConfigurationManagerMock.class.getName());

        return componentDescriptor;
    }

    /**
     * @param cacheHint the cache hint.
     */
    public void setCacheHint(String cacheHint)
    {
        this.cache = cacheHint;
    }

    /**
     * @param localCacheHint the local cache hint.
     */
    public void setLocalCacheHint(String localCacheHint)
    {
        this.localCache = localCacheHint;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.configuration.ConfigurationManager#initializeConfiguration(java.lang.Object, java.util.List,
     *      java.lang.String)
     */
    public void initializeConfiguration(Object configurationBean, List<ConfigurationSource> sources, String namespace)
        throws InitializationException
    {
        CacheManagerConfiguration cacheManagerConfiguration = (CacheManagerConfiguration) configurationBean;

        try {
            BeanUtils.setProperty(cacheManagerConfiguration, "defaultCache", this.cache);
            BeanUtils.setProperty(cacheManagerConfiguration, "defaultLocalCache", this.localCache);
        } catch (Exception e) {
            getLogger().error("Failed to set CacheManager configuration", e);
        }
    }
}
