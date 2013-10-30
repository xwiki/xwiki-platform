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
package org.xwiki.wikistream.instance.internal;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.output.UserOutputInstanceWikiStream;
import org.xwiki.wikistream.instance.internal.output.UserOutputProperties;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Validate {@link UserOutputInstanceWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class UserOutputInstanceWikiStreamTest extends AbstractOutputInstanceWikiStreamTest
{
    LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    LocalDocumentReference GROUP_CLASS = new LocalDocumentReference("XWiki", "XWikiGroups");

    @Override
    public void before() throws ComponentLookupException, XWikiException
    {
        super.before();

        Mockito.when(this.oldcore.getMockXWiki().getUserClass(Mockito.any(XWikiContext.class))).then(
            new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = (XWikiContext) invocation.getArguments()[0];

                    XWikiDocument userDocument =
                        oldcore.getMockXWiki().getDocument(
                            new DocumentReference(USER_CLASS, new WikiReference(xcontext.getDatabase())), xcontext);

                    final BaseClass userClass = userDocument.getXClass();

                    if (userDocument.isNew()) {
                        userClass.addTextField("first_name", "First Name", 30);
                        userClass.addTextField("last_name", "Last Name", 30);
                        userClass.addEmailField("email", "e-Mail", 30);
                        userClass.addPasswordField("password", "Password", 10);
                        userClass.addBooleanField("active", "Active", "active");
                        userClass.addTextAreaField("comment", "Comment", 40, 5);
                        userClass.addTextField("avatar", "Avatar", 30);
                        userClass.addTextField("phone", "Phone", 30);
                        userClass.addTextAreaField("address", "Address", 40, 3);

                        oldcore.getMockXWiki().saveDocument(userDocument, xcontext);
                    }

                    return userClass;
                }
            });
        Mockito.when(this.oldcore.getMockXWiki().getGroupClass(Mockito.any(XWikiContext.class))).then(
            new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = (XWikiContext) invocation.getArguments()[0];

                    XWikiDocument groupDocument =
                        oldcore.getMockXWiki().getDocument(
                            new DocumentReference(GROUP_CLASS, new WikiReference(xcontext.getDatabase())), xcontext);

                    final BaseClass groupClass = groupDocument.getXClass();

                    if (groupDocument.isNew()) {
                        groupClass.addTextField("member", "Member", 30);

                        oldcore.getMockXWiki().saveDocument(groupDocument, xcontext);
                    }

                    return groupClass;
                }
            });
    }

    // Tests

    @Test
    public void testImportUsersAndGroupsPreserveVersion() throws WikiStreamException, XWikiException, ParseException
    {
        UserOutputProperties outputProperties = new UserOutputProperties();

        outputProperties.setPreserveVersion(true);

        importFromXML("user1", outputProperties);

        // XWiki.user1

        XWikiDocument userDocument =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "user1"),
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

        BaseObject userObject = userDocument.getXObject(USER_CLASS);
        Assert.assertEquals(0, userObject.getNumber());
        Assert.assertEquals("user1 first name", userObject.getStringValue("first_name"));
        Assert.assertEquals("user1 last name", userObject.getStringValue("last_name"));
        Assert.assertEquals("user1@email.ext", userObject.getStringValue("email"));
        Assert.assertEquals(1, userObject.getIntValue("active"));

        // XWiki.user2

        userDocument =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "user2"),
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

        userObject = userDocument.getXObject(USER_CLASS);
        Assert.assertEquals(0, userObject.getNumber());
        Assert.assertEquals("user2 first name", userObject.getStringValue("first_name"));
        Assert.assertEquals("user2 last name", userObject.getStringValue("last_name"));
        Assert.assertEquals("user2@email.ext", userObject.getStringValue("email"));
        Assert.assertEquals(0, userObject.getIntValue("active"));

        // XWiki.group1

        XWikiDocument groupDocument =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "group1"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(groupDocument.isNew());

        BaseObject groupMemberObject0 = groupDocument.getXObject(GROUP_CLASS, 0);
        Assert.assertEquals("XWiki.user1", groupMemberObject0.getStringValue("member"));
        BaseObject groupMemberObject1 = groupDocument.getXObject(GROUP_CLASS, 1);
        Assert.assertEquals("XWiki.user2", groupMemberObject1.getStringValue("member"));
        
        // XWiki.group2
        
        groupDocument =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "group2"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(groupDocument.isNew());

        groupMemberObject0 = groupDocument.getXObject(GROUP_CLASS, 0);
        Assert.assertEquals("XWiki.group1", groupMemberObject0.getStringValue("member"));

        // XWiki.emptygroup

        groupDocument =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "emptygroup"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(groupDocument.isNew());

        groupMemberObject0 = groupDocument.getXObject(GROUP_CLASS, 0);
        Assert.assertEquals("", groupMemberObject0.getStringValue("member"));
    }
}
