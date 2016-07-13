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
package org.xwiki.component.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.model.EntityType;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Make sure to dispose the {@link ComponentManager} associated to a deleted document.
 * 
 * @version $Id$
 * @since 5.2RC1
 */
@Component
@Singleton
@Named("component.multi.DocumentDeletedListener")
public class DocumentDeletedListener implements EventListener
{
    /**
     * The list of events observed.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentDeletedEvent());

    /**
     * Store and provide {@link ComponentManager} instances.
     */
    @Inject
    private ComponentManagerManager componentManagerManager;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "component.multi.DocumentDeletedListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object o, Object context)
    {
        String document = ((DocumentDeletedEvent) event).getEventFilter().getFilter();

        ComponentManager componentManager =
            this.componentManagerManager.getComponentManager(EntityType.DOCUMENT.toString().toLowerCase() + ':'
                + document, false);

        if (componentManager instanceof Disposable) {
            try {
                ((Disposable) componentManager).dispose();
            } catch (ComponentLifecycleException e) {
                this.logger.error(String.format("Failed to dispose component manager for document [%s]", document), e);
            }
        }
    }
}
