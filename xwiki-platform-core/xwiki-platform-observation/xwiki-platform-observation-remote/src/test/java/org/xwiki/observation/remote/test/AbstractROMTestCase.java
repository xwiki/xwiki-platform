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

import org.junit.Before;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.MockConfigurationSource;
import org.xwiki.test.XWikiComponentInitializer;

/**
 * Base class to easily emulate two instances of observation manager communicate with each other by network.
 * 
 * @version $Id$
 */
public abstract class AbstractROMTestCase
{
    private XWikiComponentInitializer initializer1 = new XWikiComponentInitializer();

    private XWikiComponentInitializer initializer2 = new XWikiComponentInitializer();

    private ObservationManager observationManager1;

    private ObservationManager observationManager2;

    @Before
    public void setUp() throws Exception
    {
        this.initializer1.initializeConfigurationSource();
        this.initializer1.initializeExecution();

        this.initializer2.initializeConfigurationSource();
        this.initializer2.initializeExecution();

        getConfigurationSource1().setProperty("observation.remote.enabled", Boolean.TRUE);
        getConfigurationSource2().setProperty("observation.remote.enabled", Boolean.TRUE);

        this.observationManager1 = getComponentManager1().getInstance(ObservationManager.class);
        this.observationManager2 = getComponentManager2().getInstance(ObservationManager.class);
    }

    public EmbeddableComponentManager getComponentManager1() throws Exception
    {
        return this.initializer1.getComponentManager();
    }

    public EmbeddableComponentManager getComponentManager2() throws Exception
    {
        return this.initializer2.getComponentManager();
    }

    /**
     * @return a modifiable mock configuration source
     */
    public MockConfigurationSource getConfigurationSource1()
    {
        return this.initializer1.getConfigurationSource();
    }

    /**
     * @return a modifiable mock configuration source
     */
    public MockConfigurationSource getConfigurationSource2()
    {
        return this.initializer2.getConfigurationSource();
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
