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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.output.UserOutputInstanceWikiStream;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Validate {@link UserOutputInstanceWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class UserOutputInstanceWikiStreamTest extends AbstractOutputInstanceWikiStreamTest
{
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
                            new DocumentReference(xcontext.getDatabase(), "XWiki", "XWikiUsers"), xcontext);

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
                            new DocumentReference(xcontext.getDatabase(), "XWiki", "XWikiGroups"), xcontext);

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
    public void testUsersAndGroups() throws WikiStreamException, XWikiException
    {
        importFromXML("user1", null);

        XWikiDocument userDocument =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "XWiki", "user1"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(userDocument.isNew());
    }
}
