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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Provider;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.securityfilter.authenticator.persistent.PersistentLoginManagerInterface;
import org.xwiki.export.pdf.internal.browser.CookieFilter.CookieFilterContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Unit tests for {@link AuthenticationCookieFilter}.
 * 
 * @version $Id$
 */
@ComponentTest
class AuthenticationCookieFilterTest
{
    @InjectMockComponents
    private AuthenticationCookieFilter authCookieFilter;

    @MockComponent
    private Provider<PersistentLoginManagerInterface> loginManagerProvider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWikiRequest httpRequest;

    @Mock
    private XWikiResponse httpResponse;

    @Mock
    private CookieFilterContext cookieFilterContext;

    @Mock
    private PersistentLoginManagerInterface loginManager;

    @BeforeEach
    void configure()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getRequest()).thenReturn(this.httpRequest);
        when(this.xcontext.getResponse()).thenReturn(this.httpResponse);

        when(this.loginManagerProvider.get()).thenReturn(this.loginManager);
    }

    @Test
    void filter() throws Exception
    {
        when(this.cookieFilterContext.getBrowserIPAddress()).thenReturn("172.17.0.3");
        Cookie cookie = new Cookie("test", "before");

        when(this.loginManager.getRememberedUsername(this.httpRequest, this.httpResponse)).thenReturn("alice");
        when(this.loginManager.getRememberedPassword(this.httpRequest, this.httpResponse)).thenReturn("wonderland");

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                String ip = ((HttpServletRequest) invocation.getArgument(0)).getHeader("X-Forwarded-For");
                ((HttpServletResponse) invocation.getArgument(1)).addHeader("Set-Cookie",
                    String.format("test=value_bound_to_%s; Secure; HttpOnly", ip));
                return null;
            }
        }).when(this.loginManager).rememberLogin(any(HttpServletRequest.class), any(HttpServletResponse.class),
            eq("alice"), eq("wonderland"));

        this.authCookieFilter.filter(List.of(cookie), this.cookieFilterContext);

        assertEquals("value_bound_to_172.17.0.3", cookie.getValue());
    }

    @Test
    void filterWithoutAuthenticationCookies() throws Exception
    {
        Cookie cookie = new Cookie("test", "before");

        this.authCookieFilter.filter(List.of(cookie), this.cookieFilterContext);

        assertEquals("before", cookie.getValue());
        verify(this.loginManager, never()).rememberLogin(any(), any(), any(), any());

        when(this.loginManager.getRememberedUsername(this.httpRequest, this.httpResponse)).thenReturn("alice");

        this.authCookieFilter.filter(List.of(cookie), this.cookieFilterContext);

        assertEquals("before", cookie.getValue());
        verify(this.loginManager, never()).rememberLogin(any(), any(), any(), any());

        when(this.loginManager.getRememberedPassword(this.httpRequest, this.httpResponse)).thenReturn("wonderland");

        this.authCookieFilter.filter(List.of(cookie), this.cookieFilterContext);

        verify(this.loginManager).rememberLogin(any(HttpServletRequest.class), any(HttpServletResponse.class),
            eq("alice"), eq("wonderland"));
    }
}
