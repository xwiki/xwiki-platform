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
    private Map<RoleHint< ? >, ComponentDescriptor< ? >> descriptors = new HashMap<RoleHint< ? >, ComponentDescriptor< ? >>();
    
    private Map<RoleHint< ? >, Object> components = new HashMap<RoleHint< ? >, Object>();
    
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
        this.descriptors.put(
            new RoleHint<T>(componentDescriptor.getRole(), componentDescriptor.getRoleHint()), componentDescriptor);
    }

    /**
     * Add ability to register a component instance. Useful for unit testing.
     */
    public <T> void registerComponent(Class< T > role, String hint, Object component)
    {
        this.components.put(new RoleHint<T>(role, hint), component);
    }

    /**
     * Add ability to register a component instance. Useful for unit testing.
     */
    public <T> void registerComponent(Class< T > role, Object component)
    {
        this.components.put(new RoleHint<T>(role), component);
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#getComponentDescriptor(Class, String)
     */
    @SuppressWarnings("unchecked")
    public <T> ComponentDescriptor<T> getComponentDescriptor(Class< T > role, String roleHint)
    {
        return (ComponentDescriptor<T>) this.descriptors.get(new RoleHint<T>(role, roleHint));
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#release(Object)
     */
    public <T> void release(T component) throws ComponentLifecycleException
    {
        for (Map.Entry<RoleHint<?>, Object> entry : this.components.entrySet()) {
            if (entry.getValue() == component) {
                this.components.remove(entry.getKey());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> initializeList(Class< T > role) throws ComponentLookupException
    {
        List<T> objects = new ArrayList<T>();
        for (RoleHint<?> roleHint : this.descriptors.keySet()) {
            if (roleHint.getRole().getName().equals(role.getName())) {
                objects.add(initialize((RoleHint<T>)roleHint));
            }
        }
        return objects;
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> initializeMap(Class< ? > role) throws ComponentLookupException
    {
        Map<String, T> objects = new HashMap<String, T>();
        for (RoleHint<?> roleHint : this.descriptors.keySet()) {
            if (roleHint.getRole().getName().equals(role.getName())) {
                objects.put(roleHint.getHint(), initialize((RoleHint<T>)roleHint));
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
    
    @SuppressWarnings("unchecked")
    private <T> T getInstance(RoleHint<T> roleHint) throws Exception
    {
        T instance = null;

        // Instantiate component
        ComponentDescriptor<T> descriptor = (ComponentDescriptor<T>) this.descriptors.get(roleHint);
        if (descriptor != null) {
            Class<T> componentClass = (Class<T>) this.classLoader.loadClass(descriptor.getImplementation());
            instance = componentClass.newInstance();
            
            // Set each dependency
            for (ComponentDependency<?> dependency : descriptor.getComponentDependencies()) {
            
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
