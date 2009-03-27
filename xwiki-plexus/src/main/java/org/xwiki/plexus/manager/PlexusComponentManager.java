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
import org.xwiki.component.descriptor.ComponentProperty;
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
     * @see org.xwiki.component.manager.ComponentManager#lookup(java.lang.String)
     */
    public Object lookup(String role) throws ComponentLookupException
    {
        Object result;
        try {
            result = this.plexusContainer.lookup(role);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup component role [" + role + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookup(java.lang.String, java.lang.String)
     */
    public Object lookup(String role, String roleHint) throws ComponentLookupException
    {
        Object result;
        try {
            result = this.plexusContainer.lookup(role, roleHint);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup component role [" + role + "] for hint [" + roleHint
                + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookupMap(java.lang.String)
     */
    public Map lookupMap(String role) throws ComponentLookupException
    {
        Map result;
        try {
            result = this.plexusContainer.lookupMap(role);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup components for role [" + role + "]", e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#lookupList(java.lang.String)
     */
    public List lookupList(String role) throws ComponentLookupException
    {
        List result;
        try {
            result = this.plexusContainer.lookupList(role);
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
    public void release(Object component) throws ComponentLifecycleException
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
     * @see org.xwiki.component.manager.ComponentManager#hasComponent(java.lang.String)
     */
    public boolean hasComponent(String role)
    {
        return this.plexusContainer.hasComponent(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#hasComponent(java.lang.String, java.lang.String)
     */
    public boolean hasComponent(String role, String roleHint)
    {
        return this.plexusContainer.hasComponent(role, roleHint);
    }

    private org.codehaus.plexus.component.repository.ComponentDescriptor createPlexusComponentDescriptor(
        ComponentDescriptor componentDescriptor)
    {
        org.codehaus.plexus.component.repository.ComponentDescriptor pcd =
            new org.codehaus.plexus.component.repository.ComponentDescriptor();

        pcd.setRole(componentDescriptor.getRole());
        pcd.setRoleHint(componentDescriptor.getRoleHint());
        pcd.setImplementation(componentDescriptor.getImplementation());
        pcd.setInstantiationStrategy(componentDescriptor.getInstantiationStrategy());

        Collection<ComponentProperty> componentConfiguration = componentDescriptor.getComponentConfiguration();
        if (!componentConfiguration.isEmpty()) {
            XmlPlexusConfiguration xpc = new XmlPlexusConfiguration("");
            for (ComponentProperty property : componentConfiguration) {
                XmlPlexusConfiguration pc = new XmlPlexusConfiguration(property.getName());
                pc.setValue(property.getValue());
                xpc.addChild(pc);
            }
            pcd.setConfiguration(xpc);
        }

        Collection<ComponentDependency> componentDependencies = componentDescriptor.getComponentDependencies();
        if (!componentConfiguration.isEmpty()) {
            for (ComponentDependency dependency : componentDependencies) {
                ComponentRequirement requirement = new ComponentRequirement();
                requirement.setRole(dependency.getRole());
                requirement.setRoleHint(dependency.getRoleHint());
                pcd.addRequirement(requirement);
            }
        }

        return pcd;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#registerComponent(org.xwiki.component.descriptor.ComponentDescriptor)
     */
    public void registerComponent(ComponentDescriptor componentDescriptor)
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
}
