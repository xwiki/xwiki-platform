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
import java.util.HashMap;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.uiextension.internal.filter.SortByIdFilter;
import static org.mockito.Mockito.*;

import org.junit.Assert;

public class UIExtensionScriptServiceTest implements WikiUIExtensionConstants
{
    private ComponentManager contextComponentManager;

    private List<UIExtension> epExtensions = new ArrayList<UIExtension>();

    private UIExtensionManager uiExtensionManager;

    @Rule
    public MockitoComponentMockingRule<ScriptService> componentManager =
        new MockitoComponentMockingRule<ScriptService>(UIExtensionScriptService.class);

    @Before
    public void setUp() throws Exception
    {
        contextComponentManager = mock(ComponentManager.class);
        Provider<ComponentManager> componentManagerProvider = componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(contextComponentManager);

        this.uiExtensionManager = componentManager.getInstance(UIExtensionManager.class);
    }

    @Test
    public void verifyExtensionsAreSortedAlphabeticallyById() throws Exception
    {
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

        when(contextComponentManager.getInstance(UIExtensionManager.class, "epId"))
            .thenThrow(new ComponentLookupException("No specific manager for extension point epId"));
        when(uiExtensionManager.get("epId")).thenReturn(epExtensions);
        when(contextComponentManager.getInstance(UIExtensionFilter.class, "sortById")).thenReturn(new SortByIdFilter());

        HashMap<String, String> filters = new HashMap<String, String>();
        filters.put("sortById", "");
        UIExtensionScriptService service = (UIExtensionScriptService) componentManager.getComponentUnderTest();
        List<UIExtension> extensions = service.getExtensions("epId", filters);

        Assert.assertEquals("id1", extensions.get(0).getId());
        Assert.assertEquals("id2", extensions.get(1).getId());
        Assert.assertEquals("id3", extensions.get(2).getId());
    }
}
