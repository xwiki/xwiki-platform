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
package org.xwiki.container.servlet;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.servlet.internal.ForwardedHeader;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.stability.Unstable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Various helpers around the {@link HttpServletRequest} and {@link HttpServletResponse} API.
 * <p>
 * Takes care of proxy modifications.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public final class HttpServletUtils
{
    /**
     * RFC 7239, section 4: Forwarded.
     */
    public static final String HEADER_FORWARDED = "forwarded";

    /**
     * Apache stuff, old de-facto standard: X-Forwarded-Host.
     */
    public static final String HEADER_X_FORWARDED_HOST = "x-forwarded-host";

    /**
     * Apache stuff, old de-facto standard: X-Forwarded-Proto.
     */
    public static final String HEADER_X_FORWARDED_PROTO = "x-forwarded-proto";

    /**
     * Header containing the cache control.
     */
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    private static final String HTTP = "http";

    private static final String HTTPS = "https";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServletUtils.class);

    private HttpServletUtils()
    {
        // Utility class
    }

    /**
     * Try to extract from various http headers the URL ({@code <protocol>://<host>[:<port>]/<path>[?<querystring>]}) as
     * close as possible to the one used by the client.
     * <p>
     * In theory HttpServletRequest#getRequestURL() is supposed to take care of all that but depending on the
     * application server and its configuration it's not always reliable. One less thing to configure.
     * 
     * @param servletRequest the servlet request input
     * @return the URL as close as possible from what the client used
     * @throws MalformedURLException when an invalid URL was received
     * @since 42.0.0
     */
    @Unstable
    public static URL getSourceURL(HttpServletRequest servletRequest) throws MalformedURLException
    {
        URL baseURL = getSourceBaseURL(servletRequest);

        StringBuilder path = new StringBuilder();

        path.append(servletRequest.getRequestURI());

        if (StringUtils.isNoneEmpty(servletRequest.getQueryString())) {
            path.append('?');
            path.append(servletRequest.getQueryString());
        }

        return new URL(baseURL, path.toString());
    }

    /**
     * Try to extract from various http headers the base URL ({@code <protocol>://<host>[:<port>]}) as close as possible
     * to the one used by the client.
     * <p>
     * In theory HttpServletRequest#getRequestURL() is supposed to take care of all that but depending on the
     * application server and its configuration it's not always reliable. One less thing to configure.
     * 
     * @param servletRequest the servlet request input
     * @return the URL as close as possible from what the client used
     * @throws MalformedURLException when an invalid URL was received
     * @since 42.0.0
     */
    @Unstable
    public static URL getSourceBaseURL(HttpServletRequest servletRequest) throws MalformedURLException
    {
        StringBuilder builder = new StringBuilder();

        appendScheme(servletRequest, builder);

        builder.append("://");

        appendHostPort(servletRequest, builder);

        try {
            return new URL(builder.toString());
        } catch (MalformedURLException e) {
            // Fallback on whatever was directly received
            return getFinalBaseURL(servletRequest);
        }
    }

    private static URL getFinalBaseURL(HttpServletRequest servletRequest) throws MalformedURLException
    {
        return new URL(servletRequest.getScheme(), servletRequest.getRemoteHost(), servletRequest.getRemotePort(), "");
    }

    private static void appendScheme(HttpServletRequest request, StringBuilder builder)
    {
        // RFC 7239, section 4: Forwarded
        String forwarded = request.getHeader(HEADER_FORWARDED);
        if (StringUtils.isNotEmpty(forwarded)) {
            ForwardedHeader forwardedHeader = new ForwardedHeader(forwarded);
            if (forwardedHeader.getProto() != null) {
                builder.append(forwardedHeader.getProto());
                return;
            }
        }

        // Apache stuff, old de-facto standard: X-Forwarded-Proto.
        String proxyProto = getFirstHeaderValue(request, HEADER_X_FORWARDED_PROTO);
        if (proxyProto != null) {
            builder.append(proxyProto);
            return;
        }

        // Received scheme
        String scheme = request.getScheme();
        if (HTTP.equalsIgnoreCase(scheme) && request.isSecure()) {
            // This can happen in reverse proxy mode, if the proxy server receives HTTPS requests and forwards them
            // as HTTP to the internal web server running XWiki.
            scheme = HTTPS;
        }

        builder.append(scheme != null ? scheme : HTTP);
    }

    private static void appendHostPort(HttpServletRequest request, StringBuilder builder)
    {
        // RFC 7239, section 4: Forwarded
        String forwarded = request.getHeader(HEADER_FORWARDED);
        if (StringUtils.isNotEmpty(forwarded)) {
            ForwardedHeader forwardedHeader = new ForwardedHeader(forwarded);
            if (forwardedHeader.getHost() != null) {
                builder.append(forwardedHeader.getHost());
                return;
            }
        }

        // Apache stuff, old de-facto standard: X-Forwarded-Host
        String proxyHost = getFirstHeaderValue(request, HEADER_X_FORWARDED_HOST);
        if (proxyHost != null) {
            builder.append(proxyHost);
            return;
        }

        // Ask the application server (we don't start with that because it's very often wrong or badly configured
        // behind an HTTP proxy...)
        StringBuffer buffer = request.getRequestURL();
        if (buffer != null && !buffer.isEmpty()) {
            String requestURLString = buffer.toString();
            try {
                URL requestURL = new URL(requestURLString);
                builder.append(requestURL.getHost());
                int port = requestURL.getPort();
                if (port != -1) {
                    builder.append(':');
                    builder.append(port);
                }
                return;
            } catch (MalformedURLException e) {
                LOGGER.error("The request URL indicated by the application server is wrong: {}", requestURLString, e);
            }
        }

        // Ask the application server another way (in which we cannot be sure if the port is explicitly indicated or
        // not)
        builder.append(request.getServerName());
        int port = request.getServerPort();
        if (port != -1) {
            builder.append(':');
            builder.append(port);
        }
    }

    private static String getFirstHeaderValue(HttpServletRequest request, String key)
    {
        String value = request.getHeader(key);
        if (StringUtils.isNotEmpty(value)) {
            int index = value.indexOf(',');
            if (index != -1) {
                value = value.substring(0, index);
            }
            value = value.trim();

            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }

        return null;
    }

    /**
     * @param request the servlet request input
     * @return true if the request explicitly disable getting resources from the cache
     * @since 42.0.0
     */
    @Unstable
    public static boolean isCacheReadAllowed(HttpServletRequest request)
    {
        String headerValue = request.getHeader(HEADER_CACHE_CONTROL);
        if (headerValue != null) {
            BasicHeader cacheControlHeader = new BasicHeader(HEADER_CACHE_CONTROL, headerValue);
            for (HeaderElement element : cacheControlHeader.getElements()) {
                // no-cache
                if (element.getName().equals("no-cache")) {
                    return false;
                }
            }
        }

        return true;
    }

    // Deprecated

    /**
     * Try to extract from various http headers the URL ({@code <protocol>://<host>[:<port>]/<path>[?<querystring>]}) as
     * close as possible to the one used by the client.
     * <p>
     * In theory HttpServletRequest#getRequestURL() is supposed to take care of all that but depending on the
     * application server and its configuration it's not always reliable. One less thing to configure.
     * 
     * @param servletRequest the servlet request input
     * @return the URL as close as possible from what the client used
     */
    @Deprecated(since = "42.0.0")
    public static URL getSourceURL(javax.servlet.http.HttpServletRequest servletRequest)
    {
        try {
            return getSourceURL(JakartaServletBridge.toJakarta(servletRequest));
        } catch (MalformedURLException e) {
            // Not really supposed to happen
            throw new RuntimeException("XWiki received an invalid URL path or query string", e);
        }
    }

    /**
     * Try to extract from various http headers the base URL ({@code <protocol>://<host>[:<port>]}) as close as possible
     * to the one used by the client.
     * <p>
     * In theory HttpServletRequest#getRequestURL() is supposed to take care of all that but depending on the
     * application server and its configuration it's not always reliable. One less thing to configure.
     * 
     * @param servletRequest the servlet request input
     * @return the URL as close as possible from what the client used
     */
    @Deprecated(since = "42.0.0")
    public static URL getSourceBaseURL(javax.servlet.http.HttpServletRequest servletRequest)
    {
        try {
            return getSourceBaseURL(JakartaServletBridge.toJakarta(servletRequest));
        } catch (MalformedURLException e) {
            throw new RuntimeException("XWiki received an invalid URL", e);
        }
    }

    /**
     * @param request the servlet request input
     * @return true if the request explicitly disable getting resources from the cache
     * @since 11.8RC1
     */
    @Deprecated(since = "42.0.0")
    public static boolean isCacheReadAllowed(javax.servlet.http.HttpServletRequest request)
    {
        return isCacheReadAllowed(JakartaServletBridge.toJakarta(request));
    }
}
