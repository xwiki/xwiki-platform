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
package com.xpn.xwiki.user.impl.xwiki;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.plugin.ldap.LDAPPlugin;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SimplePrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

public class XWikiAuthServiceImpl extends AbstractXWikiAuthService
{
    private static final Log log = LogFactory.getLog(XWikiAuthServiceImpl.class);

    protected XWikiAuthenticator authenticator;

    protected XWikiAuthenticator getAuthenticator(XWikiContext context) throws XWikiException
    {
        if (authenticator != null) {
            return authenticator;
        }

        try {
            XWiki xwiki = context.getWiki();

            if ("basic".equals(xwiki.Param("xwiki.authentication"))) {
                authenticator = new MyBasicAuthenticator();
                SecurityConfig sconfig = new SecurityConfig(false);
                sconfig.setAuthMethod("BASIC");
                if (xwiki.Param("xwiki.authentication.realmname") != null) {
                    sconfig.setRealmName(xwiki.Param("xwiki.authentication.realmname"));
                } else {
                    sconfig.setRealmName("XWiki");
                }
                authenticator.init(null, sconfig);
            } else {
                authenticator = new MyFormAuthenticator();
                SecurityConfig sconfig = new SecurityConfig(false);

                sconfig.setAuthMethod("FORM");

                if (xwiki.Param("xwiki.authentication.realmname") != null) {
                    sconfig.setRealmName(xwiki.Param("xwiki.authentication.realmname"));
                } else {
                    sconfig.setRealmName("XWiki");
                }

                if (xwiki.Param("xwiki.authentication.defaultpage") != null) {
                    sconfig.setDefaultPage(xwiki.Param("xwiki.authentication.defaultpage"));
                } else {
                    sconfig.setDefaultPage("/bin/view/Main/WebHome");
                }

                if (xwiki.Param("xwiki.authentication.loginpage") != null) {
                    sconfig.setLoginPage(xwiki.Param("xwiki.authentication.loginpage"));
                } else {
                    sconfig.setLoginPage("/bin/login/XWiki/XWikiLogin");
                }

                if (xwiki.Param("xwiki.authentication.logoutpage") != null) {
                    sconfig.setLogoutPage(xwiki.Param("xwiki.authentication.logoutpage"));
                } else {
                    sconfig.setLogoutPage("/bin/logout/XWiki/XWikiLogout");
                }

                if (xwiki.Param("xwiki.authentication.errorpage") != null) {
                    sconfig.setErrorPage(xwiki.Param("xwiki.authentication.errorpage"));
                } else {
                    sconfig.setErrorPage("/bin/loginerror/XWiki/XWikiLogin");
                }

                MyPersistentLoginManager persistent = new MyPersistentLoginManager();
                if (xwiki.Param("xwiki.authentication.cookieprefix") != null) {
                    persistent.setCookiePrefix(xwiki.Param("xwiki.authentication.cookieprefix"));
                }
                if (xwiki.Param("xwiki.authentication.cookiepath") != null) {
                    persistent.setCookiePath(xwiki.Param("xwiki.authentication.cookiepath"));
                }
                if (xwiki.Param("xwiki.authentication.cookiedomains") != null) {
                    String[] cdomains =
                        StringUtils.split(xwiki.Param("xwiki.authentication.cookiedomains"), ",");
                    persistent.setCookieDomains(cdomains);
                }

                if (xwiki.Param("xwiki.authentication.cookielife") != null) {
                    persistent.setCookieLife(xwiki.Param("xwiki.authentication.cookielife"));
                }

                if (xwiki.Param("xwiki.authentication.protection") != null) {
                    persistent.setProtection(xwiki.Param("xwiki.authentication.protection"));
                }

                if (xwiki.Param("xwiki.authentication.useip") != null) {
                    persistent.setUseIP(xwiki.Param("xwiki.authentication.useip"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionalgorithm") != null) {
                    persistent.setEncryptionAlgorithm(xwiki
                        .Param("xwiki.authentication.encryptionalgorithm"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionmode") != null) {
                    persistent.setEncryptionMode(xwiki
                        .Param("xwiki.authentication.encryptionmode"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionpadding") != null) {
                    persistent.setEncryptionPadding(xwiki
                        .Param("xwiki.authentication.encryptionpadding"));
                }

                if (xwiki.Param("xwiki.authentication.validationKey") != null) {
                    persistent
                        .setValidationKey(xwiki.Param("xwiki.authentication.validationKey"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionKey") != null) {
                    persistent
                        .setEncryptionKey(xwiki.Param("xwiki.authentication.encryptionKey"));
                }

                sconfig.setPersistentLoginManager(persistent);

                MyFilterConfig fconfig = new MyFilterConfig();
                if (xwiki.Param("xwiki.authentication.loginsubmitpage") != null) {
                    fconfig.setInitParameter("loginSubmitPattern", xwiki
                        .Param("xwiki.authentication.loginsubmitpage"));
                } else {
                    fconfig.setInitParameter("loginSubmitPattern",
                        "/loginsubmit/XWiki/XWikiLogin");
                }

                authenticator.init(fconfig, sconfig);
            }

            return authenticator;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                XWikiException.ERROR_XWIKI_USER_INIT, "Cannot initialize authentication system",
                e);
        }
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        HttpServletRequest request = null;
        HttpServletResponse response = null;

        if (context.getRequest() != null) {
            request = context.getRequest().getHttpServletRequest();
        }

        if (context.getResponse() != null) {
            response = context.getResponse().getHttpServletResponse();
        }

        if (request == null) {
            return null;
        }

        XWikiAuthenticator auth = getAuthenticator(context);
        SecurityRequestWrapper wrappedRequest =
            new SecurityRequestWrapper(request, null, null, auth.getAuthMethod());
        // We need to make we will not user the principal
        // associated with the app server session
        wrappedRequest.setUserPrincipal(null);

        try {

            // Process login out (this only works with FORMS
            if (auth.processLogout(wrappedRequest, response, xwiki.getUrlPatternMatcher())) {
                if (log.isInfoEnabled()) {
                    log.info("User " + context.getUser() + " has been logged-out");
                }
                wrappedRequest.setUserPrincipal(null);
                return null;
            }

            if (auth.processLogin(wrappedRequest, response, context)) {
                return null;
            }

            Principal user = wrappedRequest.getUserPrincipal();
            if (log.isInfoEnabled()) {
                if (user != null) {
                    log.info("User " + user.getName() + " is authentified");
                }
            }

            if (user == null) {
                return null;
            }
            return new XWikiUser(user.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to authenticate and set the cookie from a username and password passed as parameters
     * 
     * @return null if the user is not authenticated properly
     */
    public XWikiUser checkAuth(String username, String password, String rememberme,
        XWikiContext context) throws XWikiException
    {
        HttpServletRequest request = null;
        HttpServletResponse response = null;

        if (context.getRequest() != null) {
            request = context.getRequest().getHttpServletRequest();
        }

        if (context.getResponse() != null) {
            response = context.getResponse().getHttpServletResponse();
        }

        if (request == null) {
            return null;
        }

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
                if (user != null) {
                    log.info("User " + user.getName() + " is authentified");
                }
            }

            if (user == null) {
                return null;
            }
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

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAuthService#authenticate(String,String,XWikiContext)
     */
    public Principal authenticate(String username, String password, XWikiContext context)
        throws XWikiException
    {
        /*
         * This method was returning null on failure so I preserved that behaviour, while adding the
         * exact error messages to the context given as argument. However, the right way to do this
         * would probably be to throw XWikiException-s.
         */

        if (username == null) {
            // If we can't find the username field then we are probably on the login screen
            return null;
        }

        // Trim the username to allow users to enter their names with spaces before or after
        String cannonicalUsername = username.replaceAll(" ", "");

        // Check for empty usernames
        if (cannonicalUsername.equals("")) {
            context.put("message", "nousername");
            return null;
        }

        // Check for empty passwords
        if ((password == null) || (password.trim().equals(""))) {
            context.put("message", "nopassword");
            return null;
        }

        // Check for superadmin
        if (isSuperAdmin(cannonicalUsername)) {
            return authenticateSuperAdmin(password, context);
        }

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        if (context != null) {
            String susername = cannonicalUsername;
            int i = cannonicalUsername.indexOf(".");
            if (i != -1) {
                susername = cannonicalUsername.substring(i + 1);
            }

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

            if (!context.isMainWiki()) {
                // Then we check in the main database
                String db = context.getDatabase();
                try {
                    context.setDatabase(context.getMainXWiki());
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

    protected String findUser(String username, XWikiContext context) throws XWikiException
    {
        String user;

        // First let's look in the cache
        if (context.getWiki().exists("XWiki." + username, context)) {
            user = "XWiki." + username;
        } else {
            // Note: The result of this search depends on the Database. If the database is
            // case-insensitive (like MySQL) then users will be able to log in by entering their
            // username in any case. For case-sensitive databases (like HSQLDB) they'll need to
            // enter it exactly as they've created it.
            String sql = "select distinct doc.fullName from XWikiDocument as doc";
            Object[][] whereParameters =
                new Object[][] { {"doc.web", "XWiki"}, {"doc.name", username}};

            List list = context.getWiki().search(sql, whereParameters, context);
            if (list.size() == 0) {
                user = null;
            } else {
                user = (String) list.get(0);
            }
        }

        return user;
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
                if (result) {
                    log.debug("Password check for user " + username + " successful");
                } else {
                    log.debug("Password check for user " + username + " failed");
                }
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
        if (param == null) {
            param = "";
        }
        return param;
    }

    protected void createUser(String user, XWikiContext context) throws XWikiException
    {
        String createuser = getParam("auth_createuser", context);
        if (log.isDebugEnabled()) {
            log.debug("Create user param is " + createuser);
        }
        if (createuser != null) {
            String wikiname = context.getWiki().clearName(user, true, true, context);
            XWikiDocument userdoc = context.getWiki().getDocument("XWiki." + wikiname, context);
            if (userdoc.isNew()) {
                if (log.isDebugEnabled()) {
                    log.debug("User page does not exist for user " + user);
                }
                if ("ldap".equals(createuser)) {
                    // Let's create the user from ldap
                    LDAPPlugin ldapplugin =
                        (LDAPPlugin) context.getWiki().getPlugin("ldap", context);
                    if (ldapplugin != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating user from ldap for user " + user);
                        }
                        ldapplugin.createUserFromLDAP(wikiname, user, null, null, context);
                    } else {
                        if (log.isErrorEnabled()) {
                            log
                                .error("Impossible to create user from LDAP because LDAP plugin is not activated");
                        }
                    }
                } else if ("empty".equals(createuser)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating emptry user for user " + user);
                    }
                    context.getWiki().createEmptyUser(wikiname, "edit", context);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("User page already exists for user " + user);
                }
            }
        }
    }
}
