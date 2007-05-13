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

package com.xpn.xwiki.user.impl.xwiki;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SimplePrincipal;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.plugin.ldap.LDAPPlugin;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

public class XWikiAuthServiceImpl implements XWikiAuthService
{
    private static final Log log = LogFactory.getLog(XWikiAuthServiceImpl.class);

    protected XWikiAuthenticator authenticator;

    protected XWikiAuthenticator getAuthenticator(XWikiContext context) throws XWikiException
    {
        if (authenticator != null)
            return authenticator;

        try {
            XWiki xwiki = context.getWiki();

            if ("basic".equals(xwiki.Param("xwiki.authentication"))) {
                authenticator = new MyBasicAuthenticator();
                SecurityConfig sconfig = new SecurityConfig(false);
                sconfig.setAuthMethod("BASIC");
                if (xwiki.Param("xwiki.authentication.realname") != null)
                    sconfig.setRealmName(xwiki.Param("xwiki.authentication.realname"));
                else
                    sconfig.setRealmName("XWiki");
                authenticator.init(null, sconfig);
            } else {
                authenticator = new MyFormAuthenticator();
                SecurityConfig sconfig = new SecurityConfig(false);

                sconfig.setAuthMethod("FORM");

                if (xwiki.Param("xwiki.authentication.realname") != null)
                    sconfig.setRealmName(xwiki.Param("xwiki.authentication.realname"));
                else
                    sconfig.setRealmName("XWiki");

                if (xwiki.Param("xwiki.authentication.defaultpage") != null)
                    sconfig.setDefaultPage(xwiki.Param("xwiki.authentication.defaultpage"));
                else
                    sconfig.setDefaultPage("/bin/view/Main/WebHome");

                if (xwiki.Param("xwiki.authentication.loginpage") != null)
                    sconfig.setLoginPage(xwiki.Param("xwiki.authentication.loginpage"));
                else
                    sconfig.setLoginPage("/bin/login/XWiki/XWikiLogin");

                if (xwiki.Param("xwiki.authentication.logoutpage") != null)
                    sconfig.setLogoutPage(xwiki.Param("xwiki.authentication.logoutpage"));
                else
                    sconfig.setLogoutPage("/bin/logout/XWiki/XWikiLogout");

                if (xwiki.Param("xwiki.authentication.errorpage") != null)
                    sconfig.setErrorPage(xwiki.Param("xwiki.authentication.errorpage"));
                else
                    sconfig.setErrorPage("/bin/loginerror/XWiki/XWikiLogin");

                MyPersistentLoginManager persistent = new MyPersistentLoginManager();
                if (xwiki.Param("xwiki.authentication.cookiepath") != null)
                    persistent.setCookiePath(xwiki.Param("xwiki.authentication.cookiepath"));
                if (xwiki.Param("xwiki.authentication.cookiedomains") != null) {
                    String[] cdomains =
                        StringUtils.split(xwiki.Param("xwiki.authentication.cookiedomains"), ",");
                    persistent.setCookieDomains(cdomains);
                }

                if (xwiki.Param("xwiki.authentication.cookielife") != null)
                    persistent.setCookieLife(xwiki.Param("xwiki.authentication.cookielife"));

                if (xwiki.Param("xwiki.authentication.protection") != null)
                    persistent.setProtection(xwiki.Param("xwiki.authentication.protection"));

                if (xwiki.Param("xwiki.authentication.useip") != null)
                    persistent.setUseIP(xwiki.Param("xwiki.authentication.useip"));

                if (xwiki.Param("xwiki.authentication.encryptionalgorithm") != null)
                    persistent.setEncryptionAlgorithm(xwiki
                        .Param("xwiki.authentication.encryptionalgorithm"));

                if (xwiki.Param("xwiki.authentication.encryptionmode") != null)
                    persistent.setEncryptionMode(xwiki
                        .Param("xwiki.authentication.encryptionmode"));

                if (xwiki.Param("xwiki.authentication.encryptionpadding") != null)
                    persistent.setEncryptionPadding(xwiki
                        .Param("xwiki.authentication.encryptionpadding"));

                if (xwiki.Param("xwiki.authentication.validationKey") != null)
                    persistent
                        .setValidationKey(xwiki.Param("xwiki.authentication.validationKey"));

                if (xwiki.Param("xwiki.authentication.encryptionKey") != null)
                    persistent
                        .setEncryptionKey(xwiki.Param("xwiki.authentication.encryptionKey"));

                sconfig.setPersistentLoginManager(persistent);

                MyFilterConfig fconfig = new MyFilterConfig();
                if (xwiki.Param("xwiki.authentication.loginsubmitpage") != null)
                    fconfig.setInitParameter("loginSubmitPattern", xwiki
                        .Param("xwiki.authentication.loginsubmitpage"));
                else
                    fconfig.setInitParameter("loginSubmitPattern",
                        "/loginsubmit/XWiki/XWikiLogin");

                authenticator.init(fconfig, sconfig);
            }

            return authenticator;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                XWikiException.ERROR_XWIKI_USER_INIT,
                "Cannot initialize authentication system",
                e);
        }
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        HttpServletRequest request = null;
        HttpServletResponse response = null;

