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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.RetrieveUsernameException;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

/**
 * Component dedicated to send reset password information by email.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component(roles = AuthenticationMailSender.class)
@Singleton
public class AuthenticationMailSender
{
    private static final String XWIKI_SPACE = "XWiki";

    private static final LocalDocumentReference RESET_PASSWORD_MAIL_TEMPLATE_REFERENCE =
        new LocalDocumentReference(XWIKI_SPACE, "ResetPasswordMailContent");

    private static final LocalDocumentReference RETRIEVE_USERNAME_TEMPLATE_REFERENCE =
        new LocalDocumentReference(XWIKI_SPACE, "ForgotUsernameMailContent");

    private static final String NO_REPLY = "no-reply@";
    private static final String FROM = "from";
    private static final String TO = "to";

    @Inject
    private MailSenderConfiguration mailSenderConfiguration;

    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @Inject
    @Named("template")
    private MimeMessageFactory<MimeMessage> mimeMessageFactory;

    @Inject
    @Named("text")
    private MimeMessageFactory<MimeMessage> textMimeMessageFactory;

    @Inject
    private MailSender mailSender;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @Inject
    private Provider<UserPropertiesResolver> userPropertiesResolverProvider;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private Logger logger;

    /**
     * Send the reset password information by email.
     *
     * @param username the name of the user for which to reset the password.
     * @param email the email of the user for which to reset the password.
     * @param resetPasswordURL the URL to use for resetting the password.
     * @throws ResetPasswordException in case of error when preparing or sending the email.
     */
    public void sendResetPasswordEmail(String username, InternetAddress email, URL resetPasswordURL) throws
        ResetPasswordException
    {
        Map<String, Object> velocityVariables = new HashMap<>();
        velocityVariables.put("userName", username);
        velocityVariables.put("passwordResetURL", resetPasswordURL.toExternalForm());

        Map<String, Object> parameters = this.getEmailParameters(email, "Reset Password", velocityVariables);

        try {
            MimeMessage message =
                this.mimeMessageFactory.createMessage(
                    this.documentReferenceResolver.resolve(RESET_PASSWORD_MAIL_TEMPLATE_REFERENCE), parameters);
            if (!this.sendMessage(message)) {
                this.logger.debug("The reset password mail to [{}] has not been sent after 1 second, check the status "
                    + "in the administration", username);
            }
        } catch (MessagingException e) {
            throw new ResetPasswordException(
                this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.emailFailed"), e);
        }
    }

    /**
     * Allows to send a text email for security purpose.
     *
     * @param userReference the user to whom to send the email.
     * @param subject the localized subject of the email
     * @param mailContent the localized content of the email
     * @throws ResetPasswordException in case of problem for preparing or sending the email
     * @since 14.6RC1
     * @since 14.4.3
     * @since 13.10.8
     */
    public void sendAuthenticationSecurityEmail(UserReference userReference, String subject, String mailContent)
        throws ResetPasswordException
    {
        XWikiContext context = this.contextProvider.get();
        String fromAddress = this.mailSenderConfiguration.getFromAddress();
        if (StringUtils.isEmpty(fromAddress)) {
            fromAddress = NO_REPLY + context.getRequest().getServerName();
        }
        UserProperties userProperties = this.userPropertiesResolverProvider.get().resolve(userReference);
        InternetAddress email = userProperties.getEmail();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM, fromAddress);
        parameters.put(TO, email);
        parameters.put("subject", subject);

        String localizedError =
            this.localizationManager.getTranslationPlain("security.authentication.security.email.error");

        try {
            MimeMessage message = this.textMimeMessageFactory.createMessage(mailContent, parameters);
            if (!this.sendMessage(message)) {
                this.logger.info("The security mail to [{}] has not been sent after 1 second, check the status in the "
                    + "administration of the wiki.", userReference);
            }
        } catch (MessagingException e) {
            throw new ResetPasswordException(localizedError, e);
        }
    }

    /**
     * Send an email containing username information related to the given user references.
     *
     * @param email the email of the user who lost their username
     * @param userReferences the list of user references attached to that email
     * @throws RetrieveUsernameException in case of problem for sending the email
     * @since 14.9
     * @since 14.4.6
     * @since 13.10.10
     */
    public void sendRetrieveUsernameEmail(InternetAddress email, Set<UserReference> userReferences)
        throws RetrieveUsernameException
    {
        Map<String, Object> velocityVariables = new HashMap<>();
        List<String> usernames = userReferences.stream()
            .map(this.userReferenceSerializer::serialize)
            .collect(Collectors.toList());
        velocityVariables.put("usernames", usernames);

        Map<String, Object> parameters = this.getEmailParameters(email, "Forgot Username", velocityVariables);

        try {
            MimeMessage message =
                this.mimeMessageFactory.createMessage(
                    this.documentReferenceResolver.resolve(RETRIEVE_USERNAME_TEMPLATE_REFERENCE), parameters);
            this.sendMessage(message);
        } catch (MessagingException e) {
            throw new RetrieveUsernameException(
                this.localizationManager.getTranslationPlain("xe.admin.forgotUsername.error.emailFailed"), e);
        }
    }

    private Map<String, Object> getEmailParameters(InternetAddress email, String type,
        Map<String, Object> velocityVariables)
    {
        XWikiContext context = this.contextProvider.get();
        String fromAddress = this.mailSenderConfiguration.getFromAddress();
        if (StringUtils.isEmpty(fromAddress)) {
            fromAddress = NO_REPLY + context.getRequest().getServerName();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(FROM, fromAddress);
        parameters.put(TO, email);
        parameters.put("language", this.contextProvider.get().getLocale());
        parameters.put("type", type);
        parameters.put("velocityVariables", velocityVariables);

        return parameters;
    }

    /**
     * Send the given message and throw an exception with the localized error in case of mail errors.
     *
     * @param message the message to be sent
     * @return {@code true} only if the mail state is {@link MailState#SEND_SUCCESS}.
     * @throws MessagingException in case of mail errors.
     */
    private boolean sendMessage(MimeMessage message) throws MessagingException
    {
        MailListener mailListener = this.mailListenerProvider.get();
        this.mailSender.sendAsynchronously(Collections.singleton(message),
            this.sessionFactory.create(Collections.emptyMap()),
            mailListener);

        MailStatusResult mailStatusResult = mailListener.getMailStatusResult();
        mailStatusResult.waitTillProcessed(1000L);
        Iterator<MailStatus> mailErrors = mailStatusResult.getAllErrors();

        if (mailErrors != null && mailErrors.hasNext()) {
            MailStatus lastError = mailErrors.next();
            throw new MessagingException(lastError.getErrorDescription());
        }

        if (mailStatusResult.isProcessed() && mailStatusResult.getAll().hasNext()) {
            MailStatus mailStatus = mailStatusResult.getAll().next();
            MailState mailState = MailState.parse(mailStatus.getState());
            return (mailState == MailState.SEND_SUCCESS);
        } else {
            return false;
        }
    }
}
