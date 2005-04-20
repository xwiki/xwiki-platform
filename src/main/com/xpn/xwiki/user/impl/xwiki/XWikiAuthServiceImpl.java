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
 * Time: 08:57:02
 */
package com.xpn.xwiki.user.impl.xwiki;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SimplePrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

public class XWikiAuthServiceImpl implements XWikiAuthService {
    private static final Log log = LogFactory.getLog(XWikiAuthServiceImpl.class);

    protected Authenticator authenticator;

    protected Authenticator getAuthenticator(XWikiContext context) throws XWikiException {
        if (authenticator!=null)
            return authenticator;

        try {
            XWiki xwiki = context.getWiki();

            if ("basic".equals(xwiki.Param("xwiki.authentication"))) {
                authenticator = new MyBasicAuthenticator();
                SecurityConfig sconfig = new SecurityConfig(false);
                sconfig.setAuthMethod("BASIC");
                if (xwiki.Param("xwiki.authentication.realname")!=null)
                    sconfig.setRealmName(xwiki.Param("xwiki.authentication.realname"));
                else
                    sconfig.setRealmName("XWiki");
                authenticator.init(null, sconfig);
            } else {
                authenticator =  new MyFormAuthenticator();
                SecurityConfig sconfig = new SecurityConfig(false);
                sconfig.setAuthMethod("FORM");
                if (xwiki.Param("xwiki.authentication.realname")!=null)
                    sconfig.setRealmName(xwiki.Param("xwiki.authentication.realname"));
                else
                    sconfig.setRealmName("XWiki");
                if (xwiki.Param("xwiki.authentication.defaultpage")!=null)
                    sconfig.setDefaultPage(xwiki.Param("xwiki.authentication.defaultpage"));
                else
                    sconfig.setDefaultPage("/bin/view/Main/WebHome");
                if (xwiki.Param("xwiki.authentication.loginpage")!=null)
                    sconfig.setLoginPage(xwiki.Param("xwiki.authentication.loginpage"));
                else
                    sconfig.setLoginPage("/bin/login/XWiki/XWikiLogin");
                if (xwiki.Param("xwiki.authentication.logoutpage")!=null)
                    sconfig.setLogoutPage(xwiki.Param("xwiki.authentication.logoutpage"));
                else
                    sconfig.setLogoutPage("/bin/logout/XWiki/XWikiLogout");
                if (xwiki.Param("xwiki.authentication.errorpage")!=null)
                    sconfig.setErrorPage(xwiki.Param("xwiki.authentication.errorpage"));
                else
                    sconfig.setErrorPage("/bin/loginerror/XWiki/XWikiLogin");


                MyPersistentLoginManager persistent = new MyPersistentLoginManager();
                if (xwiki.Param("xwiki.authentication.cookiepath")!=null)
                    persistent.setCookiePath(xwiki.Param("xwiki.authentication.cookiepath"));
                if (xwiki.Param("xwiki.authentication.cookiedomains")!=null) {
                    String[] cdomains = StringUtils.split(xwiki.Param("xwiki.authentication.cookiedomains"),",");
                    persistent.setCookieDomains(cdomains);
                }
                if (xwiki.Param("xwiki.authentication.cookielife")!=null)
                    persistent.setCookieLife(xwiki.Param("xwiki.authentication.cookielife"));
                if (xwiki.Param("xwiki.authentication.protection")!=null)
                    persistent.setProtection(xwiki.Param("xwiki.authentication.protection"));
                if (xwiki.Param("xwiki.authentication.useip")!=null)
                    persistent.setUseIP(xwiki.Param("xwiki.authentication.useip"));
                if (xwiki.Param("xwiki.authentication.encryptionalgorithm")!=null)
                    persistent.setEncryptionAlgorithm(xwiki.Param("xwiki.authentication.encryptionalgorithm"));
                if (xwiki.Param("xwiki.authentication.encryptionmode")!=null)
                    persistent.setEncryptionMode(xwiki.Param("xwiki.authentication.encryptionmode"));
                if (xwiki.Param("xwiki.authentication.encryptionpadding")!=null)
                    persistent.setEncryptionPadding(xwiki.Param("xwiki.authentication.encryptionpadding"));
                if (xwiki.Param("xwiki.authentication.validationKey")!=null)
                    persistent.setValidationKey(xwiki.Param("xwiki.authentication.validationKey"));
                if (xwiki.Param("xwiki.authentication.encryptionKey")!=null)
                    persistent.setEncryptionKey(xwiki.Param("xwiki.authentication.encryptionKey"));
                sconfig.setPersistentLoginManager(persistent);

                MyFilterConfig fconfig = new MyFilterConfig();
                if (xwiki.Param("xwiki.authentication.loginsubmitpage")!=null)
                    fconfig.setInitParameter("loginSubmitPattern", xwiki.Param("xwiki.authentication.loginsubmitpage"));
                else
                    fconfig.setInitParameter("loginSubmitPattern", "/login/XWiki/XWikiLogin");
                authenticator.init(fconfig, sconfig);
            }

            return authenticator;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                    XWikiException.ERROR_XWIKI_USER_INIT,
                    "Cannot initialize authentication system",e);
        }
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        HttpServletRequest request = null;
        HttpServletResponse response = null;

