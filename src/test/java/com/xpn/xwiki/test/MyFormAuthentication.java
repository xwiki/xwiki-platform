/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 29 avr. 2004
 * Time: 16:06:29
 */
package com.xpn.xwiki.test;

import org.apache.cactus.Cookie;
import org.apache.cactus.WebRequest;
import org.apache.cactus.client.authentication.AbstractAuthentication;
import org.apache.cactus.client.authentication.Authentication;
import org.apache.cactus.internal.WebRequestImpl;
import org.apache.cactus.internal.client.connector.http.HttpClientConnectionHelper;
import org.apache.cactus.internal.configuration.Configuration;
import org.apache.cactus.internal.configuration.WebConfiguration;
import org.apache.cactus.util.ChainedRuntimeException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Form-based authentication implementation. An instance of this class
 * can be reused across several tests as it caches the session cookie.
 * Thus the first time it is used to authenticate the user, it calls
 * the security URL (which is by default the context URL prepended by
 * "j_security_check"), caches the returned session cookie and adds the
 * cookie for the next request. The second time it is called, it simply
 * addes the session cookie for the next request.
 *
 * @since 1.5
 *
 * @version $Id: MyFormAuthentication.java 322 2004-06-29 17:58:57Z ldubost $
 */
public class MyFormAuthentication extends AbstractAuthentication
{
    /**
     * The logger.
     */
    private static final Log LOGGER =
        LogFactory.getLog(MyFormAuthentication.class);

    /**
     * The expected HTTP response status code when the authentication
     * is succeeded.
     */
    private int expectedAuthResponse = HttpURLConnection.HTTP_MOVED_TEMP;

    /**
     * The URL to use when attempting to log in, if for whatever reason
     * the default URL is incorrect.
     */
    private URL securityCheckURL;

    /**
     * The cookie name of the session.
     */
    private String sessionCookieName = "JSESSIONID";

    /**
     * We store the session cookie.
     */
    private Cookie jsessionCookie;

    /**
     * We store all cookies
     */
    private List cookies;

    /**
     * {@link org.apache.cactus.WebRequest} object that will be used to connect to the
     * security URL.
     */
    private WebRequest securityRequest = new WebRequestImpl();

    /**
     * @param theName user name of the Credential
     * @param thePassword user password of the Credential
     */
    public MyFormAuthentication(String theName, String thePassword)
    {
        super(theName, thePassword);
    }

    /**
     * @see Authentication#configure
     */
    public void configure(HttpState theState, HttpMethod theMethod,
        WebRequest theRequest, Configuration theConfiguration)
    {
        // Only authenticate the first time this instance is used.
        if (this.jsessionCookie == null)
        {
           authenticate(theRequest, theConfiguration);
        }

        // Sets the session id cookie for the next request.
        if (this.jsessionCookie != null)
        {
            for (int i=0;i<cookies.size();i++)
             theRequest.addCookie((Cookie)cookies.get(i));
        }
    }

    /**
     * @return the {@link WebRequest} that will be used to connect to the
     * security URL. It can be used to add additional HTTP parameters such
     * as proprietary ones required by some containers.
     */
    public WebRequest getSecurityRequest()
    {
        return this.securityRequest;
    }

    /**
     * This sets the URL to use when attempting to log in. This method is used
     * if for whatever reason the default URL is incorrect.
     *
     * @param theUrl A URL to use to attempt to login.
     */
    public void setSecurityCheckURL(URL theUrl)
    {
        this.securityCheckURL = theUrl;
    }

    /**
     * This returns the URL to use when attempting to log in. By default, it's
     * the context URL defined in the Cactus configuration with
     * "/j_security_check" appended.
     *
     * @param theConfiguration the Cactus configuration
     * @return the URL that is being used to attempt to login.
     */
    public URL getSecurityCheckURL(Configuration theConfiguration)
    {
        if (this.securityCheckURL == null)
        {
            // Configure default
            String stringUrl =
                ((WebConfiguration) theConfiguration).getContextURL()
                + "/j_security_check";

            try
            {
                this.securityCheckURL = new URL(stringUrl);
            }
            catch (MalformedURLException e)
            {
                throw new ChainedRuntimeException(
                    "Unable to create default Security Check URL ["
                    + stringUrl + "]");
            }
        }

        LOGGER.debug("Using security check URL [" + this.securityCheckURL
            + "]");

        return securityCheckURL;
    }


