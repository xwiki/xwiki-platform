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
 * @author sdumitriu
 */

package com.xpn.xwiki.user.impl.exo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;

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
