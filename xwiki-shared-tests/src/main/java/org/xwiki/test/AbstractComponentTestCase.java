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
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;

/**
 * Tests which needs to have XWiki Components set up should extend this class which makes the Component Manager
 * available. Use this class for JUnit 4.x tests. For JUnit 3.x tests use {@link AbstractXWikiComponentTestCase}
 * instead.
 *
 * Since XWiki 2.2M1 you should prefer using {@link org.xwiki.test.AbstractMockingComponentTestCase} instead.
 */
public class AbstractComponentTestCase extends AbstractMockingTestCase
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    /**
     * Tests that require fine-grained initializations can override this method and not call super.
     */
    @Before
    public void setUp() throws Exception
    {
        this.initializer.initializeConfigurationSource();

        // Put before execution context initialization because it could be needed for some executing context
        // initializer.
        registerComponents();

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
     * Register custom/mock components
     */
    protected void registerComponents() throws Exception
    {
        // Empty voluntarily. Extending classes can override to provide custom component registration.
    }
    
    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory) which can
     *         then be put in the XWiki Context for testing.
     */
    public EmbeddableComponentManager getComponentManager() throws Exception
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
     * @since 3.0M3
     */
    public <T> T registerMockComponent(Class<T> role, String hint, String mockId) throws Exception
    {
        DefaultComponentDescriptor<T> descriptor = createComponentDescriptor(role);
        descriptor.setRoleHint(hint);
        return registerMockComponent(descriptor, mockId);
    }

    /**
     * @since 2.4RC1
     */
    public <T> T registerMockComponent(Class<T> role, String hint) throws Exception
    {
        return registerMockComponent(role, hint, null);
    }

    /**
     * @since 2.4RC1
     */
    public <T> T registerMockComponent(Class<T> role) throws Exception
    {
        return registerMockComponent(createComponentDescriptor(role));
    }

    /**
     * @since 2.4RC1
     */
    private <T> T registerMockComponent(ComponentDescriptor<T> descriptor) throws Exception
    {
        return registerMockComponent(descriptor, null);
    }

    /**
     * @since 3.0M3
     */
    private <T> T registerMockComponent(ComponentDescriptor<T> descriptor, String mockId) throws Exception
    {
        T instance;
        if (mockId != null) {
            instance = getMockery().mock(descriptor.getRole(), mockId);
        } else {
            instance = getMockery().mock(descriptor.getRole());
        }
        getComponentManager().registerComponent(descriptor, instance);
        return instance;
    }

    /**
     * @since 2.4RC1
     */
    private <T> DefaultComponentDescriptor<T> createComponentDescriptor(Class<T> role)
    {
        DefaultComponentDescriptor<T> descriptor = new DefaultComponentDescriptor<T>();
        descriptor.setRole(role);
        return descriptor;
    }
}
