/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 2 mai 2005
 * Time: 09:42:25
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

public class GroovyTestRightService implements XWikiRightService {
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException {
        return true;
    }

    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context) throws XWikiException {
        return true;
    }

    public boolean hasProgrammingRights(XWikiContext context) {
        return true;
    }

    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context) {
        return true;
    }

    public boolean hasAdminRights(XWikiContext context) {
        return true;
    }
}
