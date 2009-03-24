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
package org.xwiki.test;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.Session;

/**
 * Tests which needs to have XWiki Components set up should extend this class which makes the Component Manager
 * available.
 */
public abstract class AbstractXWikiComponentTestCase extends MockObjectTestCase
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    /**
     * @see #getApplicationContextMock()
     */
    private Mock mockApplicationContext;
    
    /**
     * @see #getRequestMock()
     */
    private Mock mockRequest;
    
    /**
     * @see #getResponseMock()
     */
    private Mock mockResponse;
    
    /**
     * @see #getSessionMock()
     */
    private Mock mockSession;
    
    public AbstractXWikiComponentTestCase()
    {
        super();
    }

    public AbstractXWikiComponentTestCase(String testName)
    {
        super(testName);
    }

    protected void setUp() throws Exception
    {
        // Create a mock Container implementation since we're not running in a real container. We need to do this
        // since some components are using the Container component (for example to access resource such as the XWiki 
        // configuration file).
    	// Tests can use the getter methods to access the mocks and configure them as needed.
        Container container = (Container) getComponentManager().lookup(Container.ROLE);

        this.mockApplicationContext = mock(ApplicationContext.class);
        this.mockApplicationContext.stubs().method("getResource").with(contains("xwiki.properties")).will(
            returnValue(this.getClass().getClassLoader().getResource("xwiki.properties")));
        container.setApplicationContext((ApplicationContext) this.mockApplicationContext.proxy());

        this.mockRequest = mock(Request.class);
        container.setRequest((Request) this.mockRequest.proxy());
        
        this.mockResponse = mock(Response.class);
        container.setResponse((Response) this.mockResponse.proxy());
        
        this.mockSession = mock(Session.class);
        container.setSession((Session) this.mockSession.proxy());
        
        this.initializer.initialize();
    }

    protected void tearDown() throws Exception
    {
        this.initializer.shutdown();
    }
    
    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory) 
     *         which can then be put in the XWiki Context for testing.
     */
    public ComponentManager getComponentManager() throws Exception
    {
        return this.initializer.getComponentManager();
    }

    /**
     * @return the Container {@link ApplicationContext} mock
     */
    public Mock getApplicationContextMock()
    {
        return this.mockApplicationContext;
    }
    
    /**
     * @return the Container {@link Request} mock
     */
    public Mock getRequestMock()
    {
        return this.mockRequest;
    }
    
    /**
     * @return the Container {@link Response} mock
     */
    public Mock getResponseMock()
    {
        return this.mockResponse;
    }
    
    /**
     * @return the Container {@link Session} mock
     */
    public Mock getSessionMock()
    {
        return this.mockSession;
    }
}
