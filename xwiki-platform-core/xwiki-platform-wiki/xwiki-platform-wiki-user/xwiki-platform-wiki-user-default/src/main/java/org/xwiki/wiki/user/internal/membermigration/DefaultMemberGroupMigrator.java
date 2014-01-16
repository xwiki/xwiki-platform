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
    public void migrateGroups(String wikiId)
    {
        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

        DocumentReference allGroupDocRef = new DocumentReference(currentWikiId, XWiki.SYSTEM_SPACE,
                ALL_GROUP_PAGE_NAME);

        try {
            List<String> users = getAllGlobalUsersFromAllGroup(allGroupDocRef, currentWikiId);
            wikiUserManager.addMembers(users, currentWikiId);
            removeGlobalUsersFromAllGroup(allGroupDocRef, currentWikiId);
        } catch (WikiUserManagerException e) {
            logger.error("Failed to add create the member list of wiki {}. The migration of wiki members has failed",
                    currentWikiId, e);
        } catch (XWikiException e) {
            logger.error("Failed to get the XWikiAllGroup document in the wiki {}. The migration if wiki members has"
                    + " failed.", currentWikiId, e);
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
                if (isGlobalUser(userId)) {
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
                if (isGlobalUser(userId)) {
                    // remove the object
                    groupDoc.removeXObject(object);
                    modified = true;
                }
            }
        }

        if (modified) {
            xwiki.saveDocument(groupDoc, "Remove all global users from this group.", xcontext);
        }
    }

    private boolean isGlobalUser(String userId)
    {
        DocumentReference userDocRef = documentReferenceResolver.resolve(userId);
        WikiReference wiki = userDocRef.getWikiReference();
        return (wiki != null && wiki.getName().equals(wikiDescriptorManager.getMainWikiId()));
    }
}
