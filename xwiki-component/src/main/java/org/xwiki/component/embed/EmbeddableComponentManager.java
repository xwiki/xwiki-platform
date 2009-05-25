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
package org.xwiki.component.embed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.internal.ReflectionUtils;
import org.xwiki.component.internal.RoleHint;
import org.xwiki.component.logging.VoidLogger;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.LogEnabled;

/**
 * Simple implementation of {@link ComponentManager} to be used when using some XWiki modules standalone.
 *  
 * @version $Id$
 * @since 2.0M1
 */
public class EmbeddableComponentManager implements ComponentManager
{
    private Map<RoleHint, ComponentDescriptor> descriptors = new HashMap<RoleHint, ComponentDescriptor>();
    
    private Map<RoleHint, Object> components = new HashMap<RoleHint, Object>();
    
    private ClassLoader classLoader;
    
    public EmbeddableComponentManager(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    /**
     * Load all component annotations and register them as components.
     * 
     * @param classLoader the class loader to use to look for component definitions
     */
    public void initialize()
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        loader.initialize(this, this.classLoader);
    }
    
    /**
     * {@inheritDoc}
     * @see ComponentManager#lookup(Class)
     */
    public Object lookup(Class< ? > role) throws ComponentLookupException
    {
        return initialize(new RoleHint(role));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#lookup(Class, String)
     */
    public Object lookup(Class< ? > role, String roleHint) throws ComponentLookupException
    {
        return initialize(new RoleHint(role, roleHint));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#lookupList(Class)
     */
    public List lookupList(Class< ? > role) throws ComponentLookupException
    {
        return initializeList(role);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#lookupMap(Class)
     */
    public Map lookupMap(Class< ? > role) throws ComponentLookupException
    {
        return initializeMap(role);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#registerComponent(ComponentDescriptor)
     */
    public void registerComponent(ComponentDescriptor componentDescriptor) throws ComponentRepositoryException
    {
        this.descriptors.put(
            new RoleHint(componentDescriptor.getRole(), componentDescriptor.getRoleHint()), componentDescriptor);
    }

    /**
     * Add ability to register a component instance. Useful for unit testing.
     */
    public void registerComponent(Class< ? > role, String hint, Object component)
    {
        this.components.put(new RoleHint(role, hint), component);
    }

    /**
     * Add ability to register a component instance. Useful for unit testing.
     */
    public void registerComponent(Class< ? > role, Object component)
    {
        this.components.put(new RoleHint(role), component);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#getComponentDescriptor(Class, String)
     */
    public ComponentDescriptor getComponentDescriptor(Class< ? > role, String roleHint)
    {
        return this.descriptors.get(new RoleHint(role, roleHint));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#release(Object)
     */
    public void release(Object component) throws ComponentLifecycleException
    {
        for (Map.Entry<RoleHint, Object> entry : this.components.entrySet()) {
            if (entry.getValue() == component) {
                this.components.remove(entry.getKey());
            }
        }
    }

    private List initializeList(Class< ? > role) throws ComponentLookupException
    {
        List<Object> objects = new ArrayList<Object>();
        for (RoleHint roleHint : this.descriptors.keySet()) {
            if (roleHint.getRole().getName().equals(role.getName())) {
                objects.add(initialize(roleHint));
            }
        }
        return objects;
    }

    private Map initializeMap(Class< ? > role) throws ComponentLookupException
    {
        Map<String, Object> objects = new HashMap<String, Object>();
        for (RoleHint roleHint : this.descriptors.keySet()) {
            if (roleHint.getRole().getName().equals(role.getName())) {
                objects.put(roleHint.getHint(), initialize(roleHint));
            }
        }
        return objects;
    }

    private Object initialize(RoleHint roleHint) throws ComponentLookupException
    {
        Object instance;
        synchronized(this) {
            instance = this.components.get(roleHint);
            if (instance == null) {
                try {
                    instance = getInstance(roleHint);
                    if (instance == null) {
                        throw new ComponentLookupException("Failed to lookup component [" + roleHint + "]");
                    } else if (this.descriptors.get(roleHint).getInstantiationStrategy() 
                        == ComponentInstantiationStrategy.SINGLETON)
                    {
                        this.components.put(roleHint, instance);
                    }
                } catch (Exception e) {
                    throw new ComponentLookupException("Failed to lookup component [" + roleHint + "]", e);
                }
            }
        }
        return instance;
    }
    
    private Object getInstance(RoleHint roleHint) throws Exception
    {
        Object instance = null;

        // Instantiate component
        ComponentDescriptor descriptor = this.descriptors.get(roleHint);
        if (descriptor != null) {
            Class componentClass = this.classLoader.loadClass(descriptor.getImplementation());
            instance = componentClass.newInstance();
            
            // Set each dependency
            for (ComponentDependency dependency : descriptor.getComponentDependencies()) {
            
                // TODO: Handle dependency cycles

                // Handle different field types
                Object fieldValue;
                if (List.class.isAssignableFrom(dependency.getMappingType())) {
                    fieldValue = lookupList(dependency.getRole());
                } else if (Map.class.isAssignableFrom(dependency.getMappingType())) {
                    fieldValue = lookupMap(dependency.getRole());
                } else {
                    fieldValue = lookup(dependency.getRole(), dependency.getRoleHint());
                }
                
                // Set the field by introspection
                if (fieldValue != null) {
                    ReflectionUtils.setFieldValue(instance, dependency.getName(), fieldValue);
                }
            }

            // Call Lifecycle

            // LogEnabled
            if (LogEnabled.class.isAssignableFrom(componentClass)) {
                // TODO: Use a proper logger
                ((LogEnabled) instance).enableLogging(new VoidLogger());
            }
            
            // Composable
            if (Composable.class.isAssignableFrom(componentClass)) {
                ((Composable) instance).compose(this);
            }
            
            // Initializable
            if (Initializable.class.isAssignableFrom(componentClass)) {
                ((Initializable) instance).initialize();
            }
        }
        return instance;
    }
}
