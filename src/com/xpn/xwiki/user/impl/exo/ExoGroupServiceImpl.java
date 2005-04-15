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
 * Date: 5 juin 2004
 * Time: 10:48:33
 */
package com.xpn.xwiki.user.impl.exo;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.user.api.XWikiGroupService;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.Collection;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.container.PortalContainer;

public class ExoGroupServiceImpl implements XWikiGroupService {
    private OrganizationService organizationService;

    protected OrganizationService getOrganizationService() {
       if (organizationService==null) {
           PortalContainer manager = PortalContainer.getInstance();
           organizationService = (OrganizationService) manager.getComponentInstanceOfType(OrganizationService.class);
       }
       return organizationService;
   }

    public void init(XWiki xwiki) {
    }

    public void flushCache() {
    }

    public Collection listGroupsForUser(String username, XWikiContext context) throws XWikiException {
        try {
            if (username.startsWith("XWiki."))
                username = username.substring(6);
            Collection groups = getOrganizationService().findGroupsOfUser(username);
	    ArrayList list = new ArrayList();
	    if (groups==null)
		    return list;
	    Iterator it = groups.iterator();
	    while (it.hasNext()) {
	       Group group = (Group)it.next();
   	       list.add(group.getGroupName());	       
	    }
	    return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addUserToGroup(String user, String database, String group) {
    }
}
