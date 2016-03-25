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
import org.xwiki.environment.Environment;

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
     * The environment. Used for example to access temporary folder.
     */
    private Environment environment;

    /**
     * The default configuration identifier used to load cache configuration file.
     */
    private String defaultPropsId;

    /**
     * @param configuration the XWiki cache API configuration.
     * @param defaultPropsId the default configuration identifier used to load cache configuration file.
     */
    public AbstractCacheConfigurationLoader(CacheConfiguration configuration, String defaultPropsId)
    {
        this(configuration, null, defaultPropsId);
    }

    /**
     * @param configuration the XWiki cache API configuration.
     * @param environment the environment, can be null
     * @param defaultPropsId the default configuration identifier used to load cache configuration file.
     */
    public AbstractCacheConfigurationLoader(CacheConfiguration configuration, Environment environment,
        String defaultPropsId)
    {
        this.configuration = (CacheConfiguration) configuration.clone();
        this.environment = environment;
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
     * @return the path of the temporary local folder based on configuration identifier
     */
    protected String createTempDir()
    {
        String path = (String) this.configuration.get(CONFX_CACHE_PATH);

        if (path == null) {
            File file;
            if (this.environment != null) {
                file = new File(this.environment.getTemporaryDirectory().getAbsolutePath(), "cache");
            } else {
                file = new File(System.getProperty("java.io.tmpdir"), "xwiki");
            }

            if (this.configuration.getConfigurationId() == null) {
                file = new File(file, this.configuration.getConfigurationId());
            }

            if (!file.exists()) {
                file.mkdirs();
            }

            path = file.getAbsolutePath();
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
