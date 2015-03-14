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
package com.xpn.xwiki.gwt.api.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

import com.xpn.xwiki.web.XWikiRequest;

public class XWikiRequestWrapper implements XWikiRequest
{
    private final XWikiRequest request;

    public Map paramMap;

    public XWikiRequestWrapper(XWikiRequest request)
    {
        this.request = request;
    }

    public XWikiRequest getRequest()
    {
        return request;
    }

    public void setParameterMap(Map params)
    {
        paramMap = params;
    }

    // XWikiRequest

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return request.getHttpServletRequest();
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return request.getCookie(cookieName);
    }

    @Override
    public String getAuthType()
    {
        return request.getAuthType();
    }

    @Override
    public Cookie[] getCookies()
    {
        return request.getCookies();
    }

    @Override
    public long getDateHeader(String s)
    {
        return request.getDateHeader(s);
    }

    @Override
    public String getHeader(String s)
    {
        return request.getHeader(s);
    }

    @Override
    public Enumeration getHeaders(String s)
    {
        return request.getHeaders(s);
    }

    @Override
    public Enumeration getHeaderNames()
    {
        return request.getHeaderNames();
    }

    @Override
    public int getIntHeader(String s)
    {
        return request.getIntHeader(s);
    }

    @Override
    public String getMethod()
    {
        return request.getMethod();
    }

    @Override
    public String getPathInfo()
    {
        return request.getPathInfo();
    }

    @Override
    public String getPathTranslated()
    {
        return request.getPathTranslated();
    }

    @Override
    public String getContextPath()
    {
        return request.getContextPath();
    }

    @Override
    public String getQueryString()
    {
        return request.getQueryString();
    }

    @Override
    public String getRemoteUser()
    {
        return request.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String s)
    {
        return request.isUserInRole(s);
    }

    @Override
    public Principal getUserPrincipal()
    {
        return request.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId()
    {
        return request.getRequestedSessionId();
    }

    @Override
    public String getRequestURI()
    {
        return request.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return request.getRequestURL();
    }

    @Override
    public String getServletPath()
    {
        return request.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean b)
    {
        return request.getSession(b);
    }

    @Override
    public HttpSession getSession()
    {
        return request.getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return request.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        return request.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        return request.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl()
    {
        return request.isRequestedSessionIdFromUrl();
    }

    @Override
    public Object getAttribute(String s)
    {
        return request.getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return request.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding()
    {
        return request.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {
        request.setCharacterEncoding(s);
    }

    @Override
    public int getContentLength()
    {
        return request.getContentLength();
    }

    @Override
    public String getContentType()
    {
        return request.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return request.getInputStream();
    }

    @Override
    public String get(String name)
    {
        return getParameter(name);
    }

    @Override
    public String getParameter(String name)
    {
        if (paramMap == null)
            return request.getParameter(name);
        Object data = paramMap.get(name);
        if (data == null) {
            return "";
        } else if (data instanceof String)
            return (String) data;
        else if (data instanceof String[]) {
            if (((String[]) data).length > 0)
                return (((String[]) data))[0];
            else
                return "";
        } else if (data instanceof Collection) {
            if (((Collection) data).size() > 0)
                return ((Collection) data).toArray()[0].toString();
            else
                return "";
        } else {
            return data.toString();
        }
    }

    @Override
    public Enumeration getParameterNames()
    {
        if (paramMap == null)
            return request.getParameterNames();
        else {
            Set keys = paramMap.keySet();
            Vector v = new Vector();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                v.add(it.next());
            }
            return v.elements();
        }
    }

    @Override
    public String[] getParameterValues(String name)
    {
        if (paramMap == null)
            return request.getParameterValues(name);
        else {
            Object data = paramMap.get(name);
            if (data == null) {
                return new String[0];
            } else if (data instanceof String) {
                String[] result = new String[1];
                result[0] = (String) data;
                return result;
            } else if (data instanceof String[]) {
                return (String[]) data;
            } else if (data instanceof Collection) {
                String[] result = new String[((Collection) data).size()];
                Iterator it = ((Collection) data).iterator();
                int i = 0;
                while (it.hasNext()) {
                    result[i] = (String) it.next();
                    i++;
                }
                return result;
            } else {
                String[] result = new String[1];
                result[0] = data.toString();
                return result;
            }
        }

    }

    @Override
    public Map getParameterMap()
    {
        if (paramMap == null)
            return request.getParameterMap();
        else
            return paramMap;
    }

    @Override
    public String getProtocol()
    {
        return request.getProtocol();
    }

    @Override
    public String getScheme()
    {
        return request.getScheme();
    }

    @Override
    public String getServerName()
    {
        return request.getServerName();
    }

    @Override
    public int getServerPort()
    {
        return request.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return request.getReader();
    }

    @Override
    public String getRemoteAddr()
    {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost()
    {
        return request.getRemoteHost();
    }

    @Override
    public void setAttribute(String s, Object o)
    {
        request.setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s)
    {
        request.removeAttribute(s);
    }

    @Override
    public Locale getLocale()
    {
        return request.getLocale();
    }

    @Override
    public Enumeration getLocales()
    {
        return request.getLocales();
    }

    @Override
    public boolean isSecure()
    {
        return request.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s)
    {
        return request.getRequestDispatcher(s);
    }

    @Override
    public String getRealPath(String s)
    {
        return request.getRealPath(s);
    }

    @Override
    public int getRemotePort()
    {
        return request.getRemotePort();
    }

    @Override
    public String getLocalName()
    {
        return request.getLocalName();
    }

    @Override
    public String getLocalAddr()
    {
        return request.getLocalAddr();
    }

    @Override
    public int getLocalPort()
    {
        return request.getLocalPort();
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException
    {
        return request.authenticate(httpServletResponse);
    }

    @Override
    public void login(String s, String s1) throws ServletException
    {
        request.login(s, s1);
    }

    @Override
    public void logout() throws ServletException
    {
        request.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException
    {
        return request.getParts();
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException
    {
        return request.getPart(s);
    }

    @Override
    public ServletContext getServletContext()
    {
        return request.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException
    {
        return request.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IllegalStateException
    {
        return request.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted()
    {
        return request.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported()
    {
        return request.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext()
    {
        return request.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType()
    {
        return request.getDispatcherType();
    }
}
