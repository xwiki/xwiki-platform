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
package org.xwiki.rendering;

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.PlexusContainerLocator;
import org.jmock.MockObjectTestCase;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.plexus.manager.PlexusComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.DOM;
import org.xwiki.context.ExecutionContextInitializerManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import java.util.List;
import java.io.StringWriter;

public abstract class AbstractRenderingTestCase extends MockObjectTestCase
{
    private ComponentManager componentManager;
    
    protected void setUp() throws Exception
    {
        DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();
        configuration.setContainerConfiguration("/plexus.xml");
        DefaultPlexusContainer container = new DefaultPlexusContainer(configuration);
        PlexusContainerLocator locator = new PlexusContainerLocator(container);
        this.componentManager = new PlexusComponentManager(locator);

        // Initialize the Execution Context
        ExecutionContextInitializerManager ecim =
            (ExecutionContextInitializerManager) getComponentManager().lookup(ExecutionContextInitializerManager.ROLE);
        Execution execution = (Execution) getComponentManager().lookup(Execution.ROLE);
        ExecutionContext ec = new ExecutionContext();
        ecim.initialize(ec);
        execution.setContext(ec);
    }

    protected ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    protected void tearDown() throws Exception
    {
        Execution execution = (Execution) getComponentManager().lookup(Execution.ROLE);
        execution.removeContext();
    }

    protected void assertBlocks(String expected, List<Block> blocks)
    {
        // Assert the result by parsing it through the TestEventsListener to generate easily
        // assertable events.
        DOM dom = new DOM(blocks);
        StringWriter sw = new StringWriter();
        dom.traverse(new TestEventsListener(sw));
        assertEquals(expected, sw.toString());
    }
}
