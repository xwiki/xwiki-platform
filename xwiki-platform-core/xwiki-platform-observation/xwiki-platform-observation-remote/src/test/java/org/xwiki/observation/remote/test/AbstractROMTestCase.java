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
package org.xwiki.observation.remote.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * Base class to easily emulate two instances of observation manager communicate with each other by network.
 * 
 * @version $Id$
 */
public abstract class AbstractROMTestCase
{
    protected MockitoComponentManager componentManager1 = new MockitoComponentManager();

    protected MockitoComponentManager componentManager2 = new MockitoComponentManager();

    private MemoryConfigurationSource configurationSource1;

    private MemoryConfigurationSource configurationSource2;

    private ObservationManager observationManager1;

    private ObservationManager observationManager2;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.componentManager1.initializeTest(this);
        initializeExecution(this.componentManager1);
        this.configurationSource1 = this.componentManager1.registerMemoryConfigurationSource();

        this.componentManager2.initializeTest(this);
        initializeExecution(this.componentManager2);
        this.configurationSource2 = this.componentManager2.registerMemoryConfigurationSource();

        getConfigurationSource1().setProperty("observation.remote.enabled", Boolean.TRUE);
        getConfigurationSource2().setProperty("observation.remote.enabled", Boolean.TRUE);

        this.observationManager1 = getComponentManager1().getInstance(ObservationManager.class);
        this.observationManager2 = getComponentManager2().getInstance(ObservationManager.class);
    }

    @AfterEach
    private void afterEach() throws Exception
    {
        cleanExecution(this.componentManager1);
        cleanExecution(this.componentManager2);

        // Clean possible resources some components might hold
        this.componentManager1.dispose();
        this.componentManager2.dispose();

        // Make sure we mark the component manager for garbage collection as otherwise each JUnit test will
        // have an instance of the Component Manager (will all the components it's holding), leading to
        // out of memory errors when there are lots of tests...
        this.componentManager1 = null;
        this.componentManager2 = null;
    }

    private void initializeExecution(MockitoComponentManager componentManager)
        throws ComponentLookupException, ExecutionContextException
    {
        ExecutionContextManager ecm = componentManager.getInstance(ExecutionContextManager.class);

        ExecutionContext ec = new ExecutionContext();

        ecm.initialize(ec);
    }

    private void cleanExecution(MockitoComponentManager componentManager) throws ComponentLookupException
    {
        Execution execution = componentManager.getInstance(Execution.class);
        execution.removeContext();
    }

    public EmbeddableComponentManager getComponentManager1() throws Exception
    {
        return this.componentManager1;
    }

    public EmbeddableComponentManager getComponentManager2() throws Exception
    {
        return this.componentManager2;
    }

    /**
     * @return a modifiable configuration source
     */
    public MemoryConfigurationSource getConfigurationSource1()
    {
        return this.configurationSource1;
    }

    /**
     * @return a modifiable configuration source
     */
    public MemoryConfigurationSource getConfigurationSource2()
    {
        return this.configurationSource2;
    }

    public ObservationManager getObservationManager1()
    {
        return this.observationManager1;
    }

    public ObservationManager getObservationManager2()
    {
        return this.observationManager2;
    }
}
