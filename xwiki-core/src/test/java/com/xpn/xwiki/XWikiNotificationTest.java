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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.store.XWikiStoreInterface;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Verify that notifications are correctly sent in the {@link XWiki} class.
 *
 * @version $Id$
 */
public class XWikiNotificationTest extends MockObjectTestCase
{
    public class TestListener implements XWikiDocChangeNotificationInterface
    {
        public boolean hasListenerBeenCalled = false;

        public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc,
            int event, XWikiContext context)
        {
            assertEquals("Space.Page", newdoc.getFullName());
            assertNotNull("Shouldn't have been null", olddoc);
            assertTrue("Should have been new, since this is a new document", olddoc.isNew());
            this.hasListenerBeenCalled = true;
        }
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed
     * tests of the notification classes are implemented in the notification package.
     */
    public void testSaveDocumentSendNotifications() throws Exception
    {
        XWikiContext context = new XWikiContext();
        XWiki wiki = new XWiki(new XWikiConfig(), context);
        wiki.setNotificationManager(new XWikiNotificationManager());

        Mock mockStore = mock(XWikiStoreInterface.class);
        mockStore.expects(once()).method("saveXWikiDoc");
        wiki.setStore((XWikiStoreInterface) mockStore.proxy());

        TestListener listener = new TestListener();
        wiki.getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument document = new XWikiDocument("Space", "Page");

        wiki.saveDocument(document, context);
        assertTrue("Listener not called",listener.hasListenerBeenCalled);
    }
}
