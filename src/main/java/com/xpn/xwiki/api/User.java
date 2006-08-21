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
 * @author kaaloo
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
		return null;
    }
    
    public boolean isUserInGroup(String groupName) throws XWikiException {
    	XWikiGroupService groupService = context.getWiki().getGroupService(context);
    	Collection groups = groupService.listGroupsForUser(user.getUser(), context);
    	return groups.contains(groupName);
    }

}
