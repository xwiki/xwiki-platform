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
package org.xwiki.filter.instance.internal.output;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.instance.internal.AbstractInstanceFilterStreamTest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Validate {@link UserInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
@AllComponents
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

        XWikiDocument userDocument =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "user1"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(userDocument.isNew());

        Assert.assertEquals(new DocumentReference("wiki1", "XWiki", "user1"), userDocument.getCreatorReference());
        Assert.assertEquals(toDate("2000-01-10 00:00:00.0 UTC"), userDocument.getCreationDate());
        Assert.assertEquals(new DocumentReference("wiki1", "XWiki", "user1"), userDocument.getAuthorReference());
        Assert.assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getDate());
        Assert.assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getContentUpdateDate());
        Assert.assertEquals(new DocumentReference("wiki1", "XWiki", "user1"), userDocument.getContentAuthorReference());
        Assert.assertEquals(false, userDocument.isMinorEdit());
        Assert.assertEquals("Import", userDocument.getComment());

        BaseObject userObject = userDocument.getXObject(MockitoOldcoreRule.USER_CLASS);
        Assert.assertEquals(0, userObject.getNumber());
        Assert.assertEquals("user1 first name", userObject.getStringValue("first_name"));
        Assert.assertEquals("user1 last name", userObject.getStringValue("last_name"));
        Assert.assertEquals("user1@email.ext", userObject.getStringValue("email"));
        Assert.assertEquals(1, userObject.getIntValue("active"));

        // XWiki.user2

        userDocument =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "user2"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(userDocument.isNew());

        Assert.assertEquals(new DocumentReference("wiki1", "XWiki", "user2"), userDocument.getCreatorReference());
        Assert.assertEquals(toDate("2000-01-20 00:00:00.0 UTC"), userDocument.getCreationDate());
        Assert.assertEquals(new DocumentReference("wiki1", "XWiki", "user2"), userDocument.getAuthorReference());
        Assert.assertEquals(toDate("2000-01-21 00:00:00.0 UTC"), userDocument.getDate());
        Assert.assertEquals(toDate("2000-01-21 00:00:00.0 UTC"), userDocument.getContentUpdateDate());
        Assert.assertEquals(new DocumentReference("wiki1", "XWiki", "user2"), userDocument.getContentAuthorReference());
        Assert.assertEquals(false, userDocument.isMinorEdit());
        Assert.assertEquals("Import", userDocument.getComment());

        userObject = userDocument.getXObject(MockitoOldcoreRule.USER_CLASS);
        Assert.assertEquals(0, userObject.getNumber());
        Assert.assertEquals("user2 first name", userObject.getStringValue("first_name"));
        Assert.assertEquals("user2 last name", userObject.getStringValue("last_name"));
        Assert.assertEquals("user2@email.ext", userObject.getStringValue("email"));
        Assert.assertEquals(0, userObject.getIntValue("active"));

        // XWiki.group1

        XWikiDocument groupDocument =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "group1"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(groupDocument.isNew());

        BaseObject groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        Assert.assertEquals("XWiki.user1", groupMemberObject0.getStringValue("member"));
        BaseObject groupMemberObject1 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 1);
        Assert.assertEquals("XWiki.user2", groupMemberObject1.getStringValue("member"));

        // XWiki.group2

        groupDocument =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "group2"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(groupDocument.isNew());

        groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        Assert.assertEquals("XWiki.group1", groupMemberObject0.getStringValue("member"));

        // XWiki.emptygroup

        groupDocument =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "emptygroup"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(groupDocument.isNew());

        groupMemberObject0 = groupDocument.getXObject(MockitoOldcoreRule.GROUP_CLASS, 0);
        Assert.assertEquals("", groupMemberObject0.getStringValue("member"));
    }

    @Test
    public void testImportUserWithoutWiki() throws FilterException, XWikiException, ParseException
    {
        UserInstanceOutputProperties outputProperties = new UserInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("userwithoutwiki", outputProperties);

        // XWiki.user

        XWikiDocument userDocument =
            this.oldcore.getSpyXWiki().getDocument(
                new DocumentReference("wiki", "XWiki", "user"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(userDocument.isNew());

        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "user"), userDocument.getCreatorReference());
        Assert.assertEquals(toDate("2000-01-10 00:00:00.0 UTC"), userDocument.getCreationDate());
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "user"), userDocument.getAuthorReference());
        Assert.assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getDate());
        Assert.assertEquals(toDate("2000-01-11 00:00:00.0 UTC"), userDocument.getContentUpdateDate());
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "user"), userDocument.getContentAuthorReference());
        Assert.assertEquals(false, userDocument.isMinorEdit());
        Assert.assertEquals("Import", userDocument.getComment());

        BaseObject userObject = userDocument.getXObject(MockitoOldcoreRule.USER_CLASS);
        Assert.assertEquals(0, userObject.getNumber());
        Assert.assertEquals("user1 first name", userObject.getStringValue("first_name"));
        Assert.assertEquals("user1 last name", userObject.getStringValue("last_name"));
        Assert.assertEquals("user1@email.ext", userObject.getStringValue("email"));
        Assert.assertEquals(1, userObject.getIntValue("active"));
    }
}
