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

import org.junit.After;
import org.junit.Before;
import org.xwiki.component.embed.EmbeddableComponentManager;

/**
 * Tests which needs to have XWiki Components set up should extend this class which makes the Component Manager
 * available. Use this class for JUnit 4.x tests. For JUnit 3.x tests use {@link AbstractComponentTestCase— instead.
 */
public class AbstractComponentTestCase
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    /**
     * Tests that require fine-grained initializations can override this method and not call super. 
     */
    @Before
    public void setUp() throws Exception
    {
        this.initializer.initializeContainer();
        this.initializer.initializeExecution();
    }

    /**
     * Clean up test states.
     */
    @After
    public void tearDown() throws Exception
    {
        this.initializer.shutdown();
    }

    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory) which can
     *         then be put in the XWiki Context for testing.
     */
    public EmbeddableComponentManager getComponentManager() throws Exception
    {
        return this.initializer.getComponentManager();
    }
}
