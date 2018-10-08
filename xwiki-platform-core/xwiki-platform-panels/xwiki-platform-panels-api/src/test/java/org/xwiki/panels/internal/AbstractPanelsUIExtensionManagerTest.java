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
package org.xwiki.panels.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.internal.WikiUIExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractPanelsUIExtensionManager}.
 *
 * @version $Id$
 * @since 10.9RC1
 */
@ComponentTest
public class AbstractPanelsUIExtensionManagerTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> resolver;

    @InjectMockComponents
    private TestablePanelsUIExtensionManager panelUIExtensionManager;

    @MockComponent
    @Named("bar")
    private UIExtension barUIExtension;

    @MockComponent
    @Named("foo")
    private UIExtension fooExtension;

    @Mock
    private WikiUIExtension wikiUIExtensionMenu1;

    @Mock
    private PanelWikiUIExtension subwikiPanelExtension;

    @BeforeComponent
    public void setup() throws Exception
    {
        Provider<ComponentManager> contextComponentManagerProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context"
        );
        when(contextComponentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    public void get() throws Exception
    {
        String panelsReferences = "Foo.WebHome,Menu.Menu1.WebHome,subwiki:Menu.WebHome";
        panelUIExtensionManager.setConfiguration(panelsReferences);

        // First UIExtension which should match the second panel reference
        DocumentReference wikiUIExtensionMenu1DocumentReference = new DocumentReference("wiki",
            Arrays.asList("Menu", "Menu1"), "WebHome");

        when(resolver.resolve("Menu.Menu1.WebHome")).thenReturn(wikiUIExtensionMenu1DocumentReference);
        when(wikiUIExtensionMenu1.getDocumentReference()).thenReturn(wikiUIExtensionMenu1DocumentReference);
        componentManager.registerComponent(UIExtension.class, "menu1", wikiUIExtensionMenu1);

        // Second UIExtension which should not match any panel reference
        when(barUIExtension.getId()).thenReturn("Bar");

        // Third UIExtension which should match the last panel reference
        DocumentReference subwikiPanelExtensionReference = new DocumentReference("subwiki", "Menu", "WebHome");
        when(resolver.resolve("subwiki:Menu.WebHome")).thenReturn(subwikiPanelExtensionReference);
        when(subwikiPanelExtension.getDocumentReference()).thenReturn(subwikiPanelExtensionReference);
        componentManager.registerComponent(UIExtension.class, "menu", subwikiPanelExtension);

        // Forth UIExtension which should match the first panel reference
        when(fooExtension.getId()).thenReturn("Foo.WebHome");
        DocumentReference fooExtensionReference = new DocumentReference("wiki", "Foo", "WebHome");
        when(resolver.resolve("Foo.WebHome")).thenReturn(fooExtensionReference);

        List<UIExtension> expectedList = new ArrayList<>();
        expectedList.add(fooExtension);
        expectedList.add(wikiUIExtensionMenu1);
        expectedList.add(subwikiPanelExtension);

        assertEquals(expectedList, panelUIExtensionManager.get(""));
    }
}
