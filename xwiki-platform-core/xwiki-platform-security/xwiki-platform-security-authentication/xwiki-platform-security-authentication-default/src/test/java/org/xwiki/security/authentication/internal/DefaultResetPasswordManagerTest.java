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
import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.ResetPasswordManager;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
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
import static org.mockito.Mockito.mock;
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
    private Provider<ResetPasswordMailSender> resetPasswordMailSenderProvider;

    private ResetPasswordMailSender resetPasswordMailSender;

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
        when(this.xWiki.getDocument(this.userDocumentReference, this.context)).thenReturn(this.userDocument);
        this.resetPasswordMailSender = mock(ResetPasswordMailSender.class);
        when(this.resetPasswordMailSenderProvider.get()).thenReturn(this.resetPasswordMailSender);
    }

    @Test
    void requestResetPassword() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(DefaultResetPasswordManager.RESET_PASSWORD_REQUEST_CLASS_REFERENCE, true, this.context))
            .thenReturn(xObject);
        String verificationCode = "abcde1234";
        when(this.xWiki.generateRandomString(30)).thenReturn(verificationCode);
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.versionComment"))
            .thenReturn("Save verification code 42");

        ResetPasswordRequestResponse expectedResult =
            new DefaultResetPasswordRequestResponse(this.userReference, email,
                verificationCode);
        assertEquals(expectedResult, this.resetPasswordManager.requestResetPassword(this.userReference));
        verify(xObject).set(DefaultResetPasswordManager.VERIFICATION_PROPERTY, verificationCode, context);
        verify(this.xWiki).saveDocument(this.userDocument, "Save verification code 42", true, this.context);
    }

    @Test
    void requestResetPasswordUnexistingUser()
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(false);
        String exceptionMessage = "User [user:Foobar] doesn't exist";
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.noUser",
            "user:Foobar")).thenReturn(exceptionMessage);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.requestResetPassword(this.userReference));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
    }

    @Test
    void requestResetPasswordNotDocumentReferenceUser()
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
        String exceptionMessage = "User has no email address.";
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.noEmail"))
            .thenReturn(exceptionMessage);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.requestResetPassword(this.userReference));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
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
        when(this.referenceSerializer.serialize(this.userReference)).thenReturn("user:Foobar");
        when(this.userProperties.getFirstName()).thenReturn("Foo");
        when(this.userProperties.getLastName()).thenReturn("Bar");

        AuthenticationResourceReference resourceReference =
            new AuthenticationResourceReference(AuthenticationAction.RESET_PASSWORD);

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
        DefaultResetPasswordRequestResponse requestResponse =
            new DefaultResetPasswordRequestResponse(this.userReference, email,
                verificationCode);
        this.resetPasswordManager.sendResetPasswordEmailRequest(requestResponse);
        verify(this.resetPasswordMailSender).sendResetPasswordEmail("Foo Bar", email,
            new URL("http://xwiki.org/xwiki/authenticate/reset?u=user%3AFoobar&v=foobar4242"));
    }

    @Test
    void checkVerificationCode() throws Exception
    {
        when(this.userManager.exists(this.userReference)).thenReturn(true);
        InternetAddress email = new InternetAddress("foobar@xwiki.org");
        when(this.userProperties.getEmail()).thenReturn(email);
        String verificationCode = "abcd1245";
        BaseObject xObject = mock(BaseObject.class);
        when(this.userDocument
            .getXObject(DefaultResetPasswordManager.RESET_PASSWORD_REQUEST_CLASS_REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(DefaultResetPasswordManager.VERIFICATION_PROPERTY))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(DefaultResetPasswordManager.VERIFICATION_PROPERTY)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn(encodedVerificationCode);
        String newVerificationCode = "foobartest";
        when(xWiki.generateRandomString(30)).thenReturn(newVerificationCode);
        String saveComment = "Save new verification code";
        when(this.localizationManager
            .getTranslationPlain("xe.admin.passwordReset.step2.versionComment.changeValidationKey"))
            .thenReturn(saveComment);
        DefaultResetPasswordRequestResponse expected =
            new DefaultResetPasswordRequestResponse(this.userReference, email,
                newVerificationCode);

        assertEquals(expected, this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        verify(this.xWiki).saveDocument(this.userDocument, saveComment, true, context);
    }

    @Test
    void checkVerificationCodeUnexistingUser()
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(false);
        String exceptionMessage = "User [user:Foobar] doesn't exist";
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.noUser",
            "user:Foobar")).thenReturn(exceptionMessage);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(this.userReference, "some code"));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
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
            .getXObject(DefaultResetPasswordManager.RESET_PASSWORD_REQUEST_CLASS_REFERENCE))
            .thenReturn(xObject);
        String encodedVerificationCode = "encodedVerificationCode";
        when(xObject.getStringValue(DefaultResetPasswordManager.VERIFICATION_PROPERTY))
            .thenReturn(encodedVerificationCode);
        BaseClass baseClass = mock(BaseClass.class);
        when(xObject.getXClass(context)).thenReturn(baseClass);
        PasswordClass passwordClass = mock(PasswordClass.class);
        when(baseClass.get(DefaultResetPasswordManager.VERIFICATION_PROPERTY)).thenReturn(passwordClass);
        when(passwordClass.getEquivalentPassword(encodedVerificationCode, verificationCode))
            .thenReturn("anotherCode");
        String newVerificationCode = "foobartest";
        when(xWiki.generateRandomString(30)).thenReturn(newVerificationCode);
        String saveComment = "Save new verification code";
        when(this.localizationManager
            .getTranslationPlain("xe.admin.passwordReset.step2.versionComment.changeValidationKey"))
            .thenReturn(saveComment);
        String exceptionMessage = "Wrong verification code";
        when(this.localizationManager
            .getTranslationPlain("xe.admin.passwordReset.step2.error.wrongParameters", "user:Foobar"))
            .thenReturn(exceptionMessage);

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.checkVerificationCode(this.userReference, verificationCode));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
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
        verify(this.userDocument).removeXObjects(DefaultResetPasswordManager.RESET_PASSWORD_REQUEST_CLASS_REFERENCE);
        verify(xObject).setStringValue("password", newPassword);
    }

    @Test
    void resetPasswordUnexistingUser() throws Exception
    {
        when(this.userReference.toString()).thenReturn("user:Foobar");
        when(this.userManager.exists(this.userReference)).thenReturn(false);
        String exceptionMessage = "User [user:Foobar] doesn't exist";
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.noUser",
            "user:Foobar")).thenReturn(exceptionMessage);
        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordManager.resetPassword(this.userReference, "some password"));
        assertEquals(exceptionMessage, resetPasswordException.getMessage());
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
}
