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

import javax.inject.Provider;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link OldRenderingProvider}.
 * 
 * @version $Id$
 */
@ComponentList(ContextComponentManagerProvider.class)
public class OldRenderingProviderTest
{
    @Rule
    public MockitoComponentMockingRule<Provider<OldRendering>> mocker =
        new MockitoComponentMockingRule<Provider<OldRendering>>(OldRenderingProvider.class);

    @Test
    public void testInstallNewOldCoreRendering() throws Exception
    {
        OldRendering oldRendering1 = this.mocker.registerMockComponent(OldRendering.class);

        assertSame(oldRendering1, this.mocker.getComponentUnderTest().get());

        // Install new OldRendering implementation

        OldRendering oldRendering2 = this.mocker.registerMockComponent(OldRendering.class);
        assertNotSame(oldRendering1, oldRendering2);

        DefaultComponentDescriptor<OldRendering> componentDescriptor = new DefaultComponentDescriptor<OldRendering>();
        componentDescriptor.setImplementation(oldRendering2.getClass());
        componentDescriptor.setRoleType(OldRendering.class);
        ((OldRenderingProvider) this.mocker.getComponentUnderTest())
            .onNewOldRendering(componentDescriptor, this.mocker);

        assertSame(oldRendering2, this.mocker.getComponentUnderTest().get());
    }
}
