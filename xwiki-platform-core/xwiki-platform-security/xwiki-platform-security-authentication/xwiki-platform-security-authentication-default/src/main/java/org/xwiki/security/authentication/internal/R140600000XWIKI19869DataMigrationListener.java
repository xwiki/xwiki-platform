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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.ResetPasswordManager;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Listener in charge of checking if a data migration file is there and ask users to reset their passwords.
 *
 * @version $Id$
 * @since 14.6RC1
 * @since 14.4.3
 * @since 13.10.8
 */
@Component
@Singleton
@Named(R140600000XWIKI19869DataMigrationListener.NAME)
public class R140600000XWIKI19869DataMigrationListener extends AbstractEventListener
{
    static final String NAME = "R140600000XWIKI19869DataMigrationListener";
    private static final List<Event> EVENT_LIST = Collections.singletonList(new ApplicationReadyEvent());

    private static final String FILENAME = "140600000XWIKI19869DataMigration-users.txt";
    private static final String MAIL_TEMPLATE = "140600000XWIKI19869-mail.txt";
    private static final String SUBJECT_MARKER = "subject:";

    @Inject
    private Provider<ResetPasswordManager> resetPasswordManagerProvider;

    @Inject
    private Provider<AuthenticationMailSender> resetPasswordMailSenderProvider;

    @Inject
    private Provider<UserReferenceResolver<String>> userReferenceResolverProvider;

    @Inject
    private Provider<ContextualLocalizationManager> contextualLocalizationManagerProvider;

    @Inject
    private Provider<UserPropertiesResolver> userPropertiesResolver;

    @Inject
    @Named("xwikiproperties")
    private Provider<ConfigurationSource> propertiesConfigurationProvider;

    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public R140600000XWIKI19869DataMigrationListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ConfigurationSource configurationSource = this.propertiesConfigurationProvider.get();
        boolean sendSecurityEmail =
            configurationSource.getProperty("security.migration.R140600000XWIKI19869.sendSecurityEmail", true);
        boolean sendResetPasswordEmail =
            configurationSource.getProperty("security.migration.R140600000XWIKI19869.sendResetPasswordEmail", true);
        File migrationFile = new File(this.environment.getPermanentDirectory(), FILENAME);
        if ((sendSecurityEmail || sendResetPasswordEmail) && Files.exists(migrationFile.toPath())) {
            handleMigrationFile(migrationFile, sendSecurityEmail, sendResetPasswordEmail);
        }
    }

    private void handleMigrationFile(File migrationFile, boolean sendSecurityEmail, boolean sendResetPasswordEmail)
    {
        try {
            List<String> serializedReferences = Files.readAllLines(migrationFile.toPath());

            // We use a set in case there was same reference multiple times in the file (could happen if the migration
            // was restarted)
            Set<String> exploredReference = new HashSet<>();

            Pair<String, String> mailData = this.getMailData();
            for (String serializedReference : serializedReferences) {
                if (!exploredReference.contains(serializedReference)) {
                    UserReference userReference = this.userReferenceResolverProvider.get().resolve(serializedReference);
                    UserProperties userProperties = this.userPropertiesResolver.get().resolve(userReference);
                    if (userProperties.getEmail() != null) {
                        this.handleResetPassword(userReference, mailData, sendSecurityEmail, sendResetPasswordEmail);
                    } else {
                        this.logger.warn("Reset email cannot be sent for user [{}] as no email address is provided.",
                            userReference);
                    }

                    exploredReference.add(serializedReference);
                }
            }

            Files.delete(migrationFile.toPath());
        } catch (IOException e) {
            this.logger.warn("Error while trying to read the data migration file to ask user to reset their password"
                + " the root cause error was [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void handleResetPassword(UserReference userReference, Pair<String, String> mailData,
        boolean sendSecurityEmail, boolean sendResetPasswordEmail)
    {
        ResetPasswordManager resetPasswordManager = this.resetPasswordManagerProvider.get();
        try {
            ResetPasswordRequestResponse resetPasswordRequestResponse =
                resetPasswordManager.requestResetPassword(userReference);
            if (!StringUtils.isEmpty(resetPasswordRequestResponse.getVerificationCode())) {
                if (sendSecurityEmail) {
                    this.resetPasswordMailSenderProvider.get().sendAuthenticationSecurityEmail(userReference,
                        mailData.getLeft(), mailData.getRight());
                }
                if (sendResetPasswordEmail) {
                    resetPasswordManager.sendResetPasswordEmailRequest(resetPasswordRequestResponse);
                }
            }
        } catch (ResetPasswordException e) {
            this.logger.warn("Error when handling user [{}] for sending security and/or reset password email: [{}]",
                userReference,
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stack trace for the reset password request: ", e);
        }
    }

    private Pair<String, String> getMailData()
    {
        File mailTemplate = new File(this.environment.getPermanentDirectory(), MAIL_TEMPLATE);
        Pair<String, String> result = this.getMailDataFallback();
        if (Files.exists(mailTemplate.toPath())) {
            try {
                List<String> mailLines = Files.readAllLines(mailTemplate.toPath());
                result = getMailData(mailLines, result);
            } catch (IOException e) {
                this.logger.warn("Error while trying to read the security email template, "
                    + "fallback on default mail subject and content. Root cause error: [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return result;
    }

    private Pair<String, String> getMailData(List<String> mailLines, Pair<String, String> fallback)
    {
        Pair<String, String> result = fallback;
        if (!mailLines.isEmpty()) {
            String subject;
            if (mailLines.get(0).toLowerCase().startsWith(SUBJECT_MARKER)) {
                subject = mailLines.remove(0).substring(SUBJECT_MARKER.length());
            } else {
                subject = fallback.getLeft();
            }

            String content = String.join("\n", mailLines);
            result = Pair.of(subject, content);
        }
        return result;
    }

    private Pair<String, String> getMailDataFallback()
    {
        ContextualLocalizationManager contextualLocalizationManager = this.contextualLocalizationManagerProvider.get();
        String translationPrefix = "security.authentication.migration1400600000XWIKI19869.email.";
        String subject = contextualLocalizationManager.getTranslationPlain(translationPrefix + "subject");
        String content = contextualLocalizationManager.getTranslationPlain(translationPrefix + "content");
        return Pair.of(subject, content);
    }
}
