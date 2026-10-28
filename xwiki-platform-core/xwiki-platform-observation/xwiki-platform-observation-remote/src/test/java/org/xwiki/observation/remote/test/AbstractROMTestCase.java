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

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.when;

/**
 * Base class to easily emulate two (or more) instances of observation manager communicating with each other by
 * network.
 * 
 * @version $Id$
 */
@ComponentTest
public abstract class AbstractROMTestCase
{
    protected MockitoComponentManager componentManager1 = new MockitoComponentManager();

    protected MockitoComponentManager componentManager2 = new MockitoComponentManager();

    private MemoryConfigurationSource configurationSource1;

    private MemoryConfigurationSource configurationSource2;

    private ObservationManager observationManager1;

    private ObservationManager observationManager2;

    @XWikiTempDir
    private File tmpDir;

    @Mock
    private Environment environment1;

    @Mock
    private Environment environment2;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.configurationSource1 = initializeComponentManager(this.componentManager1, this.environment1, "instance1");
        this.configurationSource2 = initializeComponentManager(this.componentManager2, this.environment2, "instance2");

        this.observationManager1 = getComponentManager1().getInstance(ObservationManager.class);
        this.observationManager2 = getComponentManager2().getInstance(ObservationManager.class);
    }

    @AfterEach
    void afterEach() throws Exception
    {
        disposeComponentManager(this.componentManager1);
        disposeComponentManager(this.componentManager2);

        // Make sure we mark the component manager for garbage collection as otherwise each JUnit test will
        // have an instance of the Component Manager (will all the components it's holding), leading to
        // out of memory errors when there are lots of tests...
        this.componentManager1 = null;
        this.componentManager2 = null;
    }

    /**
     * Initialize a component manager emulating one more instance of the observation manager, with remote events
     * enabled.
     *
     * @param componentManager the component manager to initialize
     * @param environment the (mock) environment to register in the component manager
     * @param instanceName the name of the emulated instance, each instance needs its own permanent directory (where
     *            the identifier of the instance is stored)
     * @return the configuration source to use to configure the instance
     * @throws Exception when failing to initialize the component manager
     */
    protected MemoryConfigurationSource initializeComponentManager(MockitoComponentManager componentManager,
        Environment environment, String instanceName) throws Exception
    {
        componentManager.initializeTest(this);
        initializeExecution(componentManager);
        MemoryConfigurationSource configurationSource = componentManager.registerMemoryConfigurationSource();

        configurationSource.setProperty("observation.remote.enabled", Boolean.TRUE);

        DefaultComponentDescriptor<Environment> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Environment.class);
        componentDescriptor.setRoleHint("default");
        componentManager.registerComponent(componentDescriptor, environment);

        when(environment.getPermanentDirectory()).thenReturn(new File(this.tmpDir, instanceName));

        return configurationSource;
    }

    /**
     * Dispose a component manager initialized with
     * {@link #initializeComponentManager(MockitoComponentManager, Environment, String)}, emulating the stop of an
     * instance.
     *
     * @param componentManager the component manager to dispose
     * @throws Exception when failing to dispose the component manager
     */
    protected void disposeComponentManager(MockitoComponentManager componentManager) throws Exception
    {
        cleanExecution(componentManager);

        // Clean possible resources some components might hold
        componentManager.dispose();
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
