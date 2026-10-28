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
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.filters.SavedRequestManager;
import org.xwiki.csrf.internal.DefaultCSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authentication.AuthenticationConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultCSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@ComponentTest
class DefaultCSRFTokenTest
{
    /**
     * Characters that might break the layout and must thus not appear in a token.
     */
    private static final Pattern BAD_CHARACTERS = Pattern.compile("[&?*_/#^,.({\\[\\]})~!=+-]");

    /**
     * Shape of a token: 16 bytes BASE64 encoded without padding, with the URL-unsafe characters replaced by 'x'.
     */
    private static final Pattern TOKEN_SHAPE = Pattern.compile("[A-Za-z0-9x]{22}");

    /**
     * URL of the current document.
     */
    private static final String MOCK_DOCUMENT_URL = "http://host/xwiki/bin/save/Main/Test";

    /**
     * Resubmission URL.
     */
    private static final String RESUBMIT_URL = MOCK_DOCUMENT_URL;

    /**
     * The validation key shared by all the nodes of the cluster.
     */
    private static final String VALIDATION_KEY = "0123456789abcdefghijklmnopqrstuv";

    private static final String VERIFICATION_FAILED_MESSAGE =
        "Secret CSRF token verification failed for user \\[.*] \\(token: \\[.*]\\)";

    private static final DocumentReference ADMIN_REFERENCE = new DocumentReference("mainWiki", "XWiki", "Admin");

    private static final DocumentReference OTHER_USER_REFERENCE =
        new DocumentReference("mainWiki", "XWiki", "OtherUser");

    /**
     * Default token lifetime, in hours.
     */
    private static final int LIFETIME = 24;

    /**
     * A date in the middle of a time period (the periods start at midnight UTC with the default lifetime).
     */
    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    /**
     * Tested CSRF token component.
     */
    @InjectMockComponents
    private DefaultCSRFToken csrf;

    @MockComponent
    private DocumentAccessBridge mockDocumentAccessBridge;

    @MockComponent
    private AuthenticationConfiguration mockAuthenticationConfiguration;

    @MockComponent
    private EntityReferenceSerializer<String> mockSerializer;

