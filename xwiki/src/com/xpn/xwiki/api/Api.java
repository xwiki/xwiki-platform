/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * User: ludovic
 * Date: 15 mars 2004
 * Time: 01:54:43
 */

package com.xpn.xwiki.api;

import com.opensymphony.module.access.NotFoundException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;

public class Api {
    protected XWikiContext context;

    public Api(XWikiContext context) {
       this.context = context;
    }

    public boolean checkProgrammingRights() {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        XWikiDocInterface doc = context.getDoc();
        String username = doc.getAuthor();

        String docname;
        if (context.getDatabase()!=null) {
          docname = context.getDatabase() + ":" + doc.getFullName();
          username = context.getDatabase() + ":" + username;
        }
        else
          docname = doc.getFullName();

        try {
            return xwiki.getAccessmanager().userHasAccessLevel(username, docname, "programming");
        } catch (NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

}
