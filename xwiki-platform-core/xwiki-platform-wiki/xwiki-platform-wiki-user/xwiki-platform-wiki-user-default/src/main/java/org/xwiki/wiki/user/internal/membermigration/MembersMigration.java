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
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.internal.WikiCandidateMemberClassInitializer;

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
 * XWiki.XWikiAllGroup). It also update rights given to XWikiAllGroup to occurs on XWikiMemberGroup and move candidacies
 * to XWiki.XWikiMemberGroup.
 *
 * @since 5.4RC1
 * @version $Id$
 */
@Component
@Named("R54000MembersMigration")
public class MembersMigration extends AbstractHibernateDataMigration
{
    private static final String GROUP_CLASS_NAME = "XWikiGroups";

    private static final String GROUP_CLASS_MEMBER_FIELD = "member";

    private static final String ALL_GROUP_PAGE_NAME = "XWikiAllGroup";

    private static final String MEMBER_GROUP_PAGE_NAME = "XWikiMemberGroup";

    @Inject
    private MemberGroupMigrator memberGroupMigrator;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private QueryManager queryManager;

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
        String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

        DocumentReference allGroupDocRef = new DocumentReference(currentWikiId, XWiki.SYSTEM_SPACE, ALL_GROUP_PAGE_NAME);
        DocumentReference memberGroupDocRef = new DocumentReference(currentWikiId, XWiki.SYSTEM_SPACE, MEMBER_GROUP_PAGE_NAME);

        try {
            memberGroupMigrator.migrateGroups(currentWikiId);
            upgradeRights("XWikiRights", currentWikiId);
            upgradeRights("XWikiGlobalRights", currentWikiId);
            upgradeCandidacies(allGroupDocRef, memberGroupDocRef, currentWikiId);
        } catch (XWikiException e) {
            logger.error("Failed to get the XWikiAllGroup document in the wiki {}. The migration if wiki members has"
                    + " failed.", currentWikiId, e);
        }
    }

    private void upgradeRights(String rightsClass, String currentWiki)
    {
        // Get XWiki objects
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        DocumentReference classReference = new DocumentReference(currentWiki, XWiki.SYSTEM_SPACE, rightsClass);

        String propertyName = "groups";

        try {
            String queryStatement = String.format("from doc.object(XWiki.%s) objRight WHERE"
                    + " objRight.member like '%%XWiki.XWikiAllGroup'%%", rightsClass);
            Query query = queryManager.createQuery(queryStatement, Query.XWQL);
            query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "unique"));
            List<String> documentNames = query.execute();
            WikiReference currentWikiRef = new WikiReference(wikiDescriptorManager.getCurrentWikiId());
            for (String docName : documentNames) {
                DocumentReference docRef = documentReferenceResolver.resolve(docName, currentWikiRef);
                XWikiDocument doc = xwiki.getDocument(docRef, xcontext);
                List<BaseObject> objects = doc.getXObjects(classReference);
                boolean modified = false;
                if (objects != null) {
                    for (BaseObject obj : objects) {
                        List groups = obj.getListValue(propertyName);
                        int i = 0;
                        for (Object groupValue : groups) {
                            String groupStringValue = (String) groupValue;
                            if (groupStringValue.equals("XWiki.XWikiAllGroup")) {
                                groups.set(i, "XWiki.XWikiMemberGroup");
                                modified = true;
                            }
                            i++;
                        }
                        obj.setStringListValue(propertyName, groups);
                    }
                }
                if (modified) {
                    xwiki.saveDocument(doc, "Set rights for XWikiMemberGroup", xcontext);
                }
            }

        } catch (QueryException e) {
            e.printStackTrace();
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        } catch (XWikiException e) {
            e.printStackTrace();
        }
    }

    private void upgradeCandidacies(DocumentReference allGroupDocRef, DocumentReference memberGroupDocRef,
        String currentWiki) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        // We need to get the document that holds the candidacies
        XWikiDocument allGroup = xwiki.getDocument(allGroupDocRef, xcontext);

        // We need to get the new document that will hold the candidacies
        XWikiDocument memberGroup = xwiki.getDocument(memberGroupDocRef, xcontext);

        // We need to get all the old candidacies
        DocumentReference candidateClassReference = new DocumentReference(currentWiki,
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
                        oldObject.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT));
                newObject.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION,
                        oldObject.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION));
                newObject.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE,
                        oldObject.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE));

                // Remove the old object
                allGroup.removeXObject(oldObject);
            }

            // Save
            String message = "Move candidacies from XWikiAllGroup to XWikiMemberGroup.";
            xwiki.saveDocument(memberGroup, message, xcontext);
            xwiki.saveDocument(allGroup, message, xcontext);
        }
    }

}
