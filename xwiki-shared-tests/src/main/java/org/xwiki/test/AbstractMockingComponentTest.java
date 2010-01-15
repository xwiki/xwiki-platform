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

import org.jmock.Mockery;
import org.junit.Before;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.test.annotation.ComponentTest;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for Components should extend this class instead of the older
 * {@link org.xwiki.test.AbstractComponentTestCase} test class. This class automatically mocks all dependencies
 * of the component under test. The component under test is defined using a
 * {@link org.xwiki.test.annotation.ComponentTest} annotation.
 *
 * @version $Id$ 
 * @since 2.2M1
 */
public class AbstractMockingComponentTest
{
    private EmbeddableComponentManager componentManager;

    private Mockery mockery = new Mockery();

    @Before
    public void setUp() throws Exception
    {
        this.componentManager = new EmbeddableComponentManager();

        // Step 1: Create a component descriptor for the component under test from its annotations.
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        ComponentDescriptorFactory factory = new ComponentDescriptorFactory();

        ComponentTest componentTestAnnotation = getClass().getAnnotation(ComponentTest.class);
        if (componentTestAnnotation == null) {
            throw new Exception("A component test must use the @ComponentTest annotation");
        }
        Set<Class< ? >> componentRoleClasses = loader.findComponentRoleClasses(componentTestAnnotation.value());

        if (componentRoleClasses.size() > 1) {
            throw new Exception("This test framework only support testing components with a single role for the moment");
        }

        List<ComponentDescriptor> descriptors = factory.createComponentDescriptors(componentTestAnnotation.value(),
            componentRoleClasses.iterator().next());

        if (descriptors.size() > 1) {
            throw new Exception("This test framework only support testing components with a single hint for the moment");
        }

        ComponentDescriptor descriptor = descriptors.get(0);

        // Step 2: For each of its dependencies register a mock implementation
        Collection<ComponentDependency<?>> dependencyDescriptors = descriptor.getComponentDependencies();
        for (ComponentDependency<?> dependencyDescriptor : dependencyDescriptors) {
            DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
            cd.setRole(dependencyDescriptor.getRole());
            cd.setRoleHint(dependencyDescriptor.getRoleHint());
            this.componentManager.registerComponent(cd, getMockery().mock(dependencyDescriptor.getRole()));
        }

        // Step 3: Register the component under test.
        this.componentManager.registerComponent(descriptor);
    }

    /**
     * @return a configured Component Manager
     */
     public EmbeddableComponentManager getComponentManager() throws Exception
    {
        return this.componentManager;
    }

    public Mockery getMockery()
    {
        return this.mockery;
    }
}
