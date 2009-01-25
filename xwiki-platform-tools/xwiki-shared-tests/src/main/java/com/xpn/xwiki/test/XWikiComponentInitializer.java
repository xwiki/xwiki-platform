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
package com.xpn.xwiki.test;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.plexus.manager.PlexusComponentManager;
import org.xwiki.context.ExecutionContextInitializerManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;

public class XWikiComponentInitializer
{
    private ComponentManager componentManager;

    public void initialize() throws Exception
    {
        // Initialize the Execution Context
        ExecutionContextInitializerManager ecim =
            (ExecutionContextInitializerManager) getComponentManager().lookup(ExecutionContextInitializerManager.ROLE);
        Execution execution = (Execution) getComponentManager().lookup(Execution.ROLE);

        ExecutionContext ec = new ExecutionContext();

        ecim.initialize(ec);
        execution.setContext(ec);
    }

    public void shutdown() throws Exception
    {
        Execution execution = (Execution) getComponentManager().lookup(Execution.ROLE);
        execution.removeContext();

        // Make sure we mark the component manager for garbage collection as otherwise each JUnit test will
        // have an instance of the Component Manager (will all the components it's holding), leading to
        // out of memory errors when there are lots of tests...
        this.componentManager = null;
    }

    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory)
     *         which can then be put in the XWiki Context for testing.
     */
    public ComponentManager getComponentManager() throws Exception
    {
        if (this.componentManager == null) {
            DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();
            configuration.setContainerConfiguration("/plexus.xml");
            DefaultPlexusContainer container = new DefaultPlexusContainer(configuration);
            this.componentManager = new PlexusComponentManager(container);
        }

        return this.componentManager;
    }
}
