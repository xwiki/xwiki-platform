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
                this.componentManager.getInstance(CacheFactory.class, "infinispan");

            cacheFactory.getCacheManager().stop();
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup [" + InfinispanCacheFactory.class + "]", e);
        }
    }
}
