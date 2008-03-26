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

import java.util.List;
import java.util.Map;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.ServiceLocator;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentLookupException;

public class PlexusComponentManager implements ComponentManager
{
    private ServiceLocator serviceLocator;

    public PlexusComponentManager(ServiceLocator serviceLocator)
    {
        this.serviceLocator = serviceLocator;
    }

    public Object lookup(String role) throws ComponentLookupException
    {
        Object result;
        try {
            result = this.serviceLocator.lookup(role);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup component role ["
                + role + "]", e);
        }
        return result; 
    }

    public Object lookup(String role, String roleHint) throws ComponentLookupException
    {
        Object result;
        try {
            result = this.serviceLocator.lookup(role, roleHint);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup component role ["
                + role + "] for hint [" + roleHint + "]", e);
        }
        return result;
    }

    public Map lookupMap(String role) throws ComponentLookupException
    {
        Map result;
        try {
            result = this.serviceLocator.lookupMap(role);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup components for role [" 
                + role + "]", e);
        }
        return result;
    }

    public List lookupList(String role) throws ComponentLookupException
    {
        List result;
        try {
            result = this.serviceLocator.lookupList(role);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new ComponentLookupException("Failed to lookup components for role [" 
                + role + "]", e);
        }
        return result;
    }

    public void release(Object component) throws ComponentLifecycleException
    {
        try {
            this.serviceLocator.release(component);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLifecycleException e) {
            throw new ComponentLifecycleException("Failed to release component [" + component + "]", e);
        }
    }

    public boolean hasComponent(String role)
    {
        return this.serviceLocator.hasComponent(role);
    }

    public boolean hasComponent(String role, String roleHint)
    {
        return this.serviceLocator.hasComponent(role, roleHint);
    }
}
