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
package org.xwiki.component.manager;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.component.descriptor.ComponentDescriptor;

@ComponentRole
public interface ComponentManager
{
    /**
     * Find a component instance that implements that passed interface class. If the component has a singleton
     * lifecycle then this method always return the same instance.
     *  
     * @param role the class (aka role) that the component implements 
     * @return the component instance
     * @throws ComponentLookupException in case the component cannot be found 
     */
    <T> T lookup(Class< T > role) throws ComponentLookupException;
    
    /**
     * Find a component instance that implements that passed interface class. If the component has a singleton
     * lifecycle then this method always return the same instance.
     *  
     * @param role the class (aka role) that the component implements
     * @param roleHint the hint that differentiates a component implementation from another one (each component
     *        is registered with a hint; the "default" hint being the default) 
     * @return the component instance
     * @throws ComponentLookupException in case the component cannot be found 
     */
    <T> T lookup(Class< T > role, String roleHint) throws ComponentLookupException;

    <T> void release(T component) throws ComponentLifecycleException;

    <T> Map<String, T> lookupMap(Class< T > role) throws ComponentLookupException;

    <T> List< T > lookupList(Class< T > role) throws ComponentLookupException;

    /**
     * Add a component in the component repository programmaticaly.
     * <p>
     * If a component with the same role and role hint already exists it will be replaced by this provided one when
     * lookup.
     * 
     * @param componentDescriptor the descriptor of the component to register.
     * @throws ComponentRepositoryException error when registering component descriptor.
     * @since 1.7M1
     */
    <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) throws ComponentRepositoryException;

    /**
     * @param role the role identifying the component
     * @param roleHint the hint identifying the component
     * @return the descriptor for the component matching the passed parameter or null if this component doesn't exist
     * @since 2.0M1
     */
    <T> ComponentDescriptor<T> getComponentDescriptor(Class< T > role, String roleHint);
}
