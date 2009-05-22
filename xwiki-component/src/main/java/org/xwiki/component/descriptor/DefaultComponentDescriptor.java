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
package org.xwiki.component.descriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link ComponentDescriptor}.
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class DefaultComponentDescriptor extends DefaultComponentRole implements ComponentDescriptor
{
    private String implementation;

    private ComponentInstantiationStrategy instantiationStrategy = ComponentInstantiationStrategy.SINGLETON;

    private List<ComponentProperty> componentConfiguration = new ArrayList<ComponentProperty>();

    private List<ComponentDependency> componentDependencies = new ArrayList<ComponentDependency>();

    public void setImplementation(String implementation)
    {
        this.implementation = implementation;
    }

    public String getImplementation()
    {
        return implementation;
    }

    public void setInstantiationStrategy(ComponentInstantiationStrategy instantiationStrategy)
    {
        this.instantiationStrategy = instantiationStrategy;
    }

    public ComponentInstantiationStrategy getInstantiationStrategy()
    {
        return this.instantiationStrategy;
    }

    public Collection<ComponentProperty> getComponentConfiguration()
    {
        return this.componentConfiguration;
    }

    public Collection<ComponentDependency> getComponentDependencies()
    {
        return this.componentDependencies;
    }

    public void addComponentProperty(ComponentProperty componentProperty)
    {
        this.componentConfiguration.add(componentProperty);
    }

    public void addComponentProperty(String name, String value)
    {
        this.componentConfiguration.add(new DefaultComponentProperty(name, value));
    }

    public void addComponentDependency(ComponentDependency componentDependency)
    {
        this.componentDependencies.add(componentDependency);
    }

    public void addComponentDependency(Class< ? > role, String roleHint)
    {
        DefaultComponentDependency componentDependency = new DefaultComponentDependency();
        componentDependency.setRole(role);
        componentDependency.setRoleHint(roleHint);

        this.componentDependencies.add(componentDependency);
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer(super.toString());
        buffer.append(" implementation = [").append(getImplementation()).append("]");
        buffer.append(" instantiation = [").append(getInstantiationStrategy()).append("]");
        return buffer.toString();
    }
}
