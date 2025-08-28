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
package org.xwiki.wiki.internal.manager;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiCreateFailedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiCopier;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.manager.DefaultWikiManager}.
 *
 * @version $Id$
 * @since 6.0M1
 */
@ComponentTest
class DefaultWikiManagerTest
{
    @InjectMockComponents
    private DefaultWikiManager wikiManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private WikiCopier wikiCopier;

    @MockComponent
    private WikiDeleter wikiDeleter;

    @MockComponent
    private WikiCreator wikiCreator;

    @MockComponent
    private WikiConfiguration wikiConfiguration;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiStoreInterface store;

    @BeforeEach
    void setUp()
    {
        // Frequent uses
        this.xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        this.store = mock(XWikiStoreInterface.class);
        when(this.xwiki.getStore()).thenReturn(this.store);
    }

    @Test
    void idAvailable() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // Forbidden list
        when(this.xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid3,toto");
        when(this.store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);

        // When the wiki already exists
        when(this.wikiDescriptorManager.exists("wikiid1")).thenReturn(true);
        assertFalse(this.wikiManager.idAvailable("wikiid1"));

        // When the wiki does not already exists
        when(this.wikiDescriptorManager.exists("wikiid2")).thenReturn(false);
        assertTrue(this.wikiManager.idAvailable("wikiid2"));

        // When the wiki does not already exists but the id is forbidden
        when(this.wikiDescriptorManager.exists("wikiid3")).thenReturn(false);
        assertFalse(this.wikiManager.idAvailable("wikiid3"));
    }

    @Test
    void createWhenWikiExists() throws Exception
    {
        // When the wiki already exists
        when(this.wikiDescriptorManager.exists("wikiid1")).thenReturn(true);

        Throwable exception = assertThrows(WikiManagerException.class, () -> {
            this.wikiManager.create("wikiid1", "wikialias1", true);
        });
        assertEquals("The wiki id [wikiid1] is already used or is a reserved id, and thus is not available.",
            exception.getMessage());
    }

    @Test
    void createWhenWikiIdIsForbidden() throws Exception
    {
        // The wiki does not already exist
        when(this.wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // Forbidden list
        when(this.xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid1");

        Throwable exception = assertThrows(WikiManagerException.class, () -> {
            this.wikiManager.create("wikiid1", "wikialias1", true);
        });
        assertEquals("The wiki id [wikiid1] is already used or is a reserved id, and thus is not available.",
            exception.getMessage());
    }

    @Test
    void createWhenWikiIdIsValid() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // The wiki does not already exist
        when(this.wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // The wiki id is valid
        when(this.xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // The wiki name is available
        when(this.store.isWikiNameAvailable(eq("wikiid1"), any(XWikiContext.class))).thenReturn(true);

        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1", "owner");
        when(this.wikiCreator.create("wikiid1", "wikialias1", "owner")).thenReturn(descriptor);

        // Create
        WikiDescriptor newWikiDescriptor = this.wikiManager.create("wikiid1", "wikialias1", "owner", true);

        // Verify a descriptor has been returned
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki descriptor is an instance of DefaultWikiDescriptor
        assertInstanceOf(DefaultWikiDescriptor.class, newWikiDescriptor);

        // Verify that the wiki has been created
        verify(wikiCreator).create("wikiid1", "wikialias1", "owner");

        // Verify the events has been sent
        verify(observationManager).notify(new WikiCreatingEvent("wikiid1"), "wikiid1", xcontext);
        verify(observationManager).notify(new WikiCreatedEvent("wikiid1"), "wikiid1", xcontext);
    }

    @Test
    void createWhenWikiIdIsValidButFail() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // The wiki does not already exist
        when(this.wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // The wiki id is valid
        when(this.xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // The wiki name is available
        when(this.store.isWikiNameAvailable(eq("wikiid1"), any(XWikiContext.class))).thenReturn(true);

        when(this.wikiCreator.create("wikiid1", "wikialias1", "owner")).thenThrow(new WikiManagerException("error"));

        // Create
        Throwable exception = assertThrows(WikiManagerException.class, () -> {
            this.wikiManager.create("wikiid1", "wikialias1", "owner", true);
        });
        assertEquals("error", exception.getMessage());

        // Verify the events has been sent
        verify(this.observationManager).notify(new WikiCreatingEvent("wikiid1"), "wikiid1", xcontext);
        verify(this.observationManager).notify(new WikiCreateFailedEvent("wikiid1"), "wikiid1", xcontext);
    }

    @Test
    void deleteWiki() throws Exception
    {
        this.wikiManager.delete("wikiid");
        verify(this.wikiDeleter).delete("wikiid");
        verify(this.observationManager).notify(eq(new WikiDeletedEvent("wikiid")), eq("wikiid"));
    }

    @Test
    void copyWhenWikiAlreadyExists() throws Exception
    {
        when(this.store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);
        // The wiki already exists
        when(this.wikiDescriptorManager.exists("existingid")).thenReturn(true);

        Throwable exception = assertThrows(WikiManagerException.class, () -> {
            this.wikiManager.copy("wikiid", "existingid", "newwikialias", true, true, true);
        });
        assertEquals("The wiki id [existingid] is already used or is a reserved id, and thus is not available.",
            exception.getMessage());
    }

    @Test
    void copyWhenWikiAvailable() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // The wiki does not already exist
        when(this.wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // The new id is valid
        when(this.xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // The wiki name is available
        when(this.store.isWikiNameAvailable(eq("wikiid1"), any(XWikiContext.class))).thenReturn(true);

        // Other mocks
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(this.wikiCreator.create("wikiid1", "wikialias1", null)).thenReturn(descriptor);

        // Copy
        WikiDescriptor newWikiDescriptor = this.wikiManager.copy("wikiid", "wikiid1", "wikialias1", true, true, true);
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki has been created
        verify(this.wikiCreator).create("wikiid1", "wikialias1", null);
        // Verify that the wiki has been copied
        verify(this.wikiCopier).copyDocuments("wikiid", "wikiid1", true);
        // Verify that deleted documents has been copied too
        verify(this.wikiCopier).copyDeletedDocuments("wikiid", "wikiid1");
        // Verify that events has been sent
        verify(this.observationManager).notify(new WikiCopiedEvent("wikiid", "wikiid1"), "wikiid", this.xcontext);
    }
}

