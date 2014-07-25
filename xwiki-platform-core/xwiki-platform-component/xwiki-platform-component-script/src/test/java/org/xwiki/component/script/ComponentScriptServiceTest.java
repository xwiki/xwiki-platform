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

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.script.ComponentScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

import org.junit.Assert;

/**
 * Unit tests for {@link org.xwiki.component.script.ComponentScriptService}.
 *
 * @version $Id$
 * @since 4.1M2
 */
@MockingRequirement(ComponentScriptService.class)
public class ComponentScriptServiceTest extends AbstractMockingComponentTestCase
{
    private ComponentScriptService css;

    private interface SomeRole
    {
    }

    @Before
    public void configure() throws Exception
    {
        this.css = getComponentManager().getInstance(ScriptService.class, "component");
    }

    @Test
    public void getComponentManagerWhenNoProgrammingRights() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
        }});

        Assert.assertNull(this.css.getComponentManager());
    }

    @Test
    public void getComponentInstanceWithNoHintWhenNoProgrammingRights() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
        }});

        Assert.assertNull(this.css.getInstance(SomeRole.class));
    }

    @Test
    public void getComponentInstanceWithNoHintWhenProgrammingRights() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        final ComponentManager cm = getComponentManager().getInstance(ComponentManager.class, "context");
        getMockery().checking(new Expectations() {{
            oneOf(dab).hasProgrammingRights();
            will(returnValue(true));
            oneOf(cm).getInstance(SomeRole.class);
            will(returnValue(getMockery().mock(SomeRole.class)));
        }});

        Assert.assertTrue(this.css.getInstance(SomeRole.class) instanceof SomeRole);
    }

    @Test
    public void getComponentInstanceWithHintWhenNoProgrammingRights() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(dab).hasProgrammingRights();
            will(returnValue(false));
        }});

        Assert.assertNull(this.css.getInstance(SomeRole.class, "hint"));
    }

    @Test
    public void getComponentInstanceWithHintWhenProgrammingRights() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        final ComponentManager cm = getComponentManager().getInstance(ComponentManager.class, "context");
        getMockery().checking(new Expectations() {{
            oneOf(dab).hasProgrammingRights();
            will(returnValue(true));
            oneOf(cm).getInstance(SomeRole.class, "hint");
            will(returnValue(getMockery().mock(SomeRole.class)));
        }});

        Assert.assertTrue(this.css.getInstance(SomeRole.class, "hint") instanceof SomeRole);
    }

    @Test
    public void getComponentManagerWhenProgrammingRights() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(dab).hasProgrammingRights();
            will(returnValue(true));
        }});

        Assert.assertTrue(this.css.getComponentManager() instanceof ComponentManager);
    }
}
