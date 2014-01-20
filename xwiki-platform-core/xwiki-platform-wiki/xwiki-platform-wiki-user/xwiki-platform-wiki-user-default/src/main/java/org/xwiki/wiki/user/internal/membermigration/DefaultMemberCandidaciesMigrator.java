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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.user.internal.WikiCandidateMemberClassInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;

/**
 * Default implementation for {@link org.xwiki.wiki.user.internal.membermigration.MemberCandidaciesMigrator}.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component
public class DefaultMemberCandidaciesMigrator implements MemberCandidaciesMigrator
{
    private static final String ALL_GROUP_PAGE_NAME = "XWikiAllGroup";

    private static final String MEMBER_GROUP_PAGE_NAME = "XWikiMemberGroup";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void migrateCandidacies(String wikiId) throws DataMigrationException
    {
        try {
            DocumentReference allGroupDocRef = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, ALL_GROUP_PAGE_NAME);
            DocumentReference memberGroupDocRef = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE,
                    MEMBER_GROUP_PAGE_NAME);

            // XWiki objects
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();

            // We need to get the document that holds the candidacies
            XWikiDocument allGroup = xwiki.getDocument(allGroupDocRef, xcontext);

            // We need to get the new document that will hold the candidacies
            XWikiDocument memberGroup = xwiki.getDocument(memberGroupDocRef, xcontext);

            // We need to get all the old candidacies
            DocumentReference candidateClassReference = new DocumentReference(wikiId,
                    WikiCandidateMemberClassInitializer.DOCUMENT_SPACE,
                    WikiCandidateMemberClassInitializer.DOCUMENT_NAME);
            List<BaseObject> candidacyObjects = allGroup.getXObjects(candidateClassReference);
            if (candidacyObjects != null) {
                for (BaseObject oldObject : candidacyObjects) {
                    if (oldObject == null) {
                        continue;
                    }
                    // Transform the candidacy to the new class
                    BaseObject newObject = memberGroup.newXObject(candidateClassReference, xcontext);
                    newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE,
                            oldObject.getStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE));
                    newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                            oldObject.getStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS));
                    newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_USER,
                            oldObject.getStringValue(WikiCandidateMemberClassInitializer.FIELD_USER));
                    newObject.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT,
                            oldObject.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT));
                    newObject.setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN,
                            oldObject.getStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN));
                    newObject.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT,
                            oldObject.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT));
                    newObject.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT,
                            oldObject.getLargeStringValue(
                                    WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT));
                    newObject.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION,
                            oldObject.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION));
                    newObject.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE,
                            oldObject.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE));

                    // Remove the old object
                    allGroup.removeXObject(oldObject);
                }

                // Save
                String message = "[UPGRADE] Move candidacies from XWikiAllGroup to XWikiMemberGroup.";
                xwiki.saveDocument(memberGroup, message, xcontext);
                xwiki.saveDocument(allGroup, message, xcontext);
            }
        } catch (XWikiException e) {
            throw new DataMigrationException("Failed to move candidacies from XWikiAllGroup to XWikiMemberGroup.",
                    e);
        }
    }
}
