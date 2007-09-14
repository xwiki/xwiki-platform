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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.authenticator.persistent.DefaultPersistentLoginManager;
import org.securityfilter.filter.SecurityRequestWrapper;

/**
 * Class responsible for remembering the login information between requests. It uses (encrypted)
 * cookies for this. The encryption key is stored in xwiki.cfg, xwiki.authentication.encryptionKey
 * parameter.
 * 
 * The cookies used are:
 * <dl>>
 * <dt>username</dt>
 * <dd>The logged in username</dd>
 * <dt>password</dt>
 * <dd>The password</dd>
 * <dt>rememberme</dt>
 * <dd>Whether or not the authentication information should be preserved across sessions</dd>
 * <dt>validation</dt>
 * <dd>Token used for validating the cookie information. It contains hashed information about the
 * other cookies and a secret paramete, optionally binding with the current IP of the user (so that
 * the cookie cannot be reused on another computer). This binding is enabled by the parameter
 * xwiki.authentication.useip . The secret parameter is specified in
 * xwiki.authentication.validationKey</dd>
 * </dl>
 * 
 * @version $Id: $
 */
public class MyPersistentLoginManager extends DefaultPersistentLoginManager
{
    /**
     * The string used to separate the fields in the hashed validation message.
     */
    private static final String FIELD_SEPARATOR = ":";

    /**
     * Log4J logger object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(MyPersistentLoginManager.class);

    /**
     * The domain generalization for which the cookies are active. Configured by the
     * xwiki.authentication.cookiedomains parameter. If a request comes from a host not in this
     * list, then the cookie is valid only for the requested domain. If a request comes from a host
     * that partially matches a domain in this list (meaning that the value in the list is contained
     * in the requested domain), then the cookie is set for the more general value found in the
     * list. This is useful for using the same account across multiple virtual wikis, for example.
     */
    protected String[] cookieDomains;

    /**
     * The path for which the cookies are active. By default the cookie is active for all paths in
     * the configured domains.
     */
    protected String cookiePath = "/";

    /**
     * Default constructor. The configuration is done outside, in
     * {@link XWikiAuthServiceImpl#getAuthenticator(com.xpn.xwiki.XWikiContext)}, so no parameters
     * are needed at this point.
     */
    public MyPersistentLoginManager()
    {
        super();
    }

    /**
     * Setter for the {@link #cookieDomains} parameter.
     * 
     * @param cdlist The new value for {@link #cookieDomains}.
     * @see #cookieDomains
     */
    public void setCookieDomains(String[] cdlist)
    {
        cookieDomains = cdlist;
    }

    /**
     * Setter for the {@link #cookiePath} parameter.
     * 
     * @param cp The new value for {@link #cookiePath}.
     * @see #cookiePath
     */
    public void setCookiePath(String cp)
    {
        cookiePath = cp;
    }

