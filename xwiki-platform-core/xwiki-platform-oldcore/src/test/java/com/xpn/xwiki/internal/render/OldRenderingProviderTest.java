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
package com.xpn.xwiki.internal.render;

import org.junit.jupiter.api.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OldRenderingProvider}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(ContextComponentManagerProvider.class)
class OldRenderingProviderTest
{
    @MockComponent
    private OldRendering oldRendering1;

    @InjectMockComponents
    private OldRenderingProvider provider;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void installNewOldCoreRendering() throws Exception
    {
        assertSame(this.oldRendering1, this.provider.get());

        // Install new OldRendering implementation

        OldRendering oldRendering2 = mock(OldRendering.class);
        assertNotSame(this.oldRendering1, oldRendering2);

        DefaultComponentDescriptor<OldRendering> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setImplementation(oldRendering2.getClass());
        componentDescriptor.setRoleType(OldRendering.class);
        this.componentManager.registerComponent(OldRendering.class, oldRendering2);
        this.provider.onNewOldRendering(componentDescriptor, this.componentManager);

        assertSame(oldRendering2, this.provider.get());
    }
}