    /**
     * Get the cookie name of the session.
     * @return the cookie name of the session
     */
    private String getSessionCookieName()
    {
        return this.sessionCookieName;
    }

    /**
     * Set the cookie name of the session to theName.
     * If theName is null, the change request will be ignored.
     * The default is &quot;<code>JSESSIONID</code>&quot;.
     * @param theName the cookie name of the session
     */
    public void setSessionCookieName(String theName)
    {
        if (theName != null)
        {
            this.sessionCookieName = theName;
        }
    }


    /**
     * Get the expected HTTP response status code for an authentication request
     * which should be successful.
     * @return the expected HTTP response status code
     */
    protected int getExpectedAuthResponse()
    {
        return this.expectedAuthResponse;
    }

    /**
     * Set the expected HTTP response status code for an authentication request
     * which should be successful.
     * The default is HttpURLConnection.HTTP_MOVED_TEMP.
     * @param theExpectedCode the expected HTTP response status code value
     */
    public void setExpectedAuthResponse(int theExpectedCode)
    {
        this.expectedAuthResponse = theExpectedCode;
    }


    /**
     * Get a cookie required to be set by set-cookie header field.
     * @param theConnection a {@link HttpURLConnection}
     * @param theTarget the target cookie name
     * @return the {@link Cookie}
     */
    private Cookie getCookie(HttpURLConnection theConnection, String theTarget)
    {
        // Check (possible multiple) cookies for a target.
        int i = 1;
        String key = theConnection.getHeaderFieldKey(i);
        while (key != null)
        {
            if (key.equalsIgnoreCase("set-cookie"))
            {
                // Cookie is in the form:
                // "NAME=VALUE; expires=DATE; path=PATH;
                //  domain=DOMAIN_NAME; secure"
                // The only thing we care about is finding a cookie with
                // the name "JSESSIONID" and caching the value.
                String cookiestr = theConnection.getHeaderField(i);
                String nameValue = cookiestr.substring(0,
                    cookiestr.indexOf(";"));
                int equalsChar = nameValue.indexOf("=");
                String name = nameValue.substring(0, equalsChar);
                String value = nameValue.substring(equalsChar + 1);
                if (name.equalsIgnoreCase(theTarget))
                {
                      String host = theConnection.getURL().getHost();
                      // Let's force it to localhost as it seems to fail on Linux
                      host = "localhost";
                      return new Cookie(host,
                         name, value);
                }
            }
            key = theConnection.getHeaderFieldKey(++i);
        }
        return null;
    }

    private List getAllCookies(HttpURLConnection theConnection)
    {
        // Check (possible multiple) cookies for a target.
        int i = 1;
        List cookies = new ArrayList();
        String key = theConnection.getHeaderFieldKey(i);
        while (key != null)
        {
            if (key.equalsIgnoreCase("set-cookie"))
            {
                // Cookie is in the form:
                // "NAME=VALUE; expires=DATE; path=PATH;
                //  domain=DOMAIN_NAME; secure"
                // The only thing we care about is finding a cookie with
                // the name "JSESSIONID" and caching the value.
                String cookiestr = theConnection.getHeaderField(i);
                String nameValue = cookiestr.substring(0,
                    cookiestr.indexOf(";"));
                int equalsChar = nameValue.indexOf("=");
                String name = nameValue.substring(0, equalsChar);
                String value = nameValue.substring(equalsChar + 1);
                String host = theConnection.getURL().getHost();
                // Let's force it to localhost as it seems to fail on Linux
                host = "localhost";
                Cookie cookie = new Cookie(host, name, value);
                cookie.setPath("/");
                cookies.add(cookie);
            }
            key = theConnection.getHeaderFieldKey(++i);
        }
        return cookies;
    }


