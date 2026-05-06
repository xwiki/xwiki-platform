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
package com.xpn.xwiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;

import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verify that notifications are correctly sent in the {@link XWiki} class.
 * 
 * @version $Id$
 */
@Deprecated
@OldcoreTest(mockXWiki = false)
@AllComponents
class XWikiNotificationTest
{
    class TestListener implements XWikiDocChangeNotificationInterface
    {
        boolean hasListenerBeenCalled = false;

        boolean expectedNewStatus = true;

        @Override
        public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
            XWikiContext context)
        {
            assertEquals("Space.Page", newdoc.getFullName());
            assertNotNull(olddoc, "Shouldn't have been null");
            assertEquals(this.expectedNewStatus, olddoc.isNew(),
                "Should have been new, since this is a new document");
            this.hasListenerBeenCalled = true;
        }
    }

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.xwiki = this.oldcore.getSpyXWiki();
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed tests of the notification
     * classes are implemented in the notification package.
     */
    @Test
    void saveDocumentSendNotifications() throws Exception
    {
        TestListener listener = new TestListener();
        this.xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument document = new XWikiDocument(new DocumentReference("WikiDescriptor", "Space", "Page"));

        this.xwiki.saveDocument(document, this.oldcore.getXWikiContext());
        assertTrue(listener.hasListenerBeenCalled, "Listener not called");
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed tests of the notification
     * classes are implemented in the notification package.
     */
    @Test
    void saveDocumentFromAPIUsesCorrectOriginalDocument() throws Exception
    {
        XWikiRightService mockRights = this.oldcore.getMockRightService();
        when(mockRights.hasAccessLevel(anyString(), anyString(), anyString(), any())).thenReturn(true);
        when(mockRights.hasProgrammingRights(any())).thenReturn(true);

        TestListener listener = new TestListener();
        listener.expectedNewStatus = false;
        this.xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument original = new XWikiDocument(new DocumentReference("WikiDescriptor", "Space", "Page"));
        original.setNew(false);
        original.setContent("Old content");
        XWikiDocument document = original.clone();
        document.setContent("New content");
        document.setOriginalDocument(original);

        Document api = new Document(document, this.oldcore.getXWikiContext());
        api.save();
        assertTrue(listener.hasListenerBeenCalled, "Listener not called");
    }
}
