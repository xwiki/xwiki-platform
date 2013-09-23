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
package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import junit.framework.TestCase;

/**
 * Verify the {@link XWikiNotificationManager}
 * 
 * @version $Id$
 */
@Deprecated
public class XWikiNotificationManagerTest extends TestCase
{
    private XWikiNotificationManager notificationManager;

    @Override
    protected void setUp() throws Exception
    {
        notificationManager = new XWikiNotificationManager();
        super.setUp();
    }

    public void testRemoveRuleFromNamedRule()
    {
        XWikiNotificationRule rule1 = getDummyNotificationRule();
        XWikiNotificationRule rule2 = getDummyNotificationRule();
        assertNull(notificationManager.getNamedRules("testrule"));
        notificationManager.addNamedRule("testrule", rule1);
        notificationManager.addNamedRule("testrule", rule2);
        assertEquals(2, notificationManager.getNamedRules("testrule").size());
        notificationManager.removeNamedRule("testrule", rule1);
        assertEquals(1, notificationManager.getNamedRules("testrule").size());
        notificationManager.removeNamedRule("testrule", rule2);
        assertNull(notificationManager.getNamedRules("testrule"));
    }

    public void testRemoveNamedRule()
    {
        assertNull(notificationManager.getNamedRules("testrule"));
        notificationManager.addNamedRule("testrule", getDummyNotificationRule());
        notificationManager.addNamedRule("testrule", getDummyNotificationRule());
        assertEquals(2, notificationManager.getNamedRules("testrule").size());
        notificationManager.removeNamedRule("testrule");
        assertNull(notificationManager.getNamedRules("testrule"));
    }

    private XWikiNotificationRule getDummyNotificationRule()
    {
        return new XWikiNotificationRule()
        {
            @Override
            public void preverify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
            {
            }

            @Override
            public void preverify(XWikiDocument doc, String action, XWikiContext context)
            {
            }

            @Override
            public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
            {
            }

            @Override
            public void verify(XWikiDocument doc, String action, XWikiContext context)
            {
            }
        };
    }
}
