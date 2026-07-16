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

import java.util.List;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jakarta.servlet.http.Cookie;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.securityfilter.authenticator.persistent.PersistentLoginManagerInterface;
import org.xwiki.export.pdf.internal.browser.CookieFilter.CookieFilterContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.impl.xwiki.MyPersistentLoginManager;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthenticationCookieFilter}.
 * 
 * @version $Id$
 */
@ComponentTest
class AuthenticationCookieFilterTest
{
    private static final String VALIDATION_KEY = "12345678901234567890123456789012";

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

    private MyPersistentLoginManager loginManager;

    @BeforeEach
    void configure()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getRequest()).thenReturn(this.httpRequest);
        when(this.xcontext.getResponse()).thenReturn(this.httpResponse);
        this.loginManager = spy(new MyPersistentLoginManager());
        this.loginManager.setValidationKey(VALIDATION_KEY);
        this.loginManager.setEncryptionKey("1234567890123456");

        when(this.loginManagerProvider.get()).thenReturn(this.loginManager);
    }

    @Test
    void isFilterRequired() throws Exception
    {
        assertFalse(this.authCookieFilter.isFilterRequired());

        when(this.loginManager.getRememberedUsername(this.httpRequest, this.httpResponse)).thenReturn("alice");
        assertFalse(this.authCookieFilter.isFilterRequired());

        when(this.loginManager.getRememberedPassword(this.httpRequest, this.httpResponse)).thenReturn("wonderland");
        assertTrue(this.authCookieFilter.isFilterRequired());
    }

    @Test
    void filter() throws Exception
    {
        String ip = "172.17.0.3";
        when(this.cookieFilterContext.getClientIPAddress()).thenReturn(ip);
        Cookie usernameCookie = new Cookie("username", "usernameBefore");
        Cookie passwordCookie = new Cookie("password", "passwordBefore");
        Cookie validationCookie = new Cookie("validation", "validationBefore");

        String username = "alice";
        when(this.loginManager.getRememberedUsername(this.httpRequest, this.httpResponse)).thenReturn(username);
        String password = "wonderland";
        when(this.loginManager.getRememberedPassword(this.httpRequest, this.httpResponse)).thenReturn(password);

        this.authCookieFilter.filter(List.of(usernameCookie, passwordCookie, validationCookie),
            this.cookieFilterContext);

        String expectedEncryptedUsername = this.loginManager.encryptText(username);
        assertEquals(expectedEncryptedUsername, usernameCookie.getValue());
        String expectedEncryptedPassword = this.loginManager.encryptText(password);
        assertEquals(expectedEncryptedPassword, passwordCookie.getValue());

        String expectedValidation = DigestUtils.md5Hex(
            String.join(":", expectedEncryptedUsername, expectedEncryptedPassword, ip, VALIDATION_KEY));

        // This test will fail when the computation of the validation cookie changes in the persistent login manager
        // but this may be a good thing as it will force us to double check that the authentication cookie
        // filtering is still correct.
        assertEquals(expectedValidation, validationCookie.getValue());

        // Verify that the response isn't modified, e.g., by setting cookies (which should be intercepted by the
        // cookie filter).
        verifyNoInteractions(this.httpResponse);
    }

    /**
     * Test that cookies are also handled when set via response headers. This doesn't correspond to the current
     * implementation anymore but as the persistent login manager might be replaced by a custom implementation that
     * uses the previous approach it is safer to still support it and this test verifies that it actually works.
     */
    @Test
    void filterAddHeader() throws Exception
    {
        when(this.cookieFilterContext.getClientIPAddress()).thenReturn("172.17.0.3");
        Cookie cookie = new Cookie("test", "before");

        when(this.loginManager.getRememberedUsername(this.httpRequest, this.httpResponse)).thenReturn("alice");
        when(this.loginManager.getRememberedPassword(this.httpRequest, this.httpResponse)).thenReturn("wonderland");

        // Mock the previous behavior of the persistent login manager which was setting cookies via response headers
        // directly instead of using the servlet API.
        doAnswer(invocation -> {
            String ip = ((HttpServletRequest) invocation.getArgument(0)).getHeader("X-Forwarded-For");
            ((HttpServletResponse) invocation.getArgument(1)).addHeader("Set-Cookie",
                String.format("test=value_bound_to_%s; Secure; HttpOnly", ip));
            return null;
        }).when(this.loginManager).rememberLogin(any(HttpServletRequest.class), any(HttpServletResponse.class),
            eq("alice"), eq("wonderland"));

        this.authCookieFilter.filter(List.of(cookie), this.cookieFilterContext);

        assertEquals("value_bound_to_172.17.0.3", cookie.getValue());

        // Verify that the response isn't modified, e.g., by setting cookies (which should be intercepted by the
        // cookie filter).
        verifyNoInteractions(this.httpResponse);
    }

    @Test
    void filterWithoutAuthenticationCookies() throws Exception
    {
        when(this.cookieFilterContext.getClientIPAddress()).thenReturn("127.0.0.1");
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
