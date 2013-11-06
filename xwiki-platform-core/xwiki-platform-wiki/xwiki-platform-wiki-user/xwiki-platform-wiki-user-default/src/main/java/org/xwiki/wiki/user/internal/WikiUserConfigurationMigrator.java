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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserConfiguration;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigration;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migrator to convert all workspaces (WorkspaceManager.WorkspaceClass) to new WikiUserConfiguration objects.
 *
 * @version $Id$
 */
@Component
@Named("R530000WikiUserConfigurationMigrator")
public class WikiUserConfigurationMigrator implements DataMigration
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String getName()
    {
        return "Workspaces to new wiki API migrator (JIRA XWIKI-9516)";
    }

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

    private void migrateFromOldWorkspace(XWikiDocument document) throws DataMigrationException
    {
        // Try to get the old workspace object
        DocumentReference oldClassDocument = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                "WorkspaceManager", "WorkspaceClass");
        BaseObject oldObject = document.getXObject(oldClassDocument);

        // --
        // The wiki is not a workspace
        // --
        if (oldObject == null) {
            return;
        }

        // --
        // The wiki is a workspace
        // --
        // No local users
        WikiUserConfiguration configuration = new WikiUserConfiguration();
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
        configuration.setMembershypType(membershipType);

        // Wiki Id
        String wikiId = document.getDocumentReference().getName().replaceAll("XWikiServer", "").toLowerCase();
        // Save the new configuration
        try {
            wikiUserConfigurationHelper.saveConfiguration(configuration, wikiId);
        } catch (WikiUserManagerException e) {
            throw new DataMigrationException(String.format(
                    "Fail to save the new wiki user configuration page for wiki [%s].", wikiId), e);
        }

        // Delete the old object
        document.removeXObject(oldObject);

        // Save the document
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        try {
            xwiki.saveDocument(document, "Remove the old WorkspaceManager.WorkspaceClass object.", context);
        } catch (XWikiException e) {
            throw new DataMigrationException(String.format(
                    "Fail to save the document [%s] to remove the WorkspaceManager.WorkspaceClass object.",
                    document.getDocumentReference().toString()), e);
        }
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        String xwql = "SELECT XWikiDocument doc FROM doc.object(WorkspaceManager.WorkspaceClass) "
                + "obj WHERE doc.space = :space";
        try {
            Query query = queryManager.createQuery(xwql, Query.XWQL).bindValue("space", XWiki.SYSTEM_SPACE);
            List<XWikiDocument> documents = query.execute();
            for (XWikiDocument document: documents) {
                migrateFromOldWorkspace(document);
            }
        } catch (QueryException e) {
            throw new DataMigrationException("Fail get yhe list of all the workspace objects.", e);
        }
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return true;
    }
}