    /**
     * Check if the pre-auth step can be considered as succeeded or not.
     * As default, the step considered as succeeded
     * if the response status code of <code>theConnection</code>
     * is less than 400.
     *
     * @param theConnection a <code>HttpURLConnection</code> value
     * @exception Exception if the pre-auth step should be considered as failed
     */
    protected void checkPreAuthResponse(HttpURLConnection theConnection)
        throws Exception
    {
        if (theConnection.getResponseCode() >= 400)
        {
            throw new Exception("Received a status code ["
                + theConnection.getResponseCode()
                + "] and was expecting less than 400");
        }
    }


    /**
     * Get login session cookie.
     * This is the first step to start login session:
     * <dl>
     *   <dt> C-&gt;S: </dt>
     *   <dd> try to connect to a restricted resource </dd>
     *   <dt> S-&gt;C: </dt>
     *   <dd> redirect or forward to the login page with set-cookie header </dd>
     * </ol>
     * @param theRequest a request to connect to a restricted resource
     * @param theConfiguration a <code>Configuration</code> value
     * @return the <code>Cookie</code>
     */
    private Cookie getSecureSessionIdCookie(WebRequest theRequest,
        Configuration theConfiguration)
    {
        HttpURLConnection connection;
        String resource = null;

        try
        {
            // Create a helper that will connect to a restricted resource.
            WebConfiguration webConfig = (WebConfiguration) theConfiguration;
            resource = webConfig.getRedirectorURL(theRequest);

            HttpClientConnectionHelper helper =
                new HttpClientConnectionHelper(resource);

            WebRequest request =
                new WebRequestImpl((WebConfiguration) theConfiguration);

            theRequest.setAuthentication(null);

            // Make the connection using a default web request.
            connection = helper.connect(theRequest, theConfiguration);

            theRequest.setAuthentication(this);

            checkPreAuthResponse(connection);
        }
        catch (Throwable e)
        {
            throw new ChainedRuntimeException(
                "Failed to connect to the secured redirector: " + resource, e);
        }

        return getCookie(connection, getSessionCookieName());
    }


    /**
     * Check if the auth step can be considered as succeeded or not.
     * As default, the step considered as succeeded
     * if the response status code of <code>theConnection</code>
     * equals <code>getExpectedAuthResponse()</code>.
     *
     * @param theConnection a <code>HttpURLConnection</code> value
     * @exception Exception if the auth step should be considered as failed
     */
    protected void checkAuthResponse(HttpURLConnection theConnection)
        throws Exception
    {
        if (theConnection.getResponseCode() != getExpectedAuthResponse())
        {

            throw new Exception("Received a status code ["
                + theConnection.getResponseCode()
                + "] and was expecting a ["
                + getExpectedAuthResponse() + "]\nURL: "
                + theConnection.getURL().toString() + "\nPost: "
                + theConnection.getRequestProperties().toString());
        }
    }


    /**
     * Authenticate the principal by calling the security URL.
     *
     * @param theRequest the web request used to connect to the Redirector
     * @param theConfiguration the Cactus configuration
     */
    public void authenticate(WebRequest theRequest,
        Configuration theConfiguration)
    {
        this.jsessionCookie = getSecureSessionIdCookie(theRequest,
            theConfiguration);

        try
        {
            // Create a helper that will connect to the security check URL.
            HttpClientConnectionHelper helper =
                new HttpClientConnectionHelper(
                    getSecurityCheckURL(theConfiguration).toString());

            // Configure a web request with the JSESSIONID cookie,
            // the username and the password.
            WebRequest request = getSecurityRequest();
            ((WebRequestImpl) request).setConfiguration(theConfiguration);
            request.addCookie(this.jsessionCookie);
            request.addParameter("j_username", getName(),
                WebRequest.POST_METHOD);
            request.addParameter("j_password", getPassword(),
                WebRequest.POST_METHOD);
            request.addParameter("j_rememberme", "true",
                WebRequest.POST_METHOD);

            // Make the connection using the configured web request.
            HttpURLConnection connection = helper.connect(request,
                theConfiguration);

            checkAuthResponse(connection);
            cookies = getAllCookies(connection);
        }
        catch (Throwable e)
        {
            this.jsessionCookie = null;
            throw new ChainedRuntimeException(
                "Failed to authenticate the principal", e);
        }
    }
}
