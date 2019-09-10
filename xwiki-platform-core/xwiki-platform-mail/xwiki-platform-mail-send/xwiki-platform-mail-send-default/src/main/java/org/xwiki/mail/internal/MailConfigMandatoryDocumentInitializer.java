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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import liquibase.util.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.internal.configuration.SendMailConfigClassDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public EntityReference getDocumentReference()
    {
        return SendMailConfigClassDocumentConfigurationSource.MAILCONFIG_REFERENCE;
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        if (updateReferences(document)) {
            needsUpdate = true;
        }

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

        return needsUpdate;
    }

    private boolean updateReferences(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Ensure the document has a creator
        if (document.getCreatorReference() == null) {
            document.setCreatorReference(new DocumentReference(this.wikiDescriptorManager.getMainWikiId(),
                    XWiki.SYSTEM_SPACE, "superadmin"));
            needsUpdate = true;
        }

        // Ensure the document has an author
        if (document.getAuthorReference() == null) {
            document.setAuthorReference(document.getCreatorReference());
            needsUpdate = true;
        }

        // Ensure the document has a parent
        if (document.getParentReference() == null) {
            LocalDocumentReference parentReference = new LocalDocumentReference(
                    SendMailConfigClassDocumentConfigurationSource.MAIL_SPACE, "WebHome");
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
        LocalDocumentReference generalMailClassReference = new LocalDocumentReference(
                SendMailConfigClassDocumentConfigurationSource.MAIL_SPACE, "GeneralMailConfigClass");
        return addXObject(document, generalMailClassReference,
            Collections.singletonMap("obfuscateEmailAddresses", "obfuscate"));
    }

    private boolean addSendMailConfigClassXObject(XWikiDocument document)
    {
        LocalDocumentReference sendMailClassReference =
                SendMailConfigClassDocumentConfigurationSource.SENDMAILCONFIGCLASS_REFERENCE;
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put("admin_email", "from");
        fieldMappings.put("smtp_server", "host");
        fieldMappings.put("smtp_port", "port");
        fieldMappings.put("smtp_server_username", "username");
        fieldMappings.put("smtp_server_password", "password");
        fieldMappings.put("javamail_extra_props", "properties");
        return addXObject(document, sendMailClassReference, fieldMappings);
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
