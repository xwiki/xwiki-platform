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
package org.xwiki.component.internal.script;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import junit.framework.Assert;

/**
 * Unit tests for {@link org.xwiki.component.internal.script.ComponentScriptService}.
 *
 * @version $Id$
 * @since 4.1M2
 */
public class ComponentScriptServiceTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private ComponentScriptService css;

    private interface SomeRole
    {
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
        final ComponentManager cm = getComponentManager().getInstance(ComponentManager.class);
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
        final ComponentManager cm = getComponentManager().getInstance(ComponentManager.class);
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
