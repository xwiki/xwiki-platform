package com.xpn.xwiki.internal.cache.rendering;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cached item including any extensions.
 * 
 * @version $Id$
 * @since 6.2
 */
public class CachedItem
{
    /**
     * Rendered content.
     */
    public String rendered;
    
    /**
     * Map containing all extensions used in cached item. 
     */
    public Map<RenderingCacheAware, UsedExtension> extensions = new HashMap<RenderingCacheAware,
            CachedItem.UsedExtension>();

    /**
     * Extension used in cached item.
     * 
     * @version $Id$
     * @since 6.2
     */
    public static class UsedExtension
    {
        /**
         * Needed resources to rebuild extension.
         */
        public Set<String> resources;

        /**
         * Extension parameters.
         */
        public Map<String, Map<String, Object>> parameters;

        /**
         *  
         * @param resources needed resources
         * @param parameters extension parameters
         */
        public UsedExtension(Set<String> resources, Map<String, Map<String, Object>> parameters) {
            this.resources = resources;
            this.parameters = parameters;
        }
    }
}
