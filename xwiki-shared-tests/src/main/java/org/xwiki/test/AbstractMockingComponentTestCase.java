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

import org.junit.Before;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.annotation.RequirementMock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for Components should extend this class instead of the older
 * {@link org.xwiki.test.AbstractComponentTestCase} test class.
 *
 * To use this class, define one or several fields annotated with {@link RequirementMock}. These fields will have all
 * their dependencies mocked.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class AbstractMockingComponentTestCase extends AbstractMockingTestCase
{
    private EmbeddableComponentManager componentManager;

    private ComponentAnnotationLoader loader = new ComponentAnnotationLoader();

    private ComponentDescriptorFactory factory = new ComponentDescriptorFactory();

    @Before
    public void setUp() throws Exception
    {
        this.componentManager = new EmbeddableComponentManager();

        // Step 1: Register all components available
        this.loader.initialize(this.componentManager, getClass().getClassLoader());

        // Step 2: Inject all fields annotated with @RequirementMock.
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            RequirementMock requirementMock = field.getAnnotation(RequirementMock.class);
            if (requirementMock != null) {
                Class< ? > componentRoleClass = findComponentRoleClass(field, requirementMock);
                for (ComponentDescriptor descriptor :
                    factory.createComponentDescriptors(field.getType(), componentRoleClass))
                {
                    // Only use the descriptor for the specified hint
                    if ((requirementMock.hint().length() > 0 && requirementMock.hint().equals(descriptor.getRoleHint()))
                      || requirementMock.hint().length() == 0)
                    {
                        registerMockDependencies(descriptor, requirementMock);
                        getComponentManager().registerComponent(descriptor);
                        ReflectionUtils.setFieldValue(this, field.getName(),
                            getComponentManager().lookup(descriptor.getRole(), descriptor.getRoleHint()));
                        break;
                    }
                }
            }
        }
    }

    private void registerMockDependencies(ComponentDescriptor descriptor, RequirementMock requirementMock)
        throws Exception
    {
        List<Class < ? >> exceptions = Arrays.asList(requirementMock.exceptions());
        Collection<ComponentDependency<?>> dependencyDescriptors = descriptor.getComponentDependencies();
        for (ComponentDependency<?> dependencyDescriptor : dependencyDescriptors) {
            // Only register a mock if it isn't an exception
            // TODO: Handle multiple roles/hints.
            if (!exceptions.contains(dependencyDescriptor.getRole())) {
                DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
                cd.setRole(dependencyDescriptor.getRole());
                cd.setRoleHint(dependencyDescriptor.getRoleHint());
                this.componentManager.registerComponent(cd, getMockery().mock(dependencyDescriptor.getRole()));
            }
        }
    }

    private Class< ? > findComponentRoleClass(Field field, RequirementMock requirementMock)
    {
        Class< ? > componentRoleClass;

        Set<Class< ? >> componentRoleClasses = this.loader.findComponentRoleClasses(field.getType());
        if (!Object.class.getName().equals(requirementMock.value().getName())) {
            if (!componentRoleClasses.contains(requirementMock.value())) {
                throw new RuntimeException("Specified Component Role not found in component");
            } else {
                componentRoleClass = requirementMock.value();
            }
        } else {
            if (componentRoleClasses.size() > 1) {
                throw new RuntimeException(
                    "Components with several roles must explicitely specify which role to use.");
            } else {
                componentRoleClass = componentRoleClasses.iterator().next();
            }
        }
        return componentRoleClass;        
    }

    /**
     * @return a configured Component Manager
     */
     public EmbeddableComponentManager getComponentManager() throws Exception
    {
        return this.componentManager;
    }
}
