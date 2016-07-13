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
package org.xwiki.uiextension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.internal.DefaultUIExtensionManager;

@ComponentList(ContextComponentManagerProvider.class)
public class UIExtensionManagerTest
{
    @Rule
    public MockitoComponentMockingRule<UIExtensionManager> mocker =
        new MockitoComponentMockingRule<UIExtensionManager>(DefaultUIExtensionManager.class);

    @Test
    public void testGet() throws Exception
    {
        assertEquals(Arrays.asList(), this.mocker.getComponentUnderTest().get("extensionpoint"));

        UIExtension uix1 = mocker.registerMockComponent(UIExtension.class, "uix1");
        when(uix1.getExtensionPointId()).thenReturn("extensionpoint");

        UIExtension uix2 = mocker.registerMockComponent(UIExtension.class, "uix2");
        when(uix2.getExtensionPointId()).thenReturn("extensionpoint");

        UIExtension notuix = mocker.registerMockComponent(UIExtension.class, "notuix");
        when(notuix.getExtensionPointId()).thenReturn("notuix");

        assertEquals(new HashSet<UIExtension>(Arrays.asList(uix1, uix2)), new HashSet<UIExtension>(this.mocker
            .getComponentUnderTest().get("extensionpoint")));
    }
}