    /**
     * Setup a cookie: expiration date, path, domain + send it to the response.
     * 
     * @param cookie The cookie to setup.
     * @param sessionCookie Whether the cookie is only for this session, or for a longer period.
     * @param cookieDomain The domain for which the cookie is set.
     * @param response The servlet response.
     */
    public void setupCookie(Cookie cookie, boolean sessionCookie, String cookieDomain,
        HttpServletResponse response)
    {
        if (!sessionCookie) {
            setMaxAge(cookie);
        }
        cookie.setPath(cookiePath);
        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }
        addCookie(response, cookie);
    }

    /**
     * Remember a specific login using cookies.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @param username The username that's being remembered.
     * @param password The password that's being remembered.
     */
    public void rememberLogin(HttpServletRequest request, HttpServletResponse response,
        String username, String password)
    {
        String protectedUsername = username;
        String protectedPassword = password;
        if (protection.equals(PROTECTION_ALL) || protection.equals(PROTECTION_ENCRYPTION)) {
            protectedUsername = encryptText(protectedUsername);
            protectedPassword = encryptText(protectedPassword);
            if (protectedUsername == null || protectedPassword == null) {
                LOG.error("ERROR!!");
                LOG.error("There was a problem encrypting the username or password!!");
                LOG.error("Remember Me function will be disabled!!");
                return;
            }
        }

        // Let's check if the cookies should be session cookies or persistent ones.
        boolean sessionCookie = !(isTrue(request.getParameter("j_rememberme")));
        String cookieDomain = getCookieDomain(request);

        // Create client cookies to remember the login information.

        // Username
        Cookie usernameCookie = new Cookie(COOKIE_USERNAME, protectedUsername);
        setupCookie(usernameCookie, sessionCookie, cookieDomain, response);

        // Password
        Cookie passwdCookie = new Cookie(COOKIE_PASSWORD, protectedPassword);
        setupCookie(passwdCookie, sessionCookie, cookieDomain, response);

        // Remember me
        Cookie rememberCookie = new Cookie(COOKIE_REMEMBERME, !sessionCookie + "");
        setupCookie(rememberCookie, sessionCookie, cookieDomain, response);

        if (protection.equals(PROTECTION_ALL) || protection.equals(PROTECTION_VALIDATION)) {
            String validationHash =
                getValidationHash(protectedUsername, protectedPassword, request.getRemoteAddr());
            if (validationHash != null) {
                // Validation
                Cookie validationCookie = new Cookie(COOKIE_VALIDATION, validationHash);
                setupCookie(validationCookie, sessionCookie, cookieDomain, response);
            } else {
                if (LOG.isErrorEnabled()) {
                    LOG.error("WARNING!!! WARNING!!!");
                    LOG.error("PROTECTION=ALL or PROTECTION=VALIDATION was specified");
                    LOG.error("but Validation Hash could NOT be generated");
                    LOG.error("Validation has been disabled!!!!");
                }
            }
        }
        return;
    }

    /**
     * Sets the maximum age for cookies. The maximum age is configured in xwiki.cfg using the
     * xwiki.authentication.cookielife parameter (number of days). The default age is 14 days.
     * 
     * @param cookie The cookie for which the expiration date is configured.
     */
    private void setMaxAge(Cookie cookie)
    {
        try {
            cookie.setMaxAge(Math.round(60 * 60 * 24 * Float.parseFloat(cookieLife)));
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed setting cookie Max age with duration " + cookieLife);
            }
        }
    }

    /**
     * Adds a cookie to the response.
     * 
     * @param response The servlet response.
     * @param cookie The cookie to be sent.
     */
    private void addCookie(HttpServletResponse response, Cookie cookie)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding cookie: " + cookie.getDomain() + " " + cookie.getPath() + " "
                + cookie.getName() + " " + cookie.getValue());
        }
        response.addCookie(cookie);
    }

    /**
     * Compute the actual domain the cookie is supposed to be set for. Search through the list of
     * generalized domains for a partial match. If no match is found, then no specific domain is
     * used, which means that the cookie will be valid only for the requested domain.
     * 
     * @param request The servlet request.
     * @return The configured domain generalization that matches the request, or null if no match is
     *         found.
     */
    private String getCookieDomain(HttpServletRequest request)
    {
        String cookieDomain = null;
        if (cookieDomains != null) {
            String servername = request.getServerName();
            for (int i = 0; i < cookieDomains.length; i++) {
                if (servername.indexOf(cookieDomains[i]) != -1) {
                    cookieDomain = cookieDomains[i];
                    break;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cookie domain is:" + cookieDomain);
        }
        return cookieDomain;
    }

    /**
     * Get validation hash for the specified parameters. The hash includes a secret password, and
     * optionally binds the cookie to the requester's IP.
     * 
     * The hash secret is configured using the xwiki.authentication.validationKey parameter. The IP
     * binding is enabled using the xwiki.authentication.useip parameter.
     * 
     * @param username The remembered username.
     * @param password The remembered password.
     * @param clientIP The client IP of the request.
     * @return Validation hash.
     */
    private String getValidationHash(String username, String password, String clientIP)
    {
        if (validationKey == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("ERROR! >> validationKey not specified...");
                LOG.error("you are REQUIRED to specify the validatonkey in xwiki.cfg");
            }
            return null;
        }
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();

        try {
            md5 = MessageDigest.getInstance("MD5");

            sbValueBeforeMD5.append(username.toString());
            sbValueBeforeMD5.append(FIELD_SEPARATOR);
            sbValueBeforeMD5.append(password.toString());
            sbValueBeforeMD5.append(FIELD_SEPARATOR);
            if (isTrue(useIP)) {
                sbValueBeforeMD5.append(clientIP.toString());
                sbValueBeforeMD5.append(FIELD_SEPARATOR);
            }
            sbValueBeforeMD5.append(validationKey.toString());

            valueBeforeMD5 = sbValueBeforeMD5.toString();
            md5.update(valueBeforeMD5.getBytes());

            byte[] array = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }
            valueAfterMD5 = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
        } catch (Exception e) {
            LOG.error(e);
        }
        return valueAfterMD5;
    }

    /**
     * Encrypt a string. The encryption is password-based. The password can be configured using the
     * xwiki.authentication.encryptionKey parameter.
     * 
     * @param clearText The text to be encrypted.
     * @return clearText, encrypted.
     * @todo Optimize this code by creating the Cipher only once.
     */
    public String encryptText(String clearText)
    {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        try {
            Cipher c1 = Cipher.getInstance(cipherParameters);
            if (secretKey != null) {
                c1.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] clearTextBytes;
                clearTextBytes = clearText.getBytes();
                byte[] encryptedText = c1.doFinal(clearTextBytes);
                String encryptedEncodedText = encoder.encode(encryptedText);
                return encryptedEncodedText;
            }
            if (LOG.isErrorEnabled()) {
                LOG.error("ERROR! >> SecretKey not generated...");
                LOG.error("you are REQUIRED to specify the encryptionKey in xwiki.cfg");
            }
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e);
            }
        }
        return null;
    }

    /**
     * Forget a login by removing the authentication cookies.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     */
    public void forgetLogin(HttpServletRequest request, HttpServletResponse response)
    {
        ((SecurityRequestWrapper) request).setUserPrincipal(null);
        removeCookie(request, response, COOKIE_USERNAME);
        removeCookie(request, response, COOKIE_PASSWORD);
        removeCookie(request, response, COOKIE_REMEMBERME);
        removeCookie(request, response, COOKIE_VALIDATION);
        return;
    }

    /**
     * Given an array of cookies and a name, this method tries to find and return the cookie from
     * the array that has the given name. If there is no cookie matching the name in the array, null
     * is returned.
     * 
     * @param cookies The list of cookies sent by the client.
     * @param cookieName The name of the cookie to be retrieved.
     * @return The requested cookie, or null if no cookie with the given name was found.
     */
    private static Cookie getCookie(Cookie[] cookies, String cookieName)
    {
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookieName.equals(cookie.getName())) {
                    return (cookie);
                }
            }
        }
        return null;
    }

    /**
     * Remove a cookie.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @param cookieName The name of the cookie that must be removed.
     */
    private void removeCookie(HttpServletRequest request, HttpServletResponse response,
        String cookieName)
    {
        Cookie cookie = getCookie(request.getCookies(), cookieName);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setPath(cookiePath);
            String cookieDomain = getCookieDomain(request);
            if (cookieDomain != null) {
                cookie.setDomain(cookieDomain);
            }
            addCookie(response, cookie);
        }
    }

    /**
     * Check if a text is supposed to be an affirmative value ("true", "yes" or "1").
     * 
     * @param text The text to check.
     * @return true if the text is one of "true", "yes" or "1", false otherwise.
     */
    private static boolean isTrue(String text)
    {
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }
}
