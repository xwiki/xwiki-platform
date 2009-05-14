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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.internal.RoleHint;
import org.xwiki.component.manager.ComponentManager;

/**
 * Dynamically loads all components defined using Annotations and declared in META-INF/components.txt files.
 * 
 * @version $Id$
 * @since 1.8.1
 */
public class ComponentAnnotationLoader
{
    /**
     * Location in the classloader of the file defining the list of component implementation class to parser for 
     * annotations.
     */
    private static final String COMPONENT_LIST = "META-INF/components.txt";

    /**
     * Factory to create a Component Descriptor from an annotated class.
     */
    private ComponentDescriptorFactory factory = new ComponentDescriptorFactory();
    
    /**
     * Loads all components defined using annotations.
     * 
     * @param manager the component manager to use to dynamically register components
     * @param classLoader the classloader to use to look for the Component list declaration file 
     *        ({@code META-INF/components.txt})
     */
    public void initialize(ComponentManager manager, ClassLoader classLoader)
    {
        try {
            // 1) Find all components by retrieving the list defined in COMPONENT_LIST
            List<String> componentClassNames = findAnnotatedComponentClasses(classLoader);

            // 2) For each component class name found, load its class and use introspection to find the necessary 
            //    annotations required to create a Component Descriptor.
            Map<RoleHint, ComponentDescriptor> descriptorMap = new HashMap<RoleHint, ComponentDescriptor>();
            for (String componentClassName : componentClassNames) {
                Class< ? > componentClass = classLoader.loadClass(componentClassName);

                // Look for ComponentRole annotations and register one component per ComponentRole found
                for (Class< ? > componentRoleClass : findComponentRoleClasses(componentClass)) {
                    for (ComponentDescriptor descriptor : factory.createComponentDescriptors(componentClass,
                        componentRoleClass))
                    {
                        // If there's already a existing role/hint in the list of descriptors then decide which one 
                        // to keep.
                        if (descriptorMap.containsKey(new RoleHint(componentRoleClass, descriptor.getRoleHint()))) {
                            // For now the last override the first
                            // TODO: Log a message in debug mode
                            // SOP.println("WARNING: component [" + descriptor.getImplementation() + "] for role [" 
                            //    + componentRoleClass.getName() + "] and hint [" + descriptor.getRoleHint() 
                            //    + "] has already been registered with implementation [" + descriptors.get(
                            //        new RoleHint(componentRoleClass, descriptor.getRoleHint())).getImplementation() 
                            //        + "]. Ignoring it.");
                            descriptorMap.put(new RoleHint(componentRoleClass, descriptor.getRoleHint()), descriptor);
                        } else {
                            descriptorMap.put(new RoleHint(componentRoleClass, descriptor.getRoleHint()), descriptor);
                        }
                    }
                }
            }

            // 3) Activate all component descriptors
            for (ComponentDescriptor descriptor : descriptorMap.values()) {
                manager.registerComponent(descriptor);
            }
            
        } catch (Exception e) {
            // Make sure we make the calling code fail in order to fail fast and prevent the application to start
            // if something is amiss.
            throw new RuntimeException("Failed to dynamically load components with annotations", e);
        }
    }

    /**
     * Finds the interfaces that implement component roles by looking recursively in all interfaces of
     * the passed component implementation class. If the roles annotation value is specified then use 
     * the specified list instead of doing auto-discovery.
     * 
     * @param componentClass the component implementation class for which to find the component roles it implements 
     * @return the list of component role classes implemented
     */
    protected List<Class< ? >> findComponentRoleClasses(Class< ? > componentClass)
    {
        List<Class< ? >> classes = new ArrayList<Class< ? >>();
       
        Component component = componentClass.getAnnotation(Component.class);
        if (component != null && component.roles().length > 0) {
            classes.addAll(Arrays.asList(component.roles()));
        } else {
            // Look in both superclass and interfaces for @ComponentRole.
            for (Class< ? > interfaceClass : componentClass.getInterfaces()) {
                classes.addAll(findComponentRoleClasses(interfaceClass));
                for (Annotation annotation : interfaceClass.getDeclaredAnnotations()) {
                    if (annotation.annotationType().getName().equals(ComponentRole.class.getName())) {
                        classes.add(interfaceClass);
                    }
                }
            }
            
            // Note that we need to look into the superclass since the super class can itself implements an interface 
            // that has the @ComponentRole annotation.
            Class< ? > superClass = componentClass.getSuperclass();
            if (superClass != null && !superClass.getName().equals(Object.class.getName())) {
                classes.addAll(findComponentRoleClasses(superClass));
            }

        }
        
        return classes;
    }

    /**
     * Find all components defined using Annotations. These components are looked for in
     * {@link #COMPONENT_LIST} resources.
     * 
     * @param classLoader the classloader to use to load the {@link #COMPONENT_LIST} resources
     * @return the list of component implementation class names
     * @throws IOException in case of an error loading the component list resource
     */
    private List<String> findAnnotatedComponentClasses(ClassLoader classLoader) throws IOException
    {
        List<String> annotatedClassNames = new ArrayList<String>();
        Enumeration<URL> urls = classLoader.getResources(COMPONENT_LIST);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            
            // Read all components definition from the URL
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                annotatedClassNames.add(inputLine);
            }
            in.close();
        }
        return annotatedClassNames;
    }
}
