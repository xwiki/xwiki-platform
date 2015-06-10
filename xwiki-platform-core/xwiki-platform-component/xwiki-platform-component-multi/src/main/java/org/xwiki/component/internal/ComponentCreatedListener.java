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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Make sure to clean any existing context cache when a new {@link org.xwiki.component.manager.ComponentManager} is
 * created.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
@Component
@Named("org.xwiki.component.internal.ComponentCreatedListener")
@Singleton
public class ComponentCreatedListener extends AbstractEventListener
{
    @Inject
    @Named(UserComponentManager.ID)
    private Provider<ComponentManager> userComponentManagerProvider;

    @Inject
    @Named(DocumentComponentManager.ID)
    private Provider<ComponentManager> documentComponentManagerProvider;

    @Inject
    @Named(SpaceComponentManager.ID)
    private Provider<ComponentManager> spaceComponentManagerProvider;

    @Inject
    @Named(WikiComponentManager.ID)
    private Provider<ComponentManager> wikiComponentManagerProvider;

    /**
     * Default constructor.
     */
    public ComponentCreatedListener()
    {
        super(ComponentCreatedListener.class.getName(), new ComponentDescriptorAddedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        notifyComponentManager(this.userComponentManagerProvider);
        notifyComponentManager(this.documentComponentManagerProvider);
        notifyComponentManager(this.spaceComponentManagerProvider);
        notifyComponentManager(this.wikiComponentManagerProvider);
    }

    private void notifyComponentManager(Provider<ComponentManager> provider)
    {
        ComponentManager componentManager = provider.get();

        if (componentManager instanceof AbstractEntityComponentManager) {
            ((AbstractEntityComponentManager) componentManager).onComponentAdded();
        }
    }
}
