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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authentication.ResetPasswordException;

import com.xpn.xwiki.XWikiContext;

/**
 * Component dedicated to send reset password information by email.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component(roles = ResetPasswordMailSender.class)
@Singleton
public class ResetPasswordMailSender
{
    private static final LocalDocumentReference RESET_PASSWORD_MAIL_TEMPLATE_REFERENCE =
        new LocalDocumentReference("XWiki", "ResetPasswordMailContent");

    @Inject
    private MailSenderConfiguration mailSenderConfiguration;

    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @Inject
    @Named("template")
    private MimeMessageFactory<MimeMessage> mimeMessageFactory;

    @Inject
    private MailSender mailSender;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

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
        XWikiContext context = this.contextProvider.get();
        String fromAddress = this.mailSenderConfiguration.getFromAddress();
        if (StringUtils.isEmpty(fromAddress)) {
            fromAddress = "no-reply@" + context.getRequest().getServerName();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("from", fromAddress);
        parameters.put("to", email);
        parameters.put("language", context.getLocale());
        parameters.put("type", "Reset Password");
        Map<String, String> velocityVariables = new HashMap<>();
        velocityVariables.put("userName", username);
        velocityVariables.put("passwordResetURL", resetPasswordURL.toExternalForm());
        parameters.put("velocityVariables", velocityVariables);

        String localizedError =
            this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.emailFailed");

        MimeMessage message;
        try {
            message =
                this.mimeMessageFactory.createMessage(
                    this.documentReferenceResolver.resolve(RESET_PASSWORD_MAIL_TEMPLATE_REFERENCE), parameters);
        } catch (MessagingException e) {
            throw new ResetPasswordException(localizedError, e);
        }
        MailListener mailListener = this.mailListenerProvider.get();
        this.mailSender.sendAsynchronously(Collections.singleton(message),
            this.sessionFactory.create(Collections.emptyMap()),
            mailListener);

        MailStatusResult mailStatusResult = mailListener.getMailStatusResult();
        mailStatusResult.waitTillProcessed(30L);
        Iterator<MailStatus> mailErrors = mailStatusResult.getAllErrors();

        if (mailErrors != null && mailErrors.hasNext()) {
            MailStatus lastError = mailErrors.next();
            throw new ResetPasswordException(
                String.format("%s - %s", localizedError, lastError.getErrorDescription()));
        }
    }
}
