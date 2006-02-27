/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author vmassol
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiLock;

public class StoreHibernateTest extends HibernateTestCase {

    public void testLockReadWrite() throws XWikiException {
        XWikiLock lock = new XWikiLock(1, "AnyUser");

        getXWiki().getStore().saveLock(lock, getXWikiContext(), true);

        XWikiLock newlock = getXWiki().getStore().loadLock(1, getXWikiContext(), true);
        assertEquals("Same user", newlock.getUserName(), lock.getUserName());
        assertTrue("Same date", Math.abs(newlock.getDate().getTime()-lock.getDate().getTime())<1000);

        getXWiki().getStore().deleteLock(lock, getXWikiContext(), true);

        XWikiLock testlock = getXWiki().getStore().loadLock(1, getXWikiContext(), true);
        assertEquals("No lock", null, testlock);
    }

}
