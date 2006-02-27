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
 * @author ludovic
 * @author vmassol
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import java.util.List;

public class HibernateClassesTest extends HibernateTestCase {

    public void testDBListDisplayers() throws XWikiException, HibernateException {

        getXWiki().getUserClass(getXWikiContext());

        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseObject obj = doc.getObject(doc.getxWikiClass().getName(), 0);

        ClassesTest.testDisplayer("dblist", obj, doc.getxWikiClass(),
            "XWikiUsers", "<option selected='selected' value='XWikiUsers' label='XWikiUsers'>", getXWikiContext());
    }

    public void testCreateAndDeleteClass() throws XWikiException, HibernateException {
        XWiki xwiki = getXWiki();
        xwiki.getUserClass(getXWikiContext());
        List list = xwiki.getClassList(context);
        assertTrue("List should contain user class", list.contains("XWiki.XWikiUsers"));
        XWikiDocument doc = xwiki.getDocument("XWiki.XWikiUsers", context);
        xwiki.deleteDocument(doc, context);
        list = xwiki.getClassList(context);
        assertFalse("List should not contain user class", list.contains("XWiki.XWikiUsers"));
        xwiki.getUserClass(getXWikiContext());
        list = xwiki.getClassList(context);
        assertTrue("List should contain user class", list.contains("XWiki.XWikiUsers"));
    }

}
