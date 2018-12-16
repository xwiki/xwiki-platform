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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

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
import javax.servlet.http.Part;

import org.apache.commons.lang3.ArrayUtils;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 *
 * @version $Id$
 */
public class XWikiServletRequestStub implements XWikiRequest
{
    private boolean secure;

    private String scheme;

    private String protocol;

    private String queryString;

    private String contextPath;

    private String servletPath;

    private String serverName;

    private int serverPort;

    private Vector<String> headerNames;

    private Map<String, Vector<String>> headers;

    private Map<String, String[]> parameters;

    private String requestURI;

    private StringBuffer requestURL;

    private boolean daemon = true;

    public XWikiServletRequestStub()
    {
    }

    /**
     * @since 8.4RC1
     */
    public XWikiServletRequestStub(URL requestURL, Map<String, String[]> requestParameters)
    {
        if (requestURL != null) {
            this.protocol = requestURL.getProtocol();
            this.scheme = requestURL.getProtocol();

            this.serverName = requestURL.getHost();
            this.serverPort = requestURL.getPort();

            this.secure = this.protocol.equalsIgnoreCase("https");

            this.requestURI = requestURL.getPath();
            this.requestURL = new StringBuffer(requestURL.toString());

            setHost(requestURL.getHost());
        }

        this.parameters = clone(requestParameters);
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

        this.headerNames = new Vector<>();
        this.headers = new LinkedHashMap<>();
        for (Enumeration<String> enumeration = request.getHeaderNames(); enumeration.hasMoreElements();) {
            String headerName = enumeration.nextElement();

            this.headerNames.addElement(headerName);
            Vector<String> values = new Vector<>();
            for (Enumeration<String> e2 = request.getHeaders(headerName); e2.hasMoreElements();) {
                values.addElement(e2.nextElement());
            }
            this.headers.put(headerName.toLowerCase(), values);
        }

        this.parameters = clone(request.getParameterMap());
    }

    private Map<String, String[]> clone(Map<String, String[]> map)
    {
        Map<String, String[]> clone;
        if (this.parameters != null) {
            clone = new LinkedHashMap<>(map.size());
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                clone.put(entry.getKey(), entry.getValue().clone());
            }
        } else {
            clone = null;
        }

        return clone;
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
    public String getHeader(String s)
    {
        if (this.headers != null) {
            Vector<String> values = this.headers.get(s.toLowerCase());

            if (values != null) {
                values.get(0);
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
            values = new String[] { value };
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
        return null;
    }

    @Override
    public String getAuthType()
    {
        return null;
    }

    @Override
    public Cookie[] getCookies()
    {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String s)
    {
        return 0;
    }

    @Override
    public Enumeration<String> getHeaders(String s)
    {
        if (this.headers != null) {
            Vector<String> values = this.headers.get(s.toLowerCase());

            if (values != null) {
                return values.elements();
            }
        }

        return Collections.emptyEnumeration();
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return this.headerNames != null ? this.headerNames.elements() : Collections.emptyEnumeration();
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
        return null;
    }

    @Override
    public HttpSession getSession()
    {
        return null;
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
        return null;
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
        return null;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException
    {
        return null;
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
}
