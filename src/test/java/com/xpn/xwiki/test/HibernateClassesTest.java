/**
 * ===================================================================
 *
 * Copyright (c) 2005 Ludovic Dubost, All rights reserved.
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
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.*;
import org.hibernate.HibernateException;

public class HibernateClassesTest extends HibernateTestCase {

    public void testDBListDisplayers() throws XWikiException, HibernateException {

        getXWiki().getUserClass(getXWikiContext());
        
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseObject obj = doc.getObject(doc.getxWikiClass().getName(), 0);

        ClassesTest.testDisplayer("dblist", obj, doc.getxWikiClass(),
            "XWikiUsers", "<option selected='selected' value='XWikiUsers' label='XWikiUsers'>", getXWikiContext());
    }
}
