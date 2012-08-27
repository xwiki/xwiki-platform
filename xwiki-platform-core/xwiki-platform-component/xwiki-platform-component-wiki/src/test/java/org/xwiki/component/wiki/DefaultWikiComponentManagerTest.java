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
package org.xwiki.component.wiki;

import java.lang.reflect.Type;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponent;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

@MockingRequirement(DefaultWikiComponentManager.class)
public class DefaultWikiComponentManagerTest extends AbstractMockingComponentTestCase
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private final DefaultWikiComponent component = new DefaultWikiComponent(DOC_REFERENCE, TestRole.class, "roleHint");

    private WikiComponentManager manager;

    @Before
    public void configure() throws Exception
    {
        this.manager = getComponentManager().getInstance(WikiComponentManager.class);
    }

    @Test
    public void registerWikiComponent() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class);

        getMockery().checking(new Expectations()
        {{
            // The test is here
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
        }});

        this.manager.registerWikiComponent(this.component);
    }

    @Test
    public void registerAlreadyRegisteredWikiComponent() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
        }});

        this.manager.registerWikiComponent(this.component);

        try {
            this.manager.registerWikiComponent(this.component);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("Component already registered. Try unregistering it first.", expected.getMessage());
        }
    }

    @Test
    public void unregisterWikiComponent() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
            oneOf(componentManager).unregisterComponent((Type) TestRole.class, "roleHint");
        }});

        this.manager.registerWikiComponent(this.component);

        // This call would throw an exception if the component had not been unregistered
        this.manager.unregisterWikiComponents(DOC_REFERENCE);
    }

    @Test
    public void unregisterNotRegisteredWikiComponent() throws Exception
    {
        // We check that this doesn't do anything, silently
        this.manager.unregisterWikiComponents(DOC_REFERENCE);
    }
}
