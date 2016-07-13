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
package org.xwiki.component.internal;

import static org.mockito.Mockito.verify;

import java.lang.reflect.Type;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.component.script.ContextRootComponentManager}.
 * 
 * @version $Id$
 */
@ComponentList(ContextComponentManagerProvider.class)
public class ContextRootComponentManagerTest
{
    @Rule
    public MockitoComponentMockingRule<ComponentManager> mocker = new MockitoComponentMockingRule<ComponentManager>(
        ContextRootComponentManager.class);

    /**
     * The mock component manager used by the script service under test.
     */
    private ComponentManager contextComponentManager;

    private ComponentManager rootComponentManager;

    @Before
    public void before() throws Exception
    {
        this.contextComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "context");
        this.rootComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "root");
    }

    @Test
    public void test() throws Exception
    {
        this.mocker.getComponentUnderTest().getInstance(Type.class);
        this.mocker.getComponentUnderTest().getInstance(Type.class, "hint");
        this.mocker.getComponentUnderTest().getComponentDescriptor(Type.class, "hint");
        this.mocker.getComponentUnderTest().getComponentDescriptorList(Type.class);
        this.mocker.getComponentUnderTest().getComponentDescriptorList((Type)Type.class);
        this.mocker.getComponentUnderTest().getInstanceList(Type.class);
        this.mocker.getComponentUnderTest().getInstanceMap(Type.class);

        DefaultComponentDescriptor<Type> descriptor = new DefaultComponentDescriptor<>();
        this.mocker.getComponentUnderTest().registerComponent(descriptor);
        this.mocker.getComponentUnderTest().registerComponent(descriptor, Type.class);

        // Verify

        verify(contextComponentManager).getInstance(Type.class);
        verify(contextComponentManager).getInstance(Type.class, "hint");
        verify(contextComponentManager).getComponentDescriptor(Type.class, "hint");
        verify(contextComponentManager).getComponentDescriptorList(Type.class);
        verify(contextComponentManager).getComponentDescriptorList((Type)Type.class);
        verify(contextComponentManager).getInstanceList(Type.class);
        verify(contextComponentManager).getInstanceMap(Type.class);

        verify(rootComponentManager).registerComponent(descriptor);
        verify(rootComponentManager).registerComponent(descriptor, Type.class);
    }

}
