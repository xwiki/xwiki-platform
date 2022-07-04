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
package org.xwiki.rendering.internal.macro.wikibridge;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.InsufficientPrivilegesException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacroManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@ComponentTest
class DefaultWikiMacroManagerTest
{
    @InjectMockComponents
    private DefaultWikiMacroManager wikiMacroManager;

    @MockComponent
    private WikiMacroFactory wikiMacroFactory;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DocumentAccessBridge bridge;

    @MockComponent
    private ModelContext modelContext;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private DocumentReference authorReference =
        new DocumentReference("authorwiki", Arrays.asList("authorspace"), "authorpage");

    @Test
    void registerAndUnregisterWikiMacroWhenGlobalVisibilityAndAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's allowed for the GLOBAL visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.GLOBAL))
            .thenReturn(true);

        assertFalse(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        when(this.serializer.serialize(this.authorReference)).thenReturn("authorwiki:authorspace.authorpage");

        // Indicate current wiki is the main one (otherwise it won't be registered at root level)
        when(this.wikiDescriptorManager.isMainWiki(wikiMacro.getDocumentReference().getWikiReference().getName()))
            .thenReturn(true);

        // Test registration
        this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the root CM
        assertTrue(this.componentManager.hasComponent(Macro.class, "testwikimacro"));

        // Verify that the user and wiki where the macro is located have been set in the context
        verify(this.bridge).setCurrentUser("authorwiki:authorspace.authorpage");
        verify(this.modelContext).setCurrentEntityReference(wikiMacro.getDocumentReference());

        // Test unregistration
        this.wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro from the root CM
        assertFalse(this.componentManager.hasComponent(Macro.class, "testwikimacro"));
    }

    @Test
    void registerWikiMacroWhenWikiVisibilityAndAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user who's allowed for the WIKI visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.WIKI))
            .thenReturn(true);

        ComponentManager wikiComponentManager =
            this.componentManager.registerMockComponent(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Macro.class);
        componentDescriptor.setRoleHint("testwikimacro");

        // Test registration
        this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the wiki CM
        verify(wikiComponentManager).registerComponent(any(DefaultComponentDescriptor.class), eq(wikiMacro));

        when(wikiComponentManager.<Macro>getComponentDescriptor(Macro.class, "testwikimacro"))
            .thenReturn(componentDescriptor);
        when(wikiComponentManager.getInstance(Macro.class, "testwikimacro")).thenReturn(wikiMacro);

