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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.api.AuthenticationConfiguration;
import org.xwiki.security.authentication.api.AuthenticationFailureEvent;
import org.xwiki.security.authentication.api.AuthenticationFailureLimitReachedEvent;
import org.xwiki.security.authentication.api.AuthenticationFailureStrategy;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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

    private String failingLogin = "foobar";

    @BeforeComponent
    public void configure(MockitoComponentManager componentManager) throws Exception
    {
       componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    public void setup() throws Exception
    {
        when(configuration.getFailureStrategies()).thenReturn(new String[] { "strategy1", "strategy2" });
        when(configuration.getMaxAuthorizedAttempts()).thenReturn(3);
        when(configuration.getTimeWindow()).thenReturn(5);
    }

    /**
     * Ensure that a AuthenticationFailureEvent is triggered.
     */
    @Test
    public void authenticationFailureIsTriggered()
    {
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureEvent(), this.failingLogin);
    }

    /**
     * Ensure that the limit threshold is working properly and the rights events are triggered.
     */
    @Test
    public void authenticationFailureLimitReached()
    {
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));

        verify(this.observationManager, times(4)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(2)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(2)).notify(failingLogin);
        verify(this.strategy2, times(2)).notify(failingLogin);
    }

    /**
     * Ensure that the time window configuration is taken into account properly.
     */
    @Test
    public void repeatedAuthenticationFailureOutOfTimeWindow() throws InterruptedException
    {
        when(configuration.getTimeWindow()).thenReturn(1);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        Thread.sleep(1500);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
    }

    /**
     * Ensure that the max attempt configuration is taken into account properly.
     */
    @Test
    public void repeatedAuthenticationFailureDifferentThreshold()
    {
        when(configuration.getMaxAuthorizedAttempts()).thenReturn(5);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
    }

    /**
     * Ensure that the failure record reset is working properly.
     */
    @Test
    public void resetAuthFailureRecord()
    {
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        this.defaultAuthenticationFailureManager.resetAuthenticationFailureCounter(this.failingLogin);
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));

        verify(this.observationManager, times(5)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, times(1)).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
        verify(this.strategy1, times(1)).notify(failingLogin);
        verify(this.strategy2, times(1)).notify(failingLogin);
    }

    /**
     * Ensure that the threshold mechanism works properly with different login.
     */
    @Test
    public void recordAuthFailureDifferentLogin()
    {
        String login1 = this.failingLogin.toLowerCase();
        String login2 = this.failingLogin.toUpperCase();
        String login3 = "barfoo";

        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login3));

        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2));
        assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login3));

        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2));
        assertTrue(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login3));

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
    }

    /**
     * Ensure that the authentication threshold auth is deactivated if max attempt is set to 0
     */
    @Test
    public void deactivateThresholdAuthWithMaxAttempt()
    {
        when(this.configuration.getMaxAuthorizedAttempts()).thenReturn(0);

        for (int i = 0; i < 100; i++) {
            assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        }
        verify(this.observationManager, times(100)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, never()).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
    }

    /**
     * Ensure that the authentication threshold auth is deactivated if time window is set to 0
     */
    @Test
    public void deactivateThresholdAuthWithTimeWindow()
    {
        when(this.configuration.getTimeWindow()).thenReturn(0);

        for (int i = 0; i < 100; i++) {
            assertFalse(this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin));
        }
        verify(this.observationManager, times(100)).notify(new AuthenticationFailureEvent(), this.failingLogin);
        verify(this.observationManager, never()).notify(new AuthenticationFailureLimitReachedEvent(),
            this.failingLogin);
    }

    /**
     * Validate that getForm is working properly.
     */
    @Test
    public void getForm()
    {
        String formStrategy1 = "formStrategy1";
        String formStrategy2 = "formStrategy2";
        when(this.strategy1.getForm(eq(this.failingLogin))).thenReturn(formStrategy1);
        when(this.strategy2.getForm(eq(this.failingLogin))).thenReturn(formStrategy2);

        assertEquals("", this.defaultAuthenticationFailureManager.getForm(this.failingLogin));

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin);
        assertEquals(String.format("%s\n%s\n", formStrategy1, formStrategy2),
            this.defaultAuthenticationFailureManager.getForm(this.failingLogin));
    }

    /**
     * Validate that getErrorMessages is working properly.
     */
    @Test
    public void getErrorMessages()
    {
        String errorMessage1 = "errorMessage1";
        String errorMessage2 = "errorMessage2";
        when(this.strategy1.getErrorMessage(eq(this.failingLogin))).thenReturn(errorMessage1);
        when(this.strategy2.getErrorMessage(eq(this.failingLogin))).thenReturn(errorMessage2);

        assertEquals("", this.defaultAuthenticationFailureManager.getErrorMessage(this.failingLogin));

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(this.failingLogin);
        assertEquals(String.format("%s\n%s\n", errorMessage1, errorMessage2),
            this.defaultAuthenticationFailureManager.getErrorMessage(this.failingLogin));
    }

    /**
     * Validate that getForm is working properly.
     */
    @Test
    public void validateForm()
    {
        String login1 = this.failingLogin;
        String login2 = "barfoo";

        assertTrue(this.defaultAuthenticationFailureManager.validateForm(login1, null));
        assertTrue(this.defaultAuthenticationFailureManager.validateForm(login2, null));

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login1);

        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2);
        this.defaultAuthenticationFailureManager.recordAuthenticationFailure(login2);

        when(this.strategy1.validateForm(login1, null)).thenReturn(true);
        when(this.strategy2.validateForm(login1, null)).thenReturn(true);
        assertTrue(this.defaultAuthenticationFailureManager.validateForm(login1, null));

        when(this.strategy1.validateForm(login2, null)).thenReturn(true);
        when(this.strategy2.validateForm(login2, null)).thenReturn(false);
        assertFalse(this.defaultAuthenticationFailureManager.validateForm(login2, null));
    }
}
