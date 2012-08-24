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
package org.xwiki.component.internal;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.States;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link ContextComponentManager} which indirectly test
 * {@link org.xwiki.component.internal.WikiComponentManager} and
 * {@link org.xwiki.component.internal.UserComponentManager} (and their ancillary classes).
 * 
 * @version $Id$
 * @since 2.1RC1
 */
public class ContextComponentManagerTest extends AbstractComponentTestCase
{
    /**
     * Mock document access bridge.
     */
    private DocumentAccessBridge mockDocumentAccessBridge;

    public static interface Role
    {
    }

    public static class RoleImpl implements Role
    {
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Document Access Bridge Mock
        this.mockDocumentAccessBridge = registerMockComponent(DocumentAccessBridge.class);
    }

    @Test
    public void testRegisterComponentInUserComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        //@formatter:off
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getCurrentUser(); when(state.isNot("otheruser"));
                will(returnValue("user1")); 
        }});
        //@formatter:on

        ComponentManager userCM = getComponentManager().getInstance(ComponentManager.class, "user");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        userCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        state.become("otheruser");
        //@formatter:off
        getMockery().checking(new Expectations() {{
            oneOf(mockDocumentAccessBridge).getCurrentUser(); will(returnValue("user2")); 
            oneOf(mockDocumentAccessBridge).getCurrentWiki(); will(returnValue("wiki"));
        }});
        //@formatter:on

        try {
            contextCM.getInstance(Role.class);
            Assert.fail("Should have raised an exception");
        } catch (ComponentLookupException expected) {
            // No need to assert the message, we just want to ensure an exception is raised.
        }
    }

    @Test
    public void testRegisterComponentInWikiComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        //@formatter:off
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getCurrentWiki();  when(state.isNot("otherwiki"));
                will(returnValue("wiki1"));
            allowing(mockDocumentAccessBridge).getCurrentUser(); when(state.isNot("otherwiki"));
                will(returnValue("user")); 
        }});
        //@formatter:on

        // Register in the current wiki.
        ComponentManager wikiCM = getComponentManager().getInstance(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);
        wikiCM.registerComponent(cd);

        // Verify we can lookup the component from the context CM.
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another wiki in the context
        state.become("otherwiki");

        //@formatter:off
        getMockery().checking(new Expectations() {{
            oneOf(mockDocumentAccessBridge).getCurrentUser(); will(returnValue("user")); 
            allowing(mockDocumentAccessBridge).getCurrentWiki(); will(returnValue("wiki2"));
        }});
        //@formatter:on

        try {
            contextCM.getInstance(Role.class);
            Assert.fail("Should have raised an exception");
        } catch (ComponentLookupException expected) {
            // No need to assert the message, we just want to ensure an exception is raised.
        }
    }

    @Test
    public void testRegisterComponentInRootComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        //@formatter:off
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getCurrentWiki();  when(state.isNot("otherwiki"));
                will(returnValue("wiki"));
            allowing(mockDocumentAccessBridge).getCurrentUser(); when(state.isNot("otherwiki"));
                will(returnValue("user")); 
        }});
        //@formatter:on

        // Register in the current wiki.
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);
        getComponentManager().registerComponent(cd);

        // Verify we can lookup the component from the context CM.
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getInstance(Role.class));
    }

    @Test
    public void testRegisterComponentInContextComponentManagerThrowsException() throws Exception
    {
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        try {
            contextCM.registerComponent(new DefaultComponentDescriptor<Role>());
            Assert.fail("Should have thrown an exception error");
        } catch (RuntimeException expected) {
            Assert.assertEquals("The Context Component Manager should only be used for read access. Write operations "
                + "should be done against specific Component Managers.", expected.getMessage());
        }
    }
}
