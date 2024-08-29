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

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.internal.DefaultUIExtensionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
@ComponentList(ContextComponentManagerProvider.class)
class UIExtensionManagerTest
{
    @MockComponent
    @Named("uix1")
    private UIExtension uix1;

    @MockComponent
    @Named("uix2")
    private UIExtension uix2;

    @InjectMockComponents
    private DefaultUIExtensionManager manager;

    @MockComponent
    @Named("notuix")
    private UIExtension notuix;

    @Test
    void get() throws Exception
    {
        assertEquals(Arrays.asList(), this.manager.get("extensionpoint"));

        when(this.uix1.getExtensionPointId()).thenReturn("extensionpoint");
        when(this.uix2.getExtensionPointId()).thenReturn("extensionpoint");
        when(this.notuix.getExtensionPointId()).thenReturn("notuix");

        assertEquals(new HashSet<>(Arrays.asList(this.uix1, this.uix2)),
            new HashSet<>(this.manager.get("extensionpoint")));
    }

    @Test
    void getWithSpecificUIExtensionManager(MockitoComponentManager componentManager) throws Exception
    {
        assertEquals(Arrays.asList(), this.manager.get("extensionpoint"));

        UIExtensionManager specificManager =
            componentManager.registerMockComponent(UIExtensionManager.class, "extensionpoint");
        when(specificManager.get("extensionpoint")).thenReturn(Arrays.asList(this.uix1, this.uix2));

        assertEquals(new HashSet<>(Arrays.asList(this.uix1, this.uix2)),
            new HashSet<>(this.manager.get("extensionpoint")));
    }
}
