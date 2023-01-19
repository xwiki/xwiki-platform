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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.url.URLSecurityManager;

public class XWikiServletResponse implements XWikiResponse
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiServletResponse.class);

    // Regular expression taken from https://www.rfc-editor.org/rfc/rfc3986#appendix-B.
    private static final Pattern URI_PATTERN =
        Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    private static final String ERROR_TRANSFORMING_URI_LOG =
        "Error while transforming redirect to [{}] to proper URI: [{}]";

    private static final String FULL_STACK_TRACE = "Full stack trace:";

    private HttpServletResponse response;

    public XWikiServletResponse(HttpServletResponse response)
    {
        this.response = response;
    }

    @Override
    public int getStatus()
    {
        return this.response.getStatus();
    }

    @Override
    public HttpServletResponse getHttpServletResponse()
    {
        return this.response;
    }

    @Override
    public void sendRedirect(String redirect) throws IOException
    {
        if (!StringUtils.isBlank(redirect)) {
            URI uri = parseURI(redirect);
            if (uri != null) {
                if (!getURLSecurityManager().isURITrusted(uri)) {
                    LOGGER.warn(
                        "Possible phishing attack, attempting to redirect to [{}], this request has been blocked. "
                            + "If the request was legitimate, please check the URL security configuration. You "
                            + "might need to add the domain related to this request in the list of trusted domains in "
                            + "the configuration: it can be configured in xwiki.properties in url.trustedDomains.",
                        redirect);
                } else {
                    this.response.sendRedirect(uri.toString());
                }
            }
        }
    }

    private URI parseURI(String location)
    {
        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            // Attempt repairing the invalid URI similar to org.eclipse.jetty.client.HttpRedirector#sanitize by
            // extracting the different parts and then passing them to the multi-argument constructor that quotes
            // illegal characters.
            Matcher matcher = URI_PATTERN.matcher(location);
            if (matcher.matches()) {
                String scheme = matcher.group(2);
                String authority = matcher.group(4);
                String path = matcher.group(5);
                String query = matcher.group(7);
                String fragment = matcher.group(9);
                try {
                    uri = new URI(scheme, authority, path, query, fragment);
                } catch (URISyntaxException ex) {
                    LOGGER.error(ERROR_TRANSFORMING_URI_LOG, location,
                        ExceptionUtils.getRootCauseMessage(e));
                    LOGGER.debug(FULL_STACK_TRACE, e);
                }
            } else {
                LOGGER.error(ERROR_TRANSFORMING_URI_LOG, location,
                    ExceptionUtils.getRootCauseMessage(e));
                LOGGER.debug(FULL_STACK_TRACE, e);
            }
        }
        return uri;
    }

    private URLSecurityManager getURLSecurityManager()
    {
        return Utils.getComponent(URLSecurityManager.class);
    }

    @Override
    public void setContentType(String type)
    {
        this.response.setContentType(type);
    }

    @Override
    public void setBufferSize(int i)
    {
        this.response.setBufferSize(i);
    }

    @Override
    public int getBufferSize()
    {
        return this.response.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException
    {
        this.response.flushBuffer();
    }

    @Override
    public void resetBuffer()
    {
        this.response.resetBuffer();
    }

    @Override
    public boolean isCommitted()
    {
        return this.response.isCommitted();
    }

    @Override
    public void reset()
    {
        this.response.reset();
    }

    @Override
    public void setContentLength(int length)
    {
        this.response.setContentLength(length);
    }

    @Override
    public void setContentLengthLong(long len)
    {
        this.response.setContentLengthLong(len);
    }

    @Override
    public String getCharacterEncoding()
    {
        return this.response.getCharacterEncoding();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        return this.response.getWriter();
    }

    @Override
    public void setCharacterEncoding(String s)
    {
        this.response.setCharacterEncoding(s);
    }

    @Override
    public void addCookie(Cookie cookie)
    {
        this.response.addCookie(cookie);
    }

    public void addCookie(String cookieName, String cookieValue, int age)
    {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setVersion(1);
        cookie.setMaxAge(age);
        this.response.addCookie(cookie);
    }

    /**
     * Remove a cookie.
     *
     * @param request The servlet request needed to find the cookie to remove
     * @param cookieName The name of the cookie that must be removed.
     */
    @Override
    public void removeCookie(String cookieName, XWikiRequest request)
    {
        Cookie cookie = request.getCookie(cookieName);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setPath(cookie.getPath());
            addCookie(cookie);
        }
    }

    @Override
    public void setLocale(Locale locale)
    {
        this.response.setLocale(locale);
    }

    @Override
    public Locale getLocale()
    {
        return this.response.getLocale();
    }

    @Override
    public void setDateHeader(String name, long value)
    {
        this.response.setDateHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value)
    {
        this.response.setIntHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value)
    {
        this.response.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value)
    {
        this.response.addHeader(name, value);
    }

    @Override
    public void addDateHeader(String name, long value)
    {
        this.response.addDateHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        this.response.addIntHeader(name, value);
    }

    @Override
    public void setStatus(int i)
    {
        this.response.setStatus(i);
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void setStatus(int i, String s)
    {
        this.response.setStatus(i, s);
    }

    @Override
    public boolean containsHeader(String name)
    {
        return this.response.containsHeader(name);
    }

    @Override
    public String encodeURL(String s)
    {
        return this.response.encodeURL(s);
    }

    @Override
    public String encodeRedirectURL(String s)
    {
        return this.response.encodeRedirectURL(s);
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String encodeUrl(String s)
    {
        return this.response.encodeUrl(s);
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String encodeRedirectUrl(String s)
    {
        return this.response.encodeRedirectUrl(s);
    }

    @Override
    public void sendError(int i, String s) throws IOException
    {
        this.response.sendError(i, s);
    }

    @Override
    public void sendError(int i) throws IOException
    {
        this.response.sendError(i);
    }

    @Override
    public String getContentType()
    {
        return this.response.getContentType();
    }

    @Override
    public String getHeader(String s)
    {
        return this.response.getHeader(s);
    }

    @Override
    public Collection<String> getHeaders(String s)
    {
        return this.response.getHeaders(s);
    }

    @Override
    public Collection<String> getHeaderNames()
    {
        return this.response.getHeaderNames();
    }
}