        if (context.getRequest()!=null)
         request = context.getRequest().getHttpServletRequest();

        if (context.getResponse()!=null)
         response = context.getResponse().getHttpServletResponse();

        if (request==null)
            return null;

        Authenticator auth = getAuthenticator(context);
        SecurityRequestWrapper wrappedRequest = new SecurityRequestWrapper(request, null,
                null, auth.getAuthMethod());
        try {

            // Process login out (this only works with FORMS
            if (auth.processLogout(wrappedRequest, response, xwiki.getUrlPatternMatcher())) {
                if (log.isInfoEnabled()) log.info("User " + context.getUser() + " has been logged-out");
                wrappedRequest.setUserPrincipal(null);
                return null;
            }

            if (auth.getAuthMethod().equals("BASIC")) {
                if (((MyBasicAuthenticator)auth).processLogin(wrappedRequest, response, context)) {
                    return null;
                }
            } else {
                if (((MyFormAuthenticator)auth).processLogin(wrappedRequest, response, context)) {
                    return null;
                }
            }

           Principal user = wrappedRequest.getUserPrincipal();
           if (log.isInfoEnabled()) {
               if (user!=null)
                   log.info("User " + user.getName() + " is authentified");
           }

           if (user==null)
            return null;
           else
            return new XWikiUser(user.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showLogin(XWikiContext context) throws XWikiException {
        try {
            if (context.getMode()==XWikiContext.MODE_SERVLET) {
             getAuthenticator(context).showLogin(context.getRequest().getHttpServletRequest(),
                                         context.getResponse().getHttpServletResponse());
            }
        } catch (IOException e) {
            // If this fails we continue
            e.printStackTrace();
        }
    }

    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        Principal principal = null;

        if (username==null)
            return null;

        String superadmin = "superadmin";
        if (username.equals(superadmin)) {
            String superadminpassword = context.getWiki().Param("xwiki.superadminpassword");
            if ((superadminpassword!=null)&&(superadminpassword.equals(password))) {
                principal = new SimplePrincipal("XWiki.superadmin");
                return principal;
            } else {
                return null;
            }
        }

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        if (context!=null) {
            String susername = username;
            int i = username.indexOf(".");
            if (i!=-1)
                susername = username.substring(i+1);

            // First we check in the local database
            try {
                String user = findUser(susername, context);
                if (user!=null) {
                    if (checkPassword(user, password, context))
                        principal = new SimplePrincipal(user);
                }
            } catch (Exception e) {}

            if (context.isVirtual()) {
                if (principal==null) {
                    // Then we check in the main database
                    String db = context.getDatabase();
                    try {
                        context.setDatabase(context.getWiki().getDatabase());
                        try {
                            String user = findUser(susername, context);
                            if (user==null)
                                return null;
                            if (checkPassword(user, password, context))
                                principal = new SimplePrincipal(context.getDatabase() + ":" + user);
                        } catch (Exception e) {}
                    } finally {
                        context.setDatabase(db);
                    }
                }
            }
        }
        return principal;
    }

    protected String findUser(String susername, XWikiContext context) throws XWikiException {
        // First lets look in the cache
        if (context.getWiki().exists("XWiki."  + susername, context))
            return "XWiki." + susername;

        String sql = "select distinct doc.web, doc.name from XWikiDocument as doc where doc.web='XWiki' and doc.name like '" + susername + "'";
        List list = context.getWiki().search(sql, context);
        if (list.size()==0)
            return null;
        if (list.size()>1) {
            for (int i=0;i<list.size();i++) {
                Object[] result = (Object[]) list.get(0);
                if (result[1].equals(susername))
                    return result[0] + "." + result[1];
            }
        }
        Object[] result = (Object[]) list.get(0);
        return result[0] + "." + result[1];
    }

    protected boolean checkPassword(String username, String password, XWikiContext context) throws XWikiException {
        try {
            boolean result = false;
            XWikiDocument doc = context.getWiki().getDocument(username, context);
            // We only allow empty password from users having a XWikiUsers object.
            if (doc.getObject("XWiki.XWikiUsers")!=null) {
              String passwd = doc.getStringValue("XWiki.XWikiUsers", "password");
              result = (password.equals(passwd));
            }

            if (log.isDebugEnabled()) {
                if (result)
                 log.debug("(debug) Password check for user " + username + " successfull");
                else
                 log.debug("(debug) Password check for user " + username + " failed");
            }

            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }


}
