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

import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;

/**
 * Initialize a component manager used in unit tests.
 * 
 * @version $Id$
 */
public class XWikiComponentInitializer
{
    private EmbeddableComponentManager componentManager;

    private MockConfigurationSource configurationSource;

    public void initializeConfigurationSource() throws Exception
    {
        // Register the mock configuration source for different roles so that tests always use the mock
        this.configurationSource = new MockConfigurationSource();

        DefaultComponentDescriptor<ConfigurationSource> descriptor;

        descriptor = new DefaultComponentDescriptor<ConfigurationSource>();
        descriptor.setRole(ConfigurationSource.class);
        getComponentManager().registerComponent(descriptor, this.configurationSource);

        descriptor = new DefaultComponentDescriptor<ConfigurationSource>();
        descriptor.setRole(ConfigurationSource.class);
        descriptor.setRoleHint("xwikiproperties");
        getComponentManager().registerComponent(descriptor, this.configurationSource);
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
     * @return a configured Component Manager
     */
    public EmbeddableComponentManager getComponentManager() throws Exception
    {
        if (this.componentManager == null) {
            EmbeddableComponentManager ecm = new EmbeddableComponentManager();
            ecm.initialize(this.getClass().getClassLoader());
            this.componentManager = ecm;
        }

        return this.componentManager;
    }

    /**
     * @return a modifiable mock configuration source
     */
    public MockConfigurationSource getConfigurationSource()
    {
        return configurationSource;
    }
}
