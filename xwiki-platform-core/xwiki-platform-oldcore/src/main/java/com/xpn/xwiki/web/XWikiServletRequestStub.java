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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.commons.lang3.ArrayUtils;
import org.xwiki.user.UserReference;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 *
 * @version $Id$
 */
@Deprecated(since = "42.0.0")
public class XWikiServletRequestStub implements XWikiRequest
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

    private boolean secure;

    private String scheme;

    private String protocol;

    private String queryString;

    private String contextPath;

    private String servletPath;

    private String serverName;

    private int serverPort;

    private Map<String, List<String>> headers;

    private Map<String, String[]> parameters;

    private Cookie[] cookies;

    private List<Part> parts = new ArrayList<>();

    private String requestURI;

    private StringBuffer requestURL;

    private String remoteAddr;

    private boolean daemon = true;

    private HttpSession httpSession;

    public XWikiServletRequestStub()
    {
    }

    protected XWikiServletRequestStub(Builder builder)
    {
        if (builder.requestURL != null) {
            this.protocol = builder.requestURL.getProtocol();
            this.scheme = builder.requestURL.getProtocol();

            this.serverName = builder.requestURL.getHost();
            this.serverPort = builder.requestURL.getPort();

            this.secure = this.protocol.equalsIgnoreCase("https");

            this.requestURI = builder.requestURL.getPath();
            this.requestURL = new StringBuffer(builder.requestURL.toString());

            setHost(builder.requestURL.getHost());
        }

        this.contextPath = builder.contextPath;
        this.parameters = clone(builder.requestParameters);
        this.headers = cloneHeaders(builder.headers);
        this.cookies = clone(builder.cookies);
        this.remoteAddr = builder.remoteAddr;
        this.httpSession = builder.httpSession;
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

    /**
     * @param request the request to copy
     * @since 10.7RC1
     */
    public XWikiServletRequestStub(XWikiRequest request)
    {
        this.secure = request.isSecure();
        this.protocol = request.getProtocol();
        this.scheme = request.getScheme();
        this.serverName = request.getServerName();
        this.serverPort = request.getServerPort();

        this.contextPath = request.getContextPath();
        this.servletPath = request.getServletPath();

        this.queryString = request.getQueryString();

        this.requestURI = request.getRequestURI();
        this.requestURL = new StringBuffer(request.getRequestURL());

        if (request.getHeaderNames() != null) {
            this.headers = Collections.list(request.getHeaderNames()).stream()
                .map(headerName -> Map.entry(headerName, Collections.list(request.getHeaders(headerName))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right,
                    () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        }

        this.parameters = clone(request.getParameterMap());
        this.cookies = clone(request.getCookies());
        this.remoteAddr = request.getRemoteAddr();

        if (request instanceof XWikiServletRequestStub) {
            this.daemon = ((XWikiServletRequestStub) request).daemon;
            this.parts = new ArrayList<>(((XWikiServletRequestStub) request).parts);
        }
    }

    private Map<String, String[]> clone(Map<String, String[]> map)
    {
        Map<String, String[]> clone;
        if (map != null) {
            clone = new LinkedHashMap<>(map.size());
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                clone.put(entry.getKey(), entry.getValue().clone());
            }
        } else {
            clone = null;
        }

        return clone;
    }

    private Map<String, List<String>> cloneHeaders(Map<String, List<String>> headers)
    {
        if (headers == null) {
            return null;
        }

        return headers.entrySet().stream().filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().stream().collect(Collectors.toList())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right,
                () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
    }

    private Cookie[] clone(Cookie[] cookies)
    {
        if (cookies != null) {
            return Stream.of(cookies).map(Cookie::clone).toArray(Cookie[]::new);
        } else {
            return null;
        }
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public void setHost(String host)
    {
        if (this.headers == null) {
            this.headers = new LinkedHashMap<>();
        }

        this.headers.put("x-forwarded-host", new Vector<String>(Arrays.asList(host)));
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    /**
     * @since 7.1RC1
     * @since 6.4.5
     */
    public void setrequestURL(StringBuffer requestURL)
    {
        this.requestURL = requestURL;
    }

    /**
     * @since 7.2M2
     */
    public void setRequestURI(String requestURI)
    {
        this.requestURI = requestURI;
    }

    /**
     * @since 7.1RC1
     * @since 6.4.5
     */
    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    @Override
    public String getHeader(String headerName)
    {
        if (this.headers != null) {
            List<String> values = this.headers.get(headerName);

            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }

        return null;
    }

    /**
     * @since 7.3M1
     */
    public void put(String key, String value)
    {
        if (this.parameters == null) {
            this.parameters = new LinkedHashMap<>();
        }

        String[] values = this.parameters.get(key);
        if (values == null) {
            values = new String[] {value};
        } else {
            values = ArrayUtils.add(values, value);
        }
        this.parameters.put(key, values);
    }

    @Override
    public String get(String name)
    {
        return getParameter(name);
    }

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return this;
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        if (this.cookies != null) {
            return Stream.of(this.cookies).filter(cookie -> Objects.equals(cookieName, cookie.getName())).findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    @Override
    public String getAuthType()
    {
        return null;
    }

    @Override
    public Cookie[] getCookies()
    {
        return clone(this.cookies);
    }

    @Override
    public long getDateHeader(String s)
    {
        return 0;
    }

    @Override
    public Enumeration<String> getHeaders(String headerName)
    {
        if (this.headers != null) {
            List<String> values = this.headers.get(headerName);

            if (values != null) {
                return Collections.enumeration(values);
            }
        }

        return Collections.emptyEnumeration();
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return this.headers != null ? Collections.enumeration(this.headers.keySet()) : Collections.emptyEnumeration();
    }

    @Override
    public int getIntHeader(String s)
    {
        String header = getHeader(s);

        return header != null ? Integer.parseInt(header) : -1;
    }

    @Override
    public String getMethod()
    {
        return null;
    }

    @Override
    public String getPathInfo()
    {
        return null;
    }

    @Override
    public String getPathTranslated()
    {
        return null;
    }

    @Override
    public String getContextPath()
    {
        return this.contextPath;
    }

    @Override
    public String getQueryString()
    {
        return this.queryString;
    }

    @Override
    public String getRemoteUser()
    {
        return null;
    }

    @Override
    public boolean isUserInRole(String s)
    {
        return false;
    }

    @Override
    public Principal getUserPrincipal()
    {
        return null;
    }

    @Override
    public String getRequestedSessionId()
    {
        return null;
    }

    @Override
    public String getRequestURI()
    {
        return this.requestURI;
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return this.requestURL == null ? new StringBuffer() : this.requestURL;
    }

    @Override
    public String getServletPath()
    {
        return this.servletPath;
    }

    @Override
    public HttpSession getSession(boolean b)
    {
        return this.httpSession;
    }

    @Override
    public String changeSessionId()
    {
        return null;
    }

    @Override
    public HttpSession getSession()
    {
        return this.httpSession;
    }

    /**
     * Sets the HttpSession object for the current user session.
     *
     * @param httpSession the {@link HttpSession} object to be set
     * @since 15.9RC1
     */
    public void setSession(HttpSession httpSession)
    {
        this.httpSession = httpSession;
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    @Override
    public Object getAttribute(String s)
    {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return Collections.emptyEnumeration();
    }

    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {

    }

    @Override
    public int getContentLength()
    {
        return 0;
    }

    @Override
    public long getContentLengthLong()
    {
        return 0;
    }

    @Override
    public String getContentType()
    {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return null;
    }

    @Override
    public String getParameter(String s)
    {
        if (this.parameters != null) {
            String[] values = this.parameters.get(s);

            if (ArrayUtils.isNotEmpty(values)) {
                return values[0];
            }
        }

        return null;
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return this.parameters != null ? Collections.enumeration(this.parameters.keySet()) : null;
    }

    @Override
    public String[] getParameterValues(String s)
    {
        if (this.parameters != null) {
            String[] values = this.parameters.get(s);

            return values != null ? values.clone() : null;
        }

        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        return clone(this.parameters);
    }

    @Override
    public String getProtocol()
    {
        return this.protocol;
    }

    @Override
    public String getScheme()
    {
        return this.scheme;
    }

    @Override
    public String getServerName()
    {
        return this.serverName;
    }

    @Override
    public int getServerPort()
    {
        return this.serverPort;
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    @Override
    public String getRemoteAddr()
    {
        return this.remoteAddr;
    }

    @Override
    public String getRemoteHost()
    {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o)
    {

    }

    @Override
    public void removeAttribute(String s)
    {

    }

    @Override
    public Locale getLocale()
    {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales()
    {
        return null;
    }

    @Override
    public boolean isSecure()
    {
        return this.secure;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s)
    {
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String getRealPath(String s)
    {
        return null;
    }

    @Override
    public int getRemotePort()
    {
        return 0;
    }

    @Override
    public String getLocalName()
    {
        return null;
    }

    @Override
    public String getLocalAddr()
    {
        return null;
    }

    @Override
    public int getLocalPort()
    {
        return 0;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException
    {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException
    {
    }

    @Override
    public void logout() throws ServletException
    {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException
    {
        return this.parts;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException
    {
        return this.parts.stream()
            .filter(part -> Objects.equals(part.getName(), s))
            .findFirst()
            .orElse(null);
    }

    @Override
    public ServletContext getServletContext()
    {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException
    {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IllegalStateException
    {
        return null;
    }

    @Override
    public boolean isAsyncStarted()
    {
        return false;
    }

    @Override
    public boolean isAsyncSupported()
    {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext()
    {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType()
    {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException
    {
        return null;
    }

    /**
     * @return true if the request is intended to be used in a long standing daemon thread (mails, etc.) and should not
     *         be taken into account when generating a URL
     * @since 10.11RC1
     */
    public boolean isDaemon()
    {
        return this.daemon;
    }

    /**
     * @param daemon the daemon to set
     * @since 10.11RC1
     */
    public void setDaemon(boolean daemon)
    {
        this.daemon = daemon;
    }

    @Override
    public Optional<UserReference> getEffectiveAuthor()
    {
        return Optional.ofNullable((UserReference) getAttribute(XWikiServletRequest.ATTRIBUTE_EFFECTIVE_AUTHOR));
    }
}
