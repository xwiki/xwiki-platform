/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 24 mars 2004
 * Time: 12:14:08
 */

package com.xpn.xwiki.user;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import org.securityfilter.authenticator.FormAuthenticator;
import org.securityfilter.filter.SecurityFilter;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SimplePrincipal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

public class MyFormAuthenticator extends FormAuthenticator {

    private static final Log log = LogFactory.getLog(MyFormAuthenticator.class);


    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response) throws Exception {
        return processLogin(request, response, null);
    }

    /**
     * Process any login information that was included in the request, if any.
     * Returns true if SecurityFilter should abort further processing after the method completes (for example, if a
     * redirect was sent as part of the login processing).
     *
     * @param request
     * @param response
     * @return true if the filter should return after this method ends, false otherwise
     */
    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response, XWikiContext context) throws Exception {
        // process any persistent login information, if user is not already logged in,
        // persistent logins are enabled, and the persistent login info is present in this request
        if (
                persistentLoginManager != null
                && persistentLoginManager.rememberingLogin(request)
        ) {
            String username = persistentLoginManager.getRememberedUsername(request, response);
            String password = persistentLoginManager.getRememberedPassword(request, response);

            Principal principal = authenticate(username, password, context);

            if (principal != null) {
                if (log.isDebugEnabled()) log.debug("User " + principal.getName() + " has been authentified from cookie");
                request.setUserPrincipal(principal);
            } else {
                // failed authentication with remembered login, better forget login now
                persistentLoginManager.forgetLogin(request, response);
            }
        }

        // process login form submittal
        if (request.getMatchableURL().endsWith(loginSubmitPattern)) {
            String username = request.getParameter(FORM_USERNAME);
            String password = request.getParameter(FORM_PASSWORD);
            Principal principal = authenticate(username, password, context);
            if (principal != null) {
                // login successful
                if (log.isInfoEnabled()) log.info("User " + principal.getName() + " has been logged-in");

                // invalidate old session if the user was already authenticated, and they logged in as a different user
                if (request.getUserPrincipal() != null && !username.equals(request.getRemoteUser())) {
                    request.getSession().invalidate();
                }

                // manage persistent login info, if persistent login management is enabled
                if (persistentLoginManager != null) {
                    String rememberme = request.getParameter(FORM_REMEMBERME);
                    // did the user request that their login be persistent?
                    if (rememberme != null) {
                        // remember login
                        persistentLoginManager.rememberLogin(request, response, username, password);
                    } else {
                        // forget login
                        persistentLoginManager.forgetLogin(request, response);
                    }
                }

                request.setUserPrincipal(principal);
                String continueToURL = getContinueToURL(request);
                // This is the url that the user was initially accessing before being prompted for login.
                response.sendRedirect(response.encodeRedirectURL(continueToURL));
            } else {
                // login failed
                // set response status and forward to error page
                if (log.isInfoEnabled()) log.info("User " + username + " login has failed");

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                request.getRequestDispatcher(errorPage).forward(request, response);
            }
            return true;
        }

        return false;
    }

    /**
    * FormAuthenticator has a special case where the user should be sent to a default page if the user
    * spontaneously submits a login request.
    *
    * @param request
    * @return a URL to send the user to after logging in
    */
   private String getContinueToURL(HttpServletRequest request) {
      String savedURL = request.getParameter("xredirect");
      if ((savedURL==null)||(savedURL.trim().equals("")))
         savedURL = SecurityFilter.getContinueToURL(request);

      if (savedURL != null) {
         return savedURL;
      } else {
         return request.getContextPath() + defaultPage;
      }
   }

    protected Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        Principal principal = null;

        if (username==null)
         return null;

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        if (context!=null) {
            String susername = username;
            if (username.indexOf(".")==-1)
                susername = "XWiki." + username;

            if (context.isVirtual()) {
                String db = context.getDatabase();
                try {
                    // First we check in the main database
                    try {
                        context.setDatabase(context.getWiki().getDatabase());
                        if (context.getWiki().checkPassword(susername, password, context))
                            principal = new SimplePrincipal("xwiki:" + susername);
                    } catch (Exception e) {}
                } finally {
                    context.setDatabase(db);
                }
            }

            if (principal==null) {
                if (context.getWiki().checkPassword(susername, password, context))
                    principal = new SimplePrincipal(susername);
            }
        }
        else {
             principal = ((XWikiRealmAdapter)realm).authenticate(username, password, context);
        }
        return principal;
    }
}
