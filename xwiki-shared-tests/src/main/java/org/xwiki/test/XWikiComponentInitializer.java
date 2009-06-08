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
package org.xwiki.test;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;

public class XWikiComponentInitializer
{
    private ComponentManager componentManager;

    private ComponentAnnotationLoader componentAnnotationInitializer = new ComponentAnnotationLoader();
    
    /**
     * This method should be called before {@link #initializeExecution()} since some components will require the Container
     * component to be set up (for example to access resource such as the XWikiconfiguration file).
     */
    public void initializeContainer() throws Exception
    {
        Container container = getComponentManager().lookup(Container.class);

        // We set up a default stubbed ApplicationContext implementation. Tests that need a more controlled
        // version can use Mocks and call setApplicationContext(), setRequest(), etc on the Container object.
        container.setApplicationContext(new ApplicationContext()
        {
            public URL getResource(String resourceName) throws MalformedURLException
            {
                if (resourceName.contains("xwiki.properties")) {
                    return this.getClass().getClassLoader().getResource("xwiki.properties");
                }
                throw new RuntimeException("Not implemented");
            }

            public InputStream getResourceAsStream(String resourceName)
            {
                throw new RuntimeException("Not implemented");
            }

            public File getTemporaryDirectory()
            {
                throw new RuntimeException("Not implemented");
            }
        });
    }
    
    public void initializeExecution() throws Exception
    {
        // Initialize the Execution Context
        ExecutionContextManager ecm = getComponentManager().lookup(ExecutionContextManager.class);
        Execution execution = getComponentManager().lookup(Execution.class);

        ExecutionContext ec = new ExecutionContext();

        // Make sure we push this empty context in the Execution component before we call the initialization
        // so that we don't get any NPE if some initializer code asks to get the Execution Context. This 
        // happens for example with the Velocity Execution Context initializer which in turns calls the Velocity
        // Context initializers and some of them look inside the Execution Context.
        execution.setContext(ec);
        
        ecm.initialize(ec);
    }

    public void shutdown() throws Exception
    {
        Execution execution = getComponentManager().lookup(Execution.class);
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
            EmbeddableComponentManager ecm = new EmbeddableComponentManager(this.getClass().getClassLoader());
            ecm.initialize();
            this.componentManager = ecm;
        }

        return this.componentManager;
    }
}
