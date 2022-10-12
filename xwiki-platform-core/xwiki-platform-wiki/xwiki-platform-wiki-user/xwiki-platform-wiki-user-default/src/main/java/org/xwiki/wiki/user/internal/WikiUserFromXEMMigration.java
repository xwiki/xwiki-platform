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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
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
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrator to add a WikiUserConfiguration object on every existing wiki.
 *
 * @version $Id$
 * @since 5.3RC1
 */
@Component
@Named("R530010WikiUserFromXEMMigration")
@Singleton
public class WikiUserFromXEMMigration extends AbstractHibernateDataMigration
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
        return "https://jira.xwiki.org/browse/XWIKI-9516";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // XWiki 5.3, migration, to execute after xwiki-platform-wiki-workspaces-migrator.WorkspacesMigration.
        return new XWikiDBVersion(53010);
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
        if (oldObject != null || isWorkspaceTemplate(currentWikiId)) {
            // It's a workspace
            upgradeWorkspace(oldObject, currentWikiId, oldWikiDescriptor);
        } else {
            // It's a regular subwiki
            upgradeRegularSubwiki(currentWikiId);
        }
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

        // Finally, the migration is done, we can delete the old workspace object.
        if (oldObject != null) {
            deleteOldWorkspaceObject(oldObject, oldWikiDescriptor);
        }
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

        // Set the membershipType value
        if (oldObject != null) {
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
        } else {
            // If there is no workspace object, we put a default value.
            configuration.setMembershipType(MembershipType.INVITE);
        }

        // Save the new configuration
        saveConfiguration(configuration, wikiId);
    }

    private void deleteOldWorkspaceObject(BaseObject oldObject, XWikiDocument oldWikiDescriptor)
        throws DataMigrationException
    {
        // Context, XWiki
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Delete the old object
        oldWikiDescriptor.removeXObject(oldObject);

        // Save the document
        try {
            xwiki.saveDocument(oldWikiDescriptor, "[UPGRADE] Remove the old WorkspaceManager.WorkspaceClass object.",
                    context);
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
        DocumentReference oldCandidateClassReference = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE,
                "WorkspaceCandidateMemberClass");
        List<BaseObject> candidacyObjects = candidaciesDocument.getXObjects(oldCandidateClassReference);
        if (candidacyObjects != null) {
            LocalDocumentReference newCandidateClassReference = WikiCandidateMemberClassInitializer.REFERENCE;

            for (BaseObject oldObject : candidacyObjects) {
                if (oldObject == null) {
                    continue;
                }
                // Transform the candidacy to the new class
                BaseObject newObject = candidaciesDocument.newXObject(newCandidateClassReference, xcontext);
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
}
