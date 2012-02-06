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
package org.xwiki.wysiwyg.internal.plugin.alfresco.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.wysiwyg.plugin.alfresco.server.Authenticator;

/**
 * An {@link Authenticator} implementation based on SiteMinder's {@code SMSESSION} cookie.
 * 
 * @version $Id$
 */
@Component
@Named("siteMinder")
public class SiteMinderAuthenticator implements Authenticator
{
    /**
     * The list of SiteMinder cookies that are copied from the current HTTP servlet request.
     */
    private static final List<String> SITE_MINDER_COOKIES = Arrays.asList("SMSESSION");

    /**
     * The component used to access the current HTTP request, from where we copy the SMSESSION cookie.
     */
    @Inject
    private Container container;

    @Override
    public void authenticate(HttpRequestBase request)
    {
        List<Cookie> cookies = getSiteMinderCookies();
        if (cookies.isEmpty()) {
            throw new RuntimeException("Failed to authenticate request: SiteMinder cookies are missing.");
        }
        for (Header header : new BrowserCompatSpec().formatCookies(cookies)) {
            request.addHeader(header);
        }
    }

    /**
     * @return the list of SiteMinder cookies that have to be added to the HTTP request in order to authenticate it.
     */
    private List<Cookie> getSiteMinderCookies()
    {
        javax.servlet.http.Cookie[] receivedCookies =
            ((ServletRequest) container.getRequest()).getHttpServletRequest().getCookies();
        List<Cookie> cookies = new ArrayList<Cookie>();
        // Look for the SMSESSION cookie.
        for (int i = 0; i < receivedCookies.length; i++) {
            javax.servlet.http.Cookie receivedCookie = receivedCookies[i];
            if (SITE_MINDER_COOKIES.contains(receivedCookie.getName())) {
                BasicClientCookie cookie = new BasicClientCookie(receivedCookie.getName(), receivedCookie.getValue());
                cookie.setVersion(receivedCookie.getVersion());
                cookie.setDomain(receivedCookie.getDomain());
                cookie.setPath(receivedCookie.getPath());
                cookie.setSecure(receivedCookie.getSecure());
                // Set attributes EXACTLY as sent by the browser.
                cookie.setAttribute(ClientCookie.VERSION_ATTR, String.valueOf(receivedCookie.getVersion()));
                cookie.setAttribute(ClientCookie.DOMAIN_ATTR, receivedCookie.getDomain());
                cookies.add(cookie);
            }
        }
        return cookies;
    }
}