        // Test unregistration
        this.wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro against the wiki CM
        verify(wikiComponentManager).unregisterComponent(Macro.class, "testwikimacro");
    }

    @Test
    void registerWikiMacroWhenGlobalVisibilityOnSubWiki() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's allowed for the WIKI visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.WIKI))
            .thenReturn(true);

        // Indicate current wiki is a subwiki (so that it's not registered at root level)
        when(this.wikiDescriptorManager.isMainWiki(wikiMacro.getDocumentReference().getWikiReference().getName()))
            .thenReturn(false);

        ComponentManager wikiComponentManager =
            this.componentManager.registerMockComponent(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Macro.class);
        componentDescriptor.setRoleHint("testwikimacro");

        // Test registration
        this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the wiki CM
        verify(wikiComponentManager).registerComponent(componentDescriptor, wikiMacro);

        when(wikiComponentManager.<Macro>getComponentDescriptor(Macro.class, "testwikimacro"))
            .thenReturn(componentDescriptor);
        when(wikiComponentManager.getInstance(Macro.class, "testwikimacro")).thenReturn(wikiMacro);

        // Test unregistration
        this.wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro against the wiki CM
        verify(wikiComponentManager).unregisterComponent(Macro.class, "testwikimacro");
    }

    @Test
    void registerWikiMacroWhenUserVisibilityAndAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user who's allowed for the USER visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.USER))
            .thenReturn(true);

        ComponentManager userComponentManager =
            this.componentManager.registerMockComponent(ComponentManager.class, "user");
        DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Macro.class);
        componentDescriptor.setRoleHint("testwikimacro");

        // Test registration
        this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the user CM
        verify(userComponentManager).registerComponent(any(DefaultComponentDescriptor.class), eq(wikiMacro));

        when(userComponentManager.<Macro>getComponentDescriptor(Macro.class, "testwikimacro"))
            .thenReturn(componentDescriptor);
        when(userComponentManager.getInstance(Macro.class, "testwikimacro")).thenReturn(wikiMacro);

        // Test unregistration
        this.wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro against the user CM
        verify(userComponentManager).unregisterComponent(Macro.class, "testwikimacro");
    }

    @Test
    void registerWikiMacroWhenGlobalVisibilityAndNotAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's not allowed for the GLOBAL visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.GLOBAL))
            .thenReturn(false);

        Throwable exception = assertThrows(InsufficientPrivilegesException.class, () -> {
            this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        });
        assertEquals("Unable to register macro [testwikimacro] in [wiki:space.space] for visibility [GLOBAL] due "
            + "to insufficient privileges", exception.getMessage());
    }

    @Test
    void unregisterWikiMacroWhenGlobalVisibilityAndNotAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's allowed for the GLOBAL visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.GLOBAL))
            .thenReturn(true);

        assertFalse(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        when(this.serializer.serialize(this.authorReference)).thenReturn("authorwiki:authorspace.authorpage");

        // Indicate current wiki is the main one (otherwise it won't be registered at root level)
        when(this.wikiDescriptorManager.isMainWiki(wikiMacro.getDocumentReference().getWikiReference().getName()))
            .thenReturn(true);

        // Test registration
        this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(this.wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the root CM
        assertTrue(this.componentManager.hasComponent(Macro.class, "testwikimacro"));

        // Verify that the user and wiki where the macro is located have been set in the context
        verify(this.bridge).setCurrentUser("authorwiki:authorspace.authorpage");
        verify(this.modelContext).setCurrentEntityReference(wikiMacro.getDocumentReference());

        // Simulate a user who's not allowed for the GLOBAL visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.GLOBAL))
            .thenReturn(false);

        Throwable exception = assertThrows(WikiMacroException.class, () -> {
            this.wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        });
        assertEquals("Unable to unregister macro [testwikimacro] in [wiki:space.space] for visibility [GLOBAL] due "
            + "to insufficient privileges", exception.getMessage());
    }

    @Test
    void registerWikiMacroWhenWikiVisibilityAndNotAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user who's not allowed for the WIKI visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.WIKI))
            .thenReturn(false);

        Throwable exception = assertThrows(InsufficientPrivilegesException.class, () -> {
            this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        });
        assertEquals("Unable to register macro [testwikimacro] in [wiki:space.space] for visibility [WIKI] due to "
            + "insufficient privileges", exception.getMessage());
    }

    @Test
    void registerWikiMacroWhenUserVisibilityAndNotAllowed() throws Exception
    {
        WikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user who's not allowed for the USER visibility
        when(this.wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.USER))
            .thenReturn(false);

        Throwable exception = assertThrows(InsufficientPrivilegesException.class, () -> {
            this.wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        });
        assertEquals("Unable to register macro [testwikimacro] in [wiki:space.space] for visibility [USER] due "
            + "to insufficient privileges", exception.getMessage());
    }

    private WikiMacro generateWikiMacro(WikiMacroVisibility visibility) throws Exception
    {
        DocumentReference wikiMacroDocReference = new DocumentReference("wiki", Arrays.asList("space"), "space");
        WikiMacroDescriptor descriptor = new WikiMacroDescriptor.Builder().id(new MacroId("testwikimacro"))
            .name("Test Wiki Macro").description("Description").defaultCategories(Set.of("Test")).visibility(visibility)
            .contentDescriptor(new DefaultContentDescriptor())
            .parameterDescriptors(Collections.emptyList()).build();

        WikiMacro wikiMacro = mock(WikiMacro.class);
        when(wikiMacro.getDocumentReference()).thenReturn(wikiMacroDocReference);
        when(wikiMacro.getAuthorReference()).thenReturn(this.authorReference);
        when(wikiMacro.getDescriptor()).thenReturn(descriptor);

        return wikiMacro;
    }
}
