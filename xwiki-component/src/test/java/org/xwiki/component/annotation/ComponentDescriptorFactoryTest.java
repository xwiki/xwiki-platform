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

import junit.framework.TestCase;

import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

/**
 * Unit tests for {@link ComponentDescriptorFactory}.
 * 
 * @version $Id: $
 * @since 1.8.1
 */
public class ComponentDescriptorFactoryTest extends TestCase
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
        private List<FieldRole> roles;

        /**
         * Only inject FieldRole implementation with a "special" hint.
         */
        @Requirement(role = FieldRole.class, hints = {"special"})
        private List<FieldRole> specialRoles;
    }

    @Component
    public class SuperRoleImpl extends RoleImpl
    {
        @Requirement("other")
        private FieldRole fieldRole;
    }

    public void testCreateComponentDescriptor()
    {
        assertComponentDescriptor(RoleImpl.class, "default");
    }
    
    /**
     * Verify that we get the same result when we use a class that extends another class (i.e. inheritance
     * works).
     */
    public void testCreateComponentDescriptorWhenClassExtension()
    {
        assertComponentDescriptor(SuperRoleImpl.class, "other");
    }
    
    private void assertComponentDescriptor(Class< ? > componentClass, String fieldRoleName)
    {
        ComponentDescriptorFactory factory = new ComponentDescriptorFactory();
        ComponentDescriptor descriptor = factory.createComponentDescriptor(componentClass, ExtendedRole.class);
        assertEquals(componentClass.getName(), descriptor.getImplementation());
        assertEquals(ExtendedRole.class.getName(), descriptor.getRole());
        assertEquals("default", descriptor.getRoleHint());
        assertEquals(ComponentInstantiationStrategy.SINGLETON, descriptor.getInstantiationStrategy());

        Collection<ComponentDependency> deps = descriptor.getComponentDependencies(); 
        assertEquals(4, deps.size());
        Iterator<ComponentDependency> it = deps.iterator();

        ComponentDependency dep = it.next(); 
        assertEquals(FieldRole.class.getName(), dep.getRole());
        assertEquals(fieldRoleName, dep.getRoleHint());
        assertEquals(FieldRole.class.getName(), dep.getMappingType().getName());
        assertEquals("fieldRole", dep.getName());
        
        dep = it.next();
        assertEquals(FieldRole.class.getName(), dep.getRole());
        assertEquals("special", dep.getRoleHint());
        assertEquals(FieldRole.class.getName(), dep.getMappingType().getName());
        assertEquals("specialFieldRole", dep.getName());
        
        dep = it.next();
        assertEquals(FieldRole.class.getName(), dep.getRole());
        assertEquals("default", dep.getRoleHint());
        assertEquals(List.class.getName(), dep.getMappingType().getName());
        assertEquals("roles", dep.getName());

        dep = it.next();
        assertEquals(FieldRole.class.getName(), dep.getRole());
        assertEquals("default", dep.getRoleHint());
        assertEquals(List.class.getName(), dep.getMappingType().getName());
        assertEquals("specialRoles", dep.getName());
    }
}
