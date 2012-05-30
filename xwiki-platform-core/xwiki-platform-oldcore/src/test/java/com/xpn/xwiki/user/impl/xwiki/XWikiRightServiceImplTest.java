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
import java.util.Map;
import java.util.HashMap;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
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
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return new XWikiDocument(new DocumentReference(getContext().getDatabase(),
                        (String) invocation.parameterValues.get(0), "WebPreferences"));
                }
            });
        // Called from MessageToolVelocityContextInitializer.
        this.mockXWiki.stubs().method("prepareResources");

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
                @Override
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
                @Override
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
        XWikiDocument prefs = new XWikiDocument(new DocumentReference(getContext().getMainXWiki(), "XWiki", "XWikiPreferences"));
        Mock mockGlobalRightObj = mock(BaseObject.class, new Class[] {}, new Object[] {});
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("levels")).will(returnValue("programming"));
        mockGlobalRightObj.stubs().method("getStringValue").with(eq("users")).will(returnValue("XWiki.Programmer"));
        mockGlobalRightObj.stubs().method("getIntValue").with(eq("allow")).will(returnValue(1));
        mockGlobalRightObj.stubs().method("setNumber");
        mockGlobalRightObj.stubs().method("setDocumentReference");
        prefs.addObject("XWiki.XWikiGlobalRights", (BaseObject) mockGlobalRightObj.proxy());
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), eq(getContext()))
            .will(returnValue(prefs));
        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember")
            .with(eq(new DocumentReference(getContext().getMainXWiki(), "XWiki", "Programmer")), eq(0), eq(0), same(getContext()))
            .will(returnValue(Collections.EMPTY_LIST));

        // Setup the context (no context document)
        this.mockXWiki.stubs().method("getDatabase").will(returnValue("xwiki"));
        getContext().remove("doc");
        getContext().remove("sdoc");
      
        getContext().setDatabase(getContext().getMainXWiki());

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
                @Override
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

    /**
     * This test will fail unless:
     * SuperAdmin has programming permission before calling dropPermissions().
     * SuperAdmin does not have programming permission after calling dropPermissions().
     */
    public void testProgrammingRightsAfterDropPermissions()
    {
        // Nobody even superadmin gets PR after they have given it up.
        this.getContext().setUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);

        assertTrue("User does not have programming right prior to calling dropPermissions()",
                   this.rightService.hasProgrammingRights(this.getContext()));
        this.getContext().dropPermissions();
        assertFalse("Author retains programming right after calling dropPermissions()",
                   this.rightService.hasProgrammingRights(this.getContext()));
    }

    /**
     * 
     * This test will fail unless:
     * SuperAdmin has programming permission before calling Document#dropPermissions().
     * SuperAdmin does not have programming permission after calling dropPermissions().
     */
    public void testProgrammingRightsAfterDropPermissionsForRenderingCycle()
    {
        final Document doc =
            new Document(new XWikiDocument(new DocumentReference("XWiki", "Test", "Permissions")), this.getContext());

       // doc.setContentAuthor(XWikiRightService.SUPERADMIN_USER_FULLNAME);

        //this.getContext().setDoc(doc);
        this.getContext().setUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);

        assertTrue("User does not have programming right prior to calling "
                   + "doc.dropPermissions()",
                   this.rightService.hasProgrammingRights(this.getContext()));

        final Map<String, Object> backup = new HashMap<String, Object>();
        XWikiDocument.backupContext(backup, this.getContext());

        doc.dropPermissions();

        assertFalse("Author retains programming right after calling doc.dropPermissions()",
                   this.rightService.hasProgrammingRights(this.getContext()));

        final Map<String, Object> backup2 = new HashMap<String, Object>();
        XWikiDocument.backupContext(backup2, this.getContext());

        assertTrue("User does not have programming right after switching contexts.",
                   this.rightService.hasProgrammingRights(this.getContext()));

        XWikiDocument.restoreContext(backup2, this.getContext());

        assertFalse("Author did not lose programming right after switching contexts back.",
                   this.rightService.hasProgrammingRights(this.getContext()));

        XWikiDocument.restoreContext(backup, this.getContext());

        assertTrue("Author did not regain programming right after switching contexts back.",
                   this.rightService.hasProgrammingRights(this.getContext()));
    }

    public void testHasAccessLevelForDeleteRightWhenUserIsDocumentCreator() throws Exception
    {
        getContext().setDatabase(this.user.getWikiName());
        final XWikiDocument doc = new XWikiDocument(new DocumentReference(this.user.getWikiName(), "Space", "Page"));

        // Set the creator to be the user we test against since creator should get delete rights
        doc.setCreatorReference(this.user.getDocumentReference());

        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getPrefixedFullName()), ANYTHING).will(
            returnValue(doc));
        final XWikiDocument xwikiPreferences = new XWikiDocument(
            new DocumentReference(this.user.getWikiName(), "XWiki", "XWikiPreferences"));
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            returnValue(xwikiPreferences));

        assertTrue("Should allow delete rights for page creator",
            this.rightService.hasAccessLevel("delete", this.user.getFullName(), doc.getFullName(), true, getContext()));
    }

    private void assertAccessLevelForGuestUser(String level, XWikiDocument doc, boolean shouldAllow) throws Exception
    {

        if (shouldAllow) {
            assertTrue("Empty wiki should allow " + level + " for guest.",
                       this.rightService.hasAccessLevel(level, XWikiRightService.GUEST_USER_FULLNAME,
                                                        doc.getFullName(), getContext()));
        } else {
            assertFalse("Empty wiki should deny " + level + " for guest.",
                        this.rightService.hasAccessLevel(level, XWikiRightService.GUEST_USER_FULLNAME,
                                                         doc.getFullName(), getContext()));
        }
    }

    public void testHasAccessLevelOnEmptyWiki() throws Exception
    {
        getContext().setDatabase("xwiki");

        final XWikiDocument doc
            = new XWikiDocument(new DocumentReference(getContext().getDatabase(), "Space", "Page"));

        final XWikiDocument xwikiPreferences
            = new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreference"));

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember")
            .with(ANYTHING, ANYTHING, ANYTHING, ANYTHING).will(
                  returnValue(Collections.emptyList()));

        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getFullName()), ANYTHING)
            .will(returnValue(doc));

        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            returnValue(xwikiPreferences));

        this.mockXWiki.stubs().method("getXWikiPreference").with(ANYTHING, ANYTHING, ANYTHING).will(
            returnValue("false"));
        this.mockXWiki.stubs().method("getXWikiPreferenceAsInt").with(ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(0));
        this.mockXWiki.stubs().method("getSpacePreference").with(ANYTHING, ANYTHING, ANYTHING).will(
            returnValue("false"));
        this.mockXWiki.stubs().method("getSpacePreferenceAsInt").with(ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(0));

        assertAccessLevelForGuestUser("login"      , doc, true);
        assertAccessLevelForGuestUser("register"   , doc, true);
        assertAccessLevelForGuestUser("view"       , doc, true);
        assertAccessLevelForGuestUser("edit"       , doc, true);
        assertAccessLevelForGuestUser("delete"     , doc, true);
        assertAccessLevelForGuestUser("admin"      , doc, true);
        assertAccessLevelForGuestUser("programming", doc, false);
    }

    private void assertAccessLevelForAdminUser(String level, XWikiDocument doc, boolean shouldAllow) throws Exception
    {

        if (shouldAllow) {
            assertTrue(level + " for admin should be allowed.",
                       this.rightService.hasAccessLevel(level, getContext().getDatabase() + ":XWiki.Admin",
                                                        doc.getFullName(), getContext()));
        } else {
            assertFalse(level + " for admin should be denied.",
                        this.rightService.hasAccessLevel(level, getContext().getDatabase() + ":XWiki.Admin",
                                                         doc.getFullName(), getContext()));
        }
    }

    public void testAdminAccessLevels() throws Exception
    {
        getContext().setDatabase("xwiki");
        
        final XWikiDocument doc
            = new XWikiDocument(new DocumentReference(getContext().getDatabase(), "Space", "Page"));

        final XWikiDocument xwikiPreferences
            = new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreference"));

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember")
            .with(ANYTHING, ANYTHING, ANYTHING, ANYTHING).will(
                  returnValue(Collections.emptyList()));

        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getFullName()), ANYTHING)
            .will(returnValue(doc));

        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getPrefixedFullName()), ANYTHING)
            .will(returnValue(doc));

        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            returnValue(xwikiPreferences));

        this.mockXWiki.stubs().method("getXWikiPreference").with(ANYTHING, ANYTHING, ANYTHING).will(
            returnValue("false"));
        this.mockXWiki.stubs().method("getXWikiPreferenceAsInt").with(ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(0));
        this.mockXWiki.stubs().method("getSpacePreference").with(ANYTHING, ANYTHING, ANYTHING).will(
            returnValue("false"));
        this.mockXWiki.stubs().method("getSpacePreferenceAsInt").with(ANYTHING, ANYTHING, ANYTHING)
            .will(returnValue(0));

        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 1);
        preferencesObject.setStringValue("users", getContext().getDatabase() + ":XWiki.Admin");
        xwikiPreferences.addXObject(preferencesObject);

        assertAccessLevelForAdminUser("login"      , doc, true);
        assertAccessLevelForAdminUser("register"   , doc, true);
        assertAccessLevelForAdminUser("view"       , doc, true);
        assertAccessLevelForAdminUser("edit"       , doc, true);
        assertAccessLevelForAdminUser("delete"     , doc, true);
        assertAccessLevelForAdminUser("admin"      , doc, true);
        assertAccessLevelForAdminUser("programming", doc, true);

    }

    /**
     * Verify that edit rights is not sufficient for editing
     * *.WebPreferences and XWiki.XWikiPreferences, since that can be
     * used to elevate the privileges to admin.
     */
    public void testEditRightsOnWebPreferencesDocuments() throws Exception
    {

        this.mockGroupService.stubs().method("getAllGroupsReferencesForMember")
            .with(ANYTHING, ANYTHING, ANYTHING, ANYTHING).will(
                  returnValue(Collections.emptyList()));

        this.user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        this.user.setNew(false);
        getContext().setDatabase(this.user.getWikiName());
        BaseObject userObject = new BaseObject();
        userObject.setClassName("XWiki.XWikiUser");
        this.user.addXObject(userObject);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.user.getPrefixedFullName()), ANYTHING).will(
            returnValue(this.user));

        getContext().setDatabase(this.user.getWikiName());
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki", "Space", "Document"));

        this.mockXWiki.stubs().method("getDocument").with(eq(doc.getPrefixedFullName()), ANYTHING).will(
            returnValue(doc));

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiPreference"));

        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 0);
        preferencesObject.setStringValue("users", "xwiki:XWiki.UserA");
        preferences.addXObject(preferencesObject);

        this.mockXWiki.stubs().method("getDocument").with(eq("wiki:Space.WebPreferences"), ANYTHING)
            .will(returnValue(
                 new XWikiDocument(new DocumentReference("wiki",
                     "Space", "WebPreferences"))));

        this.mockXWiki.stubs().method("getDocument").with(eq("wiki:XWiki.XWikiPreferences"), ANYTHING)
            .will(returnValue(
                 new XWikiDocument(new DocumentReference("wiki",
                     "XWiki", "XWikiPreferences"))));

        this.mockXWiki.stubs().method("getDocument").with(eq("wiki:Space.XWikiPreferences"), ANYTHING)
            .will(returnValue(
                 new XWikiDocument(new DocumentReference("wiki",
                     "Space", "XWikiPreferences"))));

        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.XWikiPreferences"), ANYTHING).will(
            new CustomStub("Implements XWiki.getDocument")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    if (!getContext().getDatabase().equals("wiki")) {
                        new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreference"));
                    }

                    return preferences;
                }
            });

        assertFalse( "Programming rights have not been configured.",
            rightService.hasAccessLevel("programming", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences", getContext()));

        assertFalse( "Admin rights have not been configured.",
            rightService.hasAccessLevel("admin", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences", getContext()));

        assertFalse( "Shouldn't allow edit rights by default on WebPreferences documents.",
            rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences", getContext()));

        assertFalse( "Edit rights should be denied by default on XWiki.XWikiPreferences",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:XWiki.XWikiPreferences", getContext()));

        assertTrue( "Other documents named XWikiPreferences should be unaffected.",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.XWikiPreferences", getContext()));

        preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "edit");
        preferencesObject.setIntValue("allow", 1);
        preferencesObject.setStringValue("users", "xwiki:XWiki.UserA");
        preferences.addXObject(preferencesObject);

        assertTrue( "Edit rights have been configured.",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.Document", getContext()));

        assertFalse( "No admin rights have been configured.",
                    rightService.hasAccessLevel("admin", "xwiki:XWiki.UserA", "wiki:Space.Document", getContext()));

        assertFalse( "Edit rights should be denied WebPreferences document for non-admin users.",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences", getContext()));

        assertFalse( "Edit rights should be denied XWiki.XWikiPreferences document for non-admin users.",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:XWiki.XWikiPreferences", getContext()));

        preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 1);
        preferencesObject.setStringValue("users", "xwiki:XWiki.UserA");
        preferences.addXObject(preferencesObject);

        assertTrue( "Admin rights have been configured.",
                    rightService.hasAccessLevel("admin", "xwiki:XWiki.UserA", "wiki:Space.Document", getContext()));

        assertTrue( "Edit rights should be granted on WebPreferences document for admin users.",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences", getContext()));

        assertTrue( "Edit rights should be granted on XWiki.XWikiPreferences document for non-admin users.",
                    rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:XWiki.XWikiPreferences", getContext()));


    }


}
