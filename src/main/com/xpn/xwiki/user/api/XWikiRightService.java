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
 * Date: 4 juin 2004
 * Time: 08:29:10
 */
package com.xpn.xwiki.user.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public interface XWikiRightService {
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException;
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context) throws XWikiException;
    public boolean hasProgrammingRights(XWikiContext context);
    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context);
    public boolean hasAdminRights(XWikiContext context);
}
