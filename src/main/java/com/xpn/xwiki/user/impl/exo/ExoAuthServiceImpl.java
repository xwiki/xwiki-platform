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
 * Time: 10:48:20
 */
package com.xpn.xwiki.user.impl.exo;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import org.securityfilter.realm.SimplePrincipal;
import org.exoplatform.services.security.SecurityService;
import org.exoplatform.services.exception.ExoServiceException;
import org.exoplatform.container.PortalContainer;

import java.security.Principal;

public class ExoAuthServiceImpl extends XWikiAuthServiceImpl {
    private SecurityService securityService;

    protected SecurityService getSecurityService() {
       if (securityService==null) {
           PortalContainer manager = PortalContainer.getInstance();
           securityService = (SecurityService) manager.getComponentInstanceOfType(SecurityService.class);
       }
       return securityService;
   }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException {
        if (context.getMode()==XWikiContext.MODE_PORTLET) {
            String user = context.getRequest().getRemoteUser();
            if ((user==null)||user.equals(""))
             user = "XWiki.XWikiGuest";
            else
             user = "XWiki." + user;
            context.setUser(user);
            return new XWikiUser(user);
        } else {
            XWikiUser user = super.checkAuth(context);
            if (user==null)
                return new XWikiUser("XWiki.XWikiGuest");
            else
                return new XWikiUser("XWiki." + user.getUser());
        }
    }

    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        String superadmin = "superadmin";
        if (username.equals(superadmin)) {
            String superadminpassword = context.getWiki().Param("xwiki.superadminpassword");
            if ((superadminpassword!=null)&&(superadminpassword.equals(password))) {
                return new SimplePrincipal("XWiki.superadmin");
            } else {
                return null;
            }
        }

        try {
            if (getSecurityService().authenticate(username, password))
                return new SimplePrincipal(username);
        } catch (ExoServiceException e) {
            e.printStackTrace();
        }
        return null;
    }
}
