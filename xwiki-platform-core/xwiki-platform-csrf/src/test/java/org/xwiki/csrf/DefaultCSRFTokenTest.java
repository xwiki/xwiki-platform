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
package org.xwiki.csrf;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Random;
import java.security.SecureRandom;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.csrf.internal.DefaultCSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultCSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@ComponentTest
public class DefaultCSRFTokenTest
{
    /** URL of the current document. */
    private static final String mockDocumentUrl = "http://host/xwiki/bin/save/Main/Test";

    /** Resubmission URL. */
    private static final String resubmitUrl = mockDocumentUrl;

    /** Tested CSRF token component. */
    @InjectMockComponents
    private DefaultCSRFToken csrf;

    @MockComponent
    private DocumentAccessBridge mockDocumentAccessBridge;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    /**
     * This class is here because it doesn't require a SecureRandom generator
     * seed on each startup. Seeding a SecureRandom generator can take a very long time,
     * especially many time over which depleats the random pool on the server.
     */
    public static class InsecureCSRFToken extends DefaultCSRFToken
    {
        @Override
        public void initialize()
        {
            final Random random = new Random(System.nanoTime());
            this.setRandom(new SecureRandom() {
                private static final long serialVersionUID = 3;
                @Override
                public void nextBytes(byte[] out)
                {
                    random.nextBytes(out);
                }
            });
        }
    }

    @BeforeEach
    public void configure(MockitoComponentManager componentManager) throws Exception
    {
        // set up mocked dependencies
        final CopyStringMatcher returnValue = new CopyStringMatcher(resubmitUrl + "?", "");
        when(mockDocumentAccessBridge.getDocumentURL(any(DocumentReference.class), eq("view"), argThat(returnValue),
            anyString())).thenAnswer(returnValue);
        when(mockDocumentAccessBridge.getDocumentURL(isNull(), eq("view"), isNull(), isNull()))
            .thenReturn(mockDocumentUrl);

        // configuration
        final CSRFTokenConfiguration mockConfiguration = componentManager.getInstance(CSRFTokenConfiguration.class);
        when(mockConfiguration.isEnabled()).thenReturn(true);

        // request
        final HttpSession mockSession = mock(HttpSession.class);
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        final ServletRequest servletRequest = new ServletRequest(httpRequest);

        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(mockDocumentUrl));
        when(httpRequest.getRequestURI()).thenReturn(mockDocumentUrl);
        when(httpRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(httpRequest.getSession()).thenReturn(mockSession);
        when(mockSession.getAttribute(anyString())).thenReturn(new HashMap<>());
        // container
        final Container mockContainer = componentManager.getInstance(Container.class);
        when(mockContainer.getRequest()).thenReturn(servletRequest);
    }

    /**
     * Add a mocking role to have a logged user.
     * @throws Exception if problems occur
     */
    private void userIsLogged() throws Exception
    {
        // document access bridge
        when(mockDocumentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("mainWiki", "XWiki", "Admin"));
    }

    /**
     * Test that the secret token is a non-empty string.
     */
    @Test
    public void testToken() throws Exception
    {
        userIsLogged();

        String token = this.csrf.getToken();
        assertNotNull(token, "CSRF token is null");
        assertNotSame("", token, "CSRF token is empty string");
        assertTrue(token.length() > 20, "CSRF token is too short: \"" + token + "\"");
    }

    /**
     * Test that the secret token is a non-empty string, even for guest user.
     */
    @Test
    public void testTokenForGuestUser() throws Exception
    {
        String token = this.csrf.getToken();
        assertNotNull(token, "CSRF token is null");
        assertNotSame("", token, "CSRF token is empty string");
        assertTrue(token.length() > 20, "CSRF token is too short: \"" + token + "\"");
    }

