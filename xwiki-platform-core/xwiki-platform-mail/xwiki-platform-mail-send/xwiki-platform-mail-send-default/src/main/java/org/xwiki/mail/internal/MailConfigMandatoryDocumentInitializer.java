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
package org.xwiki.mail.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.internal.configuration.AbstractMailConfigClassDocumentConfigurationSource;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.internal.DocumentInitializerRightsManager;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.mail.internal.configuration.AbstractMailConfigClassDocumentConfigurationSource.MAIL_SPACE;
import static org.xwiki.mail.internal.configuration.AbstractSendMailConfigClassDocumentConfigurationSource.SENDMAILCONFIGCLASS_REFERENCE;

/**
 * Create the mandatory {@code Mail.MailConfig} document in the current wiki.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
@Named("Mail.MailConfig")
public class MailConfigMandatoryDocumentInitializer implements MandatoryDocumentInitializer
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentInitializerRightsManager documentInitializerRightsManager;

    @Override
    public EntityReference getDocumentReference()
    {
        return AbstractMailConfigClassDocumentConfigurationSource.MAILCONFIG_REFERENCE;
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = updateReferences(document);

        // Ensure the document is hidden, like every technical document
        if (!document.isHidden()) {
            document.setHidden(true);
            needsUpdate = true;
        }

        // Set the title
        if (document.getTitle().isEmpty()) {
            document.setTitle("Mail Configuration");
            needsUpdate = true;
        }

        // Ensure the document has a Mail.GeneralMailConfigClass xobject
        if (addGeneralMailConfigClassXObject(document)) {
            needsUpdate = true;
        }

        // Ensure the document has a Mail.SendMailConfigClass xobject
        if (addSendMailConfigClassXObject(document)) {
            needsUpdate = true;
        }

        // Migration: Since the XWiki.Configurable class used to be located in Mail.MailConfig and has been moved to
        // Mail.MailConfigurable, we need to remove it from this page in case it's there as otherwise there would be
        // duplicate Admin UI entries...
        // TODO: Ideally this code should be executed only once.
        if (removeConfigurableClass(document)) {
            needsUpdate = true;
        }

        if (this.documentInitializerRightsManager.restrictToAdmin(document)) {
            needsUpdate = true;
        }

        return needsUpdate;
    }

    private boolean updateReferences(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Ensure the document has a creator
        if (document.getAuthors().getCreator() == GuestUserReference.INSTANCE) {
            document.getAuthors().setCreator(SuperAdminUserReference.INSTANCE);
            needsUpdate = true;
        }

        // Ensure the document has an author
        if (document.getAuthors().getEffectiveMetadataAuthor() == GuestUserReference.INSTANCE) {
            document.getAuthors().setEffectiveMetadataAuthor(document.getAuthors().getCreator());
            needsUpdate = true;
        }

        // Ensure the document has a parent
        if (document.getParentReference() == null) {
            LocalDocumentReference parentReference = new LocalDocumentReference(MAIL_SPACE, "WebHome");
            document.setParentReference(parentReference);
            needsUpdate = true;
        }

        return needsUpdate;
    }

    private boolean removeConfigurableClass(XWikiDocument document)
    {
        boolean needsUpdate = false;

        LocalDocumentReference configurableClassReference =
            new LocalDocumentReference(XWiki.SYSTEM_SPACE, "ConfigurableClass");
        if (document.getXObject(configurableClassReference) != null) {
            document.removeXObjects(configurableClassReference);
            needsUpdate = true;
        }

        return needsUpdate;
    }

    private boolean addGeneralMailConfigClassXObject(XWikiDocument document)
    {
        return addXObject(document, new LocalDocumentReference(MAIL_SPACE, "GeneralMailConfigClass"),
            Map.of("obfuscateEmailAddresses", "obfuscate"));
    }

    private boolean addSendMailConfigClassXObject(XWikiDocument document)
    {
        return addXObject(document, SENDMAILCONFIGCLASS_REFERENCE, Map.of(
            "admin_email", "from",
            "smtp_server", "host",
            "smtp_port", "port",
            "smtp_server_username", "username",
            "smtp_server_password", "password",
            "javamail_extra_props", "properties"
        ));
    }

    private boolean addXObject(XWikiDocument document, LocalDocumentReference classReference,
        Map<String, String> fieldMappings)
    {
        boolean needsUpdate = false;

        if (document.getXObject(classReference) == null) {
            try {
                XWikiContext xwikiContext = this.xcontextProvider.get();
                XWiki xwiki = xwikiContext.getWiki();
                BaseObject object = document.newXObject(classReference, xwikiContext);
                // Backward compatibility: migrate data from XWiki.XWikiPreferences.
                // Only set values if they're different from the default so that values are inherited by default
                for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                    setField(entry.getKey(), entry.getValue(), object, xwiki, xwikiContext);
                }
                needsUpdate = true;
            } catch (XWikiException e) {
                this.logger.error(
                    String.format("Error adding a [%s] object to the document [%s]",
                        classReference.toString(), document.getDocumentReference().toString()));
            }
        }

        return needsUpdate;
    }

    private void setField(String oldPropertyName, String newPropertyName, BaseObject object, XWiki xwiki,
        XWikiContext xwikiContext)
    {
        String oldValue = xwiki.getSpacePreference(oldPropertyName, xwikiContext);
        if (!StringUtils.isEmpty(oldValue)) {
            object.set(newPropertyName, oldValue, xwikiContext);
        }
    }
}
