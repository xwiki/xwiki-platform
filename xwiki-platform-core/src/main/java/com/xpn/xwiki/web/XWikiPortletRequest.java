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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

public class XWikiPortletRequest implements XWikiRequest {
    protected final Log logger = LogFactory.getLog(getClass());
    public static final String ROOT_SPACE_PREF_NAME = "rootSpace";
    private PortletRequest request;

    public XWikiPortletRequest(PortletRequest request) {
        this.request = request;
    }

    public PortletRequest getPortletRequest() {
        return request;
    }

    public String get(String name) {
        return request.getParameter(name);
    }


    public String getParameter(String name) {
        String retVal = request.getParameter(name);
        if(retVal == null && name.equals("topic")) {
            String rootSpace = request.getPreferences().getValue("rootSpace", null);
            if(rootSpace != null && rootSpace.length() > 0) {
                if(logger.isDebugEnabled())
                    logger.debug("Root space [" + rootSpace + "] was getted from preferences");
                retVal = rootSpace + ".WebHome";
            }
        }
        return retVal;
    }

    public Enumeration getParameterNames() {
        return request.getParameterNames();
    }

    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
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

    public String getProperty(String s) {
        return request.getProperty(s);
    }

    public Enumeration getProperties(String s) {
        return request.getProperties(s);
    }

    public Enumeration getPropertyNames() {
        return request.getPropertyNames();
    }

    public PortalContext getPortalContext() {
        return request.getPortalContext();
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

    public Locale getLocale() {
        return request.getLocale();
    }

    public Enumeration getLocales() {
        return request.getLocales();
    }

    public boolean isSecure() {
        return request.isSecure();
    }

    public Map getParameterMap() {
        return request.getParameterMap();
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

    public void setAttribute(String s, Object o) {
        request.setAttribute(s, o);
    }

    public void removeAttribute(String s) {
        request.removeAttribute(s);
    }

    public String getResponseContentType() {
        return request.getResponseContentType();
    }

    public Enumeration getResponseContentTypes() {
        return request.getResponseContentTypes();
    }

    public String getContextPath() {
        return request.getContextPath();
    }

    public Object getAttribute(String s) {
        return request.getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        return request.getAttributeNames();
    }

    public boolean isRequestedSessionIdValid() {
        return request.isRequestedSessionIdValid();
    }

    /*
    * Implemented Servlet Function for Portlets
    * This will only work if the portlet implementation
    * makes PortletRequest extends HttpServletRequest
    *
    * Modified getHttpServletRequest to work with WebLogic Portal Implementation
    */

    public HttpServletRequest getHttpServletRequest() {
    	HttpServletRequest req = null;

    	if (request instanceof HttpServletRequest) {
        	req = (HttpServletRequest)getPortletRequest();
        }
        else {
        	// WLP impl
        	req = (HttpServletRequest)getPortletRequest().getAttribute("javax.servlet.request");
        }

        if (req == null) {
        	throw new UnsupportedOperationException();
        }

        return req;
    }

    public String getPathInfo() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getPathInfo();
        return null;
    }


    public String getServletPath() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getServletPath();
        return null;
    }
    public StringBuffer getRequestURL() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRequestURL();
        return null;
    }

    public String getQueryString() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getQueryString();
        return null;
    }

    public String getRequestURI() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRequestURI();
        return null;
    }

    public String getAuthType() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getAuthType();
        return null;
    }

    public Cookie[] getCookies() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getCookies();
        return null;
    }

    public Cookie getCookie(String cookieName) {
        if (request instanceof HttpServletRequest)
            return Util.getCookie(cookieName, getHttpServletRequest());
        else
            return null;
    }
    
    public long getDateHeader(String s) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getDateHeader(s);
        return 0;
    }

    public String getHeader(String s) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getHeader(s);
        return null;
    }

    public Enumeration getHeaders(String s) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getHeaders(s);
        return null;
    }

    public Enumeration getHeaderNames() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getHeaderNames();
        return null;
    }

    public int getIntHeader(String s) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getIntHeader(s);
        return 0;
    }

    public String getMethod() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getMethod();
        return null;
    }

    public String getCharacterEncoding() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getCharacterEncoding();
        return null;
    }

    public InputStream getPortletInputStream() throws IOException {
        if (request instanceof ActionRequest)
            return ((ActionRequest)request).getPortletInputStream();
        return null;
    }

    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        if (request instanceof HttpServletRequest)
            getHttpServletRequest().setCharacterEncoding(s);
    }

    public int getContentLength() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getContentLength();
        return 0;
    }

    public String getContentType() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getContentType();
        return null;
    }

    public ServletInputStream getInputStream() throws IOException {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getInputStream();
        return null;
    }

    public HttpSession getSession(boolean b) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getSession(b);
        return null;
    }

    public HttpSession getSession() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getSession();
        return null;
    }


    public boolean isRequestedSessionIdFromCookie() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().isRequestedSessionIdFromCookie();
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().isRequestedSessionIdFromURL();
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().isRequestedSessionIdFromUrl();
        return false;
    }

    public String getPathTranslated() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getPathTranslated();
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRequestDispatcher(s);
        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getRealPath(String s) {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRealPath(s);
        return s;
    }

    public int getRemotePort() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRemotePort();
        return 0;
    }

    public String getLocalName() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getLocalName();
        return "";
    }

    public String getLocalAddr() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getLocalAddr();
        return "";
    }

    public int getLocalPort() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getLocalPort();
        return 0;
    }


    public String getProtocol() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getProtocol();
        return null;
    }

    public BufferedReader getReader() throws IOException {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getReader();
        return null;
    }

    public String getRemoteAddr() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRemoteAddr();
        return null;
    }

    public String getRemoteHost() {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRemoteHost();
        return null;
    }

}
