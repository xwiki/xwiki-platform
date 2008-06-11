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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.upload.MultipartRequestWrapper;

import com.xpn.xwiki.util.Util;

public class XWikiServletRequest implements XWikiRequest
{
    private HttpServletRequest request;

    public XWikiServletRequest(HttpServletRequest request)
    {
        this.request = request;
    }

    /**
     * Turn Windows CP1252 Characters to iso-8859-1 characters when possible, HTML entities when needed. This filtering
     * works on Tomcat (not Jetty).
     * 
     * @param text The text to filter
     * @return filtered text
     */
    public String filterString(String text)
    {
        if (text == null) {
            return null;
        }

        // In case we are running in ISO we need to take care or some windows-1252 characters
        // that are commonly copy pasted by users from web sites or desktop applications
        // If we don't transform these characters then some databases running in the latin charset mode
        // will drop the characters and will we only see that on server restart.
        // This happens for example using MySQL both with tomcat and Jetty
        // See bug : http://jira.xwiki.org/jira/browse/XWIKI-2422
        // Source : http://www.microsoft.com/typography/unicode/1252.htm
        if (this.request.getCharacterEncoding().startsWith("ISO-8859")) {
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

    public String[] filterStringArray(String[] text)
    {
        if (text == null) {
            return null;
        }

        for (int i = 0; i < text.length; i++) {
            text[i] = filterString(text[i]);
        }
        return text;
    }

    public String get(String name)
    {
        return filterString(this.request.getParameter(name));
    }

    public HttpServletRequest getHttpServletRequest()
    {
        return this.request;
    }

    public String getAuthType()
    {
        return this.request.getAuthType();
    }

    public Cookie[] getCookies()
    {
        return this.request.getCookies();
    }

    public Cookie getCookie(String cookieName)
    {
        return Util.getCookie(cookieName, this);
    }

    public long getDateHeader(String s)
    {
        return this.request.getDateHeader(s);
    }

    public String getHeader(String s)
    {
        return this.request.getHeader(s);
    }

    public Enumeration getHeaders(String s)
    {
        return this.request.getHeaders(s);
    }

    public Enumeration getHeaderNames()
    {
        return this.request.getHeaderNames();
    }

    public int getIntHeader(String s)
    {
        return this.request.getIntHeader(s);
    }

    public String getMethod()
    {
        return this.request.getMethod();
    }

    public String getPathInfo()
    {
        return this.request.getPathInfo();
    }

    public String getPathTranslated()
    {
        return this.request.getPathTranslated();
    }

    public String getContextPath()
    {
        return this.request.getContextPath();
    }

    public String getQueryString()
    {
        return this.request.getQueryString();
    }

    public String getRemoteUser()
    {
        return this.request.getRemoteUser();
    }

    public boolean isUserInRole(String s)
    {
        return this.request.isUserInRole(s);
    }

    public Principal getUserPrincipal()
    {
        return this.request.getUserPrincipal();
    }

    public String getRequestedSessionId()
    {
        return this.request.getRequestedSessionId();
    }

    public String getRequestURI()
    {
        return this.request.getRequestURI();
    }

    public StringBuffer getRequestURL()
    {
        StringBuffer requestURL = this.request.getRequestURL();
        if ((requestURL == null) && (this.request instanceof MultipartRequestWrapper)) {
            requestURL = ((MultipartRequestWrapper) this.request).getRequest().getRequestURL();
        }
        return requestURL;
    }

    public String getServletPath()
    {
        return this.request.getServletPath();
    }

    public HttpSession getSession(boolean b)
    {
        return this.request.getSession(b);
    }

    public HttpSession getSession()
    {
        return this.request.getSession();
    }

    public boolean isRequestedSessionIdValid()
    {
        return this.request.isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return this.request.isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return this.request.isRequestedSessionIdFromURL();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean isRequestedSessionIdFromUrl()
    {
        return this.request.isRequestedSessionIdFromURL();
    }

    public Object getAttribute(String s)
    {
        return this.request.getAttribute(s);
    }

    public Enumeration getAttributeNames()
    {
        return this.request.getAttributeNames();
    }

    public String getCharacterEncoding()
    {
        return this.request.getCharacterEncoding();
    }

    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {
        this.request.setCharacterEncoding(s);
    }

    public int getContentLength()
    {
        return this.request.getContentLength();
    }

    public String getContentType()
    {
        return this.request.getContentType();
    }

    public ServletInputStream getInputStream() throws IOException
    {
        return this.request.getInputStream();
    }

    public String getParameter(String s)
    {
        return filterString(this.request.getParameter(s));
    }

    public Enumeration getParameterNames()
    {
        return this.request.getParameterNames();
    }

    public String[] getParameterValues(String s)
    {
        String[] origResult = this.request.getParameterValues(s);
        return filterStringArray(origResult);
    }

    public Map getParameterMap()
    {
        Map newMap = new HashMap();
        Map map = this.request.getParameterMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            if (value instanceof String) {
                newMap.put(key, filterString((String) value));
            } else if (value instanceof String[]) {
                newMap.put(key, filterStringArray((String[]) value));
            } else {
                newMap.put(key, value);
            }
        }
        return map;
    }

    public String getProtocol()
    {
        return this.request.getProtocol();
    }

    public String getScheme()
    {
        return this.request.getScheme();
    }

    public String getServerName()
    {
        return this.request.getServerName();
    }

    public int getServerPort()
    {
        return this.request.getServerPort();
    }

    public BufferedReader getReader() throws IOException
    {
        return this.request.getReader();
    }

    public String getRemoteAddr()
    {
        if (this.request.getHeader("x-forwarded-for") != null) {
            return this.request.getHeader("x-forwarded-for");
        }
        return this.request.getRemoteAddr();
    }

    public String getRemoteHost()
    {
        if (this.request.getHeader("x-forwarded-for") != null) {
            return this.request.getHeader("x-forwarded-for");
        }
        return this.request.getRemoteHost();
    }

    public void setAttribute(String s, Object o)
    {
        this.request.setAttribute(s, o);
    }

    public void removeAttribute(String s)
    {
        this.request.removeAttribute(s);
    }

    public Locale getLocale()
    {
        return this.request.getLocale();
    }

    public Enumeration getLocales()
    {
        return this.request.getLocales();
    }

    public boolean isSecure()
    {
        return this.request.isSecure();
    }

    public RequestDispatcher getRequestDispatcher(String s)
    {
        return this.request.getRequestDispatcher(s);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getRealPath(String s)
    {
        return this.request.getRealPath(s);
    }

    public int getRemotePort()
    {
        return this.request.getRemotePort();
    }

    public String getLocalName()
    {
        return this.request.getLocalName();
    }

    public String getLocalAddr()
    {
        return this.request.getLocalAddr();
    }

    public int getLocalPort()
    {
        return this.request.getLocalPort();
    }

    /*
     * Portlet Functions. They do nothing in the servlet environement
     */

    public boolean isWindowStateAllowed(WindowState windowState)
    {
        return false;
    }

    public boolean isPortletModeAllowed(PortletMode portletMode)
    {
        return false;
    }

    public PortletMode getPortletMode()
    {
        return null;
    }

    public WindowState getWindowState()
    {
        return null;
    }

    public PortletPreferences getPreferences()
    {
        return null;
    }

    public PortletSession getPortletSession()
    {
        return null;
    }

    public PortletSession getPortletSession(boolean b)
    {
        return null;
    }

    public PortalContext getPortalContext()
    {
        return null;
    }

    public String getProperty(String s)
    {
        return null;
    }

    public Enumeration getProperties(String s)
    {
        return null;
    }

    public Enumeration getPropertyNames()
    {
        return null;
    }

    public String getResponseContentType()
    {
        return null;
    }

    public Enumeration getResponseContentTypes()
    {
        return null;
    }

    public InputStream getPortletInputStream() throws IOException
    {
        return null;
    }
}
