/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * User: ludovic
 * Date: 15 mars 2004
 * Time: 01:54:43
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class Api {
    protected XWikiContext context;

    public Api(XWikiContext context) {
       this.context = context;
    }

    public boolean checkProgrammingRights() {
        return hasProgrammingRights();
    }

    public boolean hasProgrammingRights() {
            com.xpn.xwiki.XWiki xwiki = context.getWiki();
            return xwiki.getRightService().hasProgrammingRights(context);
    }

    public boolean hasAdminRights() {
            com.xpn.xwiki.XWiki xwiki = context.getWiki();
            return xwiki.getRightService().hasAdminRights(context);
    }

    public boolean hasAccessLevel(String right, String docname) throws XWikiException {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        return xwiki.getRightService().hasAccessLevel(right, context.getUser(), docname, context);        
    }

}
