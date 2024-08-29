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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.inject.Named;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.security.authentication.ResetPasswordManager;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R140600000XWIKI19869DataMigrationListener}.
 *
 * @version $Id$
 */
@ComponentTest
class R140600000XWIKI19869DataMigrationListenerTest
{
    @InjectMockComponents
    private R140600000XWIKI19869DataMigrationListener listener;

    @MockComponent
    private ResetPasswordManager resetPasswordManager;

    @MockComponent
    private AuthenticationMailSender resetPasswordMailSender;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource propertiesConfigurationSource;

    @MockComponent
    private Environment environment;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void onEvent(@XWikiTempDir File tmpDir) throws Exception
    {
        when(this.environment.getPermanentDirectory()).thenReturn(tmpDir);
        File migrationFile = new File(tmpDir, "140600000XWIKI19869DataMigration-users.txt");
        when(this.propertiesConfigurationSource
            .getProperty("security.migration.R140600000XWIKI19869.sendSecurityEmail", true)).thenReturn(false);
        when(this.propertiesConfigurationSource
            .getProperty("security.migration.R140600000XWIKI19869.sendResetPasswordEmail", true)).thenReturn(false);

        this.listener.onEvent(null, null, null);
        verifyNoInteractions(this.resetPasswordMailSender);
        verifyNoInteractions(this.resetPasswordManager);
        verifyNoInteractions(this.userReferenceResolver);

        Files.writeString(migrationFile.toPath(), "XWiki.Foo\nXWiki.Bar\nXWiki.Buz", StandardOpenOption.CREATE_NEW);

        this.listener.onEvent(null, null, null);
        verifyNoInteractions(this.resetPasswordMailSender);
        verifyNoInteractions(this.resetPasswordManager);
        verifyNoInteractions(this.userReferenceResolver);

        when(this.propertiesConfigurationSource
            .getProperty("security.migration.R140600000XWIKI19869.sendSecurityEmail", true)).thenReturn(true);
        when(this.propertiesConfigurationSource
            .getProperty("security.migration.R140600000XWIKI19869.sendResetPasswordEmail", true)).thenReturn(true);

        String mailFallbackSubject = "Subject fallback";
        String mailFallbackContent = "Mail content";
        when(this.contextualLocalizationManager
            .getTranslationPlain("security.authentication.migration1400600000XWIKI19869.email.subject"))
            .thenReturn(mailFallbackSubject);
        when(this.contextualLocalizationManager
            .getTranslationPlain("security.authentication.migration1400600000XWIKI19869.email.content"))
            .thenReturn(mailFallbackContent);

        UserReference fooRef = mock(UserReference.class, "foo");
        UserReference barRef = mock(UserReference.class, "bar");
        UserReference buzRef = mock(UserReference.class, "buz");

        when(this.userReferenceResolver.resolve("XWiki.Foo")).thenReturn(fooRef);
        when(this.userReferenceResolver.resolve("XWiki.Bar")).thenReturn(barRef);
        when(this.userReferenceResolver.resolve("XWiki.Buz")).thenReturn(buzRef);

        UserProperties fooProp = mock(UserProperties.class, "fooProp");
        UserProperties barProp = mock(UserProperties.class, "barProp");
        UserProperties buzProp = mock(UserProperties.class, "buzProp");

        when(this.userPropertiesResolver.resolve(fooRef)).thenReturn(fooProp);
        when(this.userPropertiesResolver.resolve(barRef)).thenReturn(barProp);
        when(this.userPropertiesResolver.resolve(buzRef)).thenReturn(buzProp);

        when(fooProp.getEmail()).thenReturn(new InternetAddress("foo@xwiki.com"));
        when(buzProp.getEmail()).thenReturn(new InternetAddress("buz@xwiki.com"));

        ResetPasswordRequestResponse responseFoo = mock(ResetPasswordRequestResponse.class);
        ResetPasswordRequestResponse responseBuz = mock(ResetPasswordRequestResponse.class);
        when(resetPasswordManager.requestResetPassword(fooRef)).thenReturn(responseFoo);
        when(resetPasswordManager.requestResetPassword(buzRef)).thenReturn(responseBuz);

        when(responseFoo.getVerificationCode()).thenReturn("some code");
        when(responseBuz.getVerificationCode()).thenReturn("some code");

        this.listener.onEvent(null, null, null);
        verify(this.resetPasswordMailSender)
            .sendAuthenticationSecurityEmail(fooRef, mailFallbackSubject, mailFallbackContent);
        verify(this.resetPasswordMailSender)
            .sendAuthenticationSecurityEmail(buzRef, mailFallbackSubject, mailFallbackContent);
        verify(this.resetPasswordMailSender, never()).sendAuthenticationSecurityEmail(eq(barRef), any(), any());

        verify(this.resetPasswordManager).sendResetPasswordEmailRequest(responseFoo);
        verify(this.resetPasswordManager).sendResetPasswordEmailRequest(responseBuz);

        assertEquals(1, this.logCapture.size());
        assertEquals("Reset email cannot be sent for user [bar] as no email address is provided.",
            this.logCapture.getMessage(0));

        // The file should have been deleted just after the first call
        this.listener.onEvent(null, null, null);

        Files.writeString(migrationFile.toPath(), "XWiki.Foo", StandardOpenOption.CREATE);
        File mailTemplate = new File(tmpDir, "140600000XWIKI19869-mail.txt");
        String mailSubject = "test";
        String mailContent = "Some mail content";
        Files.writeString(mailTemplate.toPath(),
            String.format("Subject:%s\n%s", mailSubject, mailContent), StandardOpenOption.CREATE_NEW);

        this.listener.onEvent(null, null, null);

        verify(this.resetPasswordMailSender)
            .sendAuthenticationSecurityEmail(fooRef, mailSubject, mailContent);

        verify(this.resetPasswordManager, times(2)).sendResetPasswordEmailRequest(responseFoo);

        // This one should not have been called twice
        verify(this.resetPasswordManager).sendResetPasswordEmailRequest(responseBuz);
    }
}