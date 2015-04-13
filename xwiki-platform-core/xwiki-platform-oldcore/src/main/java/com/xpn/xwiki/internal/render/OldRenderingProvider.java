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
package com.xpn.xwiki.internal.render;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Dynamic provider for the default implementation of {@link OldRendering}.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
public class OldRenderingProvider implements Provider<OldRendering>, Initializable
{
    private static final List<Event> LISTENER_EVENTS = Arrays.<Event>asList(new ComponentDescriptorAddedEvent(
        OldRendering.class));

    @Inject
    private ObservationManager observation;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    // Not injecting it directly since it triggers a lot of dependencies.
    // Also we want to possibly find it in extensions
    private OldRendering oldRendering;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        this.observation.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object componentManager, Object descriptor)
            {
                onNewOldRendering((ComponentDescriptor<OldRendering>) descriptor, (ComponentManager) componentManager);
            }

            @Override
            public String getName()
            {
                return "OldRenderingListener";
            }

            @Override
            public List<Event> getEvents()
            {
                return LISTENER_EVENTS;
            }
        });
    }

    void onNewOldRendering(ComponentDescriptor<OldRendering> descriptor, ComponentManager componentManager)
    {
        try {
            this.oldRendering = componentManager.getInstance(OldRendering.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup component [{}]", descriptor, e);
        }
    }

    @Override
    public OldRendering get()
    {
        if (this.oldRendering == null) {
            try {
                this.oldRendering = this.componentManagerProvider.get().getInstance(OldRendering.class);
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to get lookup default implementation of [" + OldRendering.class
                    + "]", e);
            }
        }

        return this.oldRendering;
    }
}
