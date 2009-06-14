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
import org.xwiki.component.internal.Composable;
import org.xwiki.component.internal.ReflectionUtils;
import org.xwiki.component.internal.RoleHint;
import org.xwiki.component.logging.VoidLogger;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
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
    private ComponentEventManager eventManager;
    
    private Map<RoleHint< ? >, ComponentDescriptor< ? >> descriptors = new HashMap<RoleHint< ? >, ComponentDescriptor< ? >>();
    
    private Map<RoleHint< ? >, Object> components = new HashMap<RoleHint< ? >, Object>();
    
    /**
     * Load all component annotations and register them as components.
     * 
     * @param classLoader the class loader to use to look for component definitions
     */
    public void initialize(ClassLoader classLoader)
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        loader.initialize(this, classLoader);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#hasComponent(Class, String)
     */
    public <T> boolean hasComponent(Class< T > role, String roleHint)
    {
        return this.components.containsKey(new RoleHint<T>(role, roleHint));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#hasComponent(Class)
     */
    public <T> boolean hasComponent(Class< T > role)
    {
        return this.components.containsKey(new RoleHint<T>(role));
    }


    /**
     * {@inheritDoc}
     * @see ComponentManager#lookup(Class)
     */
    public <T> T lookup(Class< T > role) throws ComponentLookupException
    {
        return initialize(new RoleHint<T>(role));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#lookup(Class, String)
     */
    public <T> T lookup(Class< T > role, String roleHint) throws ComponentLookupException
    {
        return initialize(new RoleHint<T>(role, roleHint));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#lookupList(Class)
     */
    public <T> List< T > lookupList(Class< T > role) throws ComponentLookupException
    {
        return initializeList(role);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#lookupMap(Class)
     */
    public <T> Map<String, T> lookupMap(Class< T > role) throws ComponentLookupException
    {
        return initializeMap(role);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#registerComponent(ComponentDescriptor)
     */
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) throws ComponentRepositoryException
    {
        synchronized(this) {
            RoleHint< ? > roleHint = new RoleHint<T>(componentDescriptor.getRole(), componentDescriptor.getRoleHint());
            this.descriptors.put(roleHint, componentDescriptor);
            // Remove any existing instance since we're replacing it
            this.components.remove(roleHint);
        }
    }

    /**
     * Add ability to register a component instance. Useful for unit testing.
     */
    public <T> void registerComponent(Class< T > role, String hint, Object component)
    {
        synchronized(this) {
            this.components.put(new RoleHint<T>(role, hint), component);
        }
    }

    /**
     * Add ability to register a component instance. Useful for unit testing.
     */
    public <T> void registerComponent(Class< T > role, Object component)
    {
        synchronized(this) {
            this.components.put(new RoleHint<T>(role), component);
        }
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#getComponentDescriptor(Class, String)
     */
    @SuppressWarnings("unchecked")
    public <T> ComponentDescriptor<T> getComponentDescriptor(Class< T > role, String roleHint)
    {
        synchronized(this) {
            return (ComponentDescriptor<T>) this.descriptors.get(new RoleHint<T>(role, roleHint));
        }
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#getComponentDescriptorList(Class)
     */
    @SuppressWarnings("unchecked")
    public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role)
    {
        synchronized(this) {
            List<ComponentDescriptor<T>> results = new ArrayList<ComponentDescriptor<T>>();
            for (Map.Entry<RoleHint< ? >, ComponentDescriptor< ? >> entry : this.descriptors.entrySet()) {
                if (entry.getKey().getRole().getName().equals(role.getName())) {
                    results.add((ComponentDescriptor<T>) entry.getValue());
                }
            }
            return results;
        }
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#release(Object)
     */
    public <T> void release(T component) throws ComponentLifecycleException
    {
        synchronized(this) {
            RoleHint< ? > key = null;
            for (Map.Entry<RoleHint<?>, Object> entry : this.components.entrySet()) {
                if (entry.getValue() == component) {
                    key = entry.getKey();
                    break;
                }
            }
            // Note that we're not removing inside the for loop above since it would cause a Concurrent
            // exception since we'd modify the map accessed by the iterator.
            if (key != null) {
                this.components.remove(key);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * @see ComponentManager#setComponentEventManager(ComponentEventManager)
     */
    public void setComponentEventManager(ComponentEventManager eventManager)
    {
        this.eventManager = eventManager;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> initializeList(Class< T > role) throws ComponentLookupException
    {
        List<T> objects = new ArrayList<T>();
        synchronized(this) {
            for (RoleHint<?> roleHint : this.descriptors.keySet()) {
                if (roleHint.getRole().getName().equals(role.getName())) {
                    objects.add(initialize((RoleHint<T>)roleHint));
                }
            }
        }
        return objects;
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> initializeMap(Class< ? > role) throws ComponentLookupException
    {
        Map<String, T> objects = new HashMap<String, T>();
        synchronized(this) {
            for (RoleHint<?> roleHint : this.descriptors.keySet()) {
                if (roleHint.getRole().getName().equals(role.getName())) {
                    objects.put(roleHint.getHint(), initialize((RoleHint<T>)roleHint));
                }
            }
        }
        return objects;
    }

    @SuppressWarnings("unchecked")
    private <T> T initialize(RoleHint<T> roleHint) throws ComponentLookupException
    {
        T instance;
        synchronized(this) {
            instance = (T) this.components.get(roleHint);
            if (instance == null) {
                try {
                    instance = createInstance(roleHint);
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
                
                // Send event about component instance creation
                if (this.eventManager != null) {
                    this.eventManager.notify(this.descriptors.get(roleHint));
                }
            }
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T createInstance(RoleHint<T> roleHint) throws Exception
    {
        T instance = null;

        // Instantiate component
        ComponentDescriptor<T> descriptor = (ComponentDescriptor<T>) this.descriptors.get(roleHint);
        if (descriptor != null) {
            instance = (T) descriptor.getImplementation().newInstance();
            
            // Set each dependency
            for (ComponentDependency<?> dependency : descriptor.getComponentDependencies()) {
            
                // TODO: Handle dependency cycles

                // Handle different field types
                Object fieldValue;
                if ((dependency.getMappingType() != null)
                    && List.class.isAssignableFrom(dependency.getMappingType()))
                {
                    fieldValue = lookupList(dependency.getRole());
                } else if ((dependency.getMappingType() != null)
                    && Map.class.isAssignableFrom(dependency.getMappingType()))
                {
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
            if (LogEnabled.class.isAssignableFrom(descriptor.getImplementation())) {
                // TODO: Use a proper logger
                ((LogEnabled) instance).enableLogging(new VoidLogger());
            }
            
            // Composable
            // Only support Composable for classes implementing ComponentManager since for all other components
            // they should have ComponentManager injected.
            if (ComponentManager.class.isAssignableFrom(descriptor.getImplementation()) 
                && Composable.class.isAssignableFrom(descriptor.getImplementation()))
            {
                ((Composable) instance).compose(this);
            }
            
            // Initializable
            if (Initializable.class.isAssignableFrom(descriptor.getImplementation())) {
                ((Initializable) instance).initialize();
            }
        }
        return instance;
    }
}
