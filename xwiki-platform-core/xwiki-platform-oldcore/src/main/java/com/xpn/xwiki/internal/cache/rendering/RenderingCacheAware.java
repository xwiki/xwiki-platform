package com.xpn.xwiki.internal.cache.rendering;

import org.xwiki.component.annotation.Role;
import com.xpn.xwiki.XWikiContext;

/**
 * Specify this component needs special care when caching rendered content.
 * 
 * @version $Id$
 * @since 2.4M1 
 */
@Role
public interface RenderingCacheAware
{
    /**
     * Obtain needed resources for this compoment to successfully restore it from cache.
     * 
     * @param context current xwiki context
     * @return resources needed for restoring this component
     */
    CachedItem.UsedExtension getCacheResources(XWikiContext context);

    /**
     * 
     * @param context current xwiki context
     * @param extension needed resources
     */
    void restoreCacheResources(XWikiContext context, CachedItem.UsedExtension extension);
}
