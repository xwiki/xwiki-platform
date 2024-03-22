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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.security.authentication.RegistrationConfiguration;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.ResetPasswordManager;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.internal.document.DocumentUserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;

/**
 * Default implementation of the {@link ResetPasswordManager}.
 * This implementation considers that the given {@link UserReference} are actually {@link DocumentUserReference} since
 * right now it's the only known implementation and the reset password mechanism lies on changing an xobject value in a
 * user a document.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component
@Singleton
public class DefaultResetPasswordManager implements ResetPasswordManager
{
    protected static final String XWIKI_SPACE = "XWiki";

    protected static final LocalDocumentReference LDAP_CLASS_REFERENCE =
        new LocalDocumentReference(XWIKI_SPACE, "LDAPProfileClass");

    protected static final LocalDocumentReference USER_CLASS_REFERENCE =
        new LocalDocumentReference(XWIKI_SPACE, "XWikiUsers");
    protected static final String TOKEN_LIFETIME = "security.authentication.resetPasswordTokenLifetime";

    @Inject
    private UserManager userManager;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> resourceReferenceSerializer;

    @Inject
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> urlNormalizer;

    @Inject
    private UserReferenceSerializer<String> referenceSerializer;

    @Inject
    private Provider<AuthenticationMailSender> resetPasswordMailSenderProvider;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    private RegistrationConfiguration registrationConfiguration;

    @Inject
    private Logger logger;

    private static final class UserInformation
    {
        private boolean userExists;
        private InternetAddress userEmail;
        private UserProperties userProperties;

        boolean canUserResetPassword()
        {
            return this.userExists && userEmail != null;
        }
    }

    private UserInformation getUserInformation(UserReference userReference) throws ResetPasswordException
    {
        // FIXME: This check shouldn't be needed if we'd have the proper API to determine which kind of
        // authentication is used.
        if (!(userReference instanceof DocumentUserReference)) {
            throw new ResetPasswordException("Only user having a page on the wiki can reset their password.");
        }
        UserInformation result = new UserInformation();

        try {
            result.userExists = this.userManager.exists(userReference);
            result.userProperties = this.userPropertiesResolver.resolve(userReference);
            result.userEmail = result.userProperties.getEmail();
            return result;
        } catch (UserException e) {
            throw new ResetPasswordException(String.format("Failed to check if user [%s] exists.", userReference), e);
        }
    }

    @Override
    public ResetPasswordRequestResponse requestResetPassword(UserReference userReference) throws ResetPasswordException
    {
        UserInformation userInformation = this.getUserInformation(userReference);
        if (userInformation.canUserResetPassword()) {
            DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
            DocumentReference reference = documentUserReference.getReference();
            XWikiContext context = this.contextProvider.get();

            try {
                XWikiDocument userDocument = context.getWiki().getDocument(reference, context);

                if (userDocument.getXObject(LDAP_CLASS_REFERENCE) != null) {
                    String exceptionMessage =
                        this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.ldapUser",
                            userReference.toString());
                    throw new ResetPasswordException(exceptionMessage);
                }

                BaseObject xObject = userDocument.getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE,
                    true, context);
                String verificationCode = context.getWiki().generateRandomString(30);
                xObject.set(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD,
                    verificationCode, context);
                xObject.setDateValue(ResetPasswordRequestClassDocumentInitializer.REQUEST_DATE_FIELD, new Date());
                String saveComment =
                    this.localizationManager.getTranslationPlain("xe.admin.passwordReset.versionComment");
                context.getWiki().saveDocument(userDocument, saveComment, true, context);

                return new DefaultResetPasswordRequestResponse(userReference, verificationCode);
            } catch (XWikiException e) {
                throw new ResetPasswordException(
                    "Error when reading user document to perform reset password request.",
                    e);
            }
        } else if (userInformation.userExists && userInformation.userEmail == null) {
            // In case the mail is not configured, we log a message to the admin.
            this.logger.info("User [{}] asked to reset their password, but did not have any email configured.",
                userReference);
        }
        return new DefaultResetPasswordRequestResponse(userReference);
    }

    @Override
    public void sendResetPasswordEmailRequest(ResetPasswordRequestResponse requestResponse)
        throws ResetPasswordException
    {
        UserInformation userInformation = this.getUserInformation(requestResponse.getUserReference());
        if (userInformation.canUserResetPassword()) {
            AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
                this.contextProvider.get().getWikiReference(),
                AuthenticationAction.RESET_PASSWORD);

            UserReference userReference = requestResponse.getUserReference();
            String serializedUserReference = this.referenceSerializer.serialize(userReference);
            // FIXME: this should be provided as part of the User API.
            String formattedName = "";
            if (!StringUtils.isBlank(userInformation.userProperties.getFirstName())) {
                formattedName += userInformation.userProperties.getFirstName();
            }
            if (!StringUtils.isBlank(userInformation.userProperties.getLastName())) {
                if (!StringUtils.isBlank(formattedName)) {
                    formattedName += " ";
                }
                formattedName += userInformation.userProperties.getLastName();
            }
            if (StringUtils.isBlank(formattedName)) {
                formattedName = serializedUserReference;
            }
            resourceReference.addParameter("u", serializedUserReference);
            resourceReference.addParameter("v", requestResponse.getVerificationCode());

            XWikiContext context = contextProvider.get();

            ExtendedURL extendedURL = null;
            try {
                extendedURL = this.resourceReferenceSerializer.serialize(resourceReference);
                extendedURL = this.urlNormalizer.normalize(extendedURL);
                URL serverURL = context.getURLFactory().getServerURL(context);
                URL externalVerificationURL = new URL(serverURL, extendedURL.serialize());

                this.resetPasswordMailSenderProvider.get()
                    .sendResetPasswordEmail(formattedName, userInformation.userEmail, externalVerificationURL);
            } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException
                     | MalformedURLException e) {
                throw new ResetPasswordException("Error when processing information for creating the email.", e);
            }
        }
    }

    private int getTokenLifeTime()
    {
        return this.configurationSource.getProperty(TOKEN_LIFETIME, 60);
    }

    /**
     * Check if the reset password token is expired.
     *
     * @param requestXObject the request xobject to determine if it's expired or not
     * @return {@code true} if the token has expired and cannot be used anymore.
     */
    private boolean isTokenExpired(BaseObject requestXObject)
    {
        int tokenLifeTime = getTokenLifeTime();
        boolean result = false;
        if (tokenLifeTime > 0) {
            Date dateValue =
                requestXObject.getDateValue(ResetPasswordRequestClassDocumentInitializer.REQUEST_DATE_FIELD);
            if (dateValue == null) {
                return true;
            } else {
                Instant saveInstant = dateValue.toInstant();
                Instant now = Instant.now();
                return (saveInstant.plus(tokenLifeTime, ChronoUnit.MINUTES).isBefore(now));
            }
        }
        return result;
    }

    @Override
    public ResetPasswordRequestResponse checkVerificationCode(UserReference userReference, String verificationCode)
        throws ResetPasswordException
    {
        ResetPasswordRequestResponse result = new DefaultResetPasswordRequestResponse(userReference);
        UserInformation userInformation = this.getUserInformation(userReference);
        if (userInformation.canUserResetPassword()) {
            XWikiContext context = this.contextProvider.get();

            DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
            DocumentReference reference = documentUserReference.getReference();
            String exceptionMessage = this.localizationManager
                .getTranslationPlain("security.authentication.resetPassword.error.badParameters");

            try {
                XWikiDocument userDocument = context.getWiki().getDocument(reference, context);
                BaseObject xObject = userDocument.getXObject(ResetPasswordRequestClassDocumentInitializer.REFERENCE);
                if (xObject == null) {
                    throw new ResetPasswordException(exceptionMessage);
                }

                String storedVerificationCode =
                    xObject.getStringValue(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD);
                BaseClass xClass = xObject.getXClass(context);
                PropertyInterface verification =
                    xClass.get(ResetPasswordRequestClassDocumentInitializer.VERIFICATION_FIELD);
                // FIXME: shouldn't we be able to get rid of this check?
                if (!(verification instanceof PasswordClass)) {
                    throw new ResetPasswordException("Bad definition of ResetPassword XClass.");
                }
                PasswordClass passwordClass = (PasswordClass) verification;
                String equivalentPassword =
                    passwordClass.getEquivalentPassword(storedVerificationCode, verificationCode);

                // If the token is expired we remove it right away to avoid any attack.
                if (this.isTokenExpired(xObject)) {
                    this.resetVerificationCode(userDocument, xObject,
                        "security.authentication.resetPassword.tokenExpired");
                    throw new ResetPasswordException(exceptionMessage);
                } else if (!storedVerificationCode.equals(equivalentPassword)) {
                    // If the token is not correct and there's no lifetime duration set, we immediately get rid of it
                    // so any bruteforce is compromised.
                    if (getTokenLifeTime() <= 0) {
                        this.resetVerificationCode(userDocument, xObject,
                            "security.authentication.resetPassword.badToken");
                    }
                    throw new ResetPasswordException(exceptionMessage);
                } else {
                    result = new DefaultResetPasswordRequestResponse(userReference, verificationCode);
                }
            } catch (XWikiException e) {
                throw new ResetPasswordException("Cannot open user document to check verification code.", e);
            }
        }
        return result;
    }

    private void resetVerificationCode(XWikiDocument userDocument, BaseObject xobject, String saveCommentTranslationKey)
        throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        userDocument.removeXObject(xobject);
        String saveComment = this.localizationManager.getTranslationPlain(saveCommentTranslationKey);
        context.getWiki().saveDocument(userDocument, saveComment, true, context);
    }

    @Override
    public void resetPassword(UserReference userReference, String newPassword)
        throws ResetPasswordException
    {
        UserInformation userInformation = this.getUserInformation(userReference);
        if (userInformation.canUserResetPassword()) {
            if (!this.isPasswordCompliantWithRegistrationRules(newPassword)) {
                throw new ResetPasswordException("The provided password is not compliant with the password security "
                    + "rules.");
            }
            XWikiContext context = this.contextProvider.get();

            DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
            DocumentReference reference = documentUserReference.getReference();

            try {
                XWikiDocument userDocument = context.getWiki().getDocument(reference, context);
                userDocument.removeXObjects(ResetPasswordRequestClassDocumentInitializer.REFERENCE);
                BaseObject userXObject = userDocument.getXObject(USER_CLASS_REFERENCE);

                // /!\ We cannot use BaseCollection#setStringValue as it's storing value in plain text.
                userXObject.set("password", newPassword, context);

                String saveComment = this.localizationManager.getTranslationPlain(
                    "xe.admin.passwordReset.step2.versionComment.passwordReset");
                context.getWiki().saveDocument(userDocument, saveComment, true, context);
            } catch (XWikiException e) {
                throw new ResetPasswordException("Cannot open user document to perform reset password.", e);
            }
        }
    }

    @Override
    public boolean isPasswordCompliantWithRegistrationRules(String newPassword)
    {
        int passwordMinimumLength = this.registrationConfiguration.getPasswordMinimumLength();
        boolean result = newPassword.length() >= passwordMinimumLength;

        if (result) {
            for (RegistrationConfiguration.PasswordRules passwordRule
                : this.registrationConfiguration.getPasswordRules()) {
                if (!passwordRule.getPattern().matcher(newPassword).matches()) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }
}