        if (context.getRequest() != null)
            request = context.getRequest().getHttpServletRequest();

        if (context.getResponse() != null)
            response = context.getResponse().getHttpServletResponse();

        if (request == null)
            return null;

        XWikiAuthenticator auth = getAuthenticator(context);
        SecurityRequestWrapper wrappedRequest =
            new SecurityRequestWrapper(request, null, null, auth.getAuthMethod());
        try {

            // Process login out (this only works with FORMS
            if (auth.processLogout(wrappedRequest, response, xwiki.getUrlPatternMatcher())) {
                if (log.isInfoEnabled())
                    log.info("User " + context.getUser() + " has been logged-out");
                wrappedRequest.setUserPrincipal(null);
                return null;
            }

            if (auth.processLogin(wrappedRequest, response, context)) {
                return null;
            }

            Principal user = wrappedRequest.getUserPrincipal();
            if (log.isInfoEnabled()) {
                if (user != null)
                    log.info("User " + user.getName() + " is authentified");
            }

            if (user == null)
                return null;
            return new XWikiUser(user.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to authenticate and set the cookie from a username and password passed as parameters
     * 
     * @param username
     * @param password
     * @param context
     * @return null if the user is not authenticated properly
     * @throws XWikiException
     */
    public XWikiUser checkAuth(String username, String password, String rememberme,
        XWikiContext context) throws XWikiException
    {
        HttpServletRequest request = null;
        HttpServletResponse response = null;

        if (context.getRequest() != null)
            request = context.getRequest().getHttpServletRequest();

        if (context.getResponse() != null)
            response = context.getResponse().getHttpServletResponse();

        if (request == null)
            return null;

        XWikiAuthenticator auth = getAuthenticator(context);
        SecurityRequestWrapper wrappedRequest =
            new SecurityRequestWrapper(request, null, null, auth.getAuthMethod());
        try {
            if (!auth.processLogin(username, password, rememberme, wrappedRequest, response,
                context)) {
                return null;
            }

            Principal user = wrappedRequest.getUserPrincipal();
            if (log.isInfoEnabled()) {
                if (user != null)
                    log.info("User " + user.getName() + " is authentified");
            }

            if (user == null)
                return null;
            return new XWikiUser(user.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showLogin(XWikiContext context) throws XWikiException
    {
        try {
            if (context.getMode() == XWikiContext.MODE_SERVLET) {
                getAuthenticator(context).showLogin(context.getRequest().getHttpServletRequest(),
                    context.getResponse().getHttpServletResponse(), context);
            }
        } catch (IOException e) {
            // If this fails we continue
            e.printStackTrace();
        }
    }

    public Principal authenticate(String username, String password, XWikiContext context)
        throws XWikiException
    {
        /*
         * This function was returning null on failure so I preserved that behaviour, while adding
         * the exact error messages to the context given as argument. However, the right way to do
         * this would probably be to throw XWikiException-s.
         */

        if ((username == null) || (username.trim().equals(""))) {
            context.put("message", "nousername");
            return null;
        }

        if ((password == null) || (password.trim().equals(""))) {
            context.put("message", "nopassword");
            return null;
        }

        String superadmin = "superadmin";
        if (username.equals(superadmin)) {
            String superadminpassword = context.getWiki().Param("xwiki.superadminpassword");
            if ((superadminpassword != null) && (superadminpassword.equals(password))) {
                Principal principal = new SimplePrincipal("XWiki.superadmin");
                return principal;
            } else {
                context.put("message", "wrongpassword");
                return null;
            }
        }

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        if (context != null) {
            String susername = username;
            int i = username.indexOf(".");
            if (i != -1)
                susername = username.substring(i + 1);

            // First we check in the local database
            try {
                String user = findUser(susername, context);
                if (user != null) {
                    if (checkPassword(user, password, context)) {
                        return new SimplePrincipal(user);
                    } else {
                        context.put("message", "wrongpassword");
                    }
                } else {
                    context.put("message", "wronguser");
                }
            } catch (Exception e) {
                // continue
            }

            if (context.isVirtual()) {
                // Then we check in the main database
                String db = context.getDatabase();
                try {
                    context.setDatabase(context.getWiki().getDatabase());
                    try {
                        String user = findUser(susername, context);
                        if (user != null) {
                            if (checkPassword(user, password, context)) {
                                return new SimplePrincipal(context.getDatabase() + ":" + user);
                            } else {
                                context.put("message", "wrongpassword");
                                return null;
                            }
                        } else {
                            context.put("message", "wronguser");
                            return null;
                        }
                    } catch (Exception e) {
                        context.put("message", "loginfailed");
                        return null;
                    }
                } finally {
                    context.setDatabase(db);
                }
            } else {
                // error message was already set
                return null;
            }
        } else {
            context.put("message", "loginfailed");
            return null;
        }
    }

    protected String findUser(String susername2, XWikiContext context) throws XWikiException
    {
        String susername = susername2.replaceAll(" ", "");

        // First lets look in the cache
        if (context.getWiki().exists("XWiki." + susername, context))
            return "XWiki." + susername;

        String sql =
            "select distinct doc.web, doc.name from XWikiDocument as doc where doc.web='XWiki' and doc.name like '"
                + Utils.SQLFilter(susername) + "'";
        List list = context.getWiki().search(sql, context);
        if (list.size() == 0)
            return null;
        if (list.size() > 1) {
            for (int i = 0; i < list.size(); i++) {
                Object[] result = (Object[]) list.get(0);
                if (result[1].equals(susername))
                    return result[0] + "." + result[1];
            }
        }
        Object[] result = (Object[]) list.get(0);
        return result[0] + "." + result[1];
    }

    protected boolean checkPassword(String username, String password, XWikiContext context)
        throws XWikiException
    {
        try {
            boolean result = false;
            XWikiDocument doc = context.getWiki().getDocument(username, context);
            // We only allow empty password from users having a XWikiUsers object.
            if (doc.getObject("XWiki.XWikiUsers") != null) {
                String passwd = doc.getStringValue("XWiki.XWikiUsers", "password");
                password =
                    ((PasswordClass) context.getWiki().getClass("XWiki.XWikiUsers", context)
                        .getField("password")).getEquivalentPassword(passwd, password);

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

    protected String getParam(String name, XWikiContext context)
    {
        String param = "";
        try {
            param = context.getWiki().getXWikiPreference(name, context);
        } catch (Exception e) {
        }
        if (param == null || "".equals(param)) {
            try {
                param =
                    context.getWiki().Param(
                        "xwiki.authentication." + StringUtils.replace(name, "auth_", "."));
            } catch (Exception e) {
            }
        }
        if (param == null)
            param = "";
        return param;
    }

    protected void createUser(String user, XWikiContext context) throws XWikiException
    {
        String createuser = getParam("auth_createuser", context);
        if (log.isDebugEnabled())
            log.debug("Create user param is " + createuser);
        if (createuser != null) {
            String wikiname = context.getWiki().clearName(user, true, true, context);
            XWikiDocument userdoc = context.getWiki().getDocument("XWiki." + wikiname, context);
            if (userdoc.isNew()) {
                if (log.isDebugEnabled())
                    log.debug("User page does not exist for user " + user);
                if ("ldap".equals(createuser)) {
                    // Let's create the user from ldap
                    LDAPPlugin ldapplugin =
                        (LDAPPlugin) context.getWiki().getPlugin("ldap", context);
                    if (ldapplugin != null) {
                        if (log.isDebugEnabled())
                            log.debug("Creating user from ldap for user " + user);
                        ldapplugin.createUserFromLDAP(wikiname, user, null, null, context);
                    } else {
                        if (log.isErrorEnabled())
                            log.error("Impossible to create user from LDAP because LDAP plugin is not activated");
                    }
                } else if ("empty".equals(createuser)) {
                    if (log.isDebugEnabled())
                        log.debug("Creating emptry user for user " + user);                            
                    context.getWiki().createEmptyUser(wikiname, "edit", context);
                }
            } else {
                if (log.isDebugEnabled())
                    log.debug("User page already exists for user " + user);
            }
        }
    }

}
