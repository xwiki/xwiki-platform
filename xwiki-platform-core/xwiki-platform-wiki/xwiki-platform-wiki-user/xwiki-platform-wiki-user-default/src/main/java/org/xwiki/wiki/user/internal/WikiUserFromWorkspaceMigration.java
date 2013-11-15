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
package org.xwiki.wiki.user.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserConfiguration;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrator to convert all workspaces (WorkspaceManager.WorkspaceClass) to new WikiUserConfiguration objects.
 *
 * @version $Id$
 */
@Component
@Named("R530000WikiUserFromWorkspaceMigration")
public class WikiUserFromWorkspaceMigration extends AbstractHibernateDataMigration
{
    private static final String WORKSPACE_CLASS_SPACE = "WorkspaceManager";

    private static final String WORKSPACE_CLASS_PAGE = "WorkspaceClass";

    @Inject
    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "http://jira.xwiki.org/browse/XWIKI-9516";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // XWiki 5.3, migration.
        return new XWikiDBVersion(53000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // We migrate only subwikis
        return !wikiDescriptorManager.getCurrentWikiId().equals(wikiDescriptorManager.getMainWikiId());
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Context, XWiki
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Current wiki
        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

        // Get the old wiki descriptor
        DocumentReference oldWikiDescriptorReference = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                XWiki.SYSTEM_SPACE, String.format("XWikiServer%s", StringUtils.capitalize(currentWikiId)));
        XWikiDocument oldWikiDescriptor = xwiki.getDocument(oldWikiDescriptorReference, context);

