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
package org.xwiki.security.authentication.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.inject.Named;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.AuthenticationConfiguration;
import org.xwiki.security.authentication.AuthenticationFailureEvent;
import org.xwiki.security.authentication.AuthenticationFailureLimitReachedEvent;
import org.xwiki.security.authentication.AuthenticationFailureStrategy;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link DefaultAuthenticationFailureManager}.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@ComponentTest
public class DefaultAuthenticationFailureManagerTest
{
    @InjectMockComponents
    private DefaultAuthenticationFailureManager defaultAuthenticationFailureManager;

    @MockComponent
    @Named("strategy1")
    private AuthenticationFailureStrategy strategy1;

    @MockComponent
    @Named("strategy2")
    private AuthenticationFailureStrategy strategy2;

    @MockComponent
    private AuthenticationConfiguration configuration;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private CacheManager cacheManager;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument userDocument;

    @Mock
    private XWiki xWiki;

    private Cache<Instant> sessionFailing;

    private String failingLogin = "foobar";

    private DocumentReference userFailingDocumentReference = new DocumentReference("xwiki", "XWiki", failingLogin);

    @BeforeComponent
    public void configure(MockitoComponentManager componentManager) throws Exception
    {
        Utils.setComponentManager(componentManager);
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);

        sessionFailing = mock(Cache.class);
        when(cacheManager.createNewCache(any())).then(invocationOnMock -> {
            CacheConfiguration cacheConfiguration = invocationOnMock.getArgument(0);
            assertEquals("xwiki.security.authentication.failingSession.cache", cacheConfiguration.getConfigurationId());
            return sessionFailing;
        });
    }

    @BeforeEach
    public void setup() throws Exception
    {
        when(configuration.getFailureStrategies()).thenReturn(new String[] { "strategy1", "strategy2" });
        when(configuration.getMaxAuthorizedAttempts()).thenReturn(3);
        when(configuration.getTimeWindow()).thenReturn(5);
        when(configuration.isAuthenticationSecurityEnabled()).thenReturn(true);
        when(contextProvider.get()).thenReturn(context);
        when(context.getMainXWiki()).thenReturn("xwiki");
        when(context.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(any(DocumentReference.class), eq(context))).thenReturn(userDocument);
    }

    private HttpServletRequest getRequest(String sessionId)
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn(sessionId);

        return request;
    }

    /**
     * Ensure that a AuthenticationFailureEvent is triggered.
     */
    @Test
    public void authenticationFailureIsTriggered()
    {
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin,
            getRequest("something")));
        verify(this.observationManager).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.sessionFailing).get("something");
    }

    /**
     * Ensure that the limit threshold is working properly and the rights events are triggered.
     */
    @Test
    void authenticationFailureLimitReached()
    {
        HttpServletRequest request = getRequest("customId");
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(4)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(2)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(2)).notify(failingLogin);
        verify(this.strategy2, times(2)).notify(failingLogin);
        verify(this.sessionFailing, times(2)).set(eq("customId"), any(Instant.class));
        verify(this.sessionFailing, times(4)).get("customId");
    }

    /**
     * Ensure that the time window accepts big values (detect possible int/long problems)
     */
    @Test
    void authenticationFailureLimitReachedBigTimeWindow()
    {
        when(this.configuration.getTimeWindow()).thenReturn(2160000); // 25 days
        HttpServletRequest request = getRequest("customId");
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(4)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(2)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(2)).notify(failingLogin);
        verify(this.strategy2, times(2)).notify(failingLogin);
        verify(this.sessionFailing, times(2)).set(eq("customId"), any(Instant.class));
        verify(this.sessionFailing, times(4)).get("customId");
    }

    @Test
    void authenticationFailureEmptyLogin()
    {
        HttpServletRequest request = getRequest("customId2");
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure("", request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(null, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure("", request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(null, request));

        verify(this.observationManager, times(2)).notify(new AuthenticationFailureEvent(), "");
        verify(this.observationManager, times(2)).notify(new AuthenticationFailureEvent(), null);
        verify(this.observationManager, never()).notify(eq(new AuthenticationFailureLimitReachedEvent()), any());
        verify(this.strategy1, never()).notify(any());
        verify(this.strategy2, never()).notify(any());
    }

    /**
     * Ensure that the time window configuration is taken into account properly.
     */
    @Test
    public void repeatedAuthenticationFailureOutOfTimeWindow() throws InterruptedException
    {
        HttpServletRequest request = getRequest("anotherId");
        when(configuration.getTimeWindow()).thenReturn(1);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        Thread.sleep(1500);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
        verify(this.sessionFailing).set(eq("anotherId"), any(Instant.class));
        verify(this.sessionFailing, times(5)).get("anotherId");
    }

    /**
     * Ensure that the max attempt configuration is taken into account properly.
     */
    @Test
    public void repeatedAuthenticationFailureDifferentThreshold()
    {
        HttpServletRequest request = getRequest("foobar");
        when(configuration.getMaxAuthorizedAttempts()).thenReturn(5);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
        verify(this.sessionFailing).set(eq("foobar"), any(Instant.class));
        verify(this.sessionFailing, times(5)).get("foobar");
    }

    /**
     * Ensure that the failure record reset is working properly.
     */
    @Test
    public void resetAuthFailureRecord()
    {
        HttpServletRequest request = getRequest("reset");
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        this.defaultAuthenticationFailureManager.resetAuthenticationFailureCounter(this.failingLogin);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
        verify(this.sessionFailing).set(eq("reset"), any(Instant.class));
        verify(this.sessionFailing, times(5)).get("reset");
    }

    /**
     * Ensure that the failure record reset is working properly.
     */
    @Test
    public void resetAuthFailureRecordWithDocumentReference()
    {
        HttpServletRequest request = getRequest("reset2");
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        this.defaultAuthenticationFailureManager.resetAuthenticationFailureCounter(this.userFailingDocumentReference);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
        verify(this.sessionFailing).set(eq("reset2"), any(Instant.class));
        verify(this.sessionFailing, times(5)).get("reset2");
    }

    /**
     * Ensure that the threshold mechanism works properly with different login.
     */
    @Test
    public void recordAuthFailureDifferentLogin()
    {
        HttpServletRequest request = getRequest("multilogin");
        String login1 = this.failingLogin.toLowerCase();
        String login2 = this.failingLogin.toUpperCase();
        String login3 = "barfoo";

        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login3, request));

        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2, request));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login3, request));

        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2, request));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login3, request));

        verify(this.observationManager, times(3)).notify(new AuthenticationFailureEvent(), login1);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            login1);
        verify(this.strategy1, times(1)).notify(login1);
        verify(this.strategy2, times(1)).notify(login1);

        verify(this.observationManager, times(3)).notify(new AuthenticationFailureEvent(), login2);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            login2);
        verify(this.strategy1, times(1)).notify(login2);
        verify(this.strategy2, times(1)).notify(login2);

        verify(this.observationManager, times(3)).notify(new AuthenticationFailureEvent(), login3);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            login3);
        verify(this.strategy1, times(1)).notify(login3);
        verify(this.strategy2, times(1)).notify(login3);
        verify(this.sessionFailing, times(3)).set(eq("multilogin"), any(Instant.class));
        verify(this.sessionFailing, times(9)).get("multilogin");
    }

    @Test
    void recordAuthenticationFailureWithFailingSession()
    {
        HttpServletRequest request = getRequest("failingSession1");

        when(this.sessionFailing.get("failingSession1")).thenReturn(new Date().toInstant());
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        when(this.sessionFailing.get("failingSession1")).thenReturn(new Date().toInstant().plus(1, ChronoUnit.DAYS));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request));

        verify(this.observationManager, times(2)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1).notify(failingLogin);
        verify(this.strategy2).notify(failingLogin);
        verify(this.sessionFailing).set(eq("failingSession1"), any(Instant.class));
    }

    /**
     * Ensure that the authentication threshold auth is deactivated if max attempt is set to 0
     */
    @Test
    public void deactivateThresholdAuthWithMaxAttempt()
    {
        HttpServletRequest request = getRequest("manyattempt");
        when(this.configuration.getMaxAuthorizedAttempts()).thenReturn(0);

        for (int i = 0; i < 100; i++) {
            assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin,
                request));
        }
        verify(this.observationManager, times(100)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, never()).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.sessionFailing, never()).set(eq("manyattempt"), any(Instant.class));
        verify(this.sessionFailing, never()).get("manyattempt");
    }

    /**
     * Ensure that the authentication threshold auth is deactivated if time window is set to 0
     */
    @Test
    public void deactivateThresholdAuthWithTimeWindow()
    {
        HttpServletRequest request = getRequest("manyattempt2");
        when(this.configuration.getTimeWindow()).thenReturn(0);

        for (int i = 0; i < 100; i++) {
            assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin,
                request));
        }
        verify(this.observationManager, times(100)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, never()).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.sessionFailing, never()).set(eq("manyattempt2"), any(Instant.class));
        verify(this.sessionFailing, never()).get("manyattempt2");
    }

    /**
     * Validate that getForm is working properly.
     */
    @Test
    public void getForm()
    {
        HttpServletRequest request = getRequest("getForm");
        String formStrategy1 = "formStrategy1";
        String formStrategy2 = "formStrategy2";
        when(this.strategy1.getForm(eq(this.failingLogin))).thenReturn(formStrategy1);
        when(this.strategy2.getForm(eq(this.failingLogin))).thenReturn(formStrategy2);

        assertEquals("", this.defaultAuthenticationFailureManager.getForm(this.failingLogin, request));

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request);
        assertEquals(String.format("%s\n%s\n", formStrategy1, formStrategy2),
            this.defaultAuthenticationFailureManager.getForm(this.failingLogin, request));
    }

    @Test
    void getFormFailingSession()
    {
        HttpServletRequest request = getRequest("failingSession2");
        String formStrategy1 = "formStrategy1";
        String formStrategy2 = "formStrategy2";
        when(this.strategy1.getForm(eq(this.failingLogin))).thenReturn(formStrategy1);
        when(this.strategy2.getForm(eq(this.failingLogin))).thenReturn(formStrategy2);

        when(this.sessionFailing.get("failingSession2")).thenReturn(new Date().toInstant());
        assertEquals("", this.defaultAuthenticationFailureManager.getForm(this.failingLogin, request));

        when(this.sessionFailing.get("failingSession2")).thenReturn(new Date().toInstant().plus(1, ChronoUnit.DAYS));
        assertEquals(String.format("%s\n%s\n", formStrategy1, formStrategy2),
            this.defaultAuthenticationFailureManager.getForm(this.failingLogin, request));
    }

    /**
     * Validate that getErrorMessages is working properly.
     */
    @Test
    public void getErrorMessages()
    {
        HttpServletRequest request = getRequest("errorMsg");
        String errorMessage1 = "errorMessage1";
        String errorMessage2 = "errorMessage2";
        when(this.strategy1.getErrorMessage(eq(this.failingLogin))).thenReturn(errorMessage1);
        when(this.strategy2.getErrorMessage(eq(this.failingLogin))).thenReturn(errorMessage2);

        assertEquals("", this.defaultAuthenticationFailureManager.getErrorMessage(this.failingLogin));

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin, request);
        assertEquals(String.format("%s\n%s\n", errorMessage1, errorMessage2),
            this.defaultAuthenticationFailureManager.getErrorMessage(this.failingLogin));
    }

    /**
     * Validate that getForm is working properly.
     */
    @Test
    public void validateForm()
    {
        HttpServletRequest request = getRequest("validate");
        String login1 = this.failingLogin;
        String login2 = "barfoo";

        assertTrue(this.defaultAuthenticationFailureManager.validateForm(login1, request));
        assertTrue(this.defaultAuthenticationFailureManager.validateForm(login2, request));

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1, request);

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2, request);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2, request);

        when(this.strategy1.validateForm(login1, null)).thenReturn(true);
        when(this.strategy2.validateForm(login1, null)).thenReturn(true);
        assertTrue(this.defaultAuthenticationFailureManager.validateForm(login1, null));

        when(this.strategy1.validateForm(login2, null)).thenReturn(true);
        when(this.strategy2.validateForm(login2, null)).thenReturn(false);
        assertFalse(this.defaultAuthenticationFailureManager.validateForm(login2, null));
    }

    @Test
    void validateFormFailingSession()
    {
        HttpServletRequest request = getRequest("failingSession3");

        when(this.sessionFailing.get("failingSession3")).thenReturn(new Date().toInstant());
        assertTrue(this.defaultAuthenticationFailureManager.validateForm(this.failingLogin, request));

        when(this.sessionFailing.get("failingSession3")).thenReturn(new Date().toInstant().plus(1, ChronoUnit.DAYS));
        assertFalse(this.defaultAuthenticationFailureManager.validateForm(this.failingLogin, request));
    }

    /**
     * Validate that getUser is working properly.
     */
    @Test
    public void getUserNotFound() throws XWikiException
    {
        when(context.getMainXWiki()).thenReturn("mainwiki");
        when(context.getWikiId()).thenReturn("currentwiki");
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);

        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(any(DocumentReference.class), eq(context))).thenReturn(xWikiDocument);
        when(xWikiDocument.isNew()).thenReturn(true);
        DocumentReference userReference = this.defaultAuthenticationFailureManager.findUser("foo");
        assertNull(userReference);
        DocumentReference globalReference = new DocumentReference("mainwiki", "XWiki", "foo");
        DocumentReference localReference = new DocumentReference("currentwiki", "XWiki", "foo");
        verify(xwiki, times(1)).getDocument(eq(globalReference), eq(context));
        verify(xwiki, times(1)).getDocument(eq(localReference), eq(context));
    }

    /**
     * Validate that getUser is working properly.
     */
    @Test
    public void getUserGlobalFound() throws XWikiException
    {
        when(context.getMainXWiki()).thenReturn("mainwiki");
        DocumentReference globalReference = new DocumentReference("mainwiki", "XWiki", "foo");
        DocumentReference localReference = new DocumentReference("currentwiki", "XWiki", "foo");
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);

        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(eq(globalReference), eq(context))).thenReturn(xWikiDocument);
        when(xWikiDocument.isNew()).thenReturn(false);
        DocumentReference userReference = this.defaultAuthenticationFailureManager.findUser("foo");
        assertEquals(globalReference, userReference);

        verify(xwiki, times(1)).getDocument(eq(globalReference), eq(context));
        verify(xwiki, never()).getDocument(eq(localReference), eq(context));
    }

    /**
     * Validate that getUser is working properly.
     */
    @Test
    public void getUserLocalFound() throws XWikiException
    {
        when(context.getMainXWiki()).thenReturn("mainwiki");
        when(context.getWikiId()).thenReturn("currentwiki");
        DocumentReference globalReference = new DocumentReference("mainwiki", "XWiki", "foo");
        DocumentReference localReference = new DocumentReference("currentwiki", "XWiki", "foo");
        XWiki xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        XWikiDocument xWikiLocalDocument = mock(XWikiDocument.class);
        XWikiDocument xWikiGlobalDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(globalReference), eq(context))).thenReturn(xWikiGlobalDocument);
        when(xwiki.getDocument(eq(localReference), eq(context))).thenReturn(xWikiLocalDocument);
        when(xWikiGlobalDocument.isNew()).thenReturn(true);
        when(xWikiLocalDocument.isNew()).thenReturn(false);
        DocumentReference userReference = this.defaultAuthenticationFailureManager.findUser("foo");
        assertEquals(localReference, userReference);

        verify(xwiki, times(1)).getDocument(eq(globalReference), eq(context));
        verify(xwiki, times(1)).getDocument(eq(localReference), eq(context));
    }

    @Test
    public void strategiesAreRebuildInCaseOfReset()
    {
        HttpServletRequest request = getRequest("reset");
        when(configuration.getFailureStrategies()).thenReturn(new String[] { "strategy1" });
        when(configuration.getMaxAuthorizedAttempts()).thenReturn(1);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure("foo", request);
        verify(configuration, times(3)).getFailureStrategies();
        verify(strategy1, times(1)).notify("foo");
        verify(strategy2, never()).notify(any());

        // we change the configuration strategy, but we don't reset the list
        when(configuration.getFailureStrategies()).thenReturn(new String[] { "strategy2" });
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure("foo", request);

        // the list is already existing, we still call the old strategy
        verify(configuration, times(6)).getFailureStrategies();
        verify(strategy1, times(1)).notify("foo");
        verify(strategy2, times(1)).notify(any());
    }

    @Test
    void sessionFailingIsClearedWhenSecurityIsDisabled()
    {
        HttpServletRequest request = getRequest("clear");
        when(configuration.isAuthenticationSecurityEnabled()).thenReturn(false);
        this.defaultAuthenticationFailureManager.getForm("foo", request);
        verify(sessionFailing).removeAll();

    }
}
