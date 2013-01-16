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
import org.xwiki.component.wiki.internal.DefaultWikiComponentManager;
import org.xwiki.component.wiki.internal.WikiComponentManagerContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

@MockingRequirement(DefaultWikiComponentManager.class)
public class DefaultWikiComponentManagerTest extends AbstractMockingComponentTestCase
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private final WikiComponent component = new TestImplementation();

    private ComponentManager rootComponentManager;

    private ComponentManager componentManager;

    private WikiComponentManager wikiComponentManager;

    private WikiComponentManagerContext wikiComponentManagerContext;

    @Before
    public void configure() throws Exception
    {
        this.wikiComponentManager = getComponentManager().getInstance(WikiComponentManager.class);
        this.rootComponentManager = getComponentManager().getInstance(ComponentManager.class);
        this.componentManager =
            getComponentManager().registerMockComponent(this.getMockery(), ComponentManager.class, "wiki");
        this.wikiComponentManagerContext = getComponentManager().getInstance(WikiComponentManagerContext.class);

        getMockery().checking(new Expectations()
        {{
            allowing(wikiComponentManagerContext).getCurrentUserReference();
            will(returnValue(AUTHOR_REFERENCE));
            allowing(wikiComponentManagerContext).getCurrentEntityReference();
            will(returnValue(DOC_REFERENCE));
            allowing(wikiComponentManagerContext).setCurrentEntityReference(DOC_REFERENCE);
            allowing(wikiComponentManagerContext).setCurrentUserReference(AUTHOR_REFERENCE);
        }});
    }

    @Test
    public void registerWikiComponent() throws Exception
    {
        getMockery().checking(new Expectations()
        {{
            oneOf(rootComponentManager).getInstance(ComponentManager.class, "wiki");
            will(returnValue(componentManager));
            // The test is here
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
        }});

        this.wikiComponentManager.registerWikiComponent(this.component);
    }

    @Test
    public void registerAlreadyRegisteredWikiComponent() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class, "wiki");

        getMockery().checking(new Expectations()
        {{
            allowing(rootComponentManager).getInstance(ComponentManager.class, "wiki");
            will(returnValue(componentManager));
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
        }});

        this.wikiComponentManager.registerWikiComponent(this.component);
        this.wikiComponentManager.registerWikiComponent(this.component);
    }

    @Test
    public void unregisterWikiComponent() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class, "wiki");

        getMockery().checking(new Expectations()
        {{
            allowing(rootComponentManager).getInstance(ComponentManager.class, "wiki");
            will(returnValue(componentManager));
            oneOf(componentManager).registerComponent(with(any(ComponentDescriptor.class)), with(any(Object.class)));
            oneOf(componentManager).unregisterComponent((Type) TestRole.class, "roleHint");
        }});

        this.wikiComponentManager.registerWikiComponent(this.component);

        // This call would throw an exception if the component had not been unregistered
        this.wikiComponentManager.unregisterWikiComponents(DOC_REFERENCE);
    }

    @Test
    public void unregisterNotRegisteredWikiComponent() throws Exception
    {
        // We check that this doesn't do anything, silently
        this.wikiComponentManager.unregisterWikiComponents(DOC_REFERENCE);
    }
}
