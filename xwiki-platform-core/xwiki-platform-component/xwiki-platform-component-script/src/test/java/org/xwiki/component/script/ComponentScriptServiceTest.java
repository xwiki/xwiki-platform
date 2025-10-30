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
package org.xwiki.component.script;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.PROGRAM;

/**
 * Unit tests for {@link ComponentScriptService}.
 *
 * @version $Id$
 * @since 4.1M2
 */
@ComponentTest
class ComponentScriptServiceTest
{
    /**
     * Used to test component lookup.
     */
    private interface SomeRole
    {
    }

    @InjectMockComponents
    private ComponentScriptService componentScriptService;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * The mock component manager used by the script service under test.
     */
    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    @Named("context/root")
    private ComponentManager contextrootComponentManager;

    @MockComponent
    @Named("root")
    private ComponentManager rootComponentManager;

    @MockComponent
    private ComponentManagerManager componentManagerManager;

    @MockComponent
    private Execution execution;

    @BeforeEach
    void setUp()
    {
        when(this.componentManagerManager.getComponentManager(null, false)).thenReturn(this.rootComponentManager);
    }

    @Test
    void getComponentManagerWhenNoProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(false);

        assertNull(this.componentScriptService.getComponentManager());
    }

    @Test
    void getRootComponentManagerWhenNoProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(false);

        assertNull(this.componentScriptService.getRootComponentManager());
    }

    @Test
    void getContextComponentManagerWhenNoProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(false);

        assertNull(this.componentScriptService.getContextComponentManager());
    }

    @Test
    void getComponentManagerWhenProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);

        assertSame(this.contextrootComponentManager, this.componentScriptService.getComponentManager());
    }

    @Test
    void getRootComponentManagerWhenProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);

        assertSame(this.rootComponentManager, this.componentScriptService.getRootComponentManager());
    }

    @Test
    void getContextComponentManagerWhenProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);

        assertSame(this.contextComponentManager, this.componentScriptService.getContextComponentManager());
    }

    @Test
    void getComponentManagerForNamespaceWhenNoProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(false);

        assertNull(this.componentScriptService.getComponentManager("wiki:xwiki"));

        verify(this.componentManagerManager, never()).getComponentManager(any(), anyBoolean());
    }

    @Test
    void getComponentManagerForNamespaceWhenProgrammingRights()
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);

        ComponentManager wikiComponentManager = mock(ComponentManager.class);
        when(this.componentManagerManager.getComponentManager("wiki:chess", false)).thenReturn(wikiComponentManager);

        assertSame(wikiComponentManager, this.componentScriptService.getComponentManager("wiki:chess"));
    }

    @Test
    void getComponentInstanceWithNoHintWhenNoProgrammingRights() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(false);

        assertNull(this.componentScriptService.getInstance(SomeRole.class));

        verify(this.contextComponentManager, never()).getInstance(SomeRole.class);
    }

    @Test
    void getComponentInstanceWithNoHintWhenProgrammingRights() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);

        SomeRole instance = mock(SomeRole.class);
        when(this.contextComponentManager.getInstance(SomeRole.class)).thenReturn(instance);

        assertSame(instance, this.componentScriptService.getInstance(SomeRole.class));
    }

    @Test
    void getComponentInstanceWithHintWhenNoProgrammingRights() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(false);

        assertNull(this.componentScriptService.getInstance(SomeRole.class, "hint"));

        verify(this.contextComponentManager, never()).getInstance(SomeRole.class, "hint");
    }

    @Test
    void getComponentInstanceWithHintWhenProgrammingRights() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);

        SomeRole instance = mock(SomeRole.class);
        when(this.contextComponentManager.getInstance(SomeRole.class, "hint")).thenReturn(instance);

        assertSame(instance, this.componentScriptService.getInstance(SomeRole.class, "hint"));
    }

    @Test
    void getComponentInstanceWhenComponentDoesntExist() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);
        when(this.contextComponentManager.getInstance(SomeRole.class)).thenThrow(new ComponentLookupException("error"));
        ExecutionContext context = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(context);

        assertNull(this.componentScriptService.getInstance(SomeRole.class));
        assertEquals("error", this.componentScriptService.getLastError().getMessage());
    }

    @Test
    void getComponentInstanceWithHintWhenComponentDoesntExist() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(PROGRAM)).thenReturn(true);
        when(this.contextComponentManager.getInstance(SomeRole.class, "hint")).thenThrow(
            new ComponentLookupException("error"));
        ExecutionContext context = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(context);

        assertNull(this.componentScriptService.getInstance(SomeRole.class, "hint"));
        assertEquals("error", this.componentScriptService.getLastError().getMessage());
    }
}
