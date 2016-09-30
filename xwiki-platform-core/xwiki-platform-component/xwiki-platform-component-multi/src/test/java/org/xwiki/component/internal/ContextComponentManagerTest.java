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

import javax.inject.Provider;

import org.jmock.Expectations;
import org.jmock.States;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.jmock.AbstractComponentTestCase;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    private WikiDescriptorManager mockWikiDescriptorManager;

    private Provider<DocumentReference> mockCurrentDocumentReferenceProvider;

    private Provider<SpaceReference> mockCurrentSpaceReferenceProvider;

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
        this.mockWikiDescriptorManager = registerMockComponent(WikiDescriptorManager.class);
        this.mockCurrentDocumentReferenceProvider = registerMockComponent(DocumentReference.TYPE_PROVIDER, "current");
        this.mockCurrentSpaceReferenceProvider = registerMockComponent(SpaceReference.TYPE_PROVIDER, "current");
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // Enabled component registration events
        StackingComponentEventManager eventManager = new StackingComponentEventManager();
        eventManager
            .setObservationManager(getComponentManager().<ObservationManager>getInstance(ObservationManager.class));
        eventManager.shouldStack(false);
        getComponentManager().setComponentEventManager(eventManager);
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
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space", new WikiReference("wiki"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki", "space", "document")));
            }
        });

        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");

        // Lookup not yet registered component (and feel the caches with null CMs)

        try {
            contextCM.getInstance(Role.class);
            Assert.fail("Should have raised an exception");
        } catch (ComponentLookupException expected) {
            // No need to assert the message, we just want to ensure an exception is raised.
        }

        // Register component for the current user
        ComponentManager userCM = getComponentManager().getInstance(ComponentManager.class, "user");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);
        userCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        Assert.assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        state.become("otheruser");
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user2")));
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space", new WikiReference("wiki"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki", "space", "document")));
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
    public void testRegisterComponentInDocumentComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                when(state.isNot("otherdocument"));
                will(returnValue("wiki1"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                when(state.isNot("otherdocument"));
                will(returnValue(new SpaceReference("space1", new WikiReference("wiki"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                when(state.isNot("otherdocument"));
                will(returnValue(new DocumentReference("wiki1", "space1", "document1")));
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                when(state.isNot("otherdocument"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
            }
        });

        ComponentManager documentCM = getComponentManager().getInstance(ComponentManager.class, "document");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        documentCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        state.become("otherdocument");
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki2"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space2", new WikiReference("wiki2"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki2", "space2", "document2")));
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
    public void testDeleteDocument() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space", new WikiReference("wiki"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki", "space", "document")));
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
            }
        });

        ComponentManager documentCM = getComponentManager().getInstance(ComponentManager.class, "document");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        documentCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getComponentDescriptor(Role.class, "default"));

        ObservationManager observationManager = getComponentManager().getInstance(ObservationManager.class);

        observationManager.notify(new DocumentDeletedEvent(new DocumentReference("wiki", "space", "document")), null,
            null);

        Assert.assertNull(contextCM.getComponentDescriptor(Role.class, "default"));
    }

    @Test
    public void testRegisterComponentInSpaceComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                when(state.isNot("otherspace"));
                will(returnValue("wiki1"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                when(state.isNot("otherspace"));
                will(returnValue(new SpaceReference("space1", new WikiReference("wiki1"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                when(state.isNot("otherspace"));
                will(returnValue(new DocumentReference("wiki1", "space1", "document1")));
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                when(state.isNot("otherspace"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
            }
        });

        ComponentManager userCM = getComponentManager().getInstance(ComponentManager.class, "space");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRole(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        userCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        state.become("otherspace");
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki2"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space2", new WikiReference("wiki2"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki2", "space2", "document2")));
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
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                when(state.isNot("otherwiki"));
                will(returnValue("wiki1"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                when(state.isNot("otherwiki"));
                will(returnValue(new SpaceReference("space1", new WikiReference("wiki1"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                when(state.isNot("otherwiki"));
                will(returnValue(new DocumentReference("wiki1", "space1", "document1")));
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
        Assert.assertNotNull(contextCM.getComponentDescriptor(Role.class, "default"));

        // Now verify that we cannot look it up anymore if there's another wiki in the context
        state.become("otherwiki");

        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki2"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space2", new WikiReference("wiki2"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki2", "space2", "document2")));
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
    public void testDeleteWiki() throws ComponentLookupException, Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                will(returnValue(new SpaceReference("space", new WikiReference("wiki"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                will(returnValue(new DocumentReference("wiki", "space", "document")));
                allowing(mockDocumentAccessBridge).getCurrentUserReference();
                will(returnValue(new DocumentReference("wiki", "XWiki", "user")));
            }
        });

        // Register in the current wiki.
        ComponentManager wikiCM = getComponentManager().getInstance(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<Role>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);
        wikiCM.registerComponent(cd);

        ComponentManager contextCM = getComponentManager().getInstance(ComponentManager.class, "context");
        Assert.assertNotNull(contextCM.getComponentDescriptor(Role.class, "default"));

        ObservationManager observationManager = getComponentManager().getInstance(ObservationManager.class);

        observationManager.notify(new WikiDeletedEvent("wiki"), null, null);

        Assert.assertNull(contextCM.getComponentDescriptor(Role.class, "default"));
    }

    @Test
    public void testRegisterComponentInRootComponentManager() throws Exception
    {
        final States state = getMockery().states("test");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                when(state.isNot("otherwiki"));
                will(returnValue("wiki"));
                allowing(mockCurrentSpaceReferenceProvider).get();
                when(state.isNot("otherwiki"));
                will(returnValue(new SpaceReference("space", new WikiReference("wiki"))));
                allowing(mockCurrentDocumentReferenceProvider).get();
                when(state.isNot("otherwiki"));
                will(returnValue(new DocumentReference("wiki", "space", "document")));
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
    public void testCreateDocumentComponentManager() throws Exception
    {
        ComponentManagerManager manager = getComponentManager().getInstance(ComponentManagerManager.class);

        NamespacedComponentManager componentManager =
            (NamespacedComponentManager) manager.getComponentManager("document:wiki1:space1.space2.document1", true);

        assertNotNull(componentManager);
        assertEquals("document:wiki1:space1.space2.document1", componentManager.getNamespace());
        assertEquals("space:wiki1:space1.space2", ((NamespacedComponentManager)componentManager.getParent()).getNamespace());
        assertEquals("space:wiki1:space1", ((NamespacedComponentManager)componentManager.getParent().getParent()).getNamespace());
        assertEquals("wiki:wiki1", ((NamespacedComponentManager)componentManager.getParent().getParent().getParent()).getNamespace());
    }

    // Failures

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
