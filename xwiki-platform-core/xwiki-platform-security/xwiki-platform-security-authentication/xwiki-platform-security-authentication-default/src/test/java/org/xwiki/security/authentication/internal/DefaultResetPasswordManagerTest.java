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

import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.security.authentication.RegistrationConfiguration;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.internal.document.DocumentUserReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultResetPasswordManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultResetPasswordManagerTest
{
    @InjectMockComponents
    private DefaultResetPasswordManager resetPasswordManager;

    @MockComponent
    private UserManager userManager;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> resourceReferenceSerializer;

    @MockComponent
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> urlNormalizer;

    @MockComponent
    private UserReferenceSerializer<String> referenceSerializer;

    @MockComponent
    private Provider<AuthenticationMailSender> resetPasswordMailSenderProvider;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @MockComponent
    private RegistrationConfiguration registrationConfiguration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private AuthenticationMailSender authenticationMailSender;

    private DocumentUserReference userReference;
    private DocumentReference userDocumentReference;
    private UserProperties userProperties;
    private XWikiContext context;
    private XWiki xWiki;
    private XWikiDocument userDocument;

    @BeforeEach
    void setup() throws Exception
    {
        this.userReference = mock(DocumentUserReference.class);
        this.userDocumentReference = mock(DocumentReference.class);
        this.userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(this.userReference)).thenReturn(this.userProperties);
        when(this.userReference.getReference()).thenReturn(this.userDocumentReference);

        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xWiki);
        this.userDocument = mock(XWikiDocument.class);
        when(this.userDocument.clone()).thenReturn(this.userDocument);
        when(this.xWiki.getDocument(this.userDocumentReference, this.context)).thenReturn(this.userDocument);
        this.authenticationMailSender = mock(AuthenticationMailSender.class);
        when(this.resetPasswordMailSenderProvider.get()).thenReturn(this.authenticationMailSender);
    }

    @Test
    void requestResetPassword() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE, true, this.context))
            .thenReturn(xObject);
        String verificationCode = "abcde1234";
        when(this.xWiki.generateRandomString(30)).thenReturn(verificationCode);
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.versionComment"))
            .thenReturn("Save verification code 42");

        ResetPasswordRequestResponse expectedResult =
            new DefaultResetPasswordRequestResponse(this.userReference, verificationCode);
        assertEquals(expectedResult, this.resetPasswordManager.requestResetPassword(this.userReference));
        verify(xObject).set(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD, verificationCode, context);
        verify(this.xWiki).saveDocument(this.userDocument, "Save verification code 42", true, this.context);
    }

    @Test
    void requestResetPasswordUnexistingUser() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(false);
        assertEquals(new DefaultResetPasswordRequestResponse(this.userReference),
            this.resetPasswordManager.requestResetPassword(this.userReference));
    }

    @Test
    void requestResetPasswordNotDocumentReferenceUser() throws Exception
    {
        UserReference otherUserReference = mock(UserReference.class);
        when(this.userManager.exists(otherUserReference)).thenReturn(true);
        String exceptionMessage = "Only user having a page on the wiki can reset their password.";
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.requestResetPassword(otherUserReference));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
    }

    @Test
    void requestResetPasswordNoEmail() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        assertEquals(new DefaultResetPasswordRequestResponse(this.userReference, null),
            this.resetPasswordManager.requestResetPassword(this.userReference));
        when(this.userReference.toString()).thenReturn("foo");
        assertEquals("User [foo] asked to reset their password, but did not have any email configured.",
            logCapture.getMessage(0));
    }

    @Test
    void requestResetPasswordLdapUser() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(DefaultResetPasswordManager.LDAP_CLASS_REFERENCE))
            .thenReturn(xObject);
        String exceptionMessage = "User [user:Foobar] is an LDAP user.";
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.ldapUser",
            "user:Foobar")).thenReturn(exceptionMessage);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.requestResetPassword(this.userReference));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
    }

    @Test
    void sendResetPasswordEmailRequest() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        when(this.referenceSerializer.serialize(this.userReference)).thenReturn("user:Foobar");
        when(this.userProperties.getFirstName()).thenReturn("Foo");
        when(this.userProperties.getLastName()).thenReturn("Bar");
        WikiReference wikiReference = new WikiReference("foo");
        when(this.context.getWikiReference()).thenReturn(wikiReference);
        AuthenticationResourceReference resourceReference =
            new AuthenticationResourceReference(wikiReference, AuthenticationAction.RESET_PASSWORD);

        String verificationCode = "foobar4242";
        resourceReference.addParameter("u", "user:Foobar");
        resourceReference.addParameter("v", verificationCode);
        ExtendedURL firstExtendedURL =
            new ExtendedURL(Arrays.asList("authenticate", "reset"), resourceReference.getParameters());
        when(this.resourceReferenceSerializer.serialize(resourceReference)).thenReturn(firstExtendedURL);
        when(this.urlNormalizer.normalize(firstExtendedURL)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "authenticate", "reset"), resourceReference.getParameters())
        );
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.context.getURLFactory()).thenReturn(urlFactory);
        when(urlFactory.getServerURL(this.context)).thenReturn(new URL("http://xwiki.org"));

        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);

        DefaultResetPasswordRequestResponse requestResponse =
            new DefaultResetPasswordRequestResponse(this.userReference, verificationCode);
        this.resetPasswordManager.sendResetPasswordEmailRequest(requestResponse);
        verify(this.authenticationMailSender).sendResetPasswordEmail("Foo Bar", email,
            new URL("http://xwiki.org/xwiki/authenticate/reset?u=user%3AFoobar&v=foobar4242"));
    }

    @Test
    void checkVerificationCodeGoodCodeNoExpirationDate() throws Exception
    {
        when(this.configurationSource.getProperty(DefaultResetPasswordManager.TOKEN_LIFETIME, 60)).thenReturn(0);
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn(encodedVerificationCode);
        DefaultResetPasswordRequestResponse expected =
            new DefaultResetPasswordRequestResponse(this.userReference, verificationCode);

        assertEquals(expected, this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        verify(this.xWiki, never()).saveDocument(eq(this.userDocument), any(), anyBoolean(), eq(context));
    }

    @Test
    void checkGoodVerificationCodeTokenLifetimeNotExpired() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn(encodedVerificationCode);
        when(this.configurationSource.getProperty(DefaultResetPasswordManager.TOKEN_LIFETIME, 60)).thenReturn(15);
        when(xObject.getDateValue(ResetPasswordRequestClassDocumentInitializer.REQUEST_DATE_FIELD))
            .thenReturn(Date.from(Instant.now().minus(14, ChronoUnit.MINUTES)));

        DefaultResetPasswordRequestResponse expected =
            new DefaultResetPasswordRequestResponse(this.userReference, verificationCode);

        assertEquals(expected, this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        verify(this.xWiki, never()).saveDocument(any(), any(), anyBoolean(), any());
    }

    @Test
    void checkGoodVerificationCodeTokenLifetimeExpired() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn(encodedVerificationCode);
        when(this.configurationSource.getProperty(DefaultResetPasswordManager.TOKEN_LIFETIME, 60)).thenReturn(15);
        when(xObject.getDateValue(ResetPasswordRequestClassDocumentInitializer.REQUEST_DATE_FIELD))
            .thenReturn(Date.from(Instant.now().minus(17, ChronoUnit.MINUTES)));

        DefaultResetPasswordRequestResponse expected =
            new DefaultResetPasswordRequestResponse(this.userReference);

        String saveComment = "Removed expired token";
        when(this.localizationManager
            .getTranslationPlain("security.authentication.resetPassword.tokenExpired"))
            .thenReturn(saveComment);

        String exceptionMessage = "Wrong verification code";
        when(this.localizationManager
            .getTranslationPlain("security.authentication.resetPassword.error.badParameters"))
            .thenReturn(exceptionMessage);

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
        verify(userDocument).removeXObjects(ResetPasswordRequestClassDocumentInitializer.REFERENCE);
        verify(this.xWiki).saveDocument(userDocument, saveComment, true, context);
    }

    @Test
    void checkVerificationCodeUnexistingUser() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(false);
        ResetPasswordRequestResponse resetPasswordRequestResponse =
            this.resetPasswordManager.checkVerificationCode(this.userReference, "some code");
        assertEquals(new DefaultResetPasswordRequestResponse(this.userReference, null), resetPasswordRequestResponse);
    }

    @Test
    void checkVerificationCodeWrongCode() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn("anotherCode");
        when(this.configurationSource.getProperty(DefaultResetPasswordManager.TOKEN_LIFETIME, 60)).thenReturn(15);
        when(xObject.getDateValue(ResetPasswordRequestClassDocumentInitializer.REQUEST_DATE_FIELD))
            .thenReturn(Date.from(Instant.now().minus(14, ChronoUnit.MINUTES)));

        String exceptionMessage = "Wrong verification code";
        when(this.localizationManager
            .getTranslationPlain("security.authentication.resetPassword.error.badParameters"))
            .thenReturn(exceptionMessage);

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
        verify(this.xWiki, never()).saveDocument(eq(this.userDocument), any(), anyBoolean(), eq(context));
    }

    @Test
    void checkVerificationCodeWrongCodeNoTokenExpiration() throws Exception
    {
        when(this.configurationSource.getProperty(DefaultResetPasswordManager.TOKEN_LIFETIME, 60)).thenReturn(0);
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn("anotherCode");
        String exceptionMessage = "Wrong verification code";
        when(this.localizationManager
            .getTranslationPlain("security.authentication.resetPassword.error.badParameters"))
            .thenReturn(exceptionMessage);
        String saveComment = "Bad token";
        when(this.localizationManager.getTranslationPlain("security.authentication.resetPassword.badToken"))
            .thenReturn(saveComment);

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
        verify(userDocument).removeXObjects(ResetPasswordRequestClassDocumentInitializer.REFERENCE);
        verify(this.xWiki).saveDocument(userDocument, saveComment, true, context);
    }

    @Test
    void checkWrongVerificationCodeTokenLifetimeExpired() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn("anotherCode");

        String saveComment = "Removed expired token";
        when(this.localizationManager
            .getTranslationPlain("security.authentication.resetPassword.tokenExpired"))
            .thenReturn(saveComment);
        String exceptionMessage = "Wrong verification code";
        when(this.localizationManager
            .getTranslationPlain("security.authentication.resetPassword.error.badParameters"))
            .thenReturn(exceptionMessage);
        when(this.configurationSource.getProperty(DefaultResetPasswordManager.TOKEN_LIFETIME, 60)).thenReturn(15);
        when(this.userDocument.getDate()).thenReturn(Date.from(Instant.now().minus(16, ChronoUnit.MINUTES)));

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
        verify(userDocument).removeXObjects(ResetPasswordRequestClassDocumentInitializer.REFERENCE);
        verify(this.xWiki).saveDocument(this.userDocument, saveComment, true, context);
    }

    @Test
    void checkVerificationCodeNotDocumentReferenceUser() throws Exception
    {
        UserReference otherUserReference = mock(UserReference.class);
        when(this.userManager.exists(otherUserReference)).thenReturn(true);
        String exceptionMessage = "Only user having a page on the wiki can reset their password.";
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(otherUserReference, "some code"));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
    }

    @Test
    void resetPassword() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(DefaultResetPasswordManager.USER_CLASS_REFERENCE))
            .thenReturn(xObject);
        String saveComment = "Change password";
        when(this.localizationManager
            .getTranslationPlain("xe.admin.passwordReset.step2.versionComment.passwordReset"))
            .thenReturn(saveComment);
        String newPassword = "mypassword";
        this.resetPasswordManager.resetPassword(this.userReference, newPassword);
        verify(this.userDocument).removeXObjects(ResetPasswordRequestClassDocumentInitializer.REFERENCE);
        verify(xObject).set("password", newPassword, context);
    }

    @Test
    void resetPasswordUnexistingUser() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(false);
        this.resetPasswordManager.resetPassword(this.userReference, "some password");
        verify(this.xWiki, never()).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xWiki, never()).saveDocument(any(XWikiDocument.class), anyString(), anyBoolean(),
            any(XWikiContext.class));
    }

    @Test
    void resetPasswordNotDocumentReferenceUser() throws Exception
    {
        UserReference otherUserReference = mock(UserReference.class);
        when(this.userManager.exists(otherUserReference)).thenReturn(true);
        String exceptionMessage = "Only user having a page on the wiki can reset their password.";
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.resetPassword(otherUserReference, "some password"));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForIsPasswordCompliantWithRegistrationRules")
    void isPasswordCompliantWithRegistrationRules(int minimumLength,
        Set<RegistrationConfiguration.PasswordRules> rules, String password, boolean expectation)
    {
        when(this.registrationConfiguration.getPasswordMinimumLength()).thenReturn(minimumLength);
        when(this.registrationConfiguration.getPasswordRules()).thenReturn(rules);
        assertEquals(expectation, this.resetPasswordManager.isPasswordCompliantWithRegistrationRules(password));
    }

    private static Stream<Arguments> provideArgumentsForIsPasswordCompliantWithRegistrationRules()
    {
        Set<RegistrationConfiguration.PasswordRules> noRules = Set.of();
        Set<RegistrationConfiguration.PasswordRules> lowerMandatory =
            Set.of(RegistrationConfiguration.PasswordRules.ONE_LOWER_CASE_CHARACTER);
        Set<RegistrationConfiguration.PasswordRules> upperMandatory =
            Set.of(RegistrationConfiguration.PasswordRules.ONE_UPPER_CASE_CHARACTER);
        Set<RegistrationConfiguration.PasswordRules> symbolMandatory =
            Set.of(RegistrationConfiguration.PasswordRules.ONE_SYMBOL_CHARACTER);
        Set<RegistrationConfiguration.PasswordRules> numberMandatory =
            Set.of(RegistrationConfiguration.PasswordRules.ONE_NUMBER_CHARACTER);
        Set<RegistrationConfiguration.PasswordRules> upperAndNumberMandatory =
            Set.of(
                RegistrationConfiguration.PasswordRules.ONE_UPPER_CASE_CHARACTER,
                RegistrationConfiguration.PasswordRules.ONE_NUMBER_CHARACTER
            );
        Set<RegistrationConfiguration.PasswordRules> lowerAndUpperMandatory =
            Set.of(
                RegistrationConfiguration.PasswordRules.ONE_LOWER_CASE_CHARACTER,
                RegistrationConfiguration.PasswordRules.ONE_UPPER_CASE_CHARACTER
            );
        Set<RegistrationConfiguration.PasswordRules> allRules =
            Set.of(
                RegistrationConfiguration.PasswordRules.ONE_LOWER_CASE_CHARACTER,
                RegistrationConfiguration.PasswordRules.ONE_UPPER_CASE_CHARACTER,
                RegistrationConfiguration.PasswordRules.ONE_NUMBER_CHARACTER,
                RegistrationConfiguration.PasswordRules.ONE_SYMBOL_CHARACTER
            );

        String onlyLower3Chars = "foo";
        String onlyLower6Chars = "fooooo";
        String lowerUpper11Chars = "foooOOOOOoo";
        String lowerNumber6Chars = "foo000";
        String lowerUpperNumber11Chars = "fooOOOOo000";
        String lowerNumberSymbolSpace9Chars = "foo_/ 000";
        String lowerSymbolNoSpace5Chars = "foo_/";
        String lowerUpperSymbolNoSpace9Chars = "fOOOOoo_/";
        String lowerUpperSymbolNumberSpace12Chars = "foOOOo_/ 000";
        String lowerUpperSymbolNumberSpaceAccents14Chars = "foOOOo_/ éé000";
        String symbolSpace3Chars = " _/";
        String symbolNumberNoSpace6Chars = "_/3434";
        String onlyUpper3Chars = "FOO";
        String onlyUpper6Chars = "FOOOOO";
        String isolatedSymbol = "bar@bar";

        return Stream.of(
            // Length 3, no rules
            Arguments.of(3, noRules, onlyLower3Chars, true),
            Arguments.of(3, noRules, onlyLower6Chars, true),
            Arguments.of(3, noRules, lowerUpper11Chars, true),
            Arguments.of(3, noRules, lowerNumber6Chars, true),
            Arguments.of(3, noRules, lowerUpperNumber11Chars, true),
            Arguments.of(3, noRules, lowerNumberSymbolSpace9Chars, true),
            Arguments.of(3, noRules, lowerSymbolNoSpace5Chars, true),
            Arguments.of(3, noRules, lowerUpperSymbolNoSpace9Chars, true),
            Arguments.of(3, noRules, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(3, noRules, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(3, noRules, symbolSpace3Chars, true),
            Arguments.of(3, noRules, symbolNumberNoSpace6Chars, true),
            Arguments.of(3, noRules, onlyUpper3Chars, true),
            Arguments.of(3, noRules, onlyUpper6Chars, true),
            Arguments.of(3, noRules, isolatedSymbol, true),
            // length 6, no rules
            Arguments.of(6, noRules, onlyLower3Chars, false),
            Arguments.of(6, noRules, onlyLower6Chars, true),
            Arguments.of(6, noRules, lowerUpper11Chars, true),
            Arguments.of(6, noRules, lowerNumber6Chars, true),
            Arguments.of(6, noRules, lowerUpperNumber11Chars, true),
            Arguments.of(6, noRules, lowerNumberSymbolSpace9Chars, true),
            Arguments.of(6, noRules, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, noRules, lowerUpperSymbolNoSpace9Chars, true),
            Arguments.of(6, noRules, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, noRules, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, noRules, symbolSpace3Chars, false),
            Arguments.of(6, noRules, symbolNumberNoSpace6Chars, true),
            Arguments.of(6, noRules, onlyUpper3Chars, false),
            Arguments.of(6, noRules, onlyUpper6Chars, true),
            Arguments.of(6, noRules, isolatedSymbol, true),
            // length 6, lower mandatory
            Arguments.of(6, lowerMandatory, onlyLower3Chars, false),
            Arguments.of(6, lowerMandatory, onlyLower6Chars, true),
            Arguments.of(6, lowerMandatory, lowerUpper11Chars, true),
            Arguments.of(6, lowerMandatory, lowerNumber6Chars, true),
            Arguments.of(6, lowerMandatory, lowerUpperNumber11Chars, true),
            Arguments.of(6, lowerMandatory, lowerNumberSymbolSpace9Chars, true),
            Arguments.of(6, lowerMandatory, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, lowerMandatory, lowerUpperSymbolNoSpace9Chars, true),
            Arguments.of(6, lowerMandatory, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, lowerMandatory, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, lowerMandatory, symbolSpace3Chars, false),
            Arguments.of(6, lowerMandatory, symbolNumberNoSpace6Chars, false),
            Arguments.of(6, lowerMandatory, onlyUpper3Chars, false),
            Arguments.of(6, lowerMandatory, onlyUpper6Chars, false),
            Arguments.of(6, lowerMandatory, isolatedSymbol, true),
            // length 6, upper mandatory
            Arguments.of(6, upperMandatory, onlyLower3Chars, false),
            Arguments.of(6, upperMandatory, onlyLower6Chars, false),
            Arguments.of(6, upperMandatory, lowerUpper11Chars, true),
            Arguments.of(6, upperMandatory, lowerNumber6Chars, false),
            Arguments.of(6, upperMandatory, lowerUpperNumber11Chars, true),
            Arguments.of(6, upperMandatory, lowerNumberSymbolSpace9Chars, false),
            Arguments.of(6, upperMandatory, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, upperMandatory, lowerUpperSymbolNoSpace9Chars, true),
            Arguments.of(6, upperMandatory, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, upperMandatory, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, upperMandatory, symbolSpace3Chars, false),
            Arguments.of(6, upperMandatory, symbolNumberNoSpace6Chars, false),
            Arguments.of(6, upperMandatory, onlyUpper3Chars, false),
            Arguments.of(6, upperMandatory, onlyUpper6Chars, true),
            Arguments.of(6, upperMandatory, isolatedSymbol, false),
            // length 6, number mandatory
            Arguments.of(6, numberMandatory, onlyLower3Chars, false),
            Arguments.of(6, numberMandatory, onlyLower6Chars, false),
            Arguments.of(6, numberMandatory, lowerUpper11Chars, false),
            Arguments.of(6, numberMandatory, lowerNumber6Chars, true),
            Arguments.of(6, numberMandatory, lowerUpperNumber11Chars, true),
            Arguments.of(6, numberMandatory, lowerNumberSymbolSpace9Chars, true),
            Arguments.of(6, numberMandatory, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, numberMandatory, lowerUpperSymbolNoSpace9Chars, false),
            Arguments.of(6, numberMandatory, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, numberMandatory, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, numberMandatory, symbolSpace3Chars, false),
            Arguments.of(6, numberMandatory, symbolNumberNoSpace6Chars, true),
            Arguments.of(6, numberMandatory, onlyUpper3Chars, false),
            Arguments.of(6, numberMandatory, onlyUpper6Chars, false),
            Arguments.of(6, numberMandatory, isolatedSymbol, false),
            // length 6, symbol mandatory
            Arguments.of(6, symbolMandatory, onlyLower3Chars, false),
            Arguments.of(6, symbolMandatory, onlyLower6Chars, false),
            Arguments.of(6, symbolMandatory, lowerUpper11Chars, false),
            Arguments.of(6, symbolMandatory, lowerNumber6Chars, false),
            Arguments.of(6, symbolMandatory, lowerUpperNumber11Chars, false),
            Arguments.of(6, symbolMandatory, lowerNumberSymbolSpace9Chars, true),
            Arguments.of(6, symbolMandatory, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, symbolMandatory, lowerUpperSymbolNoSpace9Chars, true),
            Arguments.of(6, symbolMandatory, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, symbolMandatory, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, symbolMandatory, symbolSpace3Chars, false),
            Arguments.of(6, symbolMandatory, symbolNumberNoSpace6Chars, true),
            Arguments.of(6, symbolMandatory, onlyUpper3Chars, false),
            Arguments.of(6, symbolMandatory, onlyUpper6Chars, false),
            Arguments.of(6, symbolMandatory, isolatedSymbol, true),
            // length 6, upper and number mandatory
            Arguments.of(6, upperAndNumberMandatory, onlyLower3Chars, false),
            Arguments.of(6, upperAndNumberMandatory, onlyLower6Chars, false),
            Arguments.of(6, upperAndNumberMandatory, lowerUpper11Chars, false),
            Arguments.of(6, upperAndNumberMandatory, lowerNumber6Chars, false),
            Arguments.of(6, upperAndNumberMandatory, lowerUpperNumber11Chars, true),
            Arguments.of(6, upperAndNumberMandatory, lowerNumberSymbolSpace9Chars, false),
            Arguments.of(6, upperAndNumberMandatory, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, upperAndNumberMandatory, lowerUpperSymbolNoSpace9Chars, false),
            Arguments.of(6, upperAndNumberMandatory, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, upperAndNumberMandatory, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, upperAndNumberMandatory, symbolSpace3Chars, false),
            Arguments.of(6, upperAndNumberMandatory, symbolNumberNoSpace6Chars, false),
            Arguments.of(6, upperAndNumberMandatory, onlyUpper3Chars, false),
            Arguments.of(6, upperAndNumberMandatory, onlyUpper6Chars, false),
            Arguments.of(6, upperAndNumberMandatory, isolatedSymbol, false),
            // length 6, upper and lower mandatory
            Arguments.of(6, lowerAndUpperMandatory, onlyLower3Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, onlyLower6Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, lowerUpper11Chars, true),
            Arguments.of(6, lowerAndUpperMandatory, lowerNumber6Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, lowerUpperNumber11Chars, true),
            Arguments.of(6, lowerAndUpperMandatory, lowerNumberSymbolSpace9Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, lowerUpperSymbolNoSpace9Chars, true),
            Arguments.of(6, lowerAndUpperMandatory, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, lowerAndUpperMandatory, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, lowerAndUpperMandatory, symbolSpace3Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, symbolNumberNoSpace6Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, onlyUpper3Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, onlyUpper6Chars, false),
            Arguments.of(6, lowerAndUpperMandatory, isolatedSymbol, false),
            // length 6, all rules
            Arguments.of(6, allRules, onlyLower3Chars, false),
            Arguments.of(6, allRules, onlyLower6Chars, false),
            Arguments.of(6, allRules, lowerUpper11Chars, false),
            Arguments.of(6, allRules, lowerNumber6Chars, false),
            Arguments.of(6, allRules, lowerUpperNumber11Chars, false),
            Arguments.of(6, allRules, lowerNumberSymbolSpace9Chars, false),
            Arguments.of(6, allRules, lowerSymbolNoSpace5Chars, false),
            Arguments.of(6, allRules, lowerUpperSymbolNoSpace9Chars, false),
            Arguments.of(6, allRules, lowerUpperSymbolNumberSpace12Chars, true),
            Arguments.of(6, allRules, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(6, allRules, symbolSpace3Chars, false),
            Arguments.of(6, allRules, symbolNumberNoSpace6Chars, false),
            Arguments.of(6, allRules, onlyUpper3Chars, false),
            Arguments.of(6, allRules, onlyUpper6Chars, false),
            Arguments.of(6, allRules, isolatedSymbol, false),
            // length 13, all rules
            Arguments.of(13, allRules, lowerUpperSymbolNumberSpace12Chars, false),
            Arguments.of(13, allRules, lowerUpperSymbolNumberSpaceAccents14Chars, true),
            Arguments.of(13, allRules, isolatedSymbol, false)
        );
    }
}
