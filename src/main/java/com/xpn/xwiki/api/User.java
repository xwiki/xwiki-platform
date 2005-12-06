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
 * Date: 26 mars 2004
 * Time: 09:50:16
 */

package com.xpn.xwiki.api;

import java.util.Collection;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiUser;

public class User extends Api {
    private XWikiUser user;

    public User(XWikiUser user, XWikiContext context) {
       super(context);
       this.user = user;
    }

    public XWikiUser getUser() {
        if (checkProgrammingRights())
            return user;
        else
            return null;
    }
    
    public boolean isUserInGroup(String groupName) throws XWikiException {
    	XWikiGroupService groupService = context.getWiki().getGroupService();
    	Collection groups = groupService.listGroupsForUser(user.getUser(), context);
    	return groups.contains(groupName);
    }

}
