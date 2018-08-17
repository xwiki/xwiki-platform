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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.InsufficientPrivilegesException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacroManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class DefaultWikiMacroManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiMacroManager> mocker =
        new MockitoComponentMockingRule<>(DefaultWikiMacroManager.class);

    private DocumentReference authorReference =
        new DocumentReference("authorwiki", Arrays.asList("authorspace"), "authorpage");

    @Test
    public void registerAndUnregisterWikiMacroWhenGlobalVisibilityAndAllowed() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's allowed for the GLOBAL visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.GLOBAL)).thenReturn(true);

        WikiMacroManager wikiMacroManager = this.mocker.getComponentUnderTest();
        assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        EntityReferenceSerializer<String> serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(this.authorReference)).thenReturn("authorwiki:authorspace.authorpage");

        // Indicate current wiki is the main one (otherwise it won't be registered at root level)
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.isMainWiki(wikiMacro.getDocumentReference().getWikiReference().getName()))
            .thenReturn(true);

        // Test registration
        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the root CM
        assertTrue(this.mocker.hasComponent(Macro.class, "testwikimacro"));

        // Verify that the user and wiki where the macro is located have been set in the context
        DocumentAccessBridge bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        verify(bridge).setCurrentUser("authorwiki:authorspace.authorpage");
        ModelContext modelContext = this.mocker.getInstance(ModelContext.class);
        verify(modelContext).setCurrentEntityReference(wikiMacro.getDocumentReference());

        // Test unregistration
        wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro from the root CM
        assertFalse(this.mocker.hasComponent(Macro.class, "testwikimacro"));
    }

    @Test
    public void registerWikiMacroWhenWikiVisibilityAndAllowed() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user who's allowed for the WIKI visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.WIKI)).thenReturn(true);

        ComponentManager wikiComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Macro.class);
        componentDescriptor.setRoleHint("testwikimacro");

        // Test registration
        WikiMacroManager wikiMacroManager = this.mocker.getComponentUnderTest();
        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the wiki CM
        verify(wikiComponentManager).registerComponent(any(DefaultComponentDescriptor.class), eq(wikiMacro));

        when(wikiComponentManager.<Macro>getComponentDescriptor(Macro.class, "testwikimacro"))
            .thenReturn(componentDescriptor);
        when(wikiComponentManager.getInstance(Macro.class, "testwikimacro")).thenReturn(wikiMacro);

        // Test unregistration
        wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro against the wiki CM
        verify(wikiComponentManager).unregisterComponent(Macro.class, "testwikimacro");
    }

    @Test
    public void registerWikiMacroWhenGlobalVisibilityOnSubWiki() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's allowed for the WIKI visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.WIKI)).thenReturn(true);

        // Indicate current wiki is a subwiki (so that it's not registered at root level)
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.isMainWiki(wikiMacro.getDocumentReference().getWikiReference().getName()))
            .thenReturn(false);

        ComponentManager wikiComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Macro.class);
        componentDescriptor.setRoleHint("testwikimacro");

        // Test registration
        WikiMacroManager wikiMacroManager = this.mocker.getComponentUnderTest();
        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the wiki CM
        verify(wikiComponentManager).registerComponent(componentDescriptor, wikiMacro);

        when(wikiComponentManager.<Macro>getComponentDescriptor(Macro.class, "testwikimacro"))
            .thenReturn(componentDescriptor);
        when(wikiComponentManager.getInstance(Macro.class, "testwikimacro")).thenReturn(wikiMacro);

        // Test unregistration
        wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro against the wiki CM
        verify(wikiComponentManager).unregisterComponent(Macro.class, "testwikimacro");
    }

    @Test
    public void registerWikiMacroWhenUserVisibilityAndAllowed() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user who's allowed for the USER visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.USER)).thenReturn(true);

        ComponentManager userComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "user");
        DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(Macro.class);
        componentDescriptor.setRoleHint("testwikimacro");

        // Test registration
        WikiMacroManager wikiMacroManager = this.mocker.getComponentUnderTest();
        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has registered the macro against the user CM
        verify(userComponentManager).registerComponent(any(DefaultComponentDescriptor.class), eq(wikiMacro));

        when(userComponentManager.<Macro>getComponentDescriptor(Macro.class, "testwikimacro"))
            .thenReturn(componentDescriptor);
        when(userComponentManager.getInstance(Macro.class, "testwikimacro")).thenReturn(wikiMacro);

        // Test unregistration
        wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        // Verify that the WikiMacroManager has unregistered the macro against the user CM
        verify(userComponentManager).unregisterComponent(Macro.class, "testwikimacro");
    }

    @Test(expected = InsufficientPrivilegesException.class)
    public void registerWikiMacroWhenGlobalVisibilityAndNotAllowed() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user who's not allowed for the GLOBAL visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.GLOBAL))
            .thenReturn(false);

        this.mocker.getComponentUnderTest().registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
    }

    @Test(expected = InsufficientPrivilegesException.class)
    public void registerWikiMacroWhenWikiVisibilityAndNotAllowed() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user who's not allowed for the WIKI visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.WIKI)).thenReturn(false);

        this.mocker.getComponentUnderTest().registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
    }

    @Test(expected = InsufficientPrivilegesException.class)
    public void registerWikiMacroWhenUserVisibilityAndNotAllowed() throws Exception
    {
        DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user who's not allowed for the USER visibility
        WikiMacroFactory wikiMacroFactory = this.mocker.getInstance(WikiMacroFactory.class);
        when(wikiMacroFactory.isAllowed(wikiMacro.getDocumentReference(), WikiMacroVisibility.USER)).thenReturn(false);

        this.mocker.getComponentUnderTest().registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
    }

    private DefaultWikiMacro generateWikiMacro(WikiMacroVisibility visibility) throws Exception
    {
        DocumentReference wikiMacroDocReference = new DocumentReference("wiki", Arrays.asList("space"), "space");
        WikiMacroDescriptor descriptor =
            new WikiMacroDescriptor(new MacroId("testwikimacro"), "Test Wiki Macro", "Description", "Test", visibility,
                new DefaultContentDescriptor(), Collections.<WikiMacroParameterDescriptor>emptyList());
        XDOM xdom = new XDOM(Arrays.asList(new ParagraphBlock(Arrays.<Block>asList(new WordBlock("test")))));

        DefaultWikiMacro wikiMacro = new DefaultWikiMacro(wikiMacroDocReference, authorReference, true, descriptor,
            xdom, Syntax.XWIKI_2_0, this.mocker);

        return wikiMacro;
    }
}
