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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.mail.internet.InternetAddress;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.security.authentication.api.AuthenticationResourceReference;
import org.xwiki.security.authentication.api.ResetPasswordException;
import org.xwiki.security.authentication.api.ResetPasswordManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;

public class DefaultResetPasswordManager implements ResetPasswordManager
{
    private static final LocalDocumentReference LDAP_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "LDAPProfileClass");

    private static final LocalDocumentReference RESET_PASSWORD_REQUEST_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "ResetPasswordRequestClass");

    private static final LocalDocumentReference USER_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private UserManager userManager;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> resourceReferenceSerializer;

    @Inject
    private ResetPasswordMailSender resetPasswordMailSender;

    private void checkUserReference(UserReference userReference) throws ResetPasswordException
    {
        if (!this.userManager.exists(userReference)) {
            String exceptionMessage =
                this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.noUser",
                    userReference.toString());
            throw new ResetPasswordException(exceptionMessage);
        }

        // FIXME: This check shouldn't be needed if we'd have the proper API to determine which kind of
        // authentication is used.
        if (!(userReference instanceof DocumentUserReference)) {
            throw new ResetPasswordException("Only user having a page on the wiki can reset their password.");
        }
    }

    @Override
    public InternetAddress requestResetPassword(UserReference userReference) throws ResetPasswordException
    {
        this.checkUserReference(userReference);

        UserProperties userProperties = this.userPropertiesResolver.resolve(userReference);
        InternetAddress email = userProperties.getEmail();

        if (email == null) {
            String exceptionMessage =
                this.localizationManager.getTranslationPlain("xe.admin.passwordReset.error.noEmail");
            throw new ResetPasswordException(exceptionMessage);
        }

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

            BaseObject xObject = userDocument.getXObject(RESET_PASSWORD_REQUEST_CLASS_REFERENCE, true, context);
            String verificationCode = context.getWiki().generateRandomString(30);
            xObject.set("verification", verificationCode, context);

            String saveComment =
                this.localizationManager.getTranslationPlain("xe.admin.passwordReset.versionComment");
            context.getWiki().saveDocument(userDocument, saveComment, true, context);

            AuthenticationResourceReference resourceReference =
                new AuthenticationResourceReference(
                    AuthenticationResourceReference.AuthenticationAction.RESET_PASSWORD);
            resourceReference.addParameter("u", this.referenceSerializer.serialize(reference));
            resourceReference.addParameter("v", verificationCode);
            ExtendedURL extendedURL = this.resourceReferenceSerializer.serialize(resourceReference);

            URL serverURL = context.getURLFactory().getServerURL(context);
            URL externalVerificationURL = new URL(serverURL, extendedURL.serialize());

            this.resetPasswordMailSender.sendResetPasswordEmail(reference.getName(), email, externalVerificationURL);
            return email;
        } catch (XWikiException | SerializeResourceReferenceException | UnsupportedResourceReferenceException
            | MalformedURLException e) {
            throw new ResetPasswordException("Unknown error", e);
        }
    }

    @Override
    public String checkVerificationCode(UserReference userReference, String verificationCode, boolean reset)
        throws ResetPasswordException
    {
        this.checkUserReference(userReference);
        XWikiContext context = this.contextProvider.get();

        DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
        DocumentReference reference = documentUserReference.getReference();
        String exceptionMessage =
            this.localizationManager.getTranslationPlain("xe.admin.passwordReset.step2.error.wrongParameters",
                userReference.toString());

        try {
            XWikiDocument userDocument = context.getWiki().getDocument(reference, context);
            BaseObject xObject = userDocument.getXObject(RESET_PASSWORD_REQUEST_CLASS_REFERENCE);
            if (xObject == null) {
                throw new ResetPasswordException(exceptionMessage);
            }

            String storedVerificationCode = xObject.getStringValue("verification");
            BaseClass xClass = xObject.getXClass(context);
            PropertyInterface verification = xClass.get("verification");
            if (!(verification instanceof PasswordClass)) {
                throw new ResetPasswordException("Bad definition of ResetPassword XClass.");
            }
            PasswordClass passwordClass = (PasswordClass) verification;
            String equivalentPassword =
                passwordClass.getEquivalentPassword(storedVerificationCode, verificationCode);
            if (!verificationCode.equals(equivalentPassword)) {
                throw new ResetPasswordException(exceptionMessage);
            }
            if (reset) {
                String newVerificationCode = context.getWiki().generateRandomString(30);
                xObject.set("verification", newVerificationCode, context);
                String saveComment = this.localizationManager
                    .getTranslationPlain("xe.admin.passwordReset.step2.versionComment.changeValidationKey");
                context.getWiki().saveDocument(userDocument, saveComment, true, context);
                return saveComment;
            } else {
                return null;
            }
        } catch (XWikiException e) {
            throw new ResetPasswordException("Cannot open user document.", e);
        }
    }

    @Override
    public void resetPassword(UserReference userReference, String newPassword)
        throws ResetPasswordException
    {
        this.checkUserReference(userReference);
        XWikiContext context = this.contextProvider.get();

        DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
        DocumentReference reference = documentUserReference.getReference();

        try {
            XWikiDocument userDocument = context.getWiki().getDocument(reference, context);
            userDocument.removeXObjects(RESET_PASSWORD_REQUEST_CLASS_REFERENCE);
            BaseObject userXObject = userDocument.getXObject(USER_CLASS_REFERENCE);
            userXObject.setStringValue("password", newPassword);

            String saveComment = this.localizationManager.getTranslationPlain(
                "xe.admin.passwordReset.step2.versionComment.passwordReset");
            context.getWiki().saveDocument(userDocument, saveComment, true, context);
        } catch (XWikiException e) {
            throw new ResetPasswordException("Cannot open user document.", e);
        }
    }
}
