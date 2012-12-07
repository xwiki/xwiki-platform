package com.xpn.xwiki.internal.cache.rendering;


import com.xpn.xwiki.XWikiContext;

public interface RenderingCacheAware {

    /**
     * Get additional infos to store in the cache
     * @param context the current xwiki context
     * @return additional infos for the cache
     */
    public CachedItem.UsedExtension getAdditionalCacheInfos(XWikiContext context);
    
    /**
     * Restore cache info that were previously stored using getAdditionalCacheInfos
     * @param context the current xwiki context
     * @param infos infos to restore from cache
     */
    public void restoreCachedInfos(XWikiContext context, CachedItem.UsedExtension infos);
}
