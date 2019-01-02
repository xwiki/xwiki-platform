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

import org.junit.jupiter.api.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.model.reference.DocumentReference;

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
public class UserInstanceOutputFilterStreamTest extends AbstractInstanceFilterStreamTest
{
    // Tests

    @Test
    public void testImportUsersAndGroupsPreserveVersion() throws FilterException, XWikiException, ParseException
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

        XWikiDocument groupDocument = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "XWiki", "group1"), this.oldcore.getXWikiContext());

        assertFalse(groupDocument.isNew());

        BaseObject groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        assertEquals("XWiki.user1", groupMemberObject0.getStringValue("member"));
        BaseObject groupMemberObject1 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 1);
        assertEquals("XWiki.user2", groupMemberObject1.getStringValue("member"));

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
    public void testImportUserWithoutWiki() throws FilterException, XWikiException, ParseException
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
}
