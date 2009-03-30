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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDependency;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

/**
 * Constructs a Component Descriptor out of a class definition that contains Annotations.
 * 
 * @version $Id: $
 * @since 1.8.1
 * @see ComponentAnnotationLoader
 */
public class ComponentDescriptorFactory
{
    /**
     * Create a component descriptor for the passed component implementation class and component role class.
     * 
     * @param componentClass the component implementation class
     * @param componentRoleClass the component role class
     * @return the component descriptor with resolved component dependencies
     */
    public ComponentDescriptor createComponentDescriptor(Class< ? > componentClass, Class< ? > componentRoleClass)
    {
        DefaultComponentDescriptor descriptor = new DefaultComponentDescriptor();
        descriptor.setRole(componentRoleClass.getName());
        descriptor.setImplementation(componentClass.getName());
        
        // Set the hint if it exists
        Component component = componentClass.getAnnotation(Component.class);
        if (component != null && component.value().trim().length() > 0) {
            descriptor.setRoleHint(component.value());
        } else {
            // The descriptor sets a default hint by default. 
        }

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
        for (Field field : getAllFields(componentClass)) {
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
            if (isRequirementListType(field.getType())) {
                // Only add the field to the descriptor if the user has specified a role class different than an
                // Object since we use Object as the default value when no role is specified.
                if (!requirement.role().getName().equals(Object.class.getName())) {
                    dependency.setRole(requirement.role().getName());
                } else {
                    return null;
                }
            } else {
                dependency.setRole(field.getType().getName());
            }

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
     * @param type the type for which to verify if it's a list or not
     * @return true if the type is a list (Collection or Map), false otherwise
     */
    private boolean isRequirementListType(Class< ? > type)
    {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    /**
     * @param componentClass the class for which to return all fields
     * @return all fields declared by the passed class and its superclasses
     */
    private Collection<Field> getAllFields(Class< ? > componentClass)
    {
        // Note: use a linked hash map to keep the same order as the one used to declare the fields.
        Map<String, Field> fields = new LinkedHashMap<String, Field>();
        Class< ? > targetClass = componentClass;
        while (targetClass != null) {
            for (Field field : targetClass.getDeclaredFields()) {
                // Make sure that if the same field is declared in a class and its superclass
                // only the field used in the class will be returned. Note that we need to do
                // this check since the Field object doesn't implement the equals method using 
                // the field name.
                if (!fields.containsKey(field.getName())) {
                    fields.put(field.getName(), field);
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        return fields.values();
    }
}
