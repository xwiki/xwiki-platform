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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.test.jmock.AbstractComponentTestCase;

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

    private EntityReferenceValueProvider mockCurrentValueProvider;

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
        this.mockCurrentValueProvider = registerMockComponent(EntityReferenceValueProvider.class, "current");
    }

    @Test
    public void testRegisterComponentInUserComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                when(state.isNot("otheruser"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "user1")));
            }
        });

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
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user2")));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.WIKI);
                will(returnValue("wiki"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.SPACE);
                will(returnValue("space"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.DOCUMENT);
                will(returnValue("document"));
            }
        });

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

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.WIKI);
                when(state.isNot("otherwiki"));
                will(returnValue("wiki1"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.SPACE);
                when(state.isNot("otherwiki"));
                will(returnValue("space1"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.DOCUMENT);
                when(state.isNot("otherwiki"));
                will(returnValue("document1"));
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                when(state.isNot("otherwiki"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
            }
        });

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

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.WIKI);
                will(returnValue("wiki2"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.SPACE);
                will(returnValue("space2"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.DOCUMENT);
                will(returnValue("document2"));
            }
        });

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

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.WIKI);
                when(state.isNot("otherwiki"));
                will(returnValue("wiki"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.SPACE);
                when(state.isNot("otherwiki"));
                will(returnValue("space"));
                allowing(mockCurrentValueProvider).getDefaultValue(EntityType.DOCUMENT);
                when(state.isNot("otherwiki"));
                will(returnValue("document"));
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                when(state.isNot("otherwiki"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
            }
        });

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

    @Test(expected = RuntimeException.class)
    public void testRegisterComponentInstance() throws ComponentLookupException, Exception
    {
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");

        contextCM.registerComponent(null, null);
    }

    @Test(expected = RuntimeException.class)
    public void testRegisterComponentDesciptor() throws ComponentLookupException, Exception
    {
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");

        contextCM.registerComponent(null);
    }

    @Test(expected = RuntimeException.class)
    public void testSetComponentEventManager() throws ComponentLookupException, Exception
    {
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");

        contextCM.setComponentEventManager(null);
    }

    @Test(expected = RuntimeException.class)
    public void testSetParent() throws ComponentLookupException, Exception
    {
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");

        contextCM.setParent(null);
    }
}
