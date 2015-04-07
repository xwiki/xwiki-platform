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
 */
package com.xpn.xwiki.user.impl.xwiki;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.securityfilter.authenticator.FormAuthenticator;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.filter.URLPatternMatcher;
import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation of {@link com.xpn.xwiki.user.api.XWikiAuthService}.
 *
 * @version $Id$
 */
public class XWikiAuthServiceImpl extends AbstractXWikiAuthService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAuthServiceImpl.class);

    private static final EntityReference USERCLASS_REFERENCE = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    /**
     * Used to convert a string into a proper Document Name.
     */
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "current");

    /**
     * Used to convert a Document Reference to a username to a string. Note that we must be careful not to include the
     * wiki name as part of the serialized name since user names are saved in the database (for example as the document
     * author when you create a new document) and we're only supposed to save the wiki part when the user is from
     * another wiki. This should probably be fixed in the future though but it requires changing existing code that
     * depend on this behavior.
     */
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "compactwiki");

    /**
     * Each wiki has its own authenticator.
     */
    protected Map<String, XWikiAuthenticator> authenticators = new ConcurrentHashMap<String, XWikiAuthenticator>();

    protected XWikiAuthenticator getAuthenticator(XWikiContext context) throws XWikiException
    {
        String wikiName = context.getWikiId();

        if (wikiName != null) {
            wikiName = wikiName.toLowerCase();
        }

        XWikiAuthenticator authenticator = this.authenticators.get(wikiName);

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
                    sconfig.setDefaultPage(stripContextPathFromURL(
                        context.getURLFactory().createURL(context.getWiki().getDefaultSpace(context),
                            context.getWiki().getDefaultPage(context), "view", context), context));
                }

                if (xwiki.Param("xwiki.authentication.loginpage") != null) {
                    sconfig.setLoginPage(xwiki.Param("xwiki.authentication.loginpage"));
                } else {
                    sconfig.setLoginPage(stripContextPathFromURL(
                        context.getURLFactory().createURL("XWiki", "XWikiLogin", "login", context), context));
                }

                if (xwiki.Param("xwiki.authentication.logoutpage") != null) {
                    sconfig.setLogoutPage(xwiki.Param("xwiki.authentication.logoutpage"));
                } else {
                    sconfig.setLogoutPage(stripContextPathFromURL(
                        context.getURLFactory().createURL("XWiki", "XWikiLogout", "logout", context), context));
                }

                if (xwiki.Param("xwiki.authentication.errorpage") != null) {
                    sconfig.setErrorPage(xwiki.Param("xwiki.authentication.errorpage"));
                } else {
                    sconfig.setErrorPage(stripContextPathFromURL(
                        context.getURLFactory().createURL("XWiki", "XWikiLogin", "loginerror", context), context));
                }

                MyPersistentLoginManager persistent = new MyPersistentLoginManager();
                if (xwiki.Param("xwiki.authentication.cookieprefix") != null) {
                    persistent.setCookiePrefix(xwiki.Param("xwiki.authentication.cookieprefix"));
                }
                if (xwiki.Param("xwiki.authentication.cookiepath") != null) {
                    persistent.setCookiePath(xwiki.Param("xwiki.authentication.cookiepath"));
                }
                if (xwiki.Param("xwiki.authentication.cookiedomains") != null) {
                    String[] cdomains = StringUtils.split(xwiki.Param("xwiki.authentication.cookiedomains"), ",");
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
                    persistent.setEncryptionAlgorithm(xwiki.Param("xwiki.authentication.encryptionalgorithm"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionmode") != null) {
                    persistent.setEncryptionMode(xwiki.Param("xwiki.authentication.encryptionmode"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionpadding") != null) {
                    persistent.setEncryptionPadding(xwiki.Param("xwiki.authentication.encryptionpadding"));
                }

                if (xwiki.Param("xwiki.authentication.validationKey") != null) {
                    persistent.setValidationKey(xwiki.Param("xwiki.authentication.validationKey"));
                }

                if (xwiki.Param("xwiki.authentication.encryptionKey") != null) {
                    persistent.setEncryptionKey(xwiki.Param("xwiki.authentication.encryptionKey"));
                }

                sconfig.setPersistentLoginManager(persistent);

                MyFilterConfig fconfig = new MyFilterConfig();
                fconfig.setInitParameter(FormAuthenticator.LOGIN_SUBMIT_PATTERN_KEY,
                    xwiki.Param("xwiki.authentication.loginsubmitpage", "/loginsubmit/XWiki/XWikiLogin"));

                authenticator.init(fconfig, sconfig);
            }

            this.authenticators.put(wikiName, authenticator);

            return authenticator;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                "Cannot initialize authentication system for wiki [" + wikiName + "]", e);
        }
    }

    @Override
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        // Debug time taken.
        long time = System.currentTimeMillis();

        HttpServletRequest request = null;
        HttpServletResponse response = context.getResponse();

        if (context.getRequest() != null) {
            request = context.getRequest().getHttpServletRequest();
        }

        if (request == null) {
            return null;
        }

        XWikiAuthenticator auth = getAuthenticator(context);
        SecurityRequestWrapper wrappedRequest = new SecurityRequestWrapper(request, null, null, auth.getAuthMethod());

        try {
            if (auth.processLogin(wrappedRequest, response, context)) {
                return null;
            }

            // Process logout (this only works with Forms)
            if (auth.processLogout(wrappedRequest, response, new URLPatternMatcher())) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("User " + context.getUser() + " has been logged-out");
                }
                wrappedRequest.setUserPrincipal(null);
                return null;
            }

            final String userName = getContextUserName(wrappedRequest.getUserPrincipal(), context);
            if (LOGGER.isInfoEnabled()) {
                if (userName != null) {
                    LOGGER.info("User " + userName + " is authentified");
                }
            }

            if (userName == null) {
                return null;
            }

            return new XWikiUser(userName);
        } catch (Exception e) {
            LOGGER.error("Failed to authenticate", e);

            return null;
        } finally {
            LOGGER.debug("XWikiAuthServiceImpl.checkAuth(XWikiContext) took " + (System.currentTimeMillis() - time)
                + " milliseconds to run.");
        }
    }

    /**
     * Method to authenticate and set the cookie from a username and password passed as parameters
     *
     * @return null if the user is not authenticated properly
     */
    @Override
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        HttpServletRequest request = null;
        HttpServletResponse response = context.getResponse();

        if (context.getRequest() != null) {
            request = context.getRequest().getHttpServletRequest();
        }

        if (request == null) {
            return null;
        }

        XWikiAuthenticator auth = getAuthenticator(context);
        SecurityRequestWrapper wrappedRequest = new SecurityRequestWrapper(request, null, null, auth.getAuthMethod());
        try {
            if (!auth.processLogin(username, password, rememberme, wrappedRequest, response, context)) {
                return null;
            }

            Principal principal = wrappedRequest.getUserPrincipal();
            if (LOGGER.isInfoEnabled()) {
                if (principal != null) {
                    LOGGER.info("User " + principal.getName() + " is authentified");
                }
            }

            if (principal == null) {
                return null;
            }

            return new XWikiUser(getContextUserName(principal, context));
        } catch (Exception e) {
            LOGGER.error("Failed to authenticate", e);

            return null;
        }
    }

    private String getContextUserName(Principal principal, XWikiContext context)
    {
        String contextUserName;

        if (principal != null) {
            // Ensures that the wiki part is removed if specified in the Principal name and if it's not the same wiki
            // as the current wiki.
            // TODO: Warning the code below will fail if current doc's wiki != current wiki.
            DocumentReference userDocumentReference =
                this.currentDocumentReferenceResolver.resolve(principal.getName());
            contextUserName = this.compactWikiEntityReferenceSerializer.serialize(userDocumentReference);
        } else {
            contextUserName = null;
        }

        return contextUserName;
    }

    @Override
    public void showLogin(XWikiContext context) throws XWikiException
    {
        try {
            if (context.getMode() == XWikiContext.MODE_SERVLET) {
                getAuthenticator(context).showLogin(context.getRequest().getHttpServletRequest(), context.getResponse(),
                    context);
            }
        } catch (IOException e) {
            LOGGER.error("Unknown failure when calling showLogin", e);
        }
    }

    @Override
    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException
    {
        /*
         * This method was returning null on failure so I preserved that behaviour, while adding the exact error
         * messages to the context given as argument. However, the right way to do this would probably be to throw
         * XWikiException-s.
         */

        if (username == null) {
            // If we can't find the username field then we are probably on the login screen
            return null;
        }

        // Check for empty usernames
        if (StringUtils.isBlank(username)) {
            context.put("message", "nousername");
            return null;
        }

        // Check for empty passwords
        if (StringUtils.isBlank(password)) {
            context.put("message", "nopassword");
            return null;
        }

        // Trim the username to allow users to enter their names with spaces before or after
        String cannonicalUsername = username.replaceAll(" ", "");

        // Check for superadmin
        if (isSuperAdmin(cannonicalUsername)) {
            return authenticateSuperAdmin(password, context);
        }

        // If we have the context then we are using direct mode, and we should be able to specify the database
        // This is needed for virtual mode to work
        if (context != null) {
            String susername = cannonicalUsername;
            String virtualXwikiName = null;
            int i = cannonicalUsername.indexOf(".");
            int j = cannonicalUsername.indexOf(":");

            // Extract the specified wiki name, if it exists
            if (j > 0) {
                virtualXwikiName = cannonicalUsername.substring(0, j);
            }

            // Use just the username, without a wiki or space prefix
            if (i != -1) {
                susername = cannonicalUsername.substring(i + 1);
            } else if (j > 0) {
                // The username could be in the format xwiki:Username, so strip the wiki prefix.
                susername = cannonicalUsername.substring(j + 1);
            }

            String db = context.getWikiId();

            try {
                // Set the context database to the specified wiki, if any
                if (virtualXwikiName != null) {
                    context.setWikiId(virtualXwikiName);
                }
                // Check in the current database first
                try {
                    String user = findUser(susername, context);
                    if (user != null && checkPassword(user, password, context)) {
                        return new SimplePrincipal(virtualXwikiName != null ? context.getWikiId() + ":" + user : user);
                    }
                } catch (Exception e) {
                    // continue
                }

                if (!context.isMainWiki()) {
                    // Then we check in the main database
                    context.setWikiId(context.getMainXWiki());
                    try {
                        String user = findUser(susername, context);
                        if (user != null && checkPassword(user, password, context)) {
                            return new SimplePrincipal(context.getWikiId() + ":" + user);
                        }
                    } catch (Exception e) {
                        context.put("message", "loginfailed");
                        return null;
                    }
                }

                // No user found
                context.put("message", "invalidcredentials");
                return null;

            } finally {
                context.setWikiId(db);
            }

        } else {
            LOGGER.error("XWikiContext is null");

            return null;
        }
    }

    protected String findUser(String username, XWikiContext context) throws XWikiException
    {
        String user;

        // First let's look in the cache
        if (context.getWiki().exists(new DocumentReference(context.getWikiId(), "XWiki", username), context)) {
            user = "XWiki." + username;
        } else {
            // Note: The result of this search depends on the Database. If the database is
            // case-insensitive (like MySQL) then users will be able to log in by entering their
            // username in any case. For case-sensitive databases (like HSQLDB) they'll need to
            // enter it exactly as they've created it.
            String sql = "select distinct doc.fullName from XWikiDocument as doc";
            Object[][] whereParameters = new Object[][] { { "doc.space", "XWiki" }, { "doc.name", username } };

            List<String> list = context.getWiki().search(sql, whereParameters, context);
            if (list.size() == 0) {
                user = null;
            } else {
                user = list.get(0);
            }
        }

        return user;
    }

    protected boolean checkPassword(String username, String password, XWikiContext context) throws XWikiException
    {
        long time = System.currentTimeMillis();
        try {
            boolean result = false;

            final XWikiDocument doc = context.getWiki().getDocument(username, context);
            final BaseObject userObject = doc.getXObject(USERCLASS_REFERENCE);
            // We only allow empty password from users having a XWikiUsers object.
            if (userObject != null) {
                final String stored = userObject.getStringValue("password");
                result = new PasswordClass().getEquivalentPassword(stored, password).equals(stored);
            }

            if (LOGGER.isDebugEnabled()) {
                if (result) {
                    LOGGER.debug("Password check for user " + username + " successful");
                } else {
                    LOGGER.debug("Password check for user " + username + " failed");
                }
                LOGGER.debug((System.currentTimeMillis() - time) + " milliseconds spent validating password.");
            }

            return result;
        } catch (Throwable e) {
            LOGGER.error("Failed to check password", e);

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
                param = context.getWiki().Param("xwiki.authentication." + StringUtils.replace(name, "auth_", ""));
            } catch (Exception e) {
            }
        }

        if (param == null) {
            param = "";
        }

        return param;
    }

    protected String createUser(String user, XWikiContext context) throws XWikiException
    {
        String createuser = getParam("auth_createuser", context);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create user param is " + createuser);
        }

        if (createuser != null) {
            String wikiname = context.getWiki().clearName(user, true, true, context);
            XWikiDocument userdoc =
                context.getWiki().getDocument(new DocumentReference(context.getWikiId(), "XWiki", wikiname), context);
            if (userdoc.isNew()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("User page does not exist for user " + user);
                }

                if ("empty".equals(createuser)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Creating emptry user for user " + user);
                    }

                    context.getWiki().createEmptyUser(wikiname, "edit", context);
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("User page already exists for user " + user);
                }
            }

            return wikiname;
        }

        return user;
    }

    /**
     * The authentication library we are using (SecurityFilter) requires that its URLs do not contain the context path,
     * in order to be usable with <tt>RequestDispatcher.forward</tt>. Since our URL factory include the context path in
     * the generated URLs, we use this method to remove (if needed) the context path.
     *
     * @param url The URL to process.
     * @param context The ubiquitous XWiki request context.
     * @return A <code>String</code> representation of the contextpath-free URL.
     */
    protected String stripContextPathFromURL(URL url, XWikiContext context)
    {
        String contextPath = context.getWiki().getWebAppPath(context);
        // XWiki uses contextPath in the wrong way, putting a / at the end, and not at the start. Fix this here.
        if (contextPath.endsWith("/") && !contextPath.startsWith("/")) {
            contextPath = "/" + StringUtils.chop(contextPath);
        } else if ("/".equals(contextPath)) {
            contextPath = "";
        }

        String urlPrefix = url.getProtocol() + "://" + url.getAuthority() + contextPath;

        // Since the passed URL has been potentially modified by HttpServletResponse.encodeURL() we also need to call
        // encodeURL on urlPrefix to have a matching result.
        // Note that this allows rewrite filters to customize the URL as they wish (adding jsessionid,
        // query string, etc). If the webapp is installed as root this means that they can add a leading slash,
        // transforming for example "http://server" into "http://server/?jsessionid=xxx".
        String encodedUrlPrefix = context.getResponse().encodeURL(urlPrefix);

        // Remove a potential jsessionid in the URL
        encodedUrlPrefix = encodedUrlPrefix.replaceAll(";jsessionid=.*?(?=\\?|$)", "");

        // Also remove any query string that might have been added by an outbound URL rewrite rule
        encodedUrlPrefix = StringUtils.substringBeforeLast(encodedUrlPrefix, "?");

        // Since the encodeURL can potentially add a trailing slash, make sure that the relative URL we return always
        // start with a leadig slash.
        String strippedURL = StringUtils.removeStart(url.toExternalForm(), encodedUrlPrefix);
        if (!strippedURL.startsWith("/")) {
            strippedURL = "/" + strippedURL;
        }

        return strippedURL;
    }
}
