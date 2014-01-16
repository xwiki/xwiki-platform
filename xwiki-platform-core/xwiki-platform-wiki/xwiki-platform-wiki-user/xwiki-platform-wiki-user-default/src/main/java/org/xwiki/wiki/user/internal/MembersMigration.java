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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

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
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Component that create a XWiki.XWikiMemberGroup for subwikis initialized with the list of current users (located in
 * XWiki.XWikiAllGroup). It also update rights given to XWikiAllGroup to also occurs on XWikiMemberGroup.
 *
 * @since 5.4RC1
 * @version $Id$
 */
@Component
@Named("R540000MembersMMigration")
public class MembersMigration extends AbstractHibernateDataMigration
{
    private static final String GROUP_CLASS_NAME = "XWikiGroups";

    private static final String GROUP_CLASS_MEMBER_FIELD = "member";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "http://jira.xwiki.org/browse/XWIKI-9886";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54000);
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
        try {
            List<String> users = getAllGlobalUsersFromAllGroup();
            wikiUserManager.addMembers(users, wikiDescriptorManager.getCurrentWikiId());
        } catch (WikiUserManagerException e) {
            logger.error("Failed to add create the member list of wiki {}. The migration of wiki members has failed",
                    wikiDescriptorManager.getCurrentWikiId(), e);

        } catch (XWikiException e) {
            logger.error("Failed to get the XWikiAllGroup document in the wiki {}. The migration if wiki members has"
                    + " failed.", wikiDescriptorManager.getCurrentWikiId(), e);
        }
    }

    /**
     * @return the list of global users contained in XWikiAllGroup
     */
    private List<String> getAllGlobalUsersFromAllGroup() throws XWikiException
    {
        // The result list to return
        List<String> members = new ArrayList<String>();

        // Get XWiki objects
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        // Get the XWikiAllGroup document
        DocumentReference allGroupDocRef = new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                XWiki.SYSTEM_SPACE, "XWikiAllGroup");
        XWikiDocument allGroupDoc = xwiki.getDocument(allGroupDocRef, xcontext);

        // Get the group objects
        DocumentReference classReference =
                new DocumentReference(wikiDescriptorManager.getCurrentWikiId(), XWiki.SYSTEM_SPACE, GROUP_CLASS_NAME);
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

    private boolean isGlobalUser(String userId)
    {
        DocumentReference userDocRef = documentReferenceResolver.resolve(userId);
        WikiReference wiki = userDocRef.getWikiReference();
        return (wiki != null && wiki.getName().equals(wikiDescriptorManager.getMainWikiId()));
    }

}
