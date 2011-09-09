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
package org.xwiki.component.internal.embed;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.internal.ComponentManagerFactory;
import org.xwiki.component.manager.ComponentManager;

/**
 * Create Component Manager implementation based on the Embeddable Component Manager
 * (i.e. a simple implementation of {@link ComponentManager} to be used when using 
 * some XWiki modules standalone).
 * 
 * @version $Id$
 * @since 2.1RC1
 */
@Component
@Singleton
public class EmbeddableComponentManagerFactory implements ComponentManagerFactory
{
    /**
     * The Root Component Manager used to get access to the set Component Event Manager that we
     * set by default for newly created Component Managers.
     */
    @Inject
    private ComponentManager rootComponentManager;

    @Override
    public ComponentManager createComponentManager(ComponentManager parentComponentManager)
    {
        ComponentManager cm = new EmbeddableComponentManager();
     
        // Set the parent
        cm.setParent(parentComponentManager);
        
        // Make sure the Event Manager is set so that events can be sent
        cm.setComponentEventManager(this.rootComponentManager.getComponentEventManager());
        
        return cm;
    }
}
