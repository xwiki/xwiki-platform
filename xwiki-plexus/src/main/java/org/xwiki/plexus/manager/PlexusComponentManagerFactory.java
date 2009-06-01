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
 *
 */
package org.xwiki.plexus.manager;

import org.codehaus.plexus.PlexusContainer;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.manager.ComponentManager;

/**
 * Helper class to create a XWiki Component Manager based on Plexus. It registers all components declared
 * usign annnotations as Plexus components.
 *  
 * @version $Id$
 * @since 2.0M1
 */
public class PlexusComponentManagerFactory
{
    public ComponentManager createComponentManager(PlexusContainer plexusContainer)
    {
        ComponentManager componentManager = new PlexusComponentManager(plexusContainer);
        new ComponentAnnotationLoader().initialize(componentManager, this.getClass().getClassLoader());
        
        // TODO: This should work but it doesn't. It only works for components declared in components.xml
        /*
        try {
            ObservationManager om = componentManager.lookup(ObservationManager.class);
            plexusContainer.registerComponentDiscoveryListener(new PlexusComponentDiscoveryListener(om));
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to register component discovery listener", e);
        }
        */
     
        return componentManager;
    }
}
