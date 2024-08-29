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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiCreateFailedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiCopier;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
public class DefaultWikiManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiManager> mocker =
            new MockitoComponentMockingRule(DefaultWikiManager.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private ObservationManager observationManager;

    private Logger logger;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiStoreInterface store;

    private WikiCreator wikiCreator;

    private WikiCopier wikiCopier;

    private WikiDeleter wikiDeleter;

    private WikiConfiguration wikiConfiguration;

    @Before
    public void setUp() throws Exception
    {
        // Injection
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        observationManager = mocker.getInstance(ObservationManager.class);
        wikiCopier = mocker.getInstance(WikiCopier.class);
        wikiDeleter = mocker.getInstance(WikiDeleter.class);
        wikiCreator = mocker.getInstance(WikiCreator.class);
        this.wikiConfiguration = mocker.getInstance(WikiConfiguration.class);

        // Frequent uses
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
    }

    @Test
    public void idAvailable() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // Forbidden list
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid3,toto");
        when(store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);

        // When the wiki already exists
        when(wikiDescriptorManager.exists("wikiid1")).thenReturn(true);
        assertFalse(this.mocker.getComponentUnderTest().idAvailable("wikiid1"));

        // When the wiki does not already exists
        when(wikiDescriptorManager.exists("wikiid2")).thenReturn(false);
        assertTrue(this.mocker.getComponentUnderTest().idAvailable("wikiid2"));

        // When the wiki does not already exists but the id is forbidden
        when(wikiDescriptorManager.exists("wikiid3")).thenReturn(false);
        assertFalse(this.mocker.getComponentUnderTest().idAvailable("wikiid3"));
    }

    @Test
    public void createWhenWikiExists() throws Exception
    {
        // When the wiki already exists
        when(wikiDescriptorManager.exists("wikiid1")).thenReturn(true);

        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1", true);
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void createWhenWikiIdIsForbidden() throws Exception
    {
        // The wiki does not already exist
        when(wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // Forbidden list
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid1");

        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1", true);
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void createWhenWikiIdIsValid() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // The wiki does not already exist
        when(wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // The wiki id is valid
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // The wiki name is available
        when(store.isWikiNameAvailable(eq("wikiid1"), any(XWikiContext.class))).thenReturn(true);

        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1", "owner");
        when(wikiCreator.create("wikiid1", "wikialias1", "owner")).thenReturn(descriptor);

        // Create
        WikiDescriptor newWikiDescriptor =
            this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1", "owner", true);

        // Verify a descriptor has been returned
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki descriptor is an instance of DefaultWikiDescriptor
        assertTrue(newWikiDescriptor instanceof DefaultWikiDescriptor);

        // Verify that the wiki has been created
        verify(wikiCreator).create("wikiid1", "wikialias1", "owner");

        // Verify the events has been sent
        verify(observationManager).notify(new WikiCreatingEvent("wikiid1"), "wikiid1", xcontext);
        verify(observationManager).notify(new WikiCreatedEvent("wikiid1"), "wikiid1", xcontext);
    }

    @Test
    public void createWhenWikiIdIsValidButFail() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // The wiki does not already exist
        when(wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // The wiki id is valid
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // The wiki name is available
        when(store.isWikiNameAvailable(eq("wikiid1"), any(XWikiContext.class))).thenReturn(true);

        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1", "owner");
        when(wikiCreator.create("wikiid1", "wikialias1", "owner")).thenThrow(new WikiManagerException("..."));

        // Create
        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1", "owner", true);
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }

        // verify the exception
        assertTrue(exceptionCaught);

        // Verify the events has been sent
        verify(observationManager).notify(new WikiCreatingEvent("wikiid1"), "wikiid1", xcontext);
        verify(observationManager).notify(new WikiCreateFailedEvent("wikiid1"), "wikiid1", xcontext);
    }

    @Test
    public void deleteWiki() throws Exception
    {
        this.mocker.getComponentUnderTest().delete("wikiid");
        verify(wikiDeleter).delete("wikiid");
        verify(observationManager).notify(eq(new WikiDeletedEvent("wikiid")), eq("wikiid"));
    }

    @Test
    public void copyWhenWikiAlreadyExists() throws Exception
    {
        when(store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);
        // The wiki already exists
        when(wikiDescriptorManager.exists("existingid")).thenReturn(true);
        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().copy("wikiid", "existingid", "newwikialias", true, true, true);
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void copyWhenWikiAvailable() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // The wiki does not already exist
        when(wikiDescriptorManager.exists("wikiid1")).thenReturn(false);

        // The new id is valid
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // The wiki name is available
        when(store.isWikiNameAvailable(eq("wikiid1"), any(XWikiContext.class))).thenReturn(true);

        // Other mocks
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(wikiCreator.create("wikiid1", "wikialias1", null)).thenReturn(descriptor);

        // Copy
        WikiDescriptor newWikiDescriptor = this.mocker.getComponentUnderTest().copy("wikiid", "wikiid1",
                "wikialias1", true, true, true);
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki has been created
        verify(wikiCreator).create("wikiid1", "wikialias1", null);
        // Verify that the wiki has been copied
        verify(wikiCopier).copyDocuments(eq("wikiid"), eq("wikiid1"), eq(true));
        // Verify that deleted documents has been copied too
        verify(wikiCopier).copyDeletedDocuments(eq("wikiid"), eq("wikiid1"));
        // Verify that events has been sent
        verify(observationManager).notify(new WikiCopiedEvent("wikiid", "wikiid1"), "wikiid", xcontext);
    }
}

