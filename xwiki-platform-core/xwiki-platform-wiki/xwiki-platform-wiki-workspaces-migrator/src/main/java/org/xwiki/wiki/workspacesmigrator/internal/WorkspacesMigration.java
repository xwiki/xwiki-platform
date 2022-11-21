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
package org.xwiki.wiki.workspacesmigrator.internal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrator that restores pages previously removed by WorkspaceManager.Install and remove SearchConfigSources added
 * by that script.
 *
 * @since 5.3RC1
 * @version $Id$
 */
@Component
@Named("R530000WorkspacesMigration")
@Singleton
public class WorkspacesMigration extends AbstractHibernateDataMigration
{
    private static final String WORKSPACE_CLASS_SPACE = "WorkspaceManager";

    private static final String WORKSPACE_CLASS_PAGE = "WorkspaceClass";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private DocumentRestorerFromAttachedXAR documentRestorerFromAttachedXAR;

    @Inject
    private SearchSuggestCustomConfigDeleter searchSuggestCustomConfigDeleter;

    @Inject
    private Logger logger;

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Current wiki
        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

        // Delete the search suggest config object
        deleteSearchSuggestCustomConfig(currentWikiId);

        // If the wiki is a workspace
        if (isWorkspace(currentWikiId)) {
            // Restore the documents removed by WorkspaceManager.Install
            restoreDeletedDocuments(currentWikiId);
        }
    }

    private boolean isWorkspace(String wikiId) throws XWikiException
    {
        // The main wiki is not a workspace
        if (wikiId.equals(wikiDescriptorManager.getMainWikiId())) {
            return false;
        }

        // Context, XWiki
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Get the old wiki descriptor
        DocumentReference oldWikiDescriptorReference = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                XWiki.SYSTEM_SPACE, String.format("XWikiServer%s", StringUtils.capitalize(wikiId)));
        XWikiDocument oldWikiDescriptor = xwiki.getDocument(oldWikiDescriptorReference, context);

        // Try to get the old workspace object
        DocumentReference oldClassDocument = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                WORKSPACE_CLASS_SPACE, WORKSPACE_CLASS_PAGE);
        BaseObject oldObject = oldWikiDescriptor.getXObject(oldClassDocument);

        return (oldObject != null) || isWorkspaceTemplate(wikiId);
    }

    private boolean isWorkspaceTemplate(String wikiId) throws XWikiException
    {
        // Context, XWiki
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // In the first version of the Workspace Application, workspacetemplate did not have the workspace object.
        // We test for the existence of XWiki.ManageWorkspace just to be sure that the workspacetemplate is a workspace.
        return wikiId.equals("workspacetemplate") && xwiki.exists(new DocumentReference(wikiId,
                "XWiki", "ManageWorkspace"), context);
    }

    @Override
    public String getDescription()
    {
        return "https://jira.xwiki.org/browse/XWIKI-9738";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // XWiki 5.3, migration.
        return new XWikiDBVersion(53000);
    }

    /**
     * The WorkspaceManager.Install script has added a new XWiki.SearchSuggestSourceClass object that we need to remove.
     * If we don't remove it, it will cause a conflict in DW since we have added a new object in the standard
     * distribution (see https://jira.xwiki.org/browse/XWIKI-9697).
     *
     * @param wikiId id of the wiki where the config should be removed
     */
    private void deleteSearchSuggestCustomConfig(String wikiId) throws XWikiException
    {
        searchSuggestCustomConfigDeleter.deleteSearchSuggestCustomConfig(wikiId);
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
    private void restoreDeletedDocuments(String wikiId)
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        // Create the list of documents to restore
        List<DocumentReference> documentsToRestore = new LinkedList<DocumentReference>();
        documentsToRestore.add(new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "AdminRegistrationSheet"));
        documentsToRestore.add(new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "RegistrationConfig"));
        documentsToRestore.add(new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "RegistrationHelp"));
        documentsToRestore.add(new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "AdminUsersSheet"));

        // Remove from the list the document that already exists (so we don't need to restore them)
        Iterator<DocumentReference> itDocumentsToRestore = documentsToRestore.iterator();
        while (itDocumentsToRestore.hasNext()) {
            DocumentReference docRef = itDocumentsToRestore.next();
            try {
                if (xwiki.exists(docRef, xcontext)) {
                    itDocumentsToRestore.remove();
                }
            } catch (XWikiException e) {
                this.logger.error("Failed to test the existence of document with reference [{}]", docRef, e);
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
            String documentsToRestoreAsString = new String();
            int counter = 0;
            for (DocumentReference d : documentsToRestore) {
                if (counter++ > 0) {
                    documentsToRestoreAsString += ", ";
                }
                documentsToRestoreAsString += d;
            }
            logger.warn("Failed to restore some documents: [{}]. You should import manually "
                    + "(1) xwiki-platform-administration-ui.xar and then (2) xwiki-platform-wiki-ui-wiki.xar into your"
                    + " wiki, to restore these documents.", documentsToRestoreAsString);
        }
    }

    private void restoreDocumentsFromWorkspaceXar(List<DocumentReference> documentsToRestore)
    {
        DocumentReference installDocumentReference = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                WORKSPACE_CLASS_SPACE, "Install");
        try {
            documentRestorerFromAttachedXAR.restoreDocumentFromAttachedXAR(installDocumentReference,
                    "workspace-template.xar", documentsToRestore);
        } catch (XWikiException e) {
            logger.error("Error while restoring documents from the Workspace XAR", e);
        }
    }

    private void restoreDocumentFromMainWiki(List<DocumentReference> documentsToRestore)
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();
        
        WikiReference mainWikiReference = new WikiReference(wikiDescriptorManager.getMainWikiId());
                
        Iterator<DocumentReference> itDocumentsToRestore = documentsToRestore.iterator();
        while (itDocumentsToRestore.hasNext()) {
            DocumentReference docRef = itDocumentsToRestore.next();

            // Get the corresponding doc in the main wiki
            DocumentReference mainDocRef = docRef.setWikiReference(mainWikiReference);

            try {
                // If the document exists in the main wiki, copy it
                if (xwiki.exists(mainDocRef, xcontext)) {
                    xwiki.copyDocument(mainDocRef, docRef, xcontext);
                    itDocumentsToRestore.remove();
                }
            } catch (XWikiException e) {
                logger.error("Failed to copy [{}] to [{}].", mainDocRef, docRef, e);
            }
        }
    }
}
