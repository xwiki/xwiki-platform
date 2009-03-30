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

import java.util.List;

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
    }

    @Component
    public class SuperRoleImpl extends RoleImpl
    {
    }

    public void testFindComponentRoleClasses()
    {
        assertComponentRoleClasses(RoleImpl.class);
    }

    /**
     * Verify that we get the same result when we use a class that extends another class (i.e. inheritance
     * works).
     */
    public void testFindComponentRoleClasseWhenClassExtension()
    {
        assertComponentRoleClasses(SuperRoleImpl.class);
    }

    private void assertComponentRoleClasses(Class< ? > componentClass)
    {
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<Class<?>> classes = loader.findComponentRoleClasses(componentClass);
        assertEquals(2, classes.size());
        assertEquals(Role.class.getName(), classes.get(0).getName());
        assertEquals(ExtendedRole.class.getName(), classes.get(1).getName());
    }
}
