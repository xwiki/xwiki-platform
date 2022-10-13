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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationMailSender}.
 *
 * @version $Id$
 */
@ComponentTest
class AuthenticationMailSenderTest
{
    @InjectMockComponents
    private AuthenticationMailSender resetPasswordMailSender;

    private static final LocalDocumentReference RESET_PASSWORD_MAIL_TEMPLATE_REFERENCE =
        new LocalDocumentReference("XWiki", "ResetPasswordMailContent");

    @MockComponent
    private MailSenderConfiguration mailSenderConfiguration;

    @MockComponent
    @Named("template")
    private MimeMessageFactory<MimeMessage> mimeMessageFactory;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @MockComponent
    private MailSender mailSender;

    @MockComponent
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private SessionFactory sessionFactory;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    @Named("text")
    private MimeMessageFactory<MimeMessage> textMimeMessageFactory;

    private XWikiContext xWikiContext;

    private DocumentReference templateDocumentReference;

    private MailListener mailListener;

    @BeforeComponent
    void setupComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        this.xWikiContext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(xWikiContext);

        this.templateDocumentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve(RESET_PASSWORD_MAIL_TEMPLATE_REFERENCE))
            .thenReturn(templateDocumentReference);

        this.mailListener = mock(MailListener.class);
        when(this.mailListenerProvider.get()).thenReturn(this.mailListener);
    }

    @Test
    void sendResetPasswordEmail() throws Exception
    {
        String username = "foobar";
        InternetAddress email = new InternetAddress("foobar@xwiki.com");
        URL resetPasswordUrl = new URL("http://xwiki.com/authenticate/reset?u=foobar&v=1234code");
        String fromAdress = "admin@xwiki.com";
        when(this.mailSenderConfiguration.getFromAddress()).thenReturn(fromAdress);
        when(xWikiContext.getLocale()).thenReturn(Locale.CANADA);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("from", fromAdress);
        parameters.put("to", email);
        parameters.put("language", Locale.CANADA);
        parameters.put("type", "Reset Password");
        Map<String, String> velocityVariables = new HashMap<>();
        velocityVariables.put("userName", username);
        velocityVariables.put("passwordResetURL", resetPasswordUrl.toExternalForm());
        parameters.put("velocityVariables", velocityVariables);

        Session session = Session.getInstance(new Properties());
        when(this.sessionFactory.create(Collections.emptyMap())).thenReturn(session);
        MimeMessage message = mock(MimeMessage.class);

        when(this.mimeMessageFactory.createMessage(templateDocumentReference, parameters))
            .thenReturn(message);
        MailStatusResult mailStatusResult = mock(MailStatusResult.class);
        when(this.mailListener.getMailStatusResult()).thenReturn(mailStatusResult);
        MailStatus mailStatus = mock(MailStatus.class);
        when(mailStatusResult.getAll()).thenReturn(Collections.singleton(mailStatus).iterator());
        when(mailStatusResult.isProcessed()).thenReturn(true);
        when(mailStatus.getState()).thenReturn(MailState.SEND_SUCCESS.toString());

        this.resetPasswordMailSender.sendResetPasswordEmail(username, email, resetPasswordUrl);
        verify(this.mailSender).sendAsynchronously(Collections.singleton(message), session, this.mailListener);
        verify(mailStatusResult).waitTillProcessed(1000L);
        verify(mailStatusResult).getAllErrors();
    }

    @Test
    void sendResetPasswordEmailErrorWhenPreparing() throws Exception
    {
        String username = "flop";
        InternetAddress email = new InternetAddress("flop@xwiki.com");
        URL resetPasswordUrl = new URL("http://xwiki.com/authenticate/reset?u=flop&v=1234code");
        String fromAdress = "root@xwiki.com";
        when(this.mailSenderConfiguration.getFromAddress()).thenReturn(fromAdress);
        when(xWikiContext.getLocale()).thenReturn(Locale.CANADA);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("from", fromAdress);
        parameters.put("to", email);
        parameters.put("language", Locale.CANADA);
        parameters.put("type", "Reset Password");
        Map<String, String> velocityVariables = new HashMap<>();
        velocityVariables.put("userName", username);
        velocityVariables.put("passwordResetURL", resetPasswordUrl.toExternalForm());
        parameters.put("velocityVariables", velocityVariables);

        MessagingException messagingException = new MessagingException("Error with this template");
        when(this.mimeMessageFactory.createMessage(templateDocumentReference, parameters))
            .thenThrow(messagingException);
        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.emailFailed"))
            .thenReturn("Cannot send this email");

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class, () ->
            this.resetPasswordMailSender.sendResetPasswordEmail(username, email, resetPasswordUrl));
        assertEquals("Cannot send this email", resetPasswordException.getMessage());
        assertEquals(messagingException, resetPasswordException.getCause());
        verify(this.mailSender, never()).sendAsynchronously(any(), any(), any());
    }

    @Test
    void sendResetPasswordEmailErrorWhenSending() throws Exception
    {
        String username = "barfoo";
        InternetAddress email = new InternetAddress("barfoo@xwiki.com");
        URL resetPasswordUrl = new URL("http://xwiki.com/authenticate/reset?u=barfoo&v=1234code");
        String fromAdress = "someone@xwiki.com";
        when(this.mailSenderConfiguration.getFromAddress()).thenReturn(fromAdress);
        when(xWikiContext.getLocale()).thenReturn(Locale.CANADA);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("from", fromAdress);
        parameters.put("to", email);
        parameters.put("language", Locale.CANADA);
        parameters.put("type", "Reset Password");
        Map<String, String> velocityVariables = new HashMap<>();
        velocityVariables.put("userName", username);
        velocityVariables.put("passwordResetURL", resetPasswordUrl.toExternalForm());
        parameters.put("velocityVariables", velocityVariables);

        Session session = Session.getInstance(new Properties());
        when(this.sessionFactory.create(Collections.emptyMap())).thenReturn(session);
        MimeMessage message = mock(MimeMessage.class);
        when(this.mimeMessageFactory.createMessage(templateDocumentReference, parameters))
            .thenReturn(message);
        MailStatusResult mailStatusResult = mock(MailStatusResult.class);
        when(this.mailListener.getMailStatusResult()).thenReturn(mailStatusResult);
        MailStatus mailStatus = mock(MailStatus.class);
        List<MailStatus> mailStatusList = Arrays.asList(mailStatus, mock(MailStatus.class));
        when(mailStatusResult.getAllErrors()).thenReturn(mailStatusList.iterator());
        when(mailStatus.getErrorDescription()).thenReturn("Some sending error");

        when(this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.emailFailed"))
            .thenReturn("Cannot send this email");

        ResetPasswordException resetPasswordException = assertThrows(ResetPasswordException.class,
            () -> this.resetPasswordMailSender.sendResetPasswordEmail(username, email, resetPasswordUrl));
        verify(this.mailSender).sendAsynchronously(Collections.singleton(message), session, this.mailListener);
        verify(mailStatusResult).waitTillProcessed(1000L);
        assertEquals("Cannot send this email", resetPasswordException.getMessage());
        assertEquals("Some sending error", resetPasswordException.getCause().getMessage());
    }

    @Test
    void sendAuthenticationSecurityEmail() throws MessagingException, ResetPasswordException
    {
        UserReference userReference = mock(UserReference.class);
        String content = "some content";
        String subject = "some subject";

        String fromAdress = "root@xwiki.com";
        when(this.mailSenderConfiguration.getFromAddress()).thenReturn(fromAdress);

        UserProperties userProp = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProp);

        InternetAddress userAddress = new InternetAddress("foo@xwiki.com");
        when(userProp.getEmail()).thenReturn(userAddress);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("from", fromAdress);
        parameters.put("to", userAddress);
        parameters.put("subject", subject);

        MimeMessage message = mock(MimeMessage.class);
        when(this.textMimeMessageFactory.createMessage(content, parameters)).thenReturn(message);
        Session session = Session.getInstance(new Properties());
        when(this.sessionFactory.create(Collections.emptyMap())).thenReturn(session);
        MailStatusResult mailStatusResult = mock(MailStatusResult.class);
        when(this.mailListener.getMailStatusResult()).thenReturn(mailStatusResult);
        MailStatus mailStatus = mock(MailStatus.class);
        when(mailStatusResult.getAll()).thenReturn(Collections.singleton(mailStatus).iterator());
        when(mailStatus.getState()).thenReturn(MailState.SEND_SUCCESS.toString());
        when(mailStatusResult.isProcessed()).thenReturn(true);

        this.resetPasswordMailSender.sendAuthenticationSecurityEmail(userReference, subject, content);

        verify(this.mailSender).sendAsynchronously(Collections.singleton(message), session, this.mailListener);
        verify(mailStatusResult).waitTillProcessed(1000L);
        verify(mailStatusResult).getAllErrors();

    }
}
