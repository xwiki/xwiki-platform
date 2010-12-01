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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

/**
 * Unit tests for {@link ComponentDescriptorFactory}.
 * 
 * @version $Id$
 * @since 1.8.1
 */
public class ComponentDescriptorFactoryTest
{
    @ComponentRole
    public interface FieldRole
    {
    }

    @Component
    public class FieldroleImpl implements FieldRole
    {
    }
    
    @Component("special")
    public class SpecialFieldRoleImpl implements FieldRole
    {
    }

    @ComponentRole
    public interface Role
    {
    }

    @ComponentRole
    public interface ExtendedRole extends Role
    {
    }

    @Component
    public class RoleImpl implements ExtendedRole
    {
        @Requirement
        private FieldRole fieldRole;
        
        @Requirement("special")
        private FieldRole specialFieldRole;

        /**
         * Inject all implementation of the FieldRole role. 
         */
        @Requirement(role = FieldRole.class)
        private List<FieldRole> deprecatedRoles;
        
        /**
         * Inject all implementation of the FieldRole role. 
         */
        @Requirement
        private List<FieldRole> roles;

        /**
         * Only inject FieldRole implementation with a "special" hint.
         */
        @Requirement(role = FieldRole.class, hints = {"special"})
        private List<FieldRole> deprecatedSpecialRoles;
        /**
         * Only inject FieldRole implementation with a "special" hint.
         */
        @Requirement(hints = {"special"})
        private List<FieldRole> specialRoles;
        
        /**
         * Inject all implementation of the FieldRole role. 
         */
        @Requirement
        private Map<String, FieldRole> mapRoles;
    }

    @Component
    public class SuperRoleImpl extends RoleImpl
    {
        @Requirement("other")
        private FieldRole fieldRole;
    }

    /**
     * Test that we can have a component implementing several roles.
     */
    @Component(hints = {"hint1", "hint2"})
    public class MultipleRolesImpl implements Role
    {
    }

    @Test
    public void testCreateComponentDescriptor()
    {
        assertComponentDescriptor(RoleImpl.class, "default");
    }
    
    /**
     * Verify that we get the same result when we use a class that extends another class (i.e. inheritance
     * works).
     */
    @Test
    public void testCreateComponentDescriptorWhenClassExtension()
    {
        assertComponentDescriptor(SuperRoleImpl.class, "other");
    }

    @Test
    public void testMultipleRolesForComponent()
    {
        ComponentDescriptorFactory factory = new ComponentDescriptorFactory();
        List<ComponentDescriptor> descriptors = 
            factory.createComponentDescriptors(MultipleRolesImpl.class, Role.class);
               
        Assert.assertEquals(2, descriptors.size());
        Assert.assertEquals("hint1", descriptors.get(0).getRoleHint());
        Assert.assertEquals("hint2", descriptors.get(1).getRoleHint());
    }

    private void assertComponentDescriptor(Class< ? > componentClass, String fieldRoleName)
    {
        ComponentDescriptorFactory factory = new ComponentDescriptorFactory();
        List<ComponentDescriptor> descriptors = factory.createComponentDescriptors(componentClass, ExtendedRole.class);
        
        Assert.assertEquals(1, descriptors.size());
        ComponentDescriptor descriptor = descriptors.get(0);
        
        Assert.assertEquals(componentClass.getName(), descriptor.getImplementation().getName());
        Assert.assertEquals(ExtendedRole.class.getName(), descriptor.getRole().getName());
        Assert.assertEquals("default", descriptor.getRoleHint());
        Assert.assertEquals(ComponentInstantiationStrategy.SINGLETON, descriptor.getInstantiationStrategy());

        Collection<ComponentDependency> deps = descriptor.getComponentDependencies(); 
        Assert.assertEquals(7, deps.size());
        Iterator<ComponentDependency> it = deps.iterator();

        ComponentDependency dep = it.next(); 
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals(fieldRoleName, dep.getRoleHint());
        Assert.assertEquals(FieldRole.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("fieldRole", dep.getName());
        
        dep = it.next();
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals("special", dep.getRoleHint());
        Assert.assertEquals(FieldRole.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("specialFieldRole", dep.getName());
        
        dep = it.next();
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals("default", dep.getRoleHint());
        Assert.assertEquals(List.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("deprecatedRoles", dep.getName());
        
        dep = it.next();
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals("default", dep.getRoleHint());
        Assert.assertEquals(List.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("roles", dep.getName());

        dep = it.next();
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals("default", dep.getRoleHint());
        Assert.assertEquals(List.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("deprecatedSpecialRoles", dep.getName());
        
        dep = it.next();
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals("default", dep.getRoleHint());
        Assert.assertEquals(List.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("specialRoles", dep.getName());
        
        dep = it.next();
        Assert.assertEquals(FieldRole.class.getName(), dep.getRole().getName());
        Assert.assertEquals("default", dep.getRoleHint());
        Assert.assertEquals(Map.class.getName(), dep.getMappingType().getName());
        Assert.assertEquals("mapRoles", dep.getName());
    }
}
