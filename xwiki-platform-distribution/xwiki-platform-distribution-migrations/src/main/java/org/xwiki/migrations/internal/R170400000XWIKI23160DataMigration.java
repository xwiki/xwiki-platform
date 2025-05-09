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
package org.xwiki.migrations.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

import static com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE;
import static org.xwiki.migrations.internal.XWikiPropertiesMetaFieldCleanupTaskConsumer.TASK_NAME;

/**
 * Sends {@link XWikiPreferencesDocumentInitializer#LOCAL_REFERENCE} to the {@link TaskManager} queue with task id
 * {@link XWikiPropertiesMetaFieldCleanupTaskConsumer#TASK_NAME} if the content of the {@code meta} field matches the
 * one from the XAR. Then, {@link XWikiPropertiesMetaFieldCleanupTaskConsumer} takes care of emptying the {@code meta}
 * field.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named("R170400000XWIKI23160")
@Singleton
public class R170400000XWIKI23160DataMigration extends AbstractHibernateDataMigration
{

    private static final String META_FIELD = "meta";

    /**
     * The set of expected ids for the default distribution xar files containing {@code XWiki.XWikiPreferences}.
     */
    private static final Set<String> DISTRIBUTION_XAR_IDS = Set.of(
        // Id in recent distributions
        "org.xwiki.platform:xwiki-platform-distribution-ui-base",
        // Id in older distributions
        "org.xwiki.platform:xwiki-platform-distribution-flavor"
    );

    @Inject
    private Packager packager;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private TaskManager taskManager;

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(170400000);
    }

    @Override
    public String getDescription()
    {
        return "Cleanup XWikiProperties fields used as templates when they match the default values.";
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // TODO: confirmation_email_content and validation_email_content fields also need to be migrated, see
        //  XWIKI-23164
        XWikiContext xWikiContext = getXWikiContext();
        XWiki wiki = xWikiContext.getWiki();
        // Defining the root local is important to have entity equality when retrieving the version from the XAR.
        DocumentReference xwikiPreferencesDocumentReference =
            new DocumentReference(new LocalDocumentReference(LOCAL_REFERENCE, Locale.ROOT),
                xWikiContext.getWikiReference());
        XWikiDocument xwikiPreferencesDocument = wiki.getDocument(xwikiPreferencesDocumentReference, xWikiContext);
        XWikiDocument xwikiPreferencesDocumentFromXar = loadFromXar(xwikiPreferencesDocumentReference);
        if (xwikiPreferencesDocumentFromXar != null) {
            runMigration(xWikiContext, xwikiPreferencesDocument, xwikiPreferencesDocumentFromXar);
        }
    }

    private void runMigration(XWikiContext xWikiContext, XWikiDocument xwikiPreferencesDocument,
        XWikiDocument xwikiPreferencesDocumentFromXar)
    {
        WikiReference wikiReference = xWikiContext.getWikiReference();
        BaseObject xwikiPreferencesXObject = xwikiPreferencesDocument.getXObject(LOCAL_REFERENCE);
        BaseObject xwikiPreferencesXObjectFromXar = xwikiPreferencesDocumentFromXar.getXObject(LOCAL_REFERENCE);
        if (xwikiPreferencesXObject != null && xwikiPreferencesXObjectFromXar != null) {
            String template = xwikiPreferencesXObject.getStringValue(META_FIELD);
            if (StringUtils.isNotEmpty(template)) {
                String templateFromXar = xwikiPreferencesXObjectFromXar.getStringValue(META_FIELD);
                if (template.equals(templateFromXar)) {
                    this.taskManager.addTask(wikiReference.getName(), xwikiPreferencesDocument.getId(), TASK_NAME);
                }
            }
        }
    }

    private XWikiDocument loadFromXar(DocumentReference xwikiPreferencesDocumentReference) throws DataMigrationException
    {
        // We need to cast to be able to access to the XAR-specific 'getXarInstalledExtensions' method.
        if (this.installedExtensionRepository
            instanceof XarInstalledExtensionRepository xarInstalledExtensionRepository)
        {
            Collection<XarInstalledExtension> xarInstalledExtensions =
                xarInstalledExtensionRepository.getXarInstalledExtensions(xwikiPreferencesDocumentReference);
            XarInstalledExtension xarInstalledExtension = xarInstalledExtensions.stream()
                .filter(it -> DISTRIBUTION_XAR_IDS.contains(it.getId().getId()))
                .findFirst()
                .orElse(null);
            if (xarInstalledExtension != null) {
                ExtensionId extensionId = xarInstalledExtension.getId();
                try {
                    XarEntry localDocumentReference =
                        new XarEntry(xwikiPreferencesDocumentReference.getLocalDocumentReference(),
                            "XWiki/XWikiPreferences.xml");
                    return this.packager.getXWikiDocument(xwikiPreferencesDocumentReference.getWikiReference(),
                        localDocumentReference, xarInstalledExtension);
                } catch (IOException | XarException e) {
                    throw new DataMigrationException(
                        "Failed to load [%s] from extension [%s]".formatted(xwikiPreferencesDocumentReference,
                            extensionId), e);
                }
            } else {
                // No matching extension found
                return null;
            }
        } else {
            throw new DataMigrationException(
                "The resolved [%s] component cannot be cast to [%s]".formatted(InstalledExtensionRepository.class,
                    XarInstalledExtensionRepository.class));
        }
    }
}
