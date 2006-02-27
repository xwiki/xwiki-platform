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
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.user.impl.exo;

import java.security.Principal;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.exception.ExoServiceException;
import org.exoplatform.services.security.SecurityService;
import org.securityfilter.realm.SimplePrincipal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;

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
