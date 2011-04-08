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
package com.xpn.xwiki.user.impl.xwiki;

import java.util.Arrays;
import java.util.HashSet;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public class XWikiGroupServiceImplTest extends AbstractBridgedXWikiComponentTestCase
{
    XWikiGroupServiceImpl groupService;

    private Mock mockXWiki;

    private XWikiDocument user;

    private XWikiDocument userWithSpaces;

    private XWikiDocument group;
    private BaseObject groupObject;

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.groupService = new XWikiGroupServiceImpl();

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(true));
        this.mockXWiki.stubs().method("isReadOnly").will(returnValue(false));
        this.mockXWiki.stubs().method("getWikiOwner").will(returnValue(null));
        this.mockXWiki.stubs().method("getMaxRecursiveSpaceChecks").will(returnValue(0));
        this.mockXWiki.stubs().method("getDocument").with(ANYTHING, eq("WebPreferences"), ANYTHING).will(
            new CustomStub("Implements XWiki.getDocument")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return new XWikiDocument(new DocumentReference(getContext().getDatabase(),
                        (String) invocation.parameterValues.get(0), "WebPreferences"));
                }
            });

        getContext().setWiki((XWiki) this.mockXWiki.proxy());

        this.user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        getContext().setDatabase(this.user.getWikiName());
        BaseObject userObject = new BaseObject();
        userObject.setClassName("XWiki.XWikiUser");
        this.user.addXObject(userObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.user.getPrefixedFullName()), ANYTHING).will(
            returnValue(this.user));

        this.userWithSpaces = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user with spaces"));
        getContext().setDatabase(this.userWithSpaces.getWikiName());
        BaseObject userWithSpacesObject = new BaseObject();
        userWithSpacesObject.setClassName("XWiki.XWikiUser");
        this.userWithSpaces.addXObject(userWithSpacesObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.userWithSpaces.getPrefixedFullName()), ANYTHING)
            .will(returnValue(this.userWithSpaces));
        
        this.group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));
        getContext().setDatabase(this.group.getWikiName());
        this.groupObject = new BaseObject();
        this.groupObject.setClassName("XWiki.XWikiGroups");
        this.groupObject.setStringValue("member", this.user.getFullName());
        this.group.addXObject(this.groupObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.group.getPrefixedFullName()), ANYTHING).will(
            returnValue(this.group));
        this.mockXWiki.stubs().method("getDocument").with(eq(this.group.getFullName()), ANYTHING).will(
            returnValue(this.group));
    }

    public void testListMemberForGroup() throws XWikiException
    {        
        assertEquals(new HashSet<String>(Arrays.asList(this.user.getFullName())), new HashSet<String>(this.groupService
            .listMemberForGroup(this.group.getFullName(), getContext())));
        
        this.groupObject.setStringValue("member", this.userWithSpaces.getFullName());
        
        assertEquals(new HashSet<String>(Arrays.asList(this.userWithSpaces.getFullName())), new HashSet<String>(this.groupService
            .listMemberForGroup(this.group.getFullName(), getContext())));
    }
}
