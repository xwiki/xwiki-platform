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
package org.xwiki.plexus.container;

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.plexus.manager.PlexusComponentManager;

/**
 * Utilities to manipulate Plexus Containers.
 * 
 * @version $Id:$
 * @since 2.0M1
 */
public class PlexusContainerUtils
{
    public static ComponentManager initializePlexus() throws PlexusContainerException
    {
        DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();
        DefaultPlexusContainer container = new DefaultPlexusContainer(configuration);

        // Register the XWiki Lifecycle Handler
        // TODO: Can only be used with more recent Plexus versions. When this is done the plexus.xml file
        //       can be removed.
        /*
        LifecycleHandler handler = new XWikiLifecycleHandler();
        handler.addBeginSegment(new LogEnablePhase());
        handler.addBeginSegment(new ComposePhase());
        handler.addBeginSegment(new InitializePhase());
        configuration.addLifecycleHandler(handler);
        */
        
        return new PlexusComponentManager(container);
    }
}