    @MockComponent
    private CSRFTokenConfiguration mockConfiguration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);

    @BeforeEach
    void configure(MockitoComponentManager componentManager) throws Exception
    {
        // set up mocked dependencies
        final CopyStringMatcher returnValue = new CopyStringMatcher(RESUBMIT_URL + "?", "");
        when(mockDocumentAccessBridge.getDocumentURL(any(DocumentReference.class), eq("view"), argThat(returnValue),
            anyString())).thenAnswer(returnValue);
        when(mockDocumentAccessBridge.getDocumentURL(isNull(), eq("view"), isNull(), isNull()))
            .thenReturn(MOCK_DOCUMENT_URL);

        // configuration
        when(mockConfiguration.isEnabled()).thenReturn(true);
        when(mockConfiguration.getTokenLifetime()).thenReturn(LIFETIME);
        when(mockAuthenticationConfiguration.getValidationKey()).thenReturn(VALIDATION_KEY);
        setClock(this.csrf, NOW);
        when(mockSerializer.serialize(any(EntityReference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0).toString());

        // request
        final HttpSession mockSession = mock(HttpSession.class);
        final ServletRequest servletRequest = new ServletRequest(httpRequest);

        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(MOCK_DOCUMENT_URL));
        when(httpRequest.getRequestURI()).thenReturn(MOCK_DOCUMENT_URL);
        when(httpRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(httpRequest.getSession()).thenReturn(mockSession);
        when(mockSession.getAttribute(anyString())).thenReturn(new HashMap<>());
        // container
        final Container mockContainer = componentManager.getInstance(Container.class);
        when(mockContainer.getRequest()).thenReturn(servletRequest);
    }

    /**
     * Add a mocking role to have a logged user.
     */
    private void userIsLogged()
    {
        // document access bridge
        when(mockDocumentAccessBridge.getCurrentUserReference()).thenReturn(ADMIN_REFERENCE);
    }

    /**
     * Fix the clock of the given component instance.
     */
    private void setClock(CSRFToken component, Instant instant)
    {
        ReflectionUtils.setFieldValue(component, "clock", Clock.fixed(instant, ZoneOffset.UTC));
    }

    /**
     * Create a second instance of the component, with the same mocked dependencies: another node of the cluster.
     */
    private CSRFToken newInstance(MockitoComponentManager componentManager) throws Exception
    {
        // Re-registering the descriptor drops the cached singleton: the next lookup creates a new instance.
        ComponentDescriptor<CSRFToken> descriptor = componentManager.getComponentDescriptor(CSRFToken.class, "default");
        componentManager.registerComponent(descriptor);
        CSRFToken otherNode = componentManager.getInstance(CSRFToken.class);
        assertNotSame(this.csrf, otherNode);
        setClock(otherNode, NOW);
        return otherNode;
    }

    /**
     * Test that the secret token is a non-empty string.
     */
    @Test
    void getTokenIsNotEmptyWithLoggedUser()
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
    void getTokenIsNotEmptyWithGuestUser()
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
    void getTokenSameWhenCalledTwice()
    {
        userIsLogged();

        String token1 = this.csrf.getToken();
        String token2 = this.csrf.getToken();
        assertNotNull(token1, "CSRF token is null");
        assertNotSame("", token1, "CSRF token is empty string");
        assertEquals(token1, token2, "Subsequent calls returned different tokens");
    }

    /**
     * Test that the token has the expected shape: 16 bytes BASE64 encoded without padding, layout safe.
     */
    @Test
    void getTokenShape()
    {
        assertThat(this.csrf.getToken(), matchesPattern(TOKEN_SHAPE));
        userIsLogged();
        assertThat(this.csrf.getToken(), matchesPattern(TOKEN_SHAPE));
    }

    /**
     * Test that another instance of the component (another node of the cluster) sharing the same validation key
     * computes the same token for the same user, without sharing any state.
     */
    @Test
    void getTokenSameOnAnotherInstanceWithSameKey(MockitoComponentManager componentManager) throws Exception
    {
        userIsLogged();
        String token = this.csrf.getToken();

        CSRFToken otherNode = newInstance(componentManager);
        assertEquals(token, otherNode.getToken());
        assertTrue(otherNode.isTokenValid(token));
    }

    /**
     * Test that different users get different tokens.
     */
    @Test
    void getTokenDifferentForDifferentUsers()
    {
        userIsLogged();
        String adminToken = this.csrf.getToken();

        when(mockDocumentAccessBridge.getCurrentUserReference()).thenReturn(OTHER_USER_REFERENCE);
        String otherToken = this.csrf.getToken();

        assertNotEquals(adminToken, otherToken);
        assertFalse(this.csrf.isTokenValid(adminToken));
        assertThat(logCapture.getMessage(0), matchesPattern(VERIFICATION_FAILED_MESSAGE));
    }

    /**
     * Test that an instance configured with another validation key computes another token.
     */
    @Test
    void getTokenDifferentForDifferentKey(MockitoComponentManager componentManager) throws Exception
    {
        userIsLogged();
        String token = this.csrf.getToken();

        when(mockAuthenticationConfiguration.getValidationKey()).thenReturn("vutsrqponmlkjihgfedcba9876543210");
        CSRFToken otherNode = newInstance(componentManager);
        String otherToken = otherNode.getToken();

        assertNotEquals(token, otherToken);
        assertThat(otherToken, matchesPattern(TOKEN_SHAPE));
        assertFalse(otherNode.isTokenValid(token));
        assertThat(logCapture.getMessage(0), matchesPattern(VERIFICATION_FAILED_MESSAGE));
    }

    /**
     * Test that the validation key is read only once, so that an implementation returning a new value on each call
     * (like the default method of the interface) does not break the tokens.
     */
    @Test
    void getTokenReadsTheKeyOnce()
    {
        userIsLogged();
        when(mockAuthenticationConfiguration.getValidationKey()).thenReturn(VALIDATION_KEY, "anotherKey", "yetAnother");

        String token = this.csrf.getToken();
        assertEquals(token, this.csrf.getToken());
        assertTrue(this.csrf.isTokenValid(token));
        verify(mockAuthenticationConfiguration, times(1)).getValidationKey();
    }

    /**
     * Test that a blank validation key is refused.
     */
    @Test
    void getTokenWhenKeyIsBlank()
    {
        userIsLogged();
        when(mockAuthenticationConfiguration.getValidationKey()).thenReturn("  ");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> this.csrf.getToken());
        assertEquals("No authentication validation key configured, cannot derive CSRF tokens", exception.getMessage());
    }

    /**
     * Test that a missing validation key is refused.
     */
    @Test
    void getTokenWhenKeyIsNull()
    {
        when(mockAuthenticationConfiguration.getValidationKey()).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> this.csrf.getToken());
        assertEquals("No authentication validation key configured, cannot derive CSRF tokens", exception.getMessage());
    }

    /**
     * Test that the token is stable during a time period and changes with the next one.
     */
    @Test
    void getTokenChangesWithThePeriod()
    {
        userIsLogged();
        String token = this.csrf.getToken();

        setClock(this.csrf, NOW.plusSeconds(11 * 3600));
        assertEquals(token, this.csrf.getToken(), "The token changed during the period");

        setClock(this.csrf, NOW.plusSeconds(LIFETIME * 3600));
        assertNotEquals(token, this.csrf.getToken(), "The token did not change with the period");
    }

    /**
     * Test that the token of the previous period is still valid, but not the one of an older period.
     */
    @Test
    void isTokenValidWithPreviousPeriod()
    {
        userIsLogged();
        String token = this.csrf.getToken();

        setClock(this.csrf, NOW.plusSeconds(LIFETIME * 3600));
        assertTrue(this.csrf.isTokenValid(token), "The token of the previous period was rejected");
        assertTrue(this.csrf.isTokenValid(this.csrf.getToken()));

        setClock(this.csrf, NOW.plusSeconds(2 * LIFETIME * 3600));
        assertFalse(this.csrf.isTokenValid(token), "The token of an old period was accepted");
        assertThat(logCapture.getMessage(0), matchesPattern(VERIFICATION_FAILED_MESSAGE));
    }

    /**
     * Test that the token never changes when the lifetime is 0.
     */
    @Test
    void getTokenWhenNoLifetime()
    {
        userIsLogged();
        when(mockConfiguration.getTokenLifetime()).thenReturn(0);
        String token = this.csrf.getToken();

        setClock(this.csrf, NOW.plusSeconds(365 * 24 * 3600));
        assertEquals(token, this.csrf.getToken());
        assertTrue(this.csrf.isTokenValid(token));
        assertThat(token, matchesPattern(TOKEN_SHAPE));
    }

    /**
     * Test that the guest token is stable and different from the token of a user.
     */
    @Test
    void getTokenForGuest()
    {
        String guestToken = this.csrf.getToken();
        assertEquals(guestToken, this.csrf.getToken());
        assertTrue(this.csrf.isTokenValid(guestToken));

        userIsLogged();
        assertNotEquals(guestToken, this.csrf.getToken());
    }

    /**
     * Test that clearing the token does not change it, since it is derived and not stored.
     */
    @Test
    void clearTokenLeavesTheTokenUnchanged()
    {
        userIsLogged();
        String token = this.csrf.getToken();

        this.csrf.clearToken();

        assertEquals(token, this.csrf.getToken());
        assertTrue(this.csrf.isTokenValid(token));
    }

    /**
     * Test that the produced valid secret token is indeed valid.
     */
    @Test
    void getTokenIsValid()
    {
        userIsLogged();

        String token = this.csrf.getToken();
        assertTrue(this.csrf.isTokenValid(token), "Valid token did not pass the check");
    }

    /**
     * Test that null is not valid.
     */
    @Test
    void isTokenValidWhenNull()
    {
        userIsLogged();
        assertFalse(this.csrf.isTokenValid(null), "Null passed validity check");
        assertThat(logCapture.getMessage(0), matchesPattern(VERIFICATION_FAILED_MESSAGE));
    }

    /**
     * Test that empty string is not valid.
     */
    @Test
    void isTokenValidWhenEmpty()
    {
        userIsLogged();
        assertFalse(this.csrf.isTokenValid(""), "Empty string passed validity check");
        assertThat(logCapture.getMessage(0), matchesPattern(VERIFICATION_FAILED_MESSAGE));
    }

    /**
     * Test that the prefix of the valid token is not valid.
     */
    @Test
    void prefixNotValid()
    {
        userIsLogged();

        String token = this.csrf.getToken();
        if (token != null) {
            token = token.substring(0, token.length() - 2);
        }
        assertFalse(this.csrf.isTokenValid(token), "Null passed validity check");
        assertTrue(logCapture.getMessage(0).startsWith("Secret CSRF token verification failed"));
    }

    /**
     * Test that any token is accepted when the CSRF protection is disabled.
     */
    @Test
    void isTokenValidWhenDisabled()
    {
        userIsLogged();
        when(mockConfiguration.isEnabled()).thenReturn(false);

        assertTrue(this.csrf.isTokenValid(this.csrf.getToken()));
        assertTrue(this.csrf.isTokenValid("wrong"));
        assertTrue(this.csrf.isTokenValid(null));
    }

    /**
     * Test that the resubmission URL is correct.
     */
    @Test
    void resubmissionURLWhenPost()
    {
        userIsLogged();

        when(this.httpRequest.getMethod()).thenReturn("POST");
        String url = this.csrf.getResubmissionURL();
        // srid is random, extract it from the url
        Matcher matcher = Pattern.compile(".*srid%3D([a-zA-Z0-9]+).*").matcher(url);
        String srid = matcher.matches() ? matcher.group(1) : "asdf";
        String resubmit = URLEncoder.encode(MOCK_DOCUMENT_URL + "?srid=" + srid, StandardCharsets.UTF_8);
        String back = URLEncoder.encode(MOCK_DOCUMENT_URL, StandardCharsets.UTF_8);
        String expected = RESUBMIT_URL + "?xpage=resubmit&xback=" + back + "&resubmit=" + resubmit + "&sridKey=" + srid;
        assertEquals(expected, url, "Invalid resubmission URL");
    }

    /**
     * Test that the resubmission URL is correct.
     */
    @Test
    void resubmissionURLWhenGet()
    {
        userIsLogged();

        when(this.httpRequest.getMethod()).thenReturn("GET");
        String url = this.csrf.getResubmissionURL();
        String expected = RESUBMIT_URL + "?xpage=resubmit";
        assertEquals(expected, url, "Invalid resubmission URL");
    }

    @Test
    void isResubmitAllowedForCurrentRequest()
    {
        userIsLogged();
        when(this.httpRequest.getMethod()).thenReturn("GET");
        assertFalse(this.csrf.isResubmitAllowedForCurrentRequest());
        when(this.httpRequest.getMethod()).thenReturn("POST");
        assertTrue(this.csrf.isResubmitAllowedForCurrentRequest());
    }

    @Test
    void isResubmitAllowedForRequestId()
    {
        assertFalse(this.csrf.isResubmitAllowedForRequestId(""));
        HttpSession httpSession = mock(HttpSession.class);
        when(this.httpRequest.getSession()).thenReturn(httpSession);
        assertFalse(this.csrf.isResubmitAllowedForRequestId("test"));
        when(httpSession.getAttribute(SavedRequestManager.getSavedRequestKey())).thenReturn(Map.of());
        assertFalse(this.csrf.isResubmitAllowedForRequestId("test"));
        when(httpSession.getAttribute(SavedRequestManager.getSavedRequestKey()))
            .thenReturn(Map.of("test", mock(SavedRequestManager.SavedRequest.class)));
        assertTrue(this.csrf.isResubmitAllowedForRequestId("test"));
    }

    /**
     * Test that the request URI is correct.
     */
    @Test
    void requestURIWhenPost()
    {
        userIsLogged();
        when(this.httpRequest.getMethod()).thenReturn("POST");
        String requestURI = this.csrf.getRequestURI();
        // srid is random, extract it from the url
        Matcher matcher = Pattern.compile(".*srid=([a-zA-Z0-9]+)$").matcher(requestURI);
        String srid = matcher.matches() ? matcher.group(1) : "asdf";
        String resubmit = MOCK_DOCUMENT_URL + "?srid=" + srid;
        assertEquals(resubmit, requestURI, "Invalid request URI URL");
    }

    @Test
    void requestURIWhenGet()
    {
        userIsLogged();
        assertNull(this.csrf.getRequestURI());
    }

    /**
     * Tests if the token contains any special characters that have a potential to break the layout when used in places
     * where XWiki-syntax is allowed.
     */
    @Test
    void xWikiSyntaxCompatibility()
    {
        // The token is derived from the user and the period, so vary both to cover many different tokens.
        for (int i = 0; i < 30; ++i) {
            when(mockDocumentAccessBridge.getCurrentUserReference())
                .thenReturn(new DocumentReference("mainWiki", "XWiki", "User" + i));
            setClock(this.csrf, NOW.plusSeconds((long) i * LIFETIME * 3600));
            String token = this.csrf.getToken();
            assertFalse(BAD_CHARACTERS.matcher(token).find(),
                "The token \"" + token + "\" contains a character that might break the layout");
        }
    }
}
