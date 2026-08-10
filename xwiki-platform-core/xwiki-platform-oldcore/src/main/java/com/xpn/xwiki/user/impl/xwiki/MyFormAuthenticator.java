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
import java.net.URLEncoder;
import java.security.Principal;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.securityfilter.authenticator.FormAuthenticator;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.filter.URLPatternMatcher;
import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.servlet.filters.SavedRequestManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authentication.AuthenticationFailureManager;
import org.xwiki.security.authentication.UserAuthenticatedEventNotifier;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.web.Utils;

public class MyFormAuthenticator extends FormAuthenticator implements XWikiAuthenticator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MyFormAuthenticator.class);

    private UserAuthenticatedEventNotifier userAuthenticatedEventNotifier;

    private UserAuthenticatedEventNotifier getUserAuthenticatedEventNotifier()
    {
        if ( this.userAuthenticatedEventNotifier == null ) {
            this.userAuthenticatedEventNotifier = Utils.getComponent(UserAuthenticatedEventNotifier.class);
        }
        return this.userAuthenticatedEventNotifier;
    }

    /**
     * Show the login page.
     *
     * @param request the current request
     * @param response the current response
     */
    @Override
    public void showLogin(HttpServletRequest request, HttpServletResponse response, XWikiContext context)
        throws IOException
    {
        if ("1".equals(request.getParameter("basicauth"))) {
            String realmName = context.getWiki().Param("xwiki.authentication.realmname");
            if (realmName == null) {
                realmName = "XWiki";
            }
            MyBasicAuthenticator.showLogin(request, response, realmName);
        } else {
            showLogin(request, response);
        }
    }

    @Override
    public void showLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String savedRequestId = request.getParameter(SavedRequestManager.getSavedRequestIdentifier());
        if (StringUtils.isEmpty(savedRequestId)) {
            // Save this request
            savedRequestId = SavedRequestManager.saveRequest(request);
        }
        String sridParameter = SavedRequestManager.getSavedRequestIdentifier() + "=" + savedRequestId;

        // Redirect to login page
        StringBuilder redirectBack = new StringBuilder(request.getRequestURI());
        redirectBack.append('?');
        String delimiter = "";
        if (StringUtils.isNotEmpty(request.getQueryString())) {
            redirectBack.append(request.getQueryString());
            delimiter = "&";
        }
        if (!request.getParameterMap().containsKey(SavedRequestManager.getSavedRequestIdentifier())) {
            redirectBack.append(delimiter);
            redirectBack.append(sridParameter);
        }
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + this.loginPage + "?"
            + sridParameter + "&xredirect=" + URLEncoder.encode(redirectBack.toString(), "UTF-8")));
    }

    @Override
    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response) throws Exception
    {
        return processLogin(request, response, null);
    }

    private String convertUsername(String username, XWikiContext context)
    {
        return context.getWiki().convertUsername(username, context);
    }

    /**
     * Process any login information that was included in the request, if any. Returns true if SecurityFilter should
     * abort further processing after the method completes (for example, if a redirect was sent as part of the login
     * processing).
     *
     * @param request
     * @param response
     * @return true if the filter should return after this method ends, false otherwise
     */
    @Override
    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response, XWikiContext context)
        throws Exception
    {
        try {
            Principal principal = MyBasicAuthenticator.checkLogin(request, response, context);
            if (principal != null) {
                return false;
            }
            if ("1".equals(request.getParameter("basicauth"))) {
                return true;
            }
        } catch (Exception e) {
            // in case of exception we continue on Form Auth.
            // we don't want this to interfere with the most common behavior
        }

        // process any persistent login information, if user is not already logged in,
        // persistent logins are enabled, and the persistent login info is present in this request
        if (this.persistentLoginManager != null) {
            Principal principal = request.getUserPrincipal();

            // If cookies are turned on:
            // 1) if user is not already authenticated, authenticate
            // 2) if xwiki.authentication.always is set to 1 in xwiki.cfg file, authenticate
            if (principal == null || context.getWiki().ParamAsLong("xwiki.authentication.always", 0) == 1) {
                String username =
                    convertUsername(this.persistentLoginManager.getRememberedUsername(request, response), context);
                String password = this.persistentLoginManager.getRememberedPassword(request, response);

                principal = authenticate(username, password, context);

                if (principal != null) {
                    LOGGER.debug("User [{}] has been authentified from cookie", principal.getName());

                    // make sure the Principal contains wiki name information
                    if (!StringUtils.contains(principal.getName(), ':')) {
                        principal = new SimplePrincipal(context.getWikiId() + ":" + principal.getName());
                    }

                    request.setUserPrincipal(principal);

                    this.getUserAuthenticatedEventNotifier().notify(principal.getName());

                } else {
                    // Failed to authenticate, better cleanup the user stored in the session
                    request.setUserPrincipal(null);
                    if (username != null || password != null) {
                        // Failed authentication with remembered login, better forget login now
                        this.persistentLoginManager.forgetLogin(request, response);
                    }
                }
            }
        }

        // process login form submittal
        if ((this.loginSubmitPattern != null) && request.getMatchableURL().endsWith(this.loginSubmitPattern)) {
            String username = convertUsername(request.getParameter(FORM_USERNAME), context);
            String password = request.getParameter(FORM_PASSWORD);
            String rememberme = request.getParameter(FORM_REMEMBERME);
            rememberme = (rememberme == null) ? "false" : rememberme;
            return processLogin(username, password, rememberme, request, response, context);
        }
        return false;
    }

    /**
     * Process any login information passed in parameter (username, password). Returns true if SecurityFilter should
     * abort further processing after the method completes (for example, if a redirect was sent as part of the login
     * processing).
     *
     * @param request
     * @param response
     * @return true if the filter should return after this method ends, false otherwise
     */
    @Override
    public boolean processLogin(String username, String password, String rememberme, SecurityRequestWrapper request,
        HttpServletResponse response, XWikiContext context) throws Exception
    {
        Principal principal = authenticate(username, password, context);
        AuthenticationFailureManager authenticationFailureManager =
            Utils.getComponent(AuthenticationFailureManager.class);
        if (principal != null && authenticationFailureManager.validateForm(username, request)) {
            // login successful
            LOGGER.info("User [{}] has been logged-in", principal.getName());

            authenticationFailureManager.resetAuthenticationFailureCounter(username);

            // invalidate old session if the user was already authenticated, and they logged in as a different user
            if (request.getUserPrincipal() != null && !username.equals(request.getRemoteUser())) {
                request.getSession().invalidate();
            }

            // manage persistent login info, if persistent login management is enabled
            if (this.persistentLoginManager != null) {
                // did the user request that their login be persistent?
                if (rememberme != null) {
                    // remember login
                    this.persistentLoginManager.rememberLogin(request, response, username, password);
                } else {
                    // forget login
                    this.persistentLoginManager.forgetLogin(request, response);
                }
            }

            // make sure the Principal contains wiki name information
            if (!StringUtils.contains(principal.getName(), ':')) {
                principal = new SimplePrincipal(context.getWikiId() + ":" + principal.getName());
            }

            request.setUserPrincipal(principal);

            this.getUserAuthenticatedEventNotifier().notify(principal.getName());

            Boolean bAjax = (Boolean) context.get("ajax");
            if ((bAjax == null) || (!bAjax.booleanValue())) {
                // This is the url that the user was initially accessing before being prompted for login.
                String continueToURL = getContinueToURL(request);
                response.sendRedirect(response.encodeRedirectURL(continueToURL));
            }
        } else {
            // login failed
            // set response status and forward to error page
            LOGGER.info("User [{}] login has failed", username);

            authenticationFailureManager.recordAuthenticationFailure(username, request);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        return true;
    }

    /**
     * FormAuthenticator has a special case where the user should be sent to a default page if the user spontaneously
     * submits a login request.
     *
     * @param request
     * @return a URL to send the user to after logging in
     */
    private String getContinueToURL(HttpServletRequest request)
    {
        String savedURL = request.getParameter("xredirect");
        if (StringUtils.isEmpty(savedURL)) {
            savedURL = SavedRequestManager.getOriginalUrl(request);
        }

        if (!StringUtils.isEmpty(savedURL)) {
            return savedURL;
        }
        return request.getContextPath() + this.defaultPage;
    }

    public static Principal authenticate(String username, String password, XWikiContext context) throws XWikiException
    {
        return context.getWiki().getAuthService().authenticate(username, password, context);
    }

    @Override
    public boolean processLogout(SecurityRequestWrapper securityRequestWrapper,
        HttpServletResponse httpServletResponse, URLPatternMatcher urlPatternMatcher) throws Exception
    {
        Principal requestPrincipal = securityRequestWrapper.getUserPrincipal();
        String requestFormToken = securityRequestWrapper.getParameter("form_token");
        boolean result = false;

        if (requestPrincipal != null && requestFormToken != null) {
            XWikiContext xcontext = Utils.<Provider<XWikiContext>>getComponent(XWikiContext.TYPE_PROVIDER).get();
            CSRFToken csrfToken = Utils.getComponent(CSRFToken.class);
            DocumentReferenceResolver<String> resolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING);
            DocumentReference userReference = resolver.resolve(requestPrincipal.getName());

            // In theory, this should always be null, but we still store it for safety.
            DocumentReference oldUserReference = xcontext.getUserReference();

            try {
                User user = xcontext.getWiki().getUser(userReference, xcontext);
                // If the user is disabled, it should not be set in the context and use guest's CSRF token instead.
                if (!user.isDisabled()) {
                    // We need to set the user in the context to be able to verify the CSRF token.
                    xcontext.setUserReference(userReference);
                }

                // Only attempt to log out if the CSRF token is present and valid.
                // This check is called for any logged-in request, not just logouts, so we need to account for requests
                // that do not normally use a CSRF token to not log false positives token mismatches.
                // The error key can be safely set either way since only LogoutAction will actually use it.
                if (requestFormToken != null && csrfToken.isTokenValid(requestFormToken)) {
                    // This method is the one that will check if the currently requested URL matches a logout action,
                    // and return false if not. As a consequence, even though the current method is called
                    // `processLogout`, it's actually executed on any logged-in request received, not just logouts.
                    result = super.processLogout(securityRequestWrapper, httpServletResponse, urlPatternMatcher);
                    if (result && this.persistentLoginManager != null) {
                        this.persistentLoginManager.forgetLogin(securityRequestWrapper, httpServletResponse);
                    }
                } else {
                    xcontext.put("core.logout.error.invalidCSRF", true);
                }
            } finally {
                // Reset the context user.
                xcontext.setUserReference(oldUserReference);
            }
        }

        return result;
    }
}

