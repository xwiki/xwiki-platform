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
 */
package org.xwiki.component.embed;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

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
        Assert.assertSame(RoleImpl.class, instance.getClass());

        DefaultComponentDescriptor<Role> d2 = new DefaultComponentDescriptor<Role>();
        d2.setRole(Role.class);
        d2.setImplementation(OtherRoleImpl.class);
        ecm.registerComponent(d2);

        instance = ecm.lookup(Role.class);
        Assert.assertSame(OtherRoleImpl.class, instance.getClass());
    }

    @Test
    public void testRegisterComponentInstance() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();

        DefaultComponentDescriptor<Role> d1 = new DefaultComponentDescriptor<Role>();
        d1.setRole(Role.class);
        d1.setImplementation(RoleImpl.class);
        Role instance = new RoleImpl();
        ecm.registerComponent(d1, instance);

        Assert.assertSame(instance, ecm.lookup(Role.class));
    }

    @Test
    public void testUnregisterComponent() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();

        DefaultComponentDescriptor<Role> d1 = new DefaultComponentDescriptor<Role>();
        d1.setRole(Role.class);
        d1.setImplementation(RoleImpl.class);
        ecm.registerComponent(d1);

        // Verify that the component is properly registered
        Assert.assertSame(RoleImpl.class, ecm.lookup(Role.class).getClass());

        ecm.unregisterComponent(d1.getRole(), d1.getRoleHint());

        // Verify that the component is not registered anymore
        try {
            ecm.lookup(d1.getRole());
            Assert.fail("Should have thrown a ComponentLookupException");
        } catch (ComponentLookupException expected) {
            // The exception message doesn't matter. All we need to know is that the component descriptor 
            // doesn't exist anymore.
        }
    }
    
    @Test
    public void testLookupWhenComponentInParent() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        ecm.setParent(createParentComponentManager());

        Role instance = ecm.lookup(Role.class);
        Assert.assertNotNull(instance);
    }
    
    @Test
    public void testLookupListAndMapWhenSomeComponentsInParent() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        ecm.setParent(createParentComponentManager());
        
        // Register a component with the same Role and Hint as in the parent
        DefaultComponentDescriptor<Role> cd1 = new DefaultComponentDescriptor<Role>();
        cd1.setRole(Role.class);
        cd1.setImplementation(RoleImpl.class);
        Role roleImpl = new RoleImpl();
        ecm.registerComponent(cd1, roleImpl);

        // Register a component with the same Role as in the parent but with a different hint 
        DefaultComponentDescriptor<Role> cd2 = new DefaultComponentDescriptor<Role>();
        cd2.setRole(Role.class);
        cd2.setRoleHint("hint");
        cd2.setImplementation(RoleImpl.class);
        ecm.registerComponent(cd2);

        // Verify that the components are found
        Assert.assertEquals(3, ecm.lookupList(Role.class).size());
        // Note: We find only 2 components since 2 components are registered with the same Role and Hint.
        // In this case we ensure that the component returned is the one from the client CM
        Map<String, Role> instances = ecm.lookupMap(Role.class);
        Assert.assertEquals(2, instances.size());
        Assert.assertSame(roleImpl, instances.get("default"));
    }

    private ComponentManager createParentComponentManager() throws Exception
    {
        EmbeddableComponentManager parent = new EmbeddableComponentManager();
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);
        parent.registerComponent(cd);
        return parent;
    }
    
    @Test
    public void testHasComponent() throws Exception
    {
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();

        DefaultComponentDescriptor<Role> d1 = new DefaultComponentDescriptor<Role>();
        d1.setRole(Role.class);
        d1.setRoleHint("default");
        ecm.registerComponent(d1);

        Assert.assertTrue(ecm.hasComponent(Role.class));
        Assert.assertTrue(ecm.hasComponent(Role.class, "default"));
    }
}
