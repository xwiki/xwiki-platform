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
package org.xwiki.component.embed;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

/**
 * Unit tests for {@link EmbeddableComponentManager}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class EmbeddableComponentManagerTest
{
    public static interface Role
    {
    }
    
    public static class RoleImpl implements Role
    {
    }
    
    public static class OtherRoleImpl implements Role
    {
    }
    
    @Test
    public void testGetComponentDescriptorList() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        
        DefaultComponentDescriptor<Role> d1 = new DefaultComponentDescriptor<Role>();
        d1.setRole(Role.class);
        d1.setRoleHint("hint1");
        ecm.registerComponent(d1);

        DefaultComponentDescriptor<Role> d2 = new DefaultComponentDescriptor<Role>();
        d2.setRole(Role.class);
        d2.setRoleHint("hint2");
        ecm.registerComponent(d2);
        
        List<ComponentDescriptor<Role>> cds = ecm.getComponentDescriptorList(Role.class);
        Assert.assertEquals(2, cds.size());
        Assert.assertTrue(cds.contains(d1));
        Assert.assertTrue(cds.contains(d2));
    }
    
    @Test
    public void testRegisterComponentOverExistingOne() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();

        DefaultComponentDescriptor<Role> d1 = new DefaultComponentDescriptor<Role>();
        d1.setRole(Role.class);
        d1.setImplementation(RoleImpl.class);
        ecm.registerComponent(d1);

        Object instance = ecm.lookup(Role.class);
        Assert.assertEquals(RoleImpl.class.getName(), instance.getClass().getName());
        
        DefaultComponentDescriptor<Role> d2 = new DefaultComponentDescriptor<Role>();
        d2.setRole(Role.class);
        d2.setImplementation(OtherRoleImpl.class);
        ecm.registerComponent(d2);

        instance = ecm.lookup(Role.class);
        Assert.assertEquals(OtherRoleImpl.class.getName(), instance.getClass().getName());
    }
}
