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
package org.xwiki.component.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDependency;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.util.ReflectionUtils;

/**
 * Constructs a Component Descriptor out of a class definition that contains Annotations.
 * 
 * @version $Id$
 * @since 1.8.1
 * @see ComponentAnnotationLoader
 */
public class ComponentDescriptorFactory
{
    /**
     * Create component descriptors for the passed component implementation class and component role class. There can be
     * more than one descriptor if the component class has specified several hints.
     * 
     * @param componentClass the component implementation class
     * @param componentRoleClass the component role class
     * @return the component descriptors with resolved component dependencies
     */
    public List<ComponentDescriptor> createComponentDescriptors(Class< ? > componentClass,
        Class< ? > componentRoleClass)
    {
        List<ComponentDescriptor> descriptors = new ArrayList<ComponentDescriptor>();

        // If the Component annotation has several hints specified ignore the default hint value and for each specified
        // hint create a Component Descriptor
        String[] hints;
        Component component = componentClass.getAnnotation(Component.class);
        if (component != null && component.hints().length > 0) {
            hints = component.hints();
        } else {
            if (component != null && component.value().trim().length() > 0) {
                hints = new String[] {component.value().trim()};
            } else {
                hints = new String[] {"default"};
            }
        }

        // Create the descriptors
        for (String hint : hints) {
            descriptors.add(createComponentDescriptor(componentClass, hint, componentRoleClass));
        }

        return descriptors;
    }

    /**
     * Create a component descriptor for the passed component implementation class, hint and component role class.
     * 
     * @param componentClass the component implementation class
     * @param hint the hint
     * @param componentRoleClass the component role class
     * @return the component descriptor with resolved component dependencies
     */
    private ComponentDescriptor createComponentDescriptor(Class< ? > componentClass, String hint,
        Class< ? > componentRoleClass)
    {
        DefaultComponentDescriptor descriptor = new DefaultComponentDescriptor();
        descriptor.setRole(componentRoleClass);
        descriptor.setImplementation(componentClass);
        descriptor.setRoleHint(hint);

        // Set the instantiation strategy
        InstantiationStrategy instantiationStrategy = componentClass.getAnnotation(InstantiationStrategy.class);
        if (instantiationStrategy != null) {
            descriptor.setInstantiationStrategy(instantiationStrategy.value());
        } else {
            descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        }

        // Set the requirements.
        // Note: that we need to find all fields since we can have some inherited fields which are annotated in a
        // superclass. Since Java doesn't offer a method to return all fields we have to traverse all parent classes
        // looking for declared fields.
        for (Field field : ReflectionUtils.getAllFields(componentClass)) {
            ComponentDependency dependency = createComponentDependency(field);
            if (dependency != null) {
                descriptor.addComponentDependency(dependency);
            }
        }

        return descriptor;
    }

    /**
     * @param field the field for which to extract a Component Dependency
     * @return the Component Dependency instance created from the passed field
     */
    private ComponentDependency createComponentDependency(Field field)
    {
        DefaultComponentDependency dependency = null;
        Requirement requirement = field.getAnnotation(Requirement.class);
        if (requirement != null) {
            dependency = new DefaultComponentDependency();
            dependency.setMappingType(field.getType());
            dependency.setName(field.getName());

            // Handle case of list or map
            Class< ? > role = getFieldRole(field, requirement);

            if (role == null) {
                return null;
            }

            dependency.setRole(role);

            if (requirement.value().trim().length() > 0) {
                dependency.setRoleHint(requirement.value());
            }

            // Handle hints list when specified
            if (requirement.hints().length > 0) {
                dependency.setHints(requirement.hints());
            }
        }

        return dependency;
    }

    /**
     * Extract component role frol the field to inject.
     * 
     * @param field the field to inject
     * @param requirement the Requirement attribute
     * @return the role of the field to inject
     */
    private Class< ? > getFieldRole(Field field, Requirement requirement)
    {
        Class< ? > role = null;

        // Handle case of list or map
        if (isRequirementListType(field.getType())) {
            // Only add the field to the descriptor if the user has specified a role class different than an
            // Object since we use Object as the default value when no role is specified.
            if (!requirement.role().getName().equals(Object.class.getName())) {
                role = requirement.role();
            } else {
                role = getGenericRole(field);
            }
        } else {
            role = field.getType();
        }

        return role;
    }

    /**
     * Extract generic type from the list field.
     * 
     * @param field the list field to inject
     * @return the role of the components in the list
     */
    private Class< ? > getGenericRole(Field field)
    {
        Type type = field.getGenericType();

        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] types = pType.getActualTypeArguments();
            if (types.length > 0 && types[types.length - 1] instanceof Class) {
                return (Class) types[types.length - 1];
            }
        }

        return null;
    }

    /**
     * @param type the type for which to verify if it's a list or not
     * @return true if the type is a list (Collection or Map), false otherwise
     */
    private boolean isRequirementListType(Class< ? > type)
    {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }
}
