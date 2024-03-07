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
package org.xwiki.export.pdf.internal.browser;

import java.net.HttpCookie;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.securityfilter.authenticator.persistent.PersistentLoginManagerInterface;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;

/**
 * Ensures that the XWiki authentication cookies copied from the HTTP request that triggered the PDF export work when
 * passed to the web browser used for PDF printing (especially when those cookies are bound to the user IP address which
 * is different than the IP address of the web browser used for PDF printing).
 * <p>
 * Note that instead of modifying the cookies, we could have simply set the {@code X-Forwarded-For} HTTP header, when
 * requesting the print preview page, to the IP address of the user agent that triggered the PDF export
 * <strong>BUT</strong> we would have depended on a very permissive {@code PersistentLoginManagerInterface}
 * implementation that always uses the first header value. The <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For#selecting_an_ip_address">recommended
 * practice</a> is to take into account the (configurable) number of known and trustworthy (reverse / forward) proxies,
 * in which case our value must be ignored (e.g. if there are no known proxies then the persistent login manager
 * should't use the {@code X-Forwarded-For} header at all).
 * 
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
public class AuthenticationCookieFilter implements CookieFilter
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<PersistentLoginManagerInterface> persistentLoginManagerProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void filter(List<Cookie> cookies, CookieFilterContext cookieFilterContext)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            PersistentLoginManagerInterface loginManager = this.persistentLoginManagerProvider.get();
            String userName = loginManager.getRememberedUsername(xcontext.getRequest(), xcontext.getResponse());
            String password = loginManager.getRememberedPassword(xcontext.getRequest(), xcontext.getResponse());
            if (userName == null || password == null) {
                // No need to update the cookies if the user is not logged in or if the authentication cookies are not
                // valid.
                return;
            }
            HttpServletRequest fakeRequest = new HttpServletRequestWrapper(xcontext.getRequest())
            {
                @Override
                public String getHeader(String name)
                {
                    if ("X-Forwarded-For".equals(name)) {
                        return cookieFilterContext.getBrowserIPAddress();
                    } else {
                        return super.getHeader(name);
                    }
                }

                @Override
                public String getRemoteAddr()
                {
                    return cookieFilterContext.getBrowserIPAddress();
                }
            };
            HttpServletResponse fakeResponse = new HttpServletResponseWrapper(xcontext.getResponse())
            {
                @Override
                public void addHeader(String name, String value)
                {
                    if ("Set-Cookie".equals(name)) {
                        HttpCookie.parse(value)
                            .forEach(cookie -> setCookie(cookies, cookie.getName(), cookie.getValue()));
                    }
                }
            };
            loginManager.rememberLogin(fakeRequest, fakeResponse, userName, password);
        } catch (Exception e) {
            this.logger.warn("Failed to update the XWiki authentication cookies. Root cause is [{}].",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void setCookie(List<Cookie> cookies, String name, String value)
    {
        cookies.stream().filter(cookie -> Objects.equals(cookie.getName(), name))
            .forEach(cookie -> cookie.setValue(value));
    }
}
