/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */

package com.xpn.xwiki.user.impl.exo;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.SecurityService;
import org.securityfilter.realm.SimplePrincipal;

import java.security.Principal;
import java.util.HashMap;

public class ExoAuthServiceImpl extends XWikiAuthServiceImpl {
    private SecurityService securityService_;
    private static final Log log = LogFactory.getLog(ExoAuthServiceImpl.class);

    protected SecurityService getSecurityService() {
        if (securityService_ == null) {
            PortalContainer manager = PortalContainer.getInstance();
            securityService_ = (SecurityService) manager.getComponentInstanceOfType(SecurityService.class);
        }
        return securityService_;
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException {
        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            String user = context.getRequest().getRemoteUser();
            if ((user == null) || user.equals(""))
                user = XWikiRightService.GUEST_USER_FULLNAME;
            else
                user = "XWiki." + user;
            context.setUser(user);
            return new XWikiUser(user);
        } else {
            XWikiUser user = super.checkAuth(context);
            if (user == null)
                return new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME);
            else
                return new XWikiUser("XWiki." + user.getUser());
        }
    }

    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context) throws XWikiException {
        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            String user = context.getRequest().getRemoteUser();
            if ((user == null) || user.equals(""))
                user = XWikiRightService.GUEST_USER_FULLNAME;
            else
                user = "XWiki." + user;
            context.setUser(user);
            return new XWikiUser(user);
        } else {
            XWikiUser user = super.checkAuth(username, password, rememberme, context);
            if (user == null)
                return new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME);
            else
                return new XWikiUser("XWiki." + user.getUser());
        }
    }

    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {

        // Trim the username to allow users to enter their names with spaces before or after
        String cannonicalUsername = (username==null) ? null : username.replaceAll(" ", "");

        if (isSuperAdmin(cannonicalUsername)) {
            return authenticateSuperAdmin(password, context);
        }

        SecurityService securityService = getSecurityService();
        try {
            if (securityService.authenticate(cannonicalUsername, password))
                return new SimplePrincipal(cannonicalUsername);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        if(context != null){
            String susername = cannonicalUsername;
            int i = username.indexOf(".");
            if (i!=-1) susername = cannonicalUsername.substring(i+1);
            String exo = getExo_DN(susername,context) ;

        }
        */
        return null;
    }

    public String getExo_DN(String susername, XWikiContext context) {
        String EX = null;
        if (context != null) {
            // First we check in the local database
            try {
                String user = findUser(susername, context);
                if (user != null && user.length() != 0) {
                    EX = readExo_DN(user, context);
                }
            } catch (Exception e) {
            }

            if (!context.isMainWiki()) {
                if (EX == null || EX.length() == 0) {
                    // Then we check in the main database
                    String db = context.getDatabase();
                    try {
                        context.setDatabase(context.getMainXWiki());
                        try {
                            String user = findUser(susername, context);
                            if (user != null && user.length() != 0)
                                EX = readExo_DN(user, context);
                        } catch (Exception e) {
                        }
                    } finally {
                        context.setDatabase(db);
                    }
                }
            }
        }
        return EX;
    }

    private String readExo_DN(String username, XWikiContext context) {
        String EX = null;
        try {
            XWikiDocument doc = context.getWiki().getDocument(username, context);
            // We only allow empty password from users having a XWikiUsers object.
            if (doc.getObject("XWiki.XWikiUsers") != null) {
                EX = doc.getStringValue("XWiki.XWikiUsers", "exo_ex");
            }

        } catch (Throwable e) {
        }
        return EX;
    }

    private void CreateUserFromExo(String susername, HashMap attributes, XWikiContext context) throws XWikiException {
        UserHandler userHandler = ExoGroupServiceImpl.getOrganizationService().getUserHandler();
        User user = userHandler.createUserInstance();


    }

    protected String getParam(String name, XWikiContext context) {
        String param = "";
        try {
            param = context.getWiki().getXWikiPreference(name, context);
        } catch (Exception e) {
        }
        if (param == null || "".equals(param)) {
            try {
                param = context.getWiki().Param("xwiki.authentication." + StringUtils.replace(name, "exo_", "exo."));
            } catch (Exception e) {
            }
        }
        if (param == null)
            param = "";
        return param;
    }


}
