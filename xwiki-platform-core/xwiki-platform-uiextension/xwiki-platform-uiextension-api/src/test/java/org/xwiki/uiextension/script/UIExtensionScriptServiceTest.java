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
package org.xwiki.uiextension.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.filter.SortByIdFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class UIExtensionScriptServiceTest
{
    @InjectMockComponents
    private UIExtensionScriptService uiExtensionScriptService;

    @MockComponent
    private UIExtensionManager uiExtensionManager;

    @Mock
    private ComponentManager contextComponentManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Mock
    private ComponentManager componentManager;

    @Test
    void verifyExtensionsAreSortedAlphabeticallyById() throws Exception
    {
        List<UIExtension> epExtensions = new ArrayList<>();

        // The UIX are voluntarily added in a wrong order.
        UIExtension uix3 = mock(UIExtension.class, "uix3");
        when(uix3.getId()).thenReturn("id3");
        when(uix3.getExtensionPointId()).thenReturn("epId");
        epExtensions.add(uix3);

        UIExtension uix1 = mock(UIExtension.class, "uix1");
        when(uix1.getId()).thenReturn("id1");
        when(uix1.getExtensionPointId()).thenReturn("epId");
        epExtensions.add(uix1);

        UIExtension uix2 = mock(UIExtension.class, "uix2");
        when(uix2.getId()).thenReturn("id2");
        when(uix2.getExtensionPointId()).thenReturn("epId");
        epExtensions.add(uix2);

        when(this.contextComponentManagerProvider.get()).thenReturn(this.contextComponentManager);
        when(this.contextComponentManager.getInstance(UIExtensionManager.class, "epId"))
            .thenThrow(new ComponentLookupException("No specific manager for extension point epId"));
        when(this.uiExtensionManager.get("epId")).thenReturn(epExtensions);
        when(this.contextComponentManager.getInstance(UIExtensionFilter.class, "sortById"))
            .thenReturn(new SortByIdFilter());

        List<UIExtension> extensions = this.uiExtensionScriptService.getExtensions("epId", Map.of("sortById", ""));

        assertEquals("id1", extensions.get(0).getId());
        assertEquals("id2", extensions.get(1).getId());
        assertEquals("id3", extensions.get(2).getId());
    }
}
