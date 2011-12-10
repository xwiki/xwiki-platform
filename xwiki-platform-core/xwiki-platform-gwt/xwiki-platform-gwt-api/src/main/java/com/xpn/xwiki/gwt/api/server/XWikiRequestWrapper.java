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

import com.xpn.xwiki.web.XWikiRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import javax.portlet.*;
import java.util.*;
import java.security.Principal;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;

public class XWikiRequestWrapper implements XWikiRequest {
    private final XWikiRequest request;
    public Map paramMap;

    public XWikiRequestWrapper(XWikiRequest request) {
        this.request = request;
    }

    public XWikiRequest getRequest() {
        return request;
    }

    public HttpServletRequest getHttpServletRequest() {
        return request.getHttpServletRequest();
    }

    public Cookie getCookie(String cookieName) {
        return request.getCookie(cookieName);
    }

    public String getAuthType() {
        return request.getAuthType();
    }

    public Cookie[] getCookies() {
        return request.getCookies();
    }

    public long getDateHeader(String s) {
        return request.getDateHeader(s);
    }

    public String getHeader(String s) {
        return request.getHeader(s);
    }

    public Enumeration getHeaders(String s) {
        return request.getHeaders(s);
    }

    public Enumeration getHeaderNames() {
        return request.getHeaderNames();
    }

    public int getIntHeader(String s) {
        return request.getIntHeader(s);
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getPathInfo() {
        return request.getPathInfo();
    }

    public String getPathTranslated() {
        return request.getPathTranslated();
    }

    public String getContextPath() {
        return request.getContextPath();
    }

    public String getQueryString() {
        return request.getQueryString();
    }

    public String getRemoteUser() {
        return request.getRemoteUser();
    }

    public boolean isUserInRole(String s) {
        return request.isUserInRole(s);
    }

    public Principal getUserPrincipal() {
        return request.getUserPrincipal();
    }

    public String getRequestedSessionId() {
        return request.getRequestedSessionId();
    }

    public String getRequestURI() {
        return request.getRequestURI();
    }

    public StringBuffer getRequestURL() {
        return request.getRequestURL();
    }

    public String getServletPath() {
        return request.getServletPath();
    }

    public HttpSession getSession(boolean b) {
        return request.getSession(b);
    }

    public HttpSession getSession() {
        return request.getSession();
    }

    public boolean isRequestedSessionIdValid() {
        return request.isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return request.isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL() {
        return request.isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdFromUrl() {
        return request.isRequestedSessionIdFromUrl();
    }

    public Object getAttribute(String s) {
        return request.getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        return request.getAttributeNames();
    }

    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }

    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        request.setCharacterEncoding(s);
    }

    public int getContentLength() {
        return request.getContentLength();
    }

    public String getContentType() {
        return request.getContentType();
    }

    public ServletInputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    public String get(String name) {
        return getParameter(name);
    }

    public String getParameter(String name) {
        if (paramMap==null)
         return request.getParameter(name);
        Object data = paramMap.get(name);
        if (data==null) {
            return "";
        } else if (data instanceof String)
         return (String) data;
        else if (data instanceof String[]) {
            if (((String[])data).length>0)
             return (((String[])data))[0];
            else
             return "";
        } else if (data instanceof Collection) {
            if (((Collection)data).size()>0)
             return ((Collection)data).toArray()[0].toString();
            else
             return "";
        } else {
            return data.toString();
        }
    }

    public Enumeration getParameterNames() {
        if (paramMap==null)
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

    public String[] getParameterValues(String name) {
        if (paramMap==null)
         return request.getParameterValues(name);
        else {
            Object data = paramMap.get(name);
            if (data==null) {
                return new String[0];
            } else if (data instanceof String) {
                String[] result = new String[1];
                result[0] = (String) data;
                return result;
            } else if (data instanceof String[]) {
                return (String[]) data;
            } else if (data instanceof Collection) {
                String[] result = new String[((Collection)data).size()];
                Iterator it = ((Collection)data).iterator();
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

    public Map getParameterMap() {
        if (paramMap==null)
         return request.getParameterMap();
        else
         return paramMap;
    }


    public void setParameterMap(Map params) {
        paramMap = params;
    }

    public String getProtocol() {
        return request.getProtocol();
    }

    public String getScheme() {
        return request.getScheme();
    }

    public String getServerName() {
        return request.getServerName();
    }

    public int getServerPort() {
        return request.getServerPort();
    }

    public BufferedReader getReader() throws IOException {
        return request.getReader();
    }

    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    public void setAttribute(String s, Object o) {
        request.setAttribute(s, o);
    }

    public void removeAttribute(String s) {
        request.removeAttribute(s);
    }

    public Locale getLocale() {
        return request.getLocale();
    }

    public Enumeration getLocales() {
        return request.getLocales();
    }

    public boolean isSecure() {
        return request.isSecure();
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return request.getRequestDispatcher(s);
    }

    public String getRealPath(String s) {
        return request.getRealPath(s);
    }

    public int getRemotePort() {
        return request.getRemotePort();
    }

    public String getLocalName() {
        return request.getLocalName();
    }

    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    public int getLocalPort() {
        return request.getLocalPort();
    }

    public boolean isWindowStateAllowed(WindowState windowState) {
        return request.isWindowStateAllowed(windowState);
    }

    public boolean isPortletModeAllowed(PortletMode portletMode) {
        return request.isPortletModeAllowed(portletMode);
    }

    public PortletMode getPortletMode() {
        return request.getPortletMode();
    }

    public WindowState getWindowState() {
        return request.getWindowState();
    }

    public PortletPreferences getPreferences() {
        return request.getPreferences();
    }

    public PortletSession getPortletSession() {
        return request.getPortletSession();
    }

    public PortletSession getPortletSession(boolean b) {
        return request.getPortletSession(b);
    }

    public PortalContext getPortalContext() {
        return request.getPortalContext();
    }

    public String getProperty(String s) {
        return request.getProperty(s);
    }

    public Enumeration getProperties(String s) {
        return request.getProperties(s);
    }

    public Enumeration getPropertyNames() {
        return request.getPropertyNames();
    }

    public String getResponseContentType() {
        return request.getResponseContentType();
    }

    public Enumeration getResponseContentTypes() {
        return request.getResponseContentTypes();
    }

    public InputStream getPortletInputStream() throws IOException {
        return request.getPortletInputStream();
    }
}
