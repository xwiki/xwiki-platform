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

import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import junit.framework.TestCase;

/**
 * Unit tests for {@link ComponentAnnotationLoader}.
 * 
 * @version $Id: $
 * @since 1.8.1
 */
public class ComponentAnnotationLoaderTest extends TestCase
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
        
    }

    @Component
    public class SuperRoleImpl extends RoleImpl
    {
    }

    public void testFindComponentRoleClasses()
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<Class<?>> classes = loader.findComponentRoleClasses(RoleImpl.class);
        assertEquals(2, classes.size());
        assertEquals(Role.class.getName(), classes.get(0).getName());
        assertEquals(ExtendedRole.class.getName(), classes.get(1).getName());
    }

    public void testFindComponentRoleClasseWhenClassExtension()
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<Class<?>> classes = loader.findComponentRoleClasses(SuperRoleImpl.class);
        assertEquals(2, classes.size());
        assertEquals(Role.class.getName(), classes.get(0).getName());
        assertEquals(ExtendedRole.class.getName(), classes.get(1).getName());
    }

    public void testCreateComponentDescriptor()
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        ComponentDescriptor descriptor = loader.createComponentDescriptor(RoleImpl.class, ExtendedRole.class);
        assertEquals(RoleImpl.class.getName(), descriptor.getImplementation());
        assertEquals(ExtendedRole.class.getName(), descriptor.getRole());
        assertEquals("default", descriptor.getRoleHint());
        assertEquals(ComponentInstantiationStrategy.SINGLETON, descriptor.getInstantiationStrategy());
        Collection<ComponentDependency> deps = descriptor.getComponentDependencies(); 
        assertEquals(2, deps.size());
        Iterator<ComponentDependency> it = deps.iterator();
        ComponentDependency dep = it.next(); 
        assertEquals(FieldRole.class.getName(), dep.getRole());
        assertEquals("default", dep.getRoleHint());
        dep = it.next();
        assertEquals(FieldRole.class.getName(), dep.getRole());
        assertEquals("special", dep.getRoleHint());
    }
}