        // Try to get the old workspace object
        DocumentReference oldClassDocument = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                WORKSPACE_CLASS_SPACE, WORKSPACE_CLASS_PAGE);
        BaseObject oldObject = oldWikiDescriptor.getXObject(oldClassDocument);

        // Upgrade depending of the type
        if (oldObject != null) {
            // It's a workspace
            upgradeWorkspace(oldObject, currentWikiId, oldWikiDescriptor);
        } else {
            // It's a regular subwiki
            upgradeRegularSubwiki(currentWikiId);
        }
    }

    private void saveConfiguration(WikiUserConfiguration configuration, String wikiId)
        throws DataMigrationException, XWikiException
    {
        try {
            wikiUserConfigurationHelper.saveConfiguration(configuration, wikiId);
        } catch (WikiUserManagerException e) {
            throw new DataMigrationException(String.format(
                    "Failed to save the new wiki user configuration page for wiki [%s].", wikiId), e);
        }
    }

    private void upgradeRegularSubwiki(String wikiId) throws DataMigrationException, XWikiException
    {
        // Create the new configuration
        WikiUserConfiguration configuration = new WikiUserConfiguration();
        configuration.setUserScope(UserScope.LOCAL_AND_GLOBAL);
        configuration.setMembershipType(MembershipType.INVITE);

        // Save the new configuration
        saveConfiguration(configuration, wikiId);
    }

    private void upgradeWorkspace(BaseObject oldObject, String wikiId, XWikiDocument oldWikiDescriptor)
        throws DataMigrationException, XWikiException
    {
        upgradeWorkspaceConfiguration(oldObject, wikiId, oldWikiDescriptor);

        // We also need to upgrade workspaces candidacies
        try {
            upgradeWorkspaceCandidacies(wikiId);
        } catch (XWikiException e) {
            throw new DataMigrationException("Unable to upgrade candidacies from the old Workspace Application to "
                    + "the new Wiki Application.");
        }

        restoreDeletedDocuments(wikiId);
    }

    /**
     * Convert the old WorkspaceManager.WorkspaceClass objects to the new configuration format.
     *
     * @param oldObject old workspace object
     * @param wikiId id of the wiki to upgrade
     * @param oldWikiDescriptor document that holds the old object
     * @throws DataMigrationException if problems occur
     * @throws XWikiException if problems occur
     */
    private void upgradeWorkspaceConfiguration(BaseObject oldObject, String wikiId,
        XWikiDocument oldWikiDescriptor) throws DataMigrationException, XWikiException
    {
        // Context, XWiki
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Create the new configuration
        WikiUserConfiguration configuration = new WikiUserConfiguration();

        // No local users
        configuration.setUserScope(UserScope.GLOBAL_ONLY);

        // Get the membershipType value
        String membershipTypeValue = oldObject.getStringValue("membershipType");
        MembershipType membershipType;
        try {
            membershipType = MembershipType.valueOf(membershipTypeValue.toUpperCase());
        } catch (Exception e) {
            // Default value
            membershipType = MembershipType.INVITE;
        }
        configuration.setMembershipType(membershipType);

        // Save the new configuration
        saveConfiguration(configuration, wikiId);

        // Delete the old object
        oldWikiDescriptor.removeXObject(oldObject);

        // Save the document
        try {
            xwiki.saveDocument(oldWikiDescriptor, "Remove the old WorkspaceManager.WorkspaceClass object.", context);
        } catch (XWikiException e) {
            throw new DataMigrationException(String.format(
                    "Failed to save the document [%s] to remove the WorkspaceManager.WorkspaceClass object.",
                    oldWikiDescriptor.getDocumentReference().toString()), e);
        }
    }

    /**
     * Convert the old WorkspaceManager.WorkspaceCandidateMemberClass objects to the new candidacies format.
     *
     * @param wikiId id of the wiki to upgrade
     * @throws XWikiException if problems occur
     */
    private void upgradeWorkspaceCandidacies(String wikiId) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        // We need to get the document that holds the candidacies
        DocumentReference candidaciesDocumentReference = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE,
                "XWikiAllGroup");
        XWikiDocument candidaciesDocument = xwiki.getDocument(candidaciesDocumentReference, xcontext);

        // We need to get all the old candidacies
        DocumentReference oldCandidateClassReference = new DocumentReference(wikiId, "XWiki",
                "WorkspaceCandidateMemberClass");
        List<BaseObject> candidacyObjects = candidaciesDocument.getXObjects(oldCandidateClassReference);
        if (candidacyObjects != null) {
            DocumentReference newCandidateClassReference = new DocumentReference(wikiId,
                    WikiCandidateMemberClassInitializer.DOCUMENT_SPACE,
                    WikiCandidateMemberClassInitializer.DOCUMENT_NAME);

            for (BaseObject oldObject : candidacyObjects) {
                // Transform the candidacy to the new class
                int objectNumber = candidaciesDocument.createXObject(newCandidateClassReference, xcontext);
                BaseObject newObject = candidaciesDocument.getXObject(newCandidateClassReference, objectNumber);
                newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE,
                        oldObject.getStringValue("type"));
                newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                        oldObject.getStringValue("status"));
                newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_USER,
                        oldObject.getStringValue("userName"));
                newObject.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT,
                        oldObject.getLargeStringValue("userComment"));
                newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN,
                        oldObject.getStringValue("reviewer"));
                newObject.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT,
                        oldObject.getLargeStringValue("reviewerComment"));
                newObject.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT,
                        oldObject.getLargeStringValue("reviewerPrivateComment"));
                newObject.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION,
                        oldObject.getDateValue("date"));
                newObject.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE,
                        oldObject.getDateValue("resolutionDate"));

                // Remove the old object
                candidaciesDocument.removeXObject(oldObject);
            }

            // Save
            xwiki.saveDocument(candidaciesDocument, "Upgrade candidacies from the old Workspace Application to the "
                    + "new Wiki Application.", xcontext);
        }
    }

    /**
     * The WorkspaceManager.Install script has removed some pages, that we need to restore.
     * - XWiki.AdminRegistrationSheet
     * - XWiki.RegistrationConfig
     * - XWiki.RegistrationHelp
     * - XWiki.AdminUsersSheet
     *
     * @param wikiId id of the wiki to upgrade
     */
    public void restoreDeletedDocuments(String wikiId)
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        // Create the list of documents to restore
        List<DocumentReference> documentsToRestore = new LinkedList<DocumentReference>();
        documentsToRestore.add(new DocumentReference(wikiId, "XWiki", "AdminRegistrationSheet"));
        documentsToRestore.add(new DocumentReference(wikiId, "XWiki", "RegistrationConfig"));
        documentsToRestore.add(new DocumentReference(wikiId, "XWiki", "RegistrationHelp"));
        documentsToRestore.add(new DocumentReference(wikiId, "XWiki", "AdminUsersSheet"));

        // Remove from the list the document that already exists (so we don't need to restore them)
        Iterator<DocumentReference> itDocumentsToRestore = documentsToRestore.iterator();
        while (itDocumentsToRestore.hasNext()) {
            DocumentReference docRef = itDocumentsToRestore.next();
            if (xwiki.exists(docRef, xcontext)) {
                itDocumentsToRestore.remove();
            }
        }

        // If the list is empty, there is nothing to do
        if (documentsToRestore.isEmpty()) {
            return;
        }

        // Try to restore from the workspace-template.xar
        restoreDocumentsFromWorkspaceXar(documentsToRestore);

        // If the list is empty, the job is done
        if (documentsToRestore.isEmpty()) {
            return;
        }

        // Try to copy these documents from the main wiki
        restoreDocumentFromMainWiki(documentsToRestore);

        // If the list is empty, the job is done
        if (!documentsToRestore.isEmpty()) {
            logger.error("Failed to restore some documents: [%s]", documentsToRestore);
        }
    }

    private void restoreDocumentsFromWorkspaceXar(List<DocumentReference> documentsToRestore)
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        try {
            // Get the workspace-template XAR in order to restore these pages.
            DocumentReference installDocumentReference = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                    WORKSPACE_CLASS_SPACE, "Install");
            XWikiDocument installDocument = xwiki.getDocument(installDocumentReference, xcontext);
            if(installDocument.isNew()) {
                logger.warn("WorkspaceManager.Install does not exist");
                return;
            }
            XWikiAttachment xeXar = installDocument.getAttachment("workspace-template.xar");
            if (xeXar == null) {
                logger.warn("WorkspaceManager.Install has no attachment named workspace-template.xar.");
                return;
            }

            // Open the XAR and restore the pages
            ZipArchiveInputStream zip = new ZipArchiveInputStream(xeXar.getContentInputStream(xcontext));

            ZipArchiveEntry zipEntry = zip.getNextZipEntry();
            while (zipEntry != null) {
                // Get the name of the file
                String fileName = zipEntry.getName();

                // Check if it matches with one of the document to restore
                Iterator<DocumentReference> itDocumentsToRestore = documentsToRestore.iterator();
                while (itDocumentsToRestore.hasNext()) {
                    DocumentReference docRef = itDocumentsToRestore.next();

                    // Compute what should be the filename of the document to restore
                    String fileNameToRestore = String.format("%s/%s.xml", docRef.getLastSpaceReference().getName(),
                            docRef.getName());

                    // If the file in the zip is the good one, then we use it to restore the document
                    if (fileName.equals(fileNameToRestore)) {
                        try {
                            // Restore it
                            XWikiDocument docToRestore = xwiki.getDocument(docRef, xcontext);
                            docToRestore.fromXML(zip);
                            xwiki.saveDocument(docToRestore, xcontext);
                            // We have restored this document
                            itDocumentsToRestore.remove();
                        } catch (XWikiException e) {
                            logger.warn("Failed to get [%s] from workspace-template.xar.", docRef);
                        }
                    }
                }
                // Next iteration
                zipEntry = zip.getNextZipEntry();
            }
            zip.close();
        } catch (IOException e) {
            logger.error("Error during the decompression of workspace-template.xar.");
        } catch (XWikiException e) {
            logger.error("Failed to get WorkspaceManager.Install or workspace-template.xar", e);
        }
    }

    private void restoreDocumentFromMainWiki(List<DocumentReference> documentsToRestore)
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        Iterator<DocumentReference> itDocumentsToRestore = documentsToRestore.iterator();
        while (itDocumentsToRestore.hasNext()) {
            DocumentReference docRef = itDocumentsToRestore.next();

            // Get the corresponding doc in the main wiki
            DocumentReference mainDocRef = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                    docRef.getLastSpaceReference().getName(), docRef.getName());

            // If the document exists in the main wiki, copy it
            if (xwiki.exists(mainDocRef, xcontext)) {
                try {
                    xwiki.copyDocument(mainDocRef, docRef, xcontext);
                    itDocumentsToRestore.remove();
                } catch (XWikiException e) {
                    logger.error("Failed to copy [%s] to [%s].", mainDocRef, docRef);
                }
            }
        }
    }
}
