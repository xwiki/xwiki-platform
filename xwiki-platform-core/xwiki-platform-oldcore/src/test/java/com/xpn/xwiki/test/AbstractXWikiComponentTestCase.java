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

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.test.jmock.MockingComponentManager;
import org.xwiki.test.jmock.XWikiComponentInitializer;

/**
 * Tests which needs to have XWiki Components set up should extend this class which makes the Component Manager
 * available. Use this class for JUnit 3.x tests. For JUnit 4.x tests use {@link org.xwiki.test.ComponentManagerRule}
 * instead.
 *
 * @deprecated use JUnit 4.x and {@link org.xwiki.test.ComponentManagerRule}
 */
@Deprecated
public abstract class AbstractXWikiComponentTestCase extends MockObjectTestCase
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    private Mock contextualAuthorizationManager;
    private Mock authorizationManager;

    public AbstractXWikiComponentTestCase()
    {
        super();
    }

    public AbstractXWikiComponentTestCase(String testName)
    {
        super(testName);
    }

    /**
     * Tests that require fine-grained initializations can override this method and not call super.
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        this.initializer.initializeConfigurationSource();

        // Put before execution context initialization because it could be needed for some executing context
        // initializer.
        registerComponents();

        this.initializer.initializeExecution();
    }

    /**
     * Register custom/mock components
     */
    protected void registerComponents() throws Exception
    {
        this.contextualAuthorizationManager = this.registerMockComponent(ContextualAuthorizationManager.class);
        this.authorizationManager = this.registerMockComponent(AuthorizationManager.class);

        this.contextualAuthorizationManager.stubs().method("hasAccess").will(returnValue(true));
        this.authorizationManager.stubs().method("hasAccess").will(returnValue(true));

        // Extending classes can override to provide custom component registration.
    }

    @Override
    protected void tearDown() throws Exception
    {
        this.initializer.shutdown();
    }

    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory) which can
     *         then be put in the XWiki Context for testing.
     */
    public MockingComponentManager getComponentManager() throws Exception
    {
        return this.initializer.getComponentManager();
    }

    /**
     * @return a modifiable mock configuration source
     */
    public MockConfigurationSource getConfigurationSource()
    {
        return this.initializer.getConfigurationSource();
    }

    /**
     * @return a modifiable mock contextual authorization manager.
     */
    public Mock getContextualAuthorizationManager()
    {
        return contextualAuthorizationManager;
    }

    /**
     * @return a modifiable mock authorization manager.
     */
    public Mock getAuthorizationManager()
    {
        return authorizationManager;
    }

    /**
     * @since 2.4M2
     */
    public <T> Mock registerMockComponent(Class<T> role, String hint) throws Exception
    {
        DefaultComponentDescriptor<T> descriptor = createComponentDescriptor(role);
        descriptor.setRoleHint(hint);
        return registerMockComponent(descriptor);
    }

    /**
     * @since 2.4M2
     */
    public <T> Mock registerMockComponent(Class<T> role) throws Exception
    {
        return registerMockComponent(createComponentDescriptor(role));
    }

    /**
     * @since 2.4M2
     */
    private <T> Mock registerMockComponent(ComponentDescriptor<T> descriptor) throws Exception
    {
        Mock mock = mock(descriptor.getRole());
        getComponentManager().registerComponent(descriptor, (T) mock.proxy());
        return mock;
    }

    /**
     * @since 2.4M2
     */
    private <T> DefaultComponentDescriptor<T> createComponentDescriptor(Class<T> role)
    {
        DefaultComponentDescriptor<T> descriptor = new DefaultComponentDescriptor<T>();
        descriptor.setRoleType(role);
        return descriptor;
    }
}
