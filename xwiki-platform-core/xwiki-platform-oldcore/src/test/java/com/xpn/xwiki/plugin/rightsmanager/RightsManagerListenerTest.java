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
package com.xpn.xwiki.plugin.rightsmanager;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.internal.wiki.WikiOwnerUpdater;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RightsManagerListener}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class RightsManagerListenerTest
{
    private static final DocumentReference DELETED_USER = new DocumentReference("xwiki", "XWiki", "DeletedUser");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private WikiOwnerUpdater wikiOwnerUpdater;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private ObservationContext observationContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext context;

    private final Set<String> wikis = new LinkedHashSet<>();

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.oldcore.getWikiDescriptorManager().getAllIds()).thenReturn(this.wikis);
        this.context = this.oldcore.getXWikiContext();
        this.wikis.add("xwiki");
        this.wikis.add("subwiki");
    }

    private void deleteUser(DocumentReference userReference) throws Exception
    {
        XWikiDocument userDocument = new XWikiDocument(userReference);
        userDocument.newXObject(MockitoOldcore.USER_CLASS, this.context);
        XWikiDocument deletedDocument = new XWikiDocument(userReference);
        deletedDocument.setOriginalDocument(userDocument);

        RightsManagerListener.getInstance().onEvent(new DocumentDeletedEvent(userReference), deletedDocument,
            this.context);
    }

    @Test
    void onEventCleansWikiOwnersGroupsAndRights() throws Exception
    {
        deleteUser(DELETED_USER);

        verify(this.wikiOwnerUpdater).replaceOwner(DELETED_USER, null);
        verify(this.oldcore.getMockGroupService(), times(2)).removeUserOrGroupFromAllGroups("xwiki", "XWiki",
            "DeletedUser", this.context);
        assertEquals("xwiki", this.context.getWikiId());
    }

    @Test
    void onEventWhenAWikiFails() throws Exception
    {
        doThrow(new XWikiException()).doNothing().when(this.oldcore.getMockGroupService())
            .removeUserOrGroupFromAllGroups("xwiki", "XWiki", "DeletedUser", this.context);

        deleteUser(DELETED_USER);

        // The other wiki is still cleaned.
        verify(this.oldcore.getMockGroupService(), times(2)).removeUserOrGroupFromAllGroups("xwiki", "XWiki",
            "DeletedUser", this.context);
        assertEquals(1, this.logCapture.size());
        // The wikis are not ordered, so the failing one is not known.
        assertTrue(this.logCapture.getMessage(0)
            .startsWith("Failed to clean the rights and groups of [xwiki:XWiki.DeletedUser] in the wiki ["));
        assertEquals("xwiki", this.context.getWikiId());
    }
}
