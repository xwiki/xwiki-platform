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
package org.xwiki.cache.config;

import java.util.HashMap;

/**
 * Contains all informations used to create the cache.
 * <p>
 * A configuration identifier can be defined.
 * <p>
 * It can be used to be the cache unique identifier for clustering process.
 * <p>
 * This is also used by implementations to associate the cache with a configuration file which overwrite the
 * configuration it contains. This way any cache can be tuned in a particular installation with option specifics to
 * chosen cache implementation.
 * 
 * @version $Id$
 * @see org.xwiki.cache.eviction.EntryEvictionConfiguration
 */
public class CacheConfiguration extends HashMap<String, Object>
{
    /**
     * Since this class is a Map it needs to be serializable and thus have a unique id for Serialization.
     */
    private static final long serialVersionUID = -7298684313672163845L;

    /**
     * The configuration identifier.
     */
    private String configurationId;

    /**
     * @param configurationId the configuration identifier.
     */
    public void setConfigurationId(String configurationId)
    {
        this.configurationId = configurationId;
    }

    /**
     * @return the configuration identifier.
     */
    public String getConfigurationId()
    {
        return configurationId;
    }
}
