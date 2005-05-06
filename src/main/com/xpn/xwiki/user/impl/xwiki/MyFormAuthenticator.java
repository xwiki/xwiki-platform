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

package com.xpn.xwiki.user.impl.xwiki;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.authenticator.FormAuthenticator;
import org.securityfilter.filter.SecurityFilter;
import org.securityfilter.filter.SecurityRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.io.IOException;

public class MyFormAuthenticator extends FormAuthenticator implements XWikiAuthenticator {
    private static final Log log = LogFactory.getLog(MyFormAuthenticator.class);

    /**
     * Show the login page.
     *
     * @param request  the current request
     * @param response the current response
     */
    public void showLogin(HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws IOException {
        if ("1".equals(request.getParameter("basicauth"))) {
            String realName = context.getWiki().Param("xwiki.authentication.realname");
            if (realName==null)
                realName = "XWiki";
            MyBasicAuthenticator.showLogin(request, response, realName);
        } else {
            showLogin(request, response);
        }
    }



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

        try {
            Principal principal = MyBasicAuthenticator.checkLogin(request, response, context);
            if (principal!=null) {
                return false;
            } else {
                if ("1".equals(request.getParameter("basicauth")))
                 return true;
            }
        } catch (Exception e) {
            // in case of exception we continue on Form Auth.
            // we don't want this to interfere with the most common behavior
        }

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

    public static Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        return context.getWiki().getAuthService().authenticate(username, password, context);
    }
}
