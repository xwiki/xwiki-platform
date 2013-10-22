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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroup;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.properties.WikiPropertyGroupProvider;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.WikiUserPropertyGroup;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Named(WikiUserPropertyGroupProvider.GROUP_NAME)
@Singleton
public class WikiUserPropertyGroupProvider implements WikiPropertyGroupProvider
{
    public static final String GROUP_NAME = "user";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    /**
     * Perform the upgrade from version prior to 5.3M2.
     * @return if the wiki was an old workspace
     */
    private boolean upgradeOldWorkspace(XWikiDocument descriptorDocument, WikiUserPropertyGroup group)
            throws WikiPropertyGroupException
    {
        // Try to get the old workspace object
        DocumentReference oldClassDocument = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                "WorkspaceManager", "WorkspaceClass");
        BaseObject oldObject = descriptorDocument.getXObject(oldClassDocument);

        // The wiki is not a workspace
        if (oldObject == null) {
            return false;
        }

        // The wiki is a workspace
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // No local users
        group.enableLocalUsers(false);

        // Get the membershipType value
        String membershipTypeValue = oldObject.getStringValue("membershipType");
        MembershipType membershipType;
        try {
            membershipType = MembershipType.valueOf(membershipTypeValue.toUpperCase());
        } catch (Exception e) {
            // Default value
            membershipType = MembershipType.INVITE;
        }
        group.setMembershypType(membershipType);

        // Delete the old object
        descriptorDocument.removeXObject(oldObject);

        // Save the new property group as it should be now
        save(group, descriptorDocument);

        // The wiki is a workspace
        return true;
    }

    @Override
    public WikiPropertyGroup get(String wikiId) throws WikiPropertyGroupException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        WikiUserPropertyGroup group = new WikiUserPropertyGroup(GROUP_NAME);

        try {
            // Get the document & the object
            XWikiDocument descriptorDocument = wikiDescriptorDocumentHelper.getDocumentFromWikiId(wikiId);
            if (!upgradeOldWorkspace(descriptorDocument, group)) {
                // If it is not a workspace, then read the values
                BaseObject object = descriptorDocument.getXObject(XWikiServerUserClassDocumentInitializer.SERVER_CLASS);
                // Get the enable local users value (default value: 1, for compatibility reason with old subwikis).
                group.enableLocalUsers(
                        object.getIntValue(XWikiServerUserClassDocumentInitializer.FIELD_ENABLELOCALUSERS, 1) != 0);
                // Get the membershipType value
                String membershipTypeValue = object.getStringValue(
                        XWikiServerUserClassDocumentInitializer.FIELD_MEMBERSHIPTYPE);
                MembershipType membershipType;
                try {
                    membershipType = MembershipType.valueOf(membershipTypeValue.toUpperCase());
                } catch (Exception e) {
                    // Default value
                    membershipType = MembershipType.INVITE;
                }
                group.setMembershypType(membershipType);
            }
        } catch (WikiManagerException e) {
            throw new WikiPropertyGroupException(String.format("Unable to load descriptor document for wiki %s.",
                    wikiId), e);
        }

        return group;
    }

    @Override
    public void save(WikiPropertyGroup group, String wikiId) throws WikiPropertyGroupException
    {
        WikiUserPropertyGroup userGroup = (WikiUserPropertyGroup) group;

        try {
            XWikiDocument descriptorDocument = wikiDescriptorDocumentHelper.getDocumentFromWikiId(wikiId);
            save(userGroup, descriptorDocument);
        } catch (WikiManagerException e) {
            throw new WikiPropertyGroupException(String.format("Unable to load descriptor document for wiki %s.",
                    wikiId), e);
        }
    }

    private void save(WikiUserPropertyGroup userGroup, XWikiDocument descriptorDocument)
            throws WikiPropertyGroupException
    {
        // Get the XWiki Object
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Fill the object
        BaseObject object = descriptorDocument.getXObject(XWikiServerUserClassDocumentInitializer.SERVER_CLASS);
        object.setIntValue(XWikiServerUserClassDocumentInitializer.FIELD_ENABLELOCALUSERS,
                userGroup.hasLocalUsersEnabled() ? 1 : 0);
        object.setStringValue(XWikiServerUserClassDocumentInitializer.FIELD_MEMBERSHIPTYPE,
                userGroup.getMembershipType().name().toLowerCase());

        // Save the document
        try {
            xwiki.saveDocument(descriptorDocument, String.format("Changed property group [%s].", GROUP_NAME), context);
        } catch (XWikiException e) {
            throw new WikiPropertyGroupException("Unable to save descriptor document.", e);
        }
    }

}
