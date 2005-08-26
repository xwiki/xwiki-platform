/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: Ludovic Dubost
 * Date: 3 févr. 2004
 * Time: 23:41:14
 */
package com.xpn.xwiki.user.impl.xwiki;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.securityfilter.authenticator.BasicAuthenticator;
import org.securityfilter.filter.SecurityFilter;
import org.securityfilter.filter.SecurityRequestWrapper;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class MyBasicAuthenticator extends BasicAuthenticator  implements XWikiAuthenticator {


    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response) throws Exception {
        return processLogin(request, response, null);
    }


    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response, XWikiContext context) throws Exception {
                Principal principal = checkLogin(request, response, context);

                if (principal == null) {
                    // login failed
                    // show the basic authentication window again.
                    showLogin(request.getCurrentRequest(), response);
                    return true;
                }

                return false;
    }

    public static Principal checkLogin(SecurityRequestWrapper request, HttpServletResponse response, XWikiContext context) throws Exception {
            // Always verify authentication
            String authorizationHeader = request.getHeader("Authorization");
            HttpSession session = request.getSession();
            if (authorizationHeader != null) {
                String decoded = decodeBasicAuthorizationString(authorizationHeader);
                String username = parseUsername(decoded);
                String password = parsePassword(decoded);

                Principal principal = authenticate(username, password, context);

                if (principal != null) {
                    // login successful
                    request.getSession().removeAttribute(LOGIN_ATTEMPTS);
                    request.setUserPrincipal(principal);
                    return principal;
                }
            }
            return null;
    }

    /**
     * Parse the username out of the BASIC authorization header string.
     * @param decoded
     * @return username parsed out of decoded string
     */
    public static String parseUsername(String decoded) {
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
     * @param decoded
     * @return password parsed out of decoded string
     */
    public static String parsePassword(String decoded) {
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
    public static String decodeBasicAuthorizationString(String authorization) {
        if (authorization == null || !authorization.toLowerCase().startsWith("basic ")) {
            return null;
        } else {
            authorization = authorization.substring(6).trim();
            // Decode and parse the authorization credentials
            return new String(Base64.decodeBase64(authorization.getBytes()));
        }
    }

    public static Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        return context.getWiki().getAuthService().authenticate(username, password, context);
    }

    public static void showLogin(HttpServletRequest request, HttpServletResponse response, String realmName) throws IOException {
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
       request.getSession().setAttribute(LOGIN_ATTEMPTS, new Integer(loginAttempts));

       if (loginAttempts <= MAX_ATTEMPTS) {
          response.setHeader("WWW-Authenticate", "BASIC realm=\"" + realmName + "\"");
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
       } else {
          request.getSession().removeAttribute(LOGIN_ATTEMPTS);
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
       }
    }

    public void showLogin(HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws IOException {
            showLogin(request, response, realmName);
    }
}