    /**
     * Test that the same secret token is returned on subsequent calls.
     */
    @Test
    public void testTokenTwice() throws Exception
    {
        userIsLogged();

        String token1 = this.csrf.getToken();
        String token2 = this.csrf.getToken();
        assertNotNull(token1, "CSRF token is null");
        assertNotSame("", token1, "CSRF token is empty string");
        assertEquals(token1, token2, "Subsequent calls returned different tokens");
    }

    /**
     * Test that the produced valid secret token is indeed valid.
     */
    @Test
    public void testTokenValidity() throws Exception
    {
        userIsLogged();

        String token = this.csrf.getToken();
        assertTrue(this.csrf.isTokenValid(token), "Valid token did not pass the check");
    }

    /**
     * Test that null is not valid.
     */
    @Test
    public void testNullNotValid() throws Exception
    {
        userIsLogged();
        assertFalse(this.csrf.isTokenValid(null), "Null passed validity check");
        assertTrue(logCapture.getMessage(0).startsWith("CSRFToken: Secret token verification failed, token: "
            + "\"null\", stored token:"));
    }

    /**
     * Test that empty string is not valid.
     */
    @Test
    public void testEmptyNotValid() throws Exception
    {
        userIsLogged();
        assertFalse(this.csrf.isTokenValid(""), "Empty string passed validity check");
        assertTrue(logCapture.getMessage(0).startsWith("CSRFToken: Secret token verification failed, token: "
            + "\"\", stored token:"));
    }

    /**
     * Test that the prefix of the valid token is not valid.
     */
    @Test
    public void testPrefixNotValid() throws Exception
    {
        userIsLogged();

        String token = this.csrf.getToken();
        if (token != null) {
            token = token.substring(0, token.length() - 2);
        }
        assertFalse(this.csrf.isTokenValid(token), "Null passed validity check");
        assertTrue(logCapture.getMessage(0).startsWith("CSRFToken: Secret token verification failed, token: "));
    }

    /**
     * Test that the resubmission URL is correct.
     */
    @Test
    public void testResubmissionURL() throws Exception
    {
        userIsLogged();

        String url = this.csrf.getResubmissionURL();
        // srid is random, extract it from the url
        Matcher matcher = Pattern.compile(".*srid%3D([a-zA-Z0-9]+).*").matcher(url);
        String srid = matcher.matches() ? matcher.group(1) : "asdf";
        String resubmit = URLEncoder.encode(mockDocumentUrl + "?srid=" + srid, "utf-8");
        String back = URLEncoder.encode(mockDocumentUrl, "utf-8");
        String expected = resubmitUrl + "?resubmit=" + resubmit + "&xback=" + back + "&xpage=resubmit";
        assertEquals(expected, url, "Invalid resubmission URL");
    }

    /**
     * Test that the request URI is correct.
     */
    @Test
    public void testRequestURI() throws Exception
    {
        userIsLogged();

        String requestURI = this.csrf.getRequestURI();
        // srid is random, extract it from the url
        Matcher matcher = Pattern.compile(".*srid=([a-zA-Z0-9]+)$").matcher(requestURI);
        String srid = matcher.matches() ? matcher.group(1) : "asdf";
        String resubmit = mockDocumentUrl + "?srid=" + srid;
        assertEquals(resubmit, requestURI, "Invalid request URI URL");
    }

    /**
     * Tests if the token contains any special characters that have a potential to break the layout when used in places
     * where XWiki-syntax is allowed.
     */
    @Test
    public void testXWikiSyntaxCompatibility() throws Exception
    {
        userIsLogged();

        // We cannot easily control the value of the token, so we just test if it contains any "bad" characters and hope
        // for the best. Since the probability that the token contains some specific character is about 1/3, this test
        // will start to flicker (instead of always failing) if something like XWIKI-5996 is reintroduced
        for (int i = 0; i < 30; ++i) {
            this.csrf.clearToken();
            String token = this.csrf.getToken();
            assertFalse(token.matches(".*[&?*_/#^,.({\\[\\]})~!=+-].*"),
                "The token \"" + token + "\" contains a character that might break the layout");
        }
    }
}
