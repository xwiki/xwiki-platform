package org.xwiki.rendering.async.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReference;

/**
 * Share cache containing the results of the {@link AsyncRenderer} executions.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
@Component(roles = AsyncRenderer.class)
@Singleton
public class AsyncRendererCache implements Initializable, CacheEntryListener<AsyncRendererJobStatus>
{
    @Inject
    private CacheManager cacheManager;

    private Cache<AsyncRendererJobStatus> cache;

    private Map<EntityReference, Set<String>> referenceMapping = new ConcurrentHashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.cache = this.cacheManager.createNewCache(new LRUCacheConfiguration("rendering.asyncrenderer", 500));
        } catch (CacheException e) {
            throw new InitializationException("Failed to initialize cache", e);
        }

        this.cache.addCacheEntryListener(this);
    }

    /**
     * @param key the key used to access the value in the cache.
     * @return the value associated with the provided key, or {@code null} if there is no value.
     */
    public AsyncRendererJobStatus get(String key)
    {
        return this.cache.get(key);
    }

    /**
     * Remove all the entries the cache contains.
     */
    public void flush()
    {
        this.cache.removeAll();
    }

    @Override
    public void cacheEntryAdded(CacheEntryEvent<AsyncRendererJobStatus> event)
    {
        CacheEntry<AsyncRendererJobStatus> entry = event.getEntry();
        AsyncRendererJobStatus status = entry.getValue();
        String key = entry.getKey();

        for (EntityReference reference : status.getRequest().getRenderer().getReferences()) {
            this.referenceMapping.computeIfAbsent(reference, k -> new HashSet<String>()).add(key);
        }
    }

    @Override
    public void cacheEntryRemoved(CacheEntryEvent<AsyncRendererJobStatus> event)
    {
        CacheEntry<AsyncRendererJobStatus> entry = event.getEntry();
        AsyncRendererJobStatus status = entry.getValue();
        String key = entry.getKey();

        for (EntityReference reference : status.getRequest().getRenderer().getReferences()) {
            Set<String> keys = this.referenceMapping.get(reference);

            if (keys != null) {
                keys.remove(key);

                if (keys.isEmpty()) {
                    this.referenceMapping.remove(reference);
                }
            }
        }
    }

    @Override
    public void cacheEntryModified(CacheEntryEvent<AsyncRendererJobStatus> event)
    {
        cacheEntryAdded(event);
    }
}
