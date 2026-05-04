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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContextComponentManager} which indirectly test
 * {@link org.xwiki.component.internal.WikiComponentManager} and
 * {@link org.xwiki.component.internal.UserComponentManager} (and their ancillary classes).
 *
 * @version $Id$
 * @since 2.1RC1
 */
@ComponentTest
@AllComponents
class ContextComponentManagerTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("current")
    private Provider<DocumentReference> currentDocumentReferenceProvider;

    @MockComponent
    @Named("current")
    private Provider<SpaceReference> currentSpaceReferenceProvider;

    public interface Role
    {
    }

    public static class RoleImpl implements Role
    {
    }

    @BeforeEach
    void setUp() throws Exception
    {
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ecm.initialize(new ExecutionContext());

        // Enabled component registration events
        QueueComponentEventManager eventManager = new QueueComponentEventManager();
        eventManager.setObservationManager(this.componentManager.getInstance(ObservationManager.class));
        eventManager.shouldQueue(false);
        this.componentManager.setComponentEventManager(eventManager);
    }

    @Test
    void testRegisterComponentInUserComponentManager() throws Exception
    {
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user1"));
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space", new WikiReference("wiki")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki", "space", "document"));

        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");

        // Lookup not yet registered component (and feel the caches with null CMs)
        assertThrows(ComponentLookupException.class, () -> contextCM.getInstance(Role.class));

        // Register component for the current user
        ComponentManager userCM = this.componentManager.getInstance(ComponentManager.class, "user");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);
        userCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user2"));
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space", new WikiReference("wiki")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki", "space", "document"));

        assertThrows(ComponentLookupException.class, () -> contextCM.getInstance(Role.class));
    }

    @Test
    void testRegisterComponentInDocumentComponentManager() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space1", new WikiReference("wiki")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki1", "space1", "document1"));
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));

        ComponentManager documentCM = this.componentManager.getInstance(ComponentManager.class, "document");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        documentCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki2");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space2", new WikiReference("wiki2")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki2", "space2", "document2"));

        assertThrows(ComponentLookupException.class, () -> contextCM.getInstance(Role.class));
    }

    @Test
    void testDeleteDocument() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space", new WikiReference("wiki")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki", "space", "document"));
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));

        ComponentManager documentCM = this.componentManager.getInstance(ComponentManager.class, "document");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        documentCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        assertNotNull(contextCM.getComponentDescriptor(Role.class, "default"));

        ObservationManager observationManager = this.componentManager.getInstance(ObservationManager.class);

        observationManager.notify(new DocumentDeletedEvent(new DocumentReference("wiki", "space", "document")), null,
            null);

        assertNull(contextCM.getComponentDescriptor(Role.class, "default"));
    }

    @Test
    void testRegisterComponentInSpaceComponentManager() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space1", new WikiReference("wiki1")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki1", "space1", "document1"));
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));

        ComponentManager userCM = this.componentManager.getInstance(ComponentManager.class, "space");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);

        // Register component for the current user
        userCM.registerComponent(cd);

        // Verify we can lookup the component from the Context CM
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        assertNotNull(contextCM.getInstance(Role.class));

        // Now verify that we cannot look it up anymore if there's another user in the context
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki2");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space2", new WikiReference("wiki2")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki2", "space2", "document2"));

        assertThrows(ComponentLookupException.class, () -> contextCM.getInstance(Role.class));
    }

    @Test
    void testRegisterComponentInWikiComponentManager() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space1", new WikiReference("wiki1")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki1", "space1", "document1"));
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));

        // Register in the current wiki.
        ComponentManager wikiCM = this.componentManager.getInstance(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);
        wikiCM.registerComponent(cd);

        // Verify we can lookup the component from the context CM.
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        assertNotNull(contextCM.getComponentDescriptor(Role.class, "default"));

        // Now verify that we cannot look it up anymore if there's another wiki in the context
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki2");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space2", new WikiReference("wiki2")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki2", "space2", "document2"));

        assertThrows(ComponentLookupException.class, () -> contextCM.getInstance(Role.class));
    }

    @Test
    void testDeleteWiki() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space", new WikiReference("wiki")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki", "space", "document"));
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));

        // Register in the current wiki.
        ComponentManager wikiCM = this.componentManager.getInstance(ComponentManager.class, "wiki");
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);
        wikiCM.registerComponent(cd);

        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        assertNotNull(contextCM.getComponentDescriptor(Role.class, "default"));

        ObservationManager observationManager = this.componentManager.getInstance(ObservationManager.class);

        observationManager.notify(new WikiDeletedEvent("wiki"), null, null);

        assertNull(contextCM.getComponentDescriptor(Role.class, "default"));
    }

    @Test
    void testRegisterComponentInRootComponentManager() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.currentSpaceReferenceProvider.get())
            .thenReturn(new SpaceReference("space", new WikiReference("wiki")));
        when(this.currentDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("wiki", "space", "document"));
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("wiki", "XWiki", "user"));

        // Register in the current wiki.
        DefaultComponentDescriptor<Role> cd = new DefaultComponentDescriptor<>();
        cd.setRoleType(Role.class);
        cd.setImplementation(RoleImpl.class);
        this.componentManager.registerComponent(cd);

        // Verify we can lookup the component from the context CM.
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        assertNotNull(contextCM.getInstance(Role.class));
    }

    @Test
    void testCreateDocumentComponentManager() throws Exception
    {
        ComponentManagerManager manager = this.componentManager.getInstance(ComponentManagerManager.class);

        NamespacedComponentManager componentManagerResult =
            (NamespacedComponentManager) manager.getComponentManager("document:wiki1:space1.space2.document1", true);

        assertNotNull(componentManagerResult);
        assertEquals("document:wiki1:space1.space2.document1", componentManagerResult.getNamespace());
        assertEquals("space:wiki1:space1.space2",
            ((NamespacedComponentManager) componentManagerResult.getParent()).getNamespace());
        assertEquals("space:wiki1:space1",
            ((NamespacedComponentManager) componentManagerResult.getParent().getParent()).getNamespace());
        assertEquals("wiki:wiki1",
            ((NamespacedComponentManager) componentManagerResult.getParent().getParent().getParent()).getNamespace());
    }

    // Failures

    @Test
    void testRegisterComponentInContextComponentManagerThrowsException() throws Exception
    {
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> contextCM.registerComponent(new DefaultComponentDescriptor<Role>()));
        assertEquals(
            "The Context Component Manager should only be used for read access. Write operations "
                + "should be done against specific Component Managers.",
            exception.getMessage());
    }

    @Test
    void testRegisterComponentInstance() throws Exception
    {
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");

        assertThrows(RuntimeException.class, () -> contextCM.registerComponent(null, null));
    }

    @Test
    void testRegisterComponentDescriptor() throws Exception
    {
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");

        assertThrows(RuntimeException.class, () -> contextCM.registerComponent(null));
    }

    @Test
    void testSetComponentEventManager() throws Exception
    {
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");

        assertThrows(RuntimeException.class, () -> contextCM.setComponentEventManager(null));
    }

    @Test
    void testSetParent() throws Exception
    {
        ComponentManager contextCM = this.componentManager.getInstance(ComponentManager.class, "context");

        assertThrows(RuntimeException.class, () -> contextCM.setParent(null));
    }
}
