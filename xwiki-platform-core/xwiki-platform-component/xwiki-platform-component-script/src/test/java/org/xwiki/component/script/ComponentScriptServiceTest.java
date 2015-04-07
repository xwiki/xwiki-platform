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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.component.script.ComponentScriptService}.
 * 
 * @version $Id$
 * @since 4.1M2
 */
@ComponentList(ContextComponentManagerProvider.class)
public class ComponentScriptServiceTest
{
    /**
     * Used to test component lookup.
     */
    private interface SomeRole
    {
    }

    @Rule
    public MockitoComponentMockingRule<ComponentScriptService> mocker = new MockitoComponentMockingRule<>(
        ComponentScriptService.class);

    /**
     * Used to check programming rights.
     */
    private DocumentAccessBridge dab;

    /**
     * The mock component manager used by the script service under test.
     */
    private ComponentManager contextComponentManager;

    private ComponentManager contextrootComponentManager;

    private ComponentManager rootComponentManager;

    private ComponentManagerManager componentManagerManager;

    private Execution execution;

    @Before
    public void configure() throws Exception
    {
        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);

        this.contextComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "context");
        Provider<ComponentManager> contextComponentManagerProvider = this.mocker.registerMockComponent(new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextComponentManagerProvider.get()).thenReturn(this.contextComponentManager);

        this.contextrootComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "context/root");
        Provider<ComponentManager> contextrootComponentManagerProvider = this.mocker.registerMockComponent(new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context/root");
        when(contextrootComponentManagerProvider.get()).thenReturn(this.contextrootComponentManager);

        this.rootComponentManager = this.mocker.registerMockComponent(ComponentManager.class, "root");

        this.componentManagerManager = this.mocker.getInstance(ComponentManagerManager.class);

        when(this.componentManagerManager.getComponentManager(null, false)).thenReturn(this.rootComponentManager);

        this.execution = this.mocker.getInstance(Execution.class);
    }

    @Test
    public void getComponentManagerWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getComponentManager());
    }

    @Test
    public void getRootComponentManagerWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getRootComponentManager());
    }

    @Test
    public void getContextComponentManagerWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getContextComponentManager());
    }

    @Test
    public void getComponentManagerWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        assertSame(this.contextrootComponentManager, this.mocker.getComponentUnderTest().getComponentManager());
    }

    @Test
    public void getRootComponentManagerWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        assertSame(this.rootComponentManager, this.mocker.getComponentUnderTest().getRootComponentManager());
    }

    @Test
    public void getContextComponentManagerWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        assertSame(this.contextComponentManager, this.mocker.getComponentUnderTest().getContextComponentManager());
    }

    @Test
    public void getComponentManagerForNamespaceWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getComponentManager("wiki:xwiki"));

        ComponentManagerManager componentManagerManager = this.mocker.getInstance(ComponentManagerManager.class);
        verify(componentManagerManager, never()).getComponentManager(anyString(), anyBoolean());
    }

    @Test
    public void getComponentManagerForNamespaceWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        ComponentManager wikiComponentManager = mock(ComponentManager.class);
        when(this.componentManagerManager.getComponentManager("wiki:chess", false)).thenReturn(wikiComponentManager);

        assertSame(wikiComponentManager, this.mocker.getComponentUnderTest().getComponentManager("wiki:chess"));
    }

    @Test
    public void getComponentInstanceWithNoHintWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getInstance(SomeRole.class));

        verify(this.contextComponentManager, never()).getInstance(SomeRole.class);
    }

    @Test
    public void getComponentInstanceWithNoHintWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        SomeRole instance = mock(SomeRole.class);
        when(this.contextComponentManager.getInstance(SomeRole.class)).thenReturn(instance);

        assertSame(instance, this.mocker.getComponentUnderTest().getInstance(SomeRole.class));
    }

    @Test
    public void getComponentInstanceWithHintWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getInstance(SomeRole.class, "hint"));

        verify(this.contextComponentManager, never()).getInstance(SomeRole.class, "hint");
    }

    @Test
    public void getComponentInstanceWithHintWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        SomeRole instance = mock(SomeRole.class);
        when(this.contextComponentManager.getInstance(SomeRole.class, "hint")).thenReturn(instance);

        assertSame(instance, this.mocker.getComponentUnderTest().getInstance(SomeRole.class, "hint"));
    }

    @Test
    public void getComponentInstanceWhenComponentDoesntExist() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);
        when(this.contextComponentManager.getInstance(SomeRole.class)).thenThrow(new ComponentLookupException("error"));
        ExecutionContext context = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(context);

        assertNull(this.mocker.getComponentUnderTest().getInstance(SomeRole.class));
        assertEquals("error", this.mocker.getComponentUnderTest().getLastError().getMessage());
    }

    @Test
    public void getComponentInstanceWithHintWhenComponentDoesntExist() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);
        when(this.contextComponentManager.getInstance(SomeRole.class, "hint")).thenThrow(
            new ComponentLookupException("error"));
        ExecutionContext context = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(context);

        assertNull(this.mocker.getComponentUnderTest().getInstance(SomeRole.class, "hint"));
        assertEquals("error", this.mocker.getComponentUnderTest().getLastError().getMessage());
    }
}
