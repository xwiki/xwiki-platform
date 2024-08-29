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
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.url.URLSecurityManager;

public class XWikiServletResponse extends HttpServletResponseWrapper implements XWikiResponse
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiServletResponse.class);

    public XWikiServletResponse(HttpServletResponse response)
    {
        super(response);
    }

    @Override
    public HttpServletResponse getHttpServletResponse()
    {
        return (HttpServletResponse) getResponse();
    }

    @Override
    public void sendRedirect(String redirect) throws IOException
    {
        if (!StringUtils.isBlank(redirect)) {
            URI uri;
            try {
                uri = getURLSecurityManager().parseToSafeURI(redirect);
                getHttpServletResponse().sendRedirect(uri.toString());
            } catch (URISyntaxException | SecurityException e) {
                LOGGER.warn(
                    "Possible phishing attack, attempting to redirect to [{}], this request has been blocked. "
                        + "If the request was legitimate, please check the URL security configuration. You "
                        + "might need to add the domain related to this request in the list of trusted domains in "
                        + "the configuration: it can be configured in xwiki.properties in url.trustedDomains.",
                    redirect);
                LOGGER.debug("Original error preventing the redirect: ", e);
            }
        }
    }

    private URLSecurityManager getURLSecurityManager()
    {
        return Utils.getComponent(URLSecurityManager.class);
    }

    public void addCookie(String cookieName, String cookieValue, int age)
    {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setVersion(1);
        cookie.setMaxAge(age);
        getHttpServletResponse().addCookie(cookie);
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
}
