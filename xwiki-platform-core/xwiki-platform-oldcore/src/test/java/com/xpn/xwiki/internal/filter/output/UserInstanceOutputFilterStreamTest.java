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
package com.xpn.xwiki.internal.filter.output;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.AbstractInstanceFilterStreamTest;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Validate {@link UserInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
class UserInstanceOutputFilterStreamTest extends AbstractInstanceFilterStreamTest
{
    private static final DocumentReference GROUP1 = new DocumentReference("wiki1", "XWiki", "group1");

    // Tests

    @Test
    void importUsersAndGroupsPreserveVersion() throws FilterException, XWikiException, ParseException
    {
        UserInstanceOutputProperties outputProperties = new UserInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("user1", outputProperties);

        // XWiki.user1

        XWikiDocument userDocument = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "XWiki", "user1"), this.oldcore.getXWikiContext());

        assertFalse(userDocument.isNew());

        assertEquals(new DocumentReference("wiki1", "XWiki", "user1"), userDocument.getCreatorReference());
        assertEquals(toDate("2000-01-10 00:00:00.0 UTC"), userDocument.getCreationDate());
        assertEquals(new DocumentReference("wiki1", "XWiki", "user1"), userDocument.getAuthorReference());
        assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getDate());
        assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getContentUpdateDate());
        assertEquals(new DocumentReference("wiki1", "XWiki", "user1"), userDocument.getContentAuthorReference());
        assertEquals(false, userDocument.isMinorEdit());
        assertEquals("Import", userDocument.getComment());

        BaseObject userObject = userDocument.getXObject(MockitoOldcoreRule.USER_CLASS);
        assertEquals(0, userObject.getNumber());
        assertEquals("user1 first name", userObject.getStringValue("first_name"));
        assertEquals("user1 last name", userObject.getStringValue("last_name"));
        assertEquals("user1@email.ext", userObject.getStringValue("email"));
        assertEquals(1, userObject.getIntValue("active"));

        // XWiki.user2

        userDocument = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "user2"),
            this.oldcore.getXWikiContext());

        assertFalse(userDocument.isNew());

        assertEquals(new DocumentReference("wiki1", "XWiki", "user2"), userDocument.getCreatorReference());
        assertEquals(toDate("2000-01-20 00:00:00.0 UTC"), userDocument.getCreationDate());
        assertEquals(new DocumentReference("wiki1", "XWiki", "user2"), userDocument.getAuthorReference());
        assertEquals(toDate("2000-01-21 00:00:00.0 UTC"), userDocument.getDate());
        assertEquals(toDate("2000-01-21 00:00:00.0 UTC"), userDocument.getContentUpdateDate());
        assertEquals(new DocumentReference("wiki1", "XWiki", "user2"), userDocument.getContentAuthorReference());
        assertEquals(false, userDocument.isMinorEdit());
        assertEquals("Import", userDocument.getComment());

        userObject = userDocument.getXObject(MockitoOldcoreRule.USER_CLASS);
        assertEquals(0, userObject.getNumber());
        assertEquals("user2 first name", userObject.getStringValue("first_name"));
        assertEquals("user2 last name", userObject.getStringValue("last_name"));
        assertEquals("user2@email.ext", userObject.getStringValue("email"));
        assertEquals(0, userObject.getIntValue("active"));

        // XWiki.group1

        XWikiDocument groupDocument = this.oldcore.getSpyXWiki().getDocument(GROUP1, this.oldcore.getXWikiContext());
        assertFalse(groupDocument.isNew());

        BaseObject groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        assertEquals("XWiki.user1", groupMemberObject0.getStringValue("member"));
        BaseObject groupMemberObject1 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 1);
        assertEquals("XWiki.user2", groupMemberObject1.getStringValue("member"));

        assertEquals(2, groupDocument.getXObjects(MockitoOldcoreRule.GROUP_CLASS).size());

        // XWiki.group2

        groupDocument = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "group2"),
            this.oldcore.getXWikiContext());

        assertFalse(groupDocument.isNew());

        groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        assertEquals("XWiki.group1", groupMemberObject0.getStringValue("member"));

        // XWiki.emptygroup

        groupDocument = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "emptygroup"),
            this.oldcore.getXWikiContext());

        assertFalse(groupDocument.isNew());

        groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        assertEquals("", groupMemberObject0.getStringValue("member"));
    }

    @Test
    void importUserWithoutWiki() throws FilterException, XWikiException, ParseException
    {
        UserInstanceOutputProperties outputProperties = new UserInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("userwithoutwiki", outputProperties);

        // XWiki.user

        XWikiDocument userDocument = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "XWiki", "user"), this.oldcore.getXWikiContext());

        assertFalse(userDocument.isNew());

        assertEquals(new DocumentReference("wiki", "XWiki", "user"), userDocument.getCreatorReference());
        assertEquals(toDate("2000-01-10 00:00:00.0 UTC"), userDocument.getCreationDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "user"), userDocument.getAuthorReference());
        assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getDate());
        assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getContentUpdateDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "user"), userDocument.getContentAuthorReference());
        assertEquals(false, userDocument.isMinorEdit());
        assertEquals("Import", userDocument.getComment());

        BaseObject userObject = userDocument.getXObject(MockitoOldcoreRule.USER_CLASS);
        assertEquals(0, userObject.getNumber());
        assertEquals("user1 first name", userObject.getStringValue("first_name"));
        assertEquals("user1 last name", userObject.getStringValue("last_name"));
        assertEquals("user1@email.ext", userObject.getStringValue("email"));
        assertEquals(1, userObject.getIntValue("active"));
    }

    @Test
    void importOverExistingGroupKeepsExistingMembersAndDoesNotAddDuplication() throws FilterException, XWikiException
    {
        // Ensure group1 is empty
        XWiki wiki = this.oldcore.getSpyXWiki();
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument groupDocument = wiki.getDocument(GROUP1, context).clone();
        groupDocument.removeXObjects(MockitoOldcoreRule.GROUP_CLASS);
        wiki.saveDocument(groupDocument, "Empty group1", context);

        // Add user1
        groupDocument = wiki.getDocument(GROUP1, context).clone();
        BaseObject user1Member = groupDocument.newXObject(MockitoOldcoreRule.GROUP_CLASS, context);
        user1Member.setStringValue("member", "XWiki.user1");

        // Add deletedUser
        groupDocument = wiki.getDocument(GROUP1, context).clone();
        BaseObject deletedMember = groupDocument.newXObject(MockitoOldcoreRule.GROUP_CLASS, context);
        deletedMember.setStringValue("member", "XWiki.deletedUser");

        wiki.saveDocument(groupDocument, "Add user1 and deletedUser to group1", context);

        // Add user0
        groupDocument = wiki.getDocument(GROUP1, context).clone();
        BaseObject user0Member = groupDocument.newXObject(MockitoOldcoreRule.GROUP_CLASS, context);
        user0Member.setStringValue("member", "XWiki.user0");

        // Remove deletedMember to create a null object in getXObjects
        groupDocument.removeXObject(
                groupDocument.getXObjects(MockitoOldcoreRule.GROUP_CLASS)
                    .stream()
                    .filter(o -> "XWiki.deletedUser".equals(o.getStringValue("member")))
                    .findAny()
                    .get()
        );

        // Catch abusive modifications
        groupDocument.setCached(true);

        wiki.saveDocument(groupDocument, "Add user0 to group1", context);

        // Import user1 and user2
        importFromXML("user1");

        XWikiDocument groupDocument1 = this.oldcore.getSpyXWiki().getDocument(GROUP1, this.oldcore.getXWikiContext());
        assertFalse(groupDocument1.isNew());

        List<BaseObject> memberObjects = groupDocument1.getXObjects(MockitoOldcoreRule.GROUP_CLASS);

        // We check that the list contains a null element. This is not needed, but this helps make sure we do handle
        // the null case well. A change to the user instance filter stream that gets rid of the null spots would be
        // correct.
        assertEquals(4, memberObjects.size());

        List<String> members = memberObjects.stream()
                .filter(Objects::nonNull)
                .map(o -> o.getStringValue("member"))
                .sorted()
                .toList();

        // No duplicates
        assertEquals(3, members.size());

        // We have the expected members
        assertEquals(Set.of("XWiki.user1", "XWiki.user0", "XWiki.user2"), new HashSet<>(members));
    }
}
