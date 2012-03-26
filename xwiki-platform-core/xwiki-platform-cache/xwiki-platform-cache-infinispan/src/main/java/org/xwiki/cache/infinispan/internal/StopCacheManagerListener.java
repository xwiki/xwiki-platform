package org.xwiki.cache.infinispan.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.CacheFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;

/**
 * Listen to {@link ApplicationStoppedEvent} to stop the Infinispan CacheManager.
 * 
 * @version $Id$
 */
@Component
@Named("cache.infinispan.StopCacheManager")
@Singleton
public class StopCacheManagerListener implements EventListener
{
    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ApplicationStoppedEvent());

    /**
     * Used to lookup the cache factory.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "cache.infinispan.StopCacheManager";
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        try {
            InfinispanCacheFactory cacheFactory =
                this.componentManager.lookupComponent(CacheFactory.class, "infinispan");

            cacheFactory.getCacheManager().stop();
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup [" + InfinispanCacheFactory.class + "]", e);
        }
    }
}
