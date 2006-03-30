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
 * @author ludovic
 * @author sdumitriu
 */


package com.xpn.xwiki.user.impl.xwiki;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.authenticator.persistent.DefaultPersistentLoginManager;
import org.securityfilter.filter.SecurityRequestWrapper;

public class MyPersistentLoginManager extends DefaultPersistentLoginManager {
    private static final Log log = LogFactory.getLog(MyPersistentLoginManager.class);
    protected String cookiePath = "/";
    protected String[] cookieDomains = null;


    public MyPersistentLoginManager() {
        super();
    }

    public void setCookiePath(String cp) {
        cookiePath = cp;
    }

    public void setCookieDomains(String[] cdlist) {
        cookieDomains = cdlist;
    }

    /**
     * Remember a specific login
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param username the username tha's being remembered
     * @param password the password that's being remembered
     */
    public void rememberLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            String username,
            String password
    ) throws IOException, ServletException {
        if (protection.equals(PROTECTION_ALL) || protection.equals(PROTECTION_ENCRYPTION)) {
            username = encryptText(username);
            password = encryptText(password);
            if (username == null || password == null) {
                System.out.println("ERROR!!");
                System.out.println("There was a problem encrypting the username or password!!");
                System.out.println("Remember Me function will be disabled!!");
                return;
            }
        }

        String cookieDomain = getCookieDomain(request);

        // create client cookie to store username and password
        Cookie usernameCookie = new Cookie(COOKIE_USERNAME, username);
        usernameCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
        usernameCookie.setPath(cookiePath);
        if (cookieDomain!=null)
            usernameCookie.setDomain(cookieDomain);

        addCookie(response, usernameCookie);
        Cookie passwdCookie = new Cookie(COOKIE_PASSWORD, password);
        passwdCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
        passwdCookie.setPath(cookiePath);
        if (cookieDomain!=null)
            passwdCookie.setDomain(cookieDomain);

        addCookie(response, passwdCookie);
        Cookie rememberCookie = new Cookie(COOKIE_REMEMBERME, "true");
        rememberCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
        rememberCookie.setPath(cookiePath);
        if (cookieDomain!=null)
            rememberCookie.setDomain(cookieDomain);
        addCookie(response, rememberCookie);
        if (protection.equals(PROTECTION_ALL) || protection.equals(PROTECTION_VALIDATION)) {
            String validationHash = getValidationHash(username, password, request.getRemoteAddr());
            if (validationHash != null) {
                Cookie validationCookie = new Cookie(COOKIE_VALIDATION, validationHash);
                validationCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
                validationCookie.setPath(cookiePath);
                if (cookieDomain!=null)
                    validationCookie.setDomain(cookieDomain);
                addCookie(response, validationCookie);
            } else {
                if (log.isErrorEnabled()) {
                    log.error("WARNING!!! WARNING!!!");
                    log.error("PROTECTION=ALL or PROTECTION=VALIDATION was specified");
                    log.error("but Validation Hash could NOT be generated");
                    log.error("Validation has been disabled!!!!");
                }
            }
        }
        return;
    }

    private void addCookie(HttpServletResponse response, Cookie cookie) {
        if (log.isDebugEnabled())
            log.debug("Adding cookie: " + cookie.getDomain() + " " + cookie.getPath() + " " + cookie.getName() + " " + cookie.getValue());
        response.addCookie(cookie);
    }

    private String getCookieDomain(HttpServletRequest request) {
        String cookieDomain = null;
        if (cookieDomains!=null) {
            String servername = request.getServerName();
            for (int i=0;i<cookieDomains.length;i++) {
                if (servername.indexOf(cookieDomains[i])!=-1) {
                    cookieDomain = cookieDomains[i];
                    break;
                }
            }
        }
        if (log.isDebugEnabled())
            log.debug("Cookie domain is:" + cookieDomain);
        return cookieDomain;
    }

    /**
     * Get validation hash for the specified parameters.
     *
     * @param username
     * @param password
     * @param clientIP
     * @return validation hash
     */
    private String getValidationHash(String username, String password, String clientIP) {
        if (validationKey == null) {
            if (log.isErrorEnabled()) {
             log.error("ERROR! >> validationKey not spcified....");
             log.error("ERROR! >> you are REQUIRED to specify the validatonkey in the config xml");
            }
            return null;
        }
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            if (log.isErrorEnabled())
             log.error("Error: " + e);
        }

        try {
            sbValueBeforeMD5.append(username.toString());
            sbValueBeforeMD5.append(":");
            sbValueBeforeMD5.append(password.toString());
            sbValueBeforeMD5.append(":");
            if (useIP.equals("true")) {
                sbValueBeforeMD5.append(clientIP.toString());
                sbValueBeforeMD5.append(":");
            }
            sbValueBeforeMD5.append(validationKey.toString());

            valueBeforeMD5 = sbValueBeforeMD5.toString();
            md5.update(valueBeforeMD5.getBytes());

            byte[] array = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            valueAfterMD5 = sb.toString();
        } catch (Exception e) {
            if (log.isErrorEnabled())
             log.error("Error:" + e);
        }
        return valueAfterMD5;
    }

    /**
     * Encrypt a string.
     *
     * @param clearText
     * @return clearText, encrypted
     */
    public String encryptText(String clearText) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        try {
            Cipher c1 = Cipher.getInstance(cipherParameters);
            if (secretKey != null) {
                c1.init(Cipher.ENCRYPT_MODE, secretKey);
                byte clearTextBytes[];
                clearTextBytes = clearText.getBytes();
                byte encryptedText[] = c1.doFinal(clearTextBytes);
                String encryptedEncodedText = encoder.encode(encryptedText);
                return encryptedEncodedText;
            } else {
                if (log.isErrorEnabled()) {
                 log.error("ERROR! >> SecretKey not generated ....");
                 log.error("ERROR! >> you are REQUIRED to specify the encryptionKey in the config xml");
                }
                return null;
            }
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Error: " + e);
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Forget a login
     *
     * @param request the servlet request
     * @param response the servlet response
     */
    public void forgetLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        ((SecurityRequestWrapper)request).setUserPrincipal(null);
        removeCookie(request, response, COOKIE_USERNAME);
        removeCookie(request, response, COOKIE_PASSWORD);
        removeCookie(request, response, COOKIE_REMEMBERME);
        removeCookie(request, response, COOKIE_VALIDATION);
        return;
    }

    /**
     * Given an array of cookies and a name, this method tries
     * to find and return the cookie from the array that has
     * the given name. If there is no cookie matching the name
     * in the array, null is returned.
     */
    private static Cookie getCookie(Cookie[] cookies, String cookieName) {
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
     * @param request
     * @param response
     * @param cookieName
     */
    private void removeCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        Cookie cookie = getCookie(request.getCookies(), cookieName);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setPath(cookiePath);
            String cookieDomain = getCookieDomain(request);
            if (cookieDomain!=null)
                cookie.setDomain(cookieDomain);
            addCookie(response, cookie);
        }
    }

}
