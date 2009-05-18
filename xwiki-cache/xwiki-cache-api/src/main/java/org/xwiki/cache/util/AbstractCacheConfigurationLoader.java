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
