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
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.securityfilter.authenticator.BasicAuthenticator;
import org.securityfilter.filter.SecurityFilter;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SimplePrincipal;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.AuthenticationFailureManager;
import org.xwiki.security.authentication.UserAuthenticatedEvent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

public class MyBasicAuthenticator extends BasicAuthenticator implements XWikiAuthenticator
{

    private ObservationManager observationManager;

    /**
     * Used to convert a string into a proper Document Name.
     */
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Used to convert a {@link DocumentReference} into a proper {@link UserReference}.
     */
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Override
    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response) throws Exception
    {
        return processLogin(request, response, null);
    }

    @Override
    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response, XWikiContext context)
        throws Exception
    {
        Principal principal = checkLogin(request, response, context);

        if (principal == null) {
            // login failed
            // show the basic authentication window again.
            showLogin(request.getCurrentRequest(), response);
            return true;
        } else {
            getObservationManager().notify(new UserAuthenticatedEvent(getContextUserReference(principal)), null);
        }

        return false;
    }

    @Override
    public boolean processLogin(String username, String password, String rememberme, SecurityRequestWrapper request,
        HttpServletResponse response, XWikiContext context) throws Exception
    {
        Principal principal = authenticate(username, password, context);
        if (principal != null) {
            // login successful
            request.getSession().removeAttribute(LOGIN_ATTEMPTS);

            // make sure the Principal contains wiki name information
            if (!StringUtils.contains(principal.getName(), ':')) {
                principal = new SimplePrincipal(context.getWikiId() + ":" + principal.getName());
            }

            request.setUserPrincipal(principal);

            getObservationManager().notify(new UserAuthenticatedEvent(getContextUserReference(principal)), null);

            return false;
        } else {
            // login failed
            // show the basic authentication window again.
            showLogin(request.getCurrentRequest(), response);
            return true;
        }
    }

    private static String convertUsername(String username, XWikiContext context)
    {
        return context.getWiki().convertUsername(username, context);
    }

    public static Principal checkLogin(SecurityRequestWrapper request, HttpServletResponse response,
        XWikiContext context) throws Exception
    {
        // Always verify authentication
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            String decoded = decodeBasicAuthorizationString(authorizationHeader);
            String username = convertUsername(parseUsername(decoded), context);
            String password = parsePassword(decoded);

            Principal principal = authenticate(username, password, context);

            AuthenticationFailureManager authenticationFailureManager =
                Utils.getComponent(AuthenticationFailureManager.class);

            if (principal != null && authenticationFailureManager.validateForm(username, request)) {
                // login successful
                request.getSession().removeAttribute(LOGIN_ATTEMPTS);
                authenticationFailureManager.resetAuthenticationFailureCounter(username);

                // make sure the Principal contains wiki name information
                if (!StringUtils.contains(principal.getName(), ':')) {
                    principal = new SimplePrincipal(context.getWikiId() + ":" + principal.getName());
                }

                request.setUserPrincipal(principal);

                return principal;
            } else {
                authenticationFailureManager.recordAuthenticationFailure(username, request);
            }
        }

        return null;
    }

    /**
     * Parse the user name out of the BASIC authorization header string.
     *
     * @param decoded
     * @return user name parsed out of decoded string
     */
    public static String parseUsername(String decoded)
    {
        if (decoded == null) {
            return null;
        } else {
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return null;
            } else {
                return decoded.substring(0, colon).trim();
            }
        }
    }

    /**
     * Parse the password out of the decoded BASIC authorization header string.
     *
     * @param decoded
     * @return password parsed out of decoded string
     */
    public static String parsePassword(String decoded)
    {
        if (decoded == null) {
            return null;
        } else {
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return (null);
            } else {
                return decoded.substring(colon + 1).trim();
            }
        }
    }

    /**
     * Decode the BASIC authorization string.
     *
     * @param authorization
     * @return decoded string
     */
    public static String decodeBasicAuthorizationString(String authorization)
    {
        if (authorization == null || !authorization.toLowerCase().startsWith("basic ")) {
            return null;
        } else {
            authorization = authorization.substring(6).trim();
            // Decode and parse the authorization credentials
            return new String(Base64.decodeBase64(authorization.getBytes()));
        }
    }

    public static Principal authenticate(String username, String password, XWikiContext context) throws XWikiException
    {
        return context.getWiki().getAuthService().authenticate(username, password, context);
    }

    public static void showLogin(HttpServletRequest request, HttpServletResponse response, String realmName)
        throws IOException
    {
        // save this request
        SecurityFilter.saveRequestInformation(request);

        // determine the number of login attempts
        int loginAttempts;
        if (request.getSession().getAttribute(LOGIN_ATTEMPTS) != null) {
            loginAttempts = ((Integer) request.getSession().getAttribute(LOGIN_ATTEMPTS)).intValue();
            loginAttempts += 1;
        } else {
            loginAttempts = 1;
        }
        request.getSession().setAttribute(LOGIN_ATTEMPTS, loginAttempts);

        if (loginAttempts <= MAX_ATTEMPTS) {
            response.setHeader("WWW-Authenticate", "BASIC realm=\"" + realmName + "\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            request.getSession().removeAttribute(LOGIN_ATTEMPTS);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
        }
    }

    @Override
    public void showLogin(HttpServletRequest request, HttpServletResponse response, XWikiContext context)
        throws IOException
    {
        showLogin(request, response, this.realmName);
    }

    private ObservationManager getObservationManager() {
        if ( this.observationManager == null ) {
            this.observationManager = Utils.getComponent(ObservationManager.class);
        }
        return this.observationManager;
    }

    private DocumentReferenceResolver<String> getDocumentReferenceResolver() {
        if ( this.documentReferenceResolver == null ) {
            this.documentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        }
        return this.documentReferenceResolver;
    }

    private UserReferenceResolver<DocumentReference> getUserReferenceResolver() {
        if ( this.userReferenceResolver == null ) {
            this.userReferenceResolver =
                Utils.getComponent(new DefaultParameterizedType(UserReferenceResolver.class,DocumentReference.class),
                    "current");
        }
        return this.userReferenceResolver;
    }

    private UserReference getContextUserReference(Principal principal)
    {
        UserReference contextUserReference;

        if (principal != null) {
            // Ensures that the wiki part is removed if specified in the Principal name and if it's not the same wiki
            // as the current wiki.
            DocumentReference userDocumentReference =
                getDocumentReferenceResolver().resolve(principal.getName());
            contextUserReference = getUserReferenceResolver().resolve(userDocumentReference);
        } else {
            contextUserReference = null;
        }

        return contextUserReference;
    }
}
