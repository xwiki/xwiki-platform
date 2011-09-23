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
package org.xwiki.cache.util;

import java.io.File;

import org.xwiki.cache.config.CacheConfiguration;

/**
 * Base class to load cache configuration.
 * 
 * @version $Id$
 */
public abstract class AbstractCacheConfigurationLoader
{
    /**
     * The name of the cache.path property in XWiki configuration.
     */
    private static final String CONFX_CACHE_PATH = "cache.path";

    /**
     * The XWiki cache API configuration.
     */
    private CacheConfiguration configuration;

    /**
     * The default configuration identifier used to load cache configuration file.
     */
    private String defaultPropsId = "default";

    /**
     * @param configuration the XWiki cache API configuration.
     * @param defaultPropsId the default configuration identifier used to load cache configuration file.
     */
    public AbstractCacheConfigurationLoader(CacheConfiguration configuration, String defaultPropsId)
    {
        this.configuration = configuration;
        this.defaultPropsId = defaultPropsId;
    }

    /**
     * @return the XWiki cache API configuration.
     */
    public CacheConfiguration getCacheConfiguration()
    {
        return this.configuration;
    }

    /**
     * @return the patch of the temporary local folder based on configuration identifier.
     */
    protected String createTempDir()
    {
        String path = (String) this.configuration.get(CONFX_CACHE_PATH);

        if (path == null) {
            path = System.getProperty("java.io.tmpdir") + File.separator + "xwiki";
            if (this.configuration.getConfigurationId() == null) {
                path += File.separator + this.configuration.getConfigurationId() + File.separator;
            }

            File tempDir = new File(path);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
        }

        return path;
    }

    /**
     * @return the default configuration identifier used to load cache configuration file.
     */
    public String getDefaultPropsId()
    {
        return this.defaultPropsId;
    }
}
