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

import java.util.Collections;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl}.
 * 
 * @version $Id$
 */
public class XWikiRightServiceImplTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiRightServiceImpl rightService;

    private Mock mockGroupService;

    private Mock mockXWiki;

    private XWikiDocument user;

    private XWikiDocument group;

    private XWikiDocument group2;

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.rightService = new XWikiRightServiceImpl();

        this.mockGroupService = mock(XWikiGroupService.class, new Class[] {}, new Object[] {});

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(true));
        this.mockXWiki.stubs().method("getGroupService").will(returnValue(this.mockGroupService.proxy()));
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
        this.user.setNew(false);
        getContext().setDatabase(this.user.getWikiName());
        BaseObject userObject = new BaseObject();
        userObject.setClassName("XWiki.XWikiUser");
        this.user.addXObject(userObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.user.getPrefixedFullName()), ANYTHING).will(
            returnValue(this.user));

        this.group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));
        this.group.setNew(false);
        getContext().setDatabase(this.group.getWikiName());
        BaseObject groupObject = new BaseObject();
        groupObject.setClassName("XWiki.XWikiGroups");
        groupObject.setStringValue("member", this.user.getFullName());
        this.group.addXObject(groupObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.group.getPrefixedFullName()), ANYTHING).will(
            returnValue(this.group));

        this.group2 = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "group2"));
        this.group2.setNew(false);
        getContext().setDatabase(this.group2.getWikiName());
        BaseObject group2Object = new BaseObject();
        group2Object.setClassName("XWiki.XWikiGroups");
        group2Object.setStringValue("member", this.user.getPrefixedFullName());
        this.group2.addXObject(groupObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.group2.getPrefixedFullName()), ANYTHING).will(
            returnValue(this.group2));

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember").with(
            eq(this.user.getDocumentReference()), ANYTHING, ANYTHING, ANYTHING).will(
            new CustomStub("Implements XWikiGroupService.getAllGroupsReferencesForMember")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiContext context = (XWikiContext) invocation.parameterValues.get(3);

                    if (context.getDatabase().equals(group.getWikiName())) {
                        return Collections.singleton(group.getDocumentReference());
                    } else if (context.getDatabase().equals(group2.getWikiName())) {
                        return Collections.singleton(group2.getDocumentReference());
                    } else {
                        return Collections.emptyList();
                    }
                }
            });

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember").with(
            eq(this.group.getDocumentReference()), ANYTHING, ANYTHING, ANYTHING).will(
            returnValue(Collections.emptyList()));
        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember").with(
            eq(this.group2.getDocumentReference()), ANYTHING, ANYTHING, ANYTHING).will(
            returnValue(Collections.emptyList()));
    }

    /**
     * Test if checkRight() take care of users's groups from other wikis.
     */
    public void testCheckRight() throws XWikiRightNotFoundException, XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki2", "Space", "Page"));

        Mock mockGlobalRightObj = mock(BaseObject.class, new Class[] {}, new Object[] {});
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("levels")).will(returnValue("view"));
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("groups")).will(
            returnValue(this.group.getPrefixedFullName()));
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("users")).will(returnValue(""));
        mockGlobalRightObj.stubs().method("getIntValue").with(eq("allow")).will(returnValue(1));
        mockGlobalRightObj.stubs().method("setNumber");
        mockGlobalRightObj.stubs().method("setDocumentReference");

        doc.addObject("XWiki.XWikiGlobalRights", (BaseObject) mockGlobalRightObj.proxy());

        getContext().setDatabase("wiki2");

        boolean result =
            this.rightService.checkRight(this.user.getPrefixedFullName(), doc, "view", true, true, true, getContext());

        assertTrue(this.user.getPrefixedFullName() + " does not have global view right on wiki2", result);
    }

    public void testHasAccessLevelWhithUserFromAnotherWiki() throws XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference(this.group2.getWikiName(), "Space", "Page"));

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "XWikiPreference"));
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "view");
        preferencesObject.setIntValue("allow", 1);
        preferences.addXObject(preferencesObject);
        preferences.setNew(false);

        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            new CustomStub("Implements XWiki.getDocument")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    if (!getContext().getDatabase().equals("wiki2")) {
                        new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreference"));
                    }

                    return preferences;
                }
            });
        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getPrefixedFullName()), ANYTHING).will(
            returnValue(doc));

        getContext().setDatabase("wiki");

        assertFalse("User from another wiki has right on a local wiki", this.rightService.hasAccessLevel("view",
            this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true, getContext()));

        // direct user rights

        preferencesObject.setStringValue("users", this.user.getPrefixedFullName());

        getContext().setDatabase(this.user.getWikiName());

        assertTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
                getContext()));

        // user group rights

        preferencesObject.removeField("users");

        // group from user's wiki

        preferencesObject.setStringValue("groups", this.group.getPrefixedFullName());

        getContext().setDatabase(this.user.getWikiName());

        assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
                getContext()));

        // group from document's wiki

        preferencesObject.setStringValue("groups", this.group2.getFullName());

        getContext().setDatabase(this.user.getWikiName());
        
        assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
                getContext()));

        // user is wiki owner

        preferencesObject.removeField("groups");
        this.mockXWiki.stubs().method("getWikiOwner").with(eq(doc.getWikiName()), ANYTHING).will(
            returnValue(this.user.getPrefixedFullName()));

        getContext().setDatabase(this.user.getWikiName());

        assertTrue("Wiki owner from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue("Wiki owner group from another wiki does not have right on a local wiki when tested from user wiki",
            this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true,
                getContext()));
        assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
            this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
                getContext()));
    }

    public void testHasAccessLevelWhithOnlyPageAsReference() throws XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));

        final XWikiDocument preferences =
            new XWikiDocument(new DocumentReference(doc.getWikiName(), doc.getSpaceName(), "WebPreferences"));
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "view");
        preferencesObject.setIntValue("allow", 1);
        preferences.addXObject(preferencesObject);
        preferences.setNew(false);

        this.mockXWiki.stubs().method("getDocument").with(eq(preferences.getSpaceName()),
            eq(preferences.getPageName()), ANYTHING).will(returnValue(preferences));
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            returnValue(new XWikiDocument(
                new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreferences"))));
        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getPrefixedFullName()), ANYTHING).will(
            returnValue(doc));

        getContext().setDatabase("wiki");
        getContext().setDoc(doc);

        assertFalse("Failed to check right with only page name", this.rightService.hasAccessLevel("view", this.user
            .getPageName(), doc.getPageName(), true, getContext()));
    }

    /**
     * Test that programming rights are checked on the context user when no context document is set.
     */
    public void testProgrammingRightsWhenNoContextDocumentIsSet()
    {
        // Setup an XWikiPreferences document granting programming rights to XWiki.Programmer
        XWikiDocument prefs = new XWikiDocument("XWiki", "XWikiPreferences");
        Mock mockGlobalRightObj = mock(BaseObject.class, new Class[] {}, new Object[] {});
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("levels")).will(returnValue("programming,admin"));
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("users")).will(returnValue("XWiki.Programmer"));
        mockGlobalRightObj.stubs().method("getIntValue").with(eq("allow")).will(returnValue(1));
        mockGlobalRightObj.stubs().method("setNumber");
        mockGlobalRightObj.stubs().method("setDocumentReference");
        prefs.addObject("XWiki.XWikiGlobalRights", (BaseObject) mockGlobalRightObj.proxy());
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), eq(getContext())).will(
            returnValue(prefs));

        // Setup the context (no context document)
        this.mockXWiki.stubs().method("getDatabase").will(returnValue("xwiki"));
        getContext().remove("doc");
        getContext().remove("sdoc");

        // XWiki.Programmer should have PR, as per the global rights.
        getContext().setUser("XWiki.Programmer");
        assertTrue(this.rightService.hasProgrammingRights(getContext()));

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember").with(
            eq(new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER)), ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(Collections.emptyList()));

        // Guests should not have PR
        getContext().setUser(XWikiRightService.GUEST_USER_FULLNAME);
        assertFalse(this.rightService.hasProgrammingRights(getContext()));

        // superadmin should always have PR
        getContext().setUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        assertTrue(this.rightService.hasProgrammingRights(getContext()));
    }

    public void testHasAccessLevelWhithGuestUser() throws XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki2", "Space", "Page"));

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "XWikiPreference"));
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "view");
        preferencesObject.setIntValue("allow", 1);
        preferences.addXObject(preferencesObject);

        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            new CustomStub("Implements XWiki.getDocument")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    if (!getContext().getDatabase().equals("wiki2")) {
                        new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreference"));
                    }

                    return preferences;
                }
            });
        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getPrefixedFullName()), ANYTHING).will(
            returnValue(doc));
        this.mockXWiki.stubs().method("getXWikiPreference").with(eq("authenticate_view"), ANYTHING, ANYTHING).will(
            returnValue("false"));
        this.mockXWiki.stubs().method("getXWikiPreferenceAsInt").with(eq("authenticate_view"), ANYTHING, ANYTHING)
            .will(returnValue(0));
        this.mockXWiki.stubs().method("getSpacePreference").with(eq("authenticate_view"), ANYTHING, ANYTHING).will(
            returnValue("false"));
        this.mockXWiki.stubs().method("getSpacePreferenceAsInt").with(eq("authenticate_view"), ANYTHING, ANYTHING)
            .will(returnValue(0));

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember").with(
            eq(new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER)), ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(Collections.emptyList()));
        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember").with(
            eq(new DocumentReference("wiki2", "XWiki", XWikiRightService.GUEST_USER)), ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(Collections.emptyList()));

        getContext().setDatabase("wiki");

        assertFalse("Guest has wiew right on the document", this.rightService.hasAccessLevel("view",
            XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true, getContext()));

        // direct user rights

        preferencesObject.setStringValue("users", XWikiRightService.GUEST_USER_FULLNAME);

        getContext().setDatabase("wiki");

        assertTrue("Guest does not have right on the document when tested from another wiki", this.rightService
            .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true,
                getContext()));

        getContext().setDatabase(doc.getDatabase());

        assertTrue("Guest does not have right on the document when tested from the document wiki", this.rightService
            .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true,
                getContext()));
    }
}
