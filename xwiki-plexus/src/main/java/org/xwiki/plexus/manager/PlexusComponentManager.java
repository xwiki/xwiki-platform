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
package org.xwiki.plexus.manager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDependency;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

/**
 * @version $Id$
 */
public class PlexusComponentManager implements ComponentManager
{
    private PlexusContainer plexusContainer;

    public PlexusComponentManager(PlexusContainer plexusContainer)
    {
        this.plexusContainer = plexusContainer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookup(Class)
     */
    public <T> T lookup(Class< T > role) throws ComponentLookupException
    {
        T result;
        try {
            result = (T) this.plexusContainer.lookup(role.getName());
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup component role [" + role + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookup(Class, String)
     */
    public <T> T lookup(Class< T > role, String roleHint) throws ComponentLookupException
    {
        T result;
        try {
            result = (T) this.plexusContainer.lookup(role.getName(), roleHint);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup component role [" + role + "] for hint [" + roleHint
                + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookupMap(Class)
     */
    public <T> Map<String, T> lookupMap(Class< T > role) throws ComponentLookupException
    {
        Map<String, T> result;
        try {
            result = this.plexusContainer.lookupMap(role.getName());
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup components for role [" + role + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookupList(Class)
     */
    public <T> List< T > lookupList(Class< T > role) throws ComponentLookupException
    {
        List<T> result;
        try {
            result = this.plexusContainer.lookupList(role.getName());
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup components for role [" + role + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#release(java.lang.Object)
     */
    public <T> void release(T component) throws ComponentLifecycleException
    {
        try {
            this.plexusContainer.release(component);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLifecycleException e) {
            throw new ComponentLifecycleException("Failed to release component [" + component + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#registerComponent(org.xwiki.component.descriptor.ComponentDescriptor)
     */
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) 
        throws ComponentRepositoryException
    {
        org.codehaus.plexus.component.repository.ComponentDescriptor pcd =
            createPlexusComponentDescriptor(componentDescriptor);

        try {
            this.plexusContainer.addComponentDescriptor(pcd);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentRepositoryException e) {
            throw new ComponentRepositoryException("Failed add component descriptor [" + componentDescriptor + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see ComponentManager#getComponentDescriptor(Class, String)
     * @since 2.0M1
     */
    public <T> ComponentDescriptor<T> getComponentDescriptor(Class< T > role, String roleHint)
    {
        org.codehaus.plexus.component.repository.ComponentDescriptor pcd =
            this.plexusContainer.getComponentDescriptor(role.getName(), roleHint);

        DefaultComponentDescriptor<T> descriptor = null;
        
        if (pcd != null) {
            descriptor = new DefaultComponentDescriptor<T>();
            descriptor.setImplementation(pcd.getImplementation());
            descriptor.setRoleHint(pcd.getRoleHint());
            descriptor.setRole((Class<T>) loadClass(pcd.getRole()));
            
            if ("per-lookup".equals(pcd.getInstantiationStrategy())) {
                descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP);
            } else {
                descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
            }
    
            // Copy dependencies
            for (ComponentRequirement requirement : (List<ComponentRequirement>) pcd.getRequirements()) {
                DefaultComponentDependency dependency = new DefaultComponentDependency();
                dependency.setRole(loadClass(requirement.getRole()));
                dependency.setRoleHint(requirement.getRoleHint());
                dependency.setMappingType(loadClass(requirement.getFieldMappingType()));
                dependency.setName(requirement.getFieldName());
                
                // TODO: Handle specific hints when we move to a more recent Plexus version.
                // See createPlexusComponentDescriptor
                descriptor.addComponentDependency(dependency);
            }
        }
        
        return descriptor;
    }
    
    private org.codehaus.plexus.component.repository.ComponentDescriptor createPlexusComponentDescriptor(
        ComponentDescriptor<?> componentDescriptor)
    {
        org.codehaus.plexus.component.repository.ComponentDescriptor pcd =
            new org.codehaus.plexus.component.repository.ComponentDescriptor();

        pcd.setRole(componentDescriptor.getRole().getName());
        pcd.setRoleHint(componentDescriptor.getRoleHint());
        pcd.setImplementation(componentDescriptor.getImplementation());
        
        switch (componentDescriptor.getInstantiationStrategy()) {
            case PER_LOOKUP:
                pcd.setInstantiationStrategy("per-lookup");
                break;
            default:
                pcd.setInstantiationStrategy("singleton");
        }

        Collection<ComponentDependency<?>> componentDependencies = componentDescriptor.getComponentDependencies();
        for (ComponentDependency<?> dependency : componentDependencies) {
            ComponentRequirement requirement;
            
            // Handles several hints in case of lists (collections or maps)
            if (Collection.class.isAssignableFrom(dependency.getMappingType())
                || Map.class.isAssignableFrom(dependency.getMappingType()))
            {
                // TODO: Uncomment when we move to a more recent Plexus version which implements
                // ComponentRequirementList.
                /*
                String[] hints = dependency.getHints();
                if (hints != null && hints.length > 0) {
                    ((ComponentRequirementList)requirement).setRoleHints(Arrays.asList(hints));
                }
                */
                requirement = new ComponentRequirement();
            } else {
                requirement = new ComponentRequirement();
            }

            requirement.setRole(dependency.getRole().getName());
            requirement.setRoleHint(dependency.getRoleHint());
            requirement.setFieldMappingType(dependency.getMappingType().getName());
            requirement.setFieldName(dependency.getName());
            
            pcd.addRequirement(requirement);
        }

        return pcd;
    }

    private Class< ? > loadClass(String className)
    {
        Class< ? > result;
        try {
            result = this.getClass().getClassLoader().loadClass(className);
        } catch (Exception e) {
            // This is not supposed to happen since the Class was able to created the first time the component was
            // registered.
            throw new RuntimeException("Failed to load class [" + className + "]");
        }
        return result;
    }
}
