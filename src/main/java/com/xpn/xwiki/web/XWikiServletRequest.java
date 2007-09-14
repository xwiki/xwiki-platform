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

package com.xpn.xwiki.web;

import org.apache.struts.upload.MultipartRequestWrapper;

import javax.portlet.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.util.Util;

public class XWikiServletRequest implements XWikiRequest {
    private HttpServletRequest request;

    public XWikiServletRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String get(String name) {
        return request.getParameter(name);
    }

    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    public String getAuthType() {
        return request.getAuthType();
    }

    public Cookie[] getCookies() {
        return request.getCookies();
    }

    public Cookie getCookie(String cookieName) {
        return Util.getCookie(cookieName, this);
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
            StringBuffer requestURL = request.getRequestURL();
            if ((requestURL==null)&&(request instanceof MultipartRequestWrapper))
                requestURL = ((MultipartRequestWrapper) request).getRequest().getRequestURL();
            return requestURL;
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

    /**
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl() {
        return request.isRequestedSessionIdFromURL();
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

    public String getParameter(String s) {
        return request.getParameter(s);
    }

    public Enumeration getParameterNames() {
        return request.getParameterNames();
    }

    public String[] getParameterValues(String s) {
        return request.getParameterValues(s);
    }

    public Map getParameterMap() {
        return request.getParameterMap();
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
	if (request.getHeader("x-forwarded-for") != null)
	    return request.getHeader("x-forwarded-for");
        return request.getRemoteAddr();
    }

    public String getRemoteHost() {
	if (request.getHeader("x-forwarded-for") != null)
	    return request.getHeader("x-forwarded-for");
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

    /**
     * @deprecated
     */
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

    /*
    * Portlet Functions. They do nothing in  the servlet environement
    */

    public boolean isWindowStateAllowed(WindowState windowState) {
        return false;
    }

    public boolean isPortletModeAllowed(PortletMode portletMode) {
        return false;
    }

    public PortletMode getPortletMode() {
        return null;
    }

    public WindowState getWindowState() {
        return null;
    }

    public PortletPreferences getPreferences() {
        return null;
    }

    public PortletSession getPortletSession() {
        return null;
    }

    public PortletSession getPortletSession(boolean b) {
        return null;
    }

    public PortalContext getPortalContext() {
        return null;
    }

    public String getProperty(String s) {
        return null;
    }

    public Enumeration getProperties(String s) {
        return null;
    }

    public Enumeration getPropertyNames() {
        return null;
    }

    public String getResponseContentType() {
        return null;
    }

    public Enumeration getResponseContentTypes() {
        return null;
    }

    public InputStream getPortletInputStream() throws IOException {
        return null;
    }
}


