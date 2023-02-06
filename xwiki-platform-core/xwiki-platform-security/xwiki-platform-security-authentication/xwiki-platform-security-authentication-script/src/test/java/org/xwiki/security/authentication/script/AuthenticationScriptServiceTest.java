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
package org.xwiki.security.authentication.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationConfiguration;
import org.xwiki.security.authentication.AuthenticationFailureManager;
import org.xwiki.security.authentication.AuthenticationFailureStrategy;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.ResetPasswordManager;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.security.authentication.RetrieveUsernameManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class AuthenticationScriptServiceTest
{
    @InjectMockComponents
    private AuthenticationScriptService scriptService;

    @MockComponent
    private AuthenticationFailureManager authenticationFailureManager;

    @MockComponent
    private AuthenticationConfiguration authenticationConfiguration;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> defaultResourceReferenceSerializer;

    @MockComponent
    private ResetPasswordManager resetPasswordManager;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private RetrieveUsernameManager retrieveUsernameManager;

    @MockComponent
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> urlNormalizer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext xWikiContext;

    @BeforeEach
    void setup()
    {
        this.xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
    }

    @Test
    void getForm()
    {
        String username = "foobar";
        XWikiRequest xWikiRequest = mock(XWikiRequest.class);
        when(this.xWikiContext.getRequest()).thenReturn(xWikiRequest);
        when(this.authenticationFailureManager.getForm(username, xWikiRequest)).thenReturn("my specific form");
        assertEquals("my specific form", this.scriptService.getForm(username));
    }

    @Test
    void getErrorMessage()
    {
        String username = "foobar";
        when(this.authenticationFailureManager.getErrorMessage(username)).thenReturn("Some error message");
        assertEquals("Some error message", this.scriptService.getErrorMessage(username));
    }

    @Test
    void getAuthenticationConfiguration()
    {
        assertEquals(this.authenticationConfiguration, this.scriptService.getAuthenticationConfiguration());
    }

    @Test
    void getAuthenticationFailureAvailableStrategies(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerMockComponent(AuthenticationFailureStrategy.class, "instance1");
        componentManager.registerMockComponent(AuthenticationFailureStrategy.class, "instance2");

        Set<String> expectedResult = new HashSet<>(Arrays.asList("instance1", "instance2"));
        assertEquals(expectedResult, this.scriptService.getAuthenticationFailureAvailableStrategies());
    }

    @Test
    void resetAuthenticationFailureCounter()
    {
        String username = "foobar";
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        this.scriptService.resetAuthenticationFailureCounter(username);
        verify(this.authenticationFailureManager).resetAuthenticationFailureCounter(username);
    }

    @Test
    void resetAuthenticationFailureCounterWithoutPR()
    {
        String username = "foobar";
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        this.scriptService.resetAuthenticationFailureCounter(username);
        verify(this.authenticationFailureManager, never()).resetAuthenticationFailureCounter(username);
    }

    @Test
    void getAuthenticationURL() throws Exception
    {
        String action = AuthenticationAction.RETRIEVE_USERNAME.getRequestParameter();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("u", "foo");
        parameters.put("v", "bar");

        WikiReference wikiReference = new WikiReference("current");
        when(this.xWikiContext.getWikiReference()).thenReturn(wikiReference);

        AuthenticationResourceReference resourceReference =
            new AuthenticationResourceReference(wikiReference, AuthenticationAction.RETRIEVE_USERNAME);
        resourceReference.addParameter("u", "foo");
        resourceReference.addParameter("v", "bar");

        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.defaultResourceReferenceSerializer.serialize(resourceReference)).thenReturn(extendedURL);

        ExtendedURL extendedURLNormalized = mock(ExtendedURL.class);
        when(this.urlNormalizer.normalize(extendedURL)).thenReturn(extendedURLNormalized);
        when(extendedURLNormalized.serialize()).thenReturn("http://something");

        assertEquals("http://something", this.scriptService.getAuthenticationURL(action, parameters));
    }

    @Test
    void requestResetPassword() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        UserReference userReference = mock(UserReference.class);
        ResetPasswordRequestResponse requestResponse = mock(ResetPasswordRequestResponse.class);
        when(this.resetPasswordManager.requestResetPassword(userReference)).thenReturn(requestResponse);
        InternetAddress userEmail = new InternetAddress("acme@xwiki.org");

        this.scriptService.requestResetPassword(userReference);
        verify(this.resetPasswordManager).sendResetPasswordEmailRequest(requestResponse);
    }

    @Test
    void requestResetPasswordWithoutPR() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);

        this.scriptService.requestResetPassword(mock(UserReference.class));
        verify(this.resetPasswordManager, never()).requestResetPassword(any());
        verify(this.resetPasswordManager, never()).sendResetPasswordEmailRequest(any());
    }

    @Test
    void checkVerificationCode() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        String verificationCode = "verificationCode";
        ResetPasswordRequestResponse requestResponse = mock(ResetPasswordRequestResponse.class);
        String newVerificationCode = "4242";
        when(this.resetPasswordManager.checkVerificationCode(userReference, verificationCode))
            .thenReturn(requestResponse);
        when(requestResponse.getVerificationCode()).thenReturn(newVerificationCode);
        assertEquals(newVerificationCode, this.scriptService.checkVerificationCode(userReference, verificationCode));
    }

    @Test
    void checkVerificationCodeBadCode() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        String verificationCode = "verificationCode";
        ResetPasswordException expectedException = new ResetPasswordException("Bad verification code");
        when(this.resetPasswordManager.checkVerificationCode(userReference, verificationCode))
            .thenThrow(expectedException);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.scriptService.checkVerificationCode(userReference, verificationCode));
        assertEquals(expectedException, resetPasswordException);
    }

    @Test
    void resetPassword() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        String verificationCode = "code";
        String newPassword = "foobar";

        this.scriptService.resetPassword(userReference, verificationCode, newPassword);
        verify(this.resetPasswordManager).checkVerificationCode(userReference, verificationCode);
        verify(this.resetPasswordManager).resetPassword(userReference, newPassword);
    }

    @Test
    void resetPasswordBadCode() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        String verificationCode = "verificationCode";
        ResetPasswordException expectedException = new ResetPasswordException("Bad verification code");
        when(this.resetPasswordManager.checkVerificationCode(userReference, verificationCode))
            .thenThrow(expectedException);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.scriptService.resetPassword(userReference, verificationCode, "some password"));
        assertEquals(expectedException, resetPasswordException);
        verify(this.resetPasswordManager, never()).resetPassword(eq(userReference), any());
    }

    @Test
    void retrieveUsernameAndSendEmail() throws Exception
    {
        String email = "foo@bar.com";
        when(this.retrieveUsernameManager.findUsers(email)).thenReturn(Collections.emptySet());
        this.scriptService.retrieveUsernameAndSendEmail(email);

        verify(this.retrieveUsernameManager).findUsers(email);
        verify(this.retrieveUsernameManager, never()).sendRetrieveUsernameEmail(any(), any());

        Set<UserReference> userReferences = Collections.singleton(mock(UserReference.class));
        when(this.retrieveUsernameManager.findUsers(email)).thenReturn(userReferences);

        this.scriptService.retrieveUsernameAndSendEmail(email);

        verify(this.retrieveUsernameManager, times(2)).findUsers(email);
        verify(this.retrieveUsernameManager).sendRetrieveUsernameEmail(email, userReferences);
    }
}
