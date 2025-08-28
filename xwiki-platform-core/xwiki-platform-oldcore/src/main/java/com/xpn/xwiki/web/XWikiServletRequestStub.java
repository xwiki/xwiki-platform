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
package com.xpn.xwiki.web;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.xwiki.container.servlet.HttpServletRequestStub;
import org.xwiki.jakartabridge.JavaxToJakartaWrapper;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.util.Util;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 *
 * @version $Id$
 * @deprecated use {@link HttpServletRequestStub} instead
 */
// TODO: uncomment the annotation when XWiki Standard scripts are fully migrated to the new API
// @Deprecated(since = "17.0.0RC1")
public class XWikiServletRequestStub extends HttpServletRequestWrapper
    implements XWikiRequest, JavaxToJakartaWrapper<HttpServletRequestStub>
{
    /**
     * Builder for {@link XWikiServletRequestStub}.
     * 
     * @version $Id$
     * @since 14.10
     */
    public static class Builder
    {
        private URL requestURL;

        private String contextPath;

        private Map<String, String[]> requestParameters;

        private Map<String, List<String>> headers;

        private Cookie[] cookies;

        private String remoteAddr;

        private HttpSession httpSession;

        /**
         * Default constructor.
         */
        public Builder()
        {
        }

        /**
         * @param requestURL the request URL
         * @return this builder
         */
        public Builder setRequestURL(URL requestURL)
        {
            this.requestURL = requestURL;
            return this;
        }

        /**
         * @param contextPath the context path
         * @return this builder
         */
        public Builder setContextPath(String contextPath)
        {
            this.contextPath = contextPath;
            return this;
        }

        /**
         * @param requestParameters the request parameters
         * @return this builder
         */
        public Builder setRequestParameters(Map<String, String[]> requestParameters)
        {
            this.requestParameters = requestParameters;
            return this;
        }

        /**
         * @param headers the request headers
         * @return this builder
         */
        public Builder setHeaders(Map<String, List<String>> headers)
        {
            this.headers = headers;
            return this;
        }

        /**
         * @param cookies the request cookies
         * @return this builder
         */
        public Builder setCookies(Cookie[] cookies)
        {
            this.cookies = cookies;
            return this;
        }

        /**
         * @param remoteAddr the remote address
         * @return this builder
         */
        public Builder setRemoteAddr(String remoteAddr)
        {
            this.remoteAddr = remoteAddr;
            return this;
        }

        /**
         * @param httpSession the http session to initialize the {@link XWikiServletRequestStub} instance with
         * @return the current builder
         * @since 15.9RC1
         */
        public Builder setHttpSession(HttpSession httpSession)
        {
            this.httpSession = httpSession;
            return this;
        }

        /**
         * @return the built {@link XWikiServletRequestStub} instance
         */
        public XWikiServletRequestStub build()
        {
            return new XWikiServletRequestStub(this);
        }
    }

    public XWikiServletRequestStub()
    {
        this(new HttpServletRequestStub());
    }

    /**
     * @param request the request to copy
     * @since 10.7RC1
     */
    public XWikiServletRequestStub(XWikiRequest request)
    {
        this(new HttpServletRequestStub(JakartaServletBridge.toJakarta(request)));

        if (request instanceof XWikiServletRequestStub requestStub) {
            setDaemon(requestStub.isDaemon());
        }
    }

    /**
     * @param jakarta the request wrap
     * @since 17.0.0RC1
     */
    @Unstable
    public XWikiServletRequestStub(HttpServletRequestStub jakarta)
    {
        super(JakartaServletBridge.toJavax(jakarta));
    }

    protected XWikiServletRequestStub(Builder builder)
    {
        this(new HttpServletRequestStub.Builder().setRequestURL(builder.requestURL).setContextPath(builder.contextPath)
            .setRequestParameters(builder.requestParameters).setHeaders(builder.headers)
            .setRemoteAddr(builder.remoteAddr).setCookies(JakartaServletBridge.toJakarta(builder.cookies))
            .setHttpSession(JakartaServletBridge.toJakarta(builder.httpSession)).build());
    }

    /**
     * @since 8.4RC1
     * @deprecated use the dedicated {@link Builder} instead
     */
    @Deprecated
    public XWikiServletRequestStub(URL requestURL, Map<String, String[]> requestParameters)
    {
        this(requestURL, null, requestParameters);
    }

    /**
     * @since 10.11.1
     * @since 11.0
     * @deprecated use the dedicated {@link Builder} instead
     */
    @Deprecated
    public XWikiServletRequestStub(URL requestURL, String contextPath, Map<String, String[]> requestParameters)
    {
        this(requestURL, contextPath, requestParameters, new Cookie[0]);
    }

    /**
     * @since 14.4.2
     * @since 14.5
     * @deprecated use the dedicated {@link Builder} instead
     */
    @Deprecated
    public XWikiServletRequestStub(URL requestURL, String contextPath, Map<String, String[]> requestParameters,
        Cookie[] cookies)
    {
        this(new Builder().setRequestURL(requestURL).setContextPath(contextPath).setRequestParameters(requestParameters)
            .setCookies(cookies));
    }

    public void setContextPath(String contextPath)
    {
        getJakarta().setContextPath(contextPath);
    }

    public void setHost(String host)
    {
        getJakarta().setHost(host);
    }

    public void setScheme(String scheme)
    {
        getJakarta().setScheme(scheme);
    }

    /**
     * @since 7.1RC1
     * @since 6.4.5
     */
    public void setrequestURL(StringBuffer requestURL)
    {
        getJakarta().setrequestURL(requestURL);
    }

    /**
     * @since 7.2M2
     */
    public void setRequestURI(String requestURI)
    {
        getJakarta().setRequestURI(requestURI);
    }

    /**
     * @since 7.1RC1
     * @since 6.4.5
     */
    public void setServerName(String serverName)
    {
        getJakarta().setServerName(serverName);
    }

    /**
     * @since 7.3M1
     */
    public void put(String key, String value)
    {
        getJakarta().put(key, value);
    }

    /**
     * Sets the HttpSession object for the current user session.
     *
     * @param httpSession the {@link HttpSession} object to be set
     * @since 15.9RC1
     */
    public void setSession(HttpSession httpSession)
    {
        getJakarta().setSession(JakartaServletBridge.toJakarta(httpSession));
    }

    /**
     * @return true if the request is intended to be used in a long standing daemon thread (mails, etc.) and should not
     *         be taken into account when generating a URL
     * @since 10.11RC1
     */
    public boolean isDaemon()
    {
        return getJakarta().isDaemon();
    }

    /**
     * @param daemon the daemon to set
     * @since 10.11RC1
     */
    public void setDaemon(boolean daemon)
    {
        getJakarta().setDaemon(daemon);
    }

    // JavaxToJakartaWrapper

    @Override
    public HttpServletRequestStub getJakarta()
    {
        if (getRequest() instanceof JavaxToJakartaWrapper wrapper) {
            return (HttpServletRequestStub) wrapper.getJakarta();
        }

        return null;
    }

    // XWikiRequest

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        // For retro compatibility reason it's expected that #getHttpServletRequest() return this in the case of a
        // XWikiServletRequestStub
        return this;
    }

    @Override
    public String get(String name)
    {
        return getRequest().getParameter(name);
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return Util.getCookie(cookieName, this);
    }
}
