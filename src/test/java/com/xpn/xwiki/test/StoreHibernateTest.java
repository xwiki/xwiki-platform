/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
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
