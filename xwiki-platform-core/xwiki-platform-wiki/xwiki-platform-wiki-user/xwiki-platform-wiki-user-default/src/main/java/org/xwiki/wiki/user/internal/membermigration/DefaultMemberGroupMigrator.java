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
package org.xwiki.wiki.user.internal.membermigration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;

/**
 * Default implementation for {@link org.xwiki.wiki.user.internal.membermigration.MemberGroupMigrator}.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component
public class DefaultMemberGroupMigrator implements MemberGroupMigrator
{
    private static final String GROUP_CLASS_NAME = "XWikiGroups";

    private static final String GROUP_CLASS_MEMBER_FIELD = "member";

    private static final String ALL_GROUP_PAGE_NAME = "XWikiAllGroup";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void migrateGroups(String wikiId) throws DataMigrationException
    {
        DocumentReference allGroupDocRef = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE,
                ALL_GROUP_PAGE_NAME);
        try {
            List<String> users = getAllGlobalUsersFromAllGroup(allGroupDocRef, wikiId);
            wikiUserManager.addMembers(users, wikiId);
            removeGlobalUsersFromAllGroup(allGroupDocRef, wikiId);
        } catch (WikiUserManagerException e) {
            throw new DataMigrationException(String.format("Failed to create the member list of wiki [%s].",
                    wikiId), e);
        } catch (XWikiException e) {
            throw new DataMigrationException(String.format("Failed to get or save the XWiki.XWikiAllGroup document"
                    + " in the wiki [%s].", wikiId), e);
        }

    }

    /**
     * @return the list of global users contained in XWikiAllGroup
     */
    private List<String> getAllGlobalUsersFromAllGroup(DocumentReference allGroupDocRef, String currentWiki)
        throws XWikiException
    {
        // The result list to return
        List<String> members = new ArrayList<String>();

        // Get XWiki objects
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the XWikiAllGroup document
        XWikiDocument allGroupDoc = xwiki.getDocument(allGroupDocRef, xcontext);

        // Reference to the current wiki
        WikiReference wikiReference = new WikiReference(currentWiki);

        // Get the group objects
        DocumentReference classReference =
                new DocumentReference(currentWiki, XWiki.SYSTEM_SPACE, GROUP_CLASS_NAME);
        List<BaseObject> memberObjects = allGroupDoc.getXObjects(classReference);
        if (memberObjects != null) {
            // For each member object
            for (BaseObject object : memberObjects) {
                if (object == null) {
                    continue;
                }
                // Get the userId
                String userId = object.getStringValue(GROUP_CLASS_MEMBER_FIELD);
                // If the user is global
                if (isGlobalUser(userId, wikiReference)) {
                    // Add it the the list
                    members.add(userId);
                }
            }
        }

        return members;
    }

    private void removeGlobalUsersFromAllGroup(DocumentReference allGroupDocRef, String currentWiki)
        throws XWikiException
    {
        // Get XWiki objects
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the document
        XWikiDocument groupDoc = xwiki.getDocument(allGroupDocRef, xcontext);

        // Reference to the current wiki
        WikiReference wikiReference = new WikiReference(currentWiki);

        // Document status
        boolean modified = false;

        // Get the group objects
        DocumentReference classReference =
                new DocumentReference(currentWiki, XWiki.SYSTEM_SPACE, GROUP_CLASS_NAME);
        List<BaseObject> memberObjects = groupDoc.getXObjects(classReference);
        if (memberObjects != null) {
            // For each member object
            for (BaseObject object : memberObjects) {
                if (object == null) {
                    continue;
                }
                String userId = object.getStringValue(GROUP_CLASS_MEMBER_FIELD);
                if (isGlobalUser(userId, wikiReference)) {
                    // remove the object
                    groupDoc.removeXObject(object);
                    modified = true;
                }
            }
        }

        if (modified) {
            xwiki.saveDocument(groupDoc, "[UPGRADE] Remove all global users from this group.", xcontext);
        }
    }

    private boolean isGlobalUser(String userId, WikiReference currentWiki)
    {
        if (userId.equals("")) {
            return false;
        }
        DocumentReference userDocRef = documentReferenceResolver.resolve(userId, currentWiki);
        WikiReference wiki = userDocRef.getWikiReference();
        return (wiki != null && wiki.getName().equals(wikiDescriptorManager.getMainWikiId()));
    }
}
