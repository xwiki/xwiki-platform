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

import java.lang.reflect.Type;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ContextRootComponentManager}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(ContextComponentManagerProvider.class)
class ContextRootComponentManagerTest
{
    @InjectMockComponents
    private ContextRootComponentManager contextRootComponentManager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    @Named("root")
    private ComponentManager rootComponentManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void test() throws Exception
    {
        this.contextRootComponentManager.getInstance(Type.class);
        this.contextRootComponentManager.getInstance(Type.class, "hint");
        this.contextRootComponentManager.getComponentDescriptor(Type.class, "hint");
        this.contextRootComponentManager.getComponentDescriptorList(Type.class);
        this.contextRootComponentManager.getComponentDescriptorList((Type)Type.class);
        this.contextRootComponentManager.getInstanceList(Type.class);
        this.contextRootComponentManager.getInstanceMap(Type.class);

        DefaultComponentDescriptor<Type> descriptor = new DefaultComponentDescriptor<>();
        this.contextRootComponentManager.registerComponent(descriptor);
        this.contextRootComponentManager.registerComponent(descriptor, Type.class);

        // Verify

        verify(this.contextComponentManager).getInstance(Type.class);
        verify(this.contextComponentManager).getInstance(Type.class, "hint");
        verify(this.contextComponentManager).getComponentDescriptor(Type.class, "hint");
        verify(this.contextComponentManager).getComponentDescriptorList(Type.class);
        verify(this.contextComponentManager).getComponentDescriptorList((Type)Type.class);
        verify(this.contextComponentManager).getInstanceList(Type.class);
        verify(this.contextComponentManager).getInstanceMap(Type.class);

        verify(this.rootComponentManager).registerComponent(descriptor);
        verify(this.rootComponentManager).registerComponent(descriptor, Type.class);
    }
}
