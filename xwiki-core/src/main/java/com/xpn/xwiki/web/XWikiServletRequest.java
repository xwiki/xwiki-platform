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
import java.util.*;

import com.xpn.xwiki.util.Util;

public class XWikiServletRequest implements XWikiRequest {
    private HttpServletRequest request;

    public XWikiServletRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Turn Windows CP1252 Characters to iso-8859-1 characters when possible, HTML entities when needed.
     * This filtering works on Tomcat (not Jetty).
     *
     * @param text The text to filter
     * @return filtered text
     */
    public String filterString(String text) {
        if (text==null)
            return null;

        // In case we are running in ISO we need to take care or some windows-1252 characters
        // that are commonly copy pasted by users from web sites or desktop applications
        // If we don't transform these characters then some databases running in the latin charset mode
        // will drop the characters and will we only see that on server restart.
        // This happens for example using MySQL both with tomcat and Jetty
        // See bug : http://jira.xwiki.org/jira/browse/XWIKI-2422
        // Source : http://www.microsoft.com/typography/unicode/1252.htm
        if (request.getCharacterEncoding().startsWith("ISO-8859")) {
            // EURO SIGN
            text = text.replaceAll("\u0080", "&euro;");
            // SINGLE LOW-9 QUOTATION MARK
            text = text.replaceAll("\u0082", "&sbquo;");
            // LATIN SMALL LETTER F WITH HOOK
            text = text.replaceAll("\u0083", "&fnof;");
            // DOUBLE LOW-9 QUOTATION MARK
            text = text.replaceAll("\u0084", "&bdquo;");
            // HORIZONTAL ELLIPSIS, entity : &hellip;
            text = text.replaceAll("\u0085", "...");
            // DAGGER
            text = text.replaceAll("\u0086", "&dagger;");
            // DOUBLE DAGGER
            text = text.replaceAll("\u0087", "&Dagger;");
            // MODIFIER LETTER CIRCUMFLEX ACCENT
            text = text.replaceAll("\u0088", "&circ;");
            // PER MILLE SIGN
            text = text.replaceAll("\u0089", "&permil;");
            // LATIN CAPITAL LETTER S WITH CARON
            text = text.replaceAll("\u008a", "&Scaron;");
            // SINGLE LEFT-POINTING ANGLE QUOTATION MARK, entity : &lsaquo;
            text = text.replaceAll("\u008b", "'");
            // LATIN CAPITAL LIGATURE OE
            text = text.replaceAll("\u008c", "&OElig;");
            // LATIN CAPITAL LETTER Z WITH CARON
            text = text.replaceAll("\u008e", "&#381;");
            // LEFT SINGLE QUOTATION MARK, entity : &lsquo;
            text = text.replaceAll("\u0091", "'");
            // RIGHT SINGLE QUOTATION MARK, entity : &rsquo;
            text = text.replaceAll("\u0092", "'");
            // LEFT DOUBLE QUOTATION MARK, entity : &ldquo;
            text = text.replaceAll("\u0093", "\"");
            // RIGHT DOUBLE QUOTATION MARK, entity : &rdquo;
            text = text.replaceAll("\u0094", "\"");
            // BULLET
            text = text.replaceAll("\u0095", "&bull;");
            // EN DASH, entity : &ndash;
            text = text.replaceAll("\u0096", "-");
            // EM DASH, entity : &mdash;
            text = text.replaceAll("\u0097", "-");
            // SMALL TILDE
            text = text.replaceAll("\u0098", "&tilde;");
            // TRADE MARK SIGN
            text = text.replaceAll("\u0099", "&trade;");
            // LATIN SMALL LETTER S WITH CARON
            text = text.replaceAll("\u009a", "&scaron;");
            // SINGLE RIGHT-POINTING ANGLE QUOTATION MARK, entity : &rsaquo;
            text = text.replaceAll("\u009b", "'");
            // LATIN SMALL LIGATURE OE
            text = text.replaceAll("\u009c", "&oelig;");
            // LATIN SMALL LETTER Z WITH CARON
            text = text.replaceAll("\u009e", "&#382;");
            // LATIN CAPITAL LETTER Y WITH DIAERESIS
            text = text.replaceAll("\u009f", "&Yuml;");

        }
        return text;
    }

    public String[] filterStringArray(String[] text) {
        if (text==null)
         return null;

        for (int i=0;i<text.length;i++) {
            text[i] = filterString(text[i]);
        }
        return text;
    }

    public String get(String name) {
        return filterString(request.getParameter(name));
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
        return filterString(request.getParameter(s));
    }

    public Enumeration getParameterNames() {
        return request.getParameterNames();
    }

    public String[] getParameterValues(String s) {
        String[] origResult = request.getParameterValues(s);
        return filterStringArray(origResult);
    }

    public Map getParameterMap() {
        Map newMap = new HashMap();
        Map map = request.getParameterMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
           String key = (String) it.next();
           Object value = map.get(key);
           if (value instanceof String)
             newMap.put(key, filterString((String) value));
           else if (value instanceof String[])
             newMap.put(key, filterStringArray((String[]) value));
           else
             newMap.put(key, value);
        }
        return map;
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


