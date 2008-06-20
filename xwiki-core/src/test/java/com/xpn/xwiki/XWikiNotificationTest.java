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
 *
 */
package com.xpn.xwiki;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * Verify that notifications are correctly sent in the {@link XWiki} class.
 * 
 * @version $Id$
 */
public class XWikiNotificationTest extends AbstractXWikiComponentTestCase
{
    public class TestListener implements XWikiDocChangeNotificationInterface
    {
        public boolean hasListenerBeenCalled = false;

        public boolean expectedNewStatus = true;

        public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
            XWikiContext context)
        {
            assertEquals("Space.Page", newdoc.getFullName());
            assertNotNull("Shouldn't have been null", olddoc);
            assertEquals("Should have been new, since this is a new document", this.expectedNewStatus, olddoc.isNew());
            this.hasListenerBeenCalled = true;
        }
    }

    XWikiContext context;

    XWiki xwiki;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.context = new XWikiContext();
        this.xwiki = new XWiki();
        this.context.setWiki(this.xwiki);

        this.xwiki.setNotificationManager(new XWikiNotificationManager());

        Mock mockStore = mock(XWikiStoreInterface.class);
        mockStore.expects(atLeastOnce()).method("saveXWikiDoc");
        this.xwiki.setStore((XWikiStoreInterface) mockStore.proxy());
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed tests of the notification
     * classes are implemented in the notification package.
     */
    public void testSaveDocumentSendNotifications() throws Exception
    {
        TestListener listener = new TestListener();
        this.xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument document = new XWikiDocument("Space", "Page");

        this.xwiki.saveDocument(document, this.context);
        assertTrue("Listener not called", listener.hasListenerBeenCalled);
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed tests of the notification
     * classes are implemented in the notification package.
     */
    public void testSaveDocumentFromAPIUsesCorrectOriginalDocument() throws Exception
    {
        Mock mockRights = mock(XWikiRightService.class);
        mockRights.stubs().method("hasAccessLevel").will(returnValue(true));
        this.xwiki.setRightService((XWikiRightService) mockRights.proxy());

        TestListener listener = new TestListener();
        listener.expectedNewStatus = false;
        this.xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument original = new XWikiDocument("Space", "Page");
        original.setNew(false);
        original.setContent("Old content");
        XWikiDocument document = new XWikiDocument("Space", "Page");
        document.setContent("New content");
        document.setOriginalDocument(original);

        Document api = new Document(document, this.context);
        api.save();
        assertTrue("Listener not called", listener.hasListenerBeenCalled);
    }
}
