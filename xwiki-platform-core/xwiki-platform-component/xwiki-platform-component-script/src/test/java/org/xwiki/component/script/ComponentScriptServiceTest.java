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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.component.script.ComponentScriptService}.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class ComponentScriptServiceTest
{
    /**
     * Used to test component lookup.
     */
    private interface SomeRole
    {
    }

    @Rule
    public MockitoComponentMockingRule<ComponentScriptService> mocker =
        new MockitoComponentMockingRule<ComponentScriptService>(ComponentScriptService.class);

    /**
     * Used to check programming rights.
     */
    private DocumentAccessBridge dab;

    /**
     * The mock component manager used by the script service under test.
     */
    private ComponentManager contextComponentManager;

    @Before
    public void configure() throws Exception
    {
        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        this.contextComponentManager = this.mocker.getInstance(ComponentManager.class, "context");
    }

    @Test
    public void getComponentManagerWhenNoProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getComponentManager());
    }

    @Test
    public void getComponentManagerWhenProgrammingRights() throws Exception
    {
        when(this.dab.hasProgrammingRights()).thenReturn(true);

        assertEquals(this.contextComponentManager, this.mocker.getComponentUnderTest().getComponentManager());
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

        ComponentManagerManager componentManagerManager = this.mocker.getInstance(ComponentManagerManager.class);
        ComponentManager wikiComponentManager = mock(ComponentManager.class);
        when(componentManagerManager.getComponentManager("wiki:chess", false)).thenReturn(wikiComponentManager);

        assertEquals(wikiComponentManager, this.mocker.getComponentUnderTest().getComponentManager("wiki:chess"));
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

        assertEquals(instance, this.mocker.getComponentUnderTest().getInstance(SomeRole.class));
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

        assertEquals(instance, this.mocker.getComponentUnderTest().getInstance(SomeRole.class, "hint"));
    }
}
