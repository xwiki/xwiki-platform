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
import java.util.HashMap;
import java.util.Map;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({DefaultExecutionContextManager.class, DefaultExecution.class })
class XWikiRightServiceImplTest
{
    private static final EntityReference XWIKIPREFERENCES_REFERENCE = new EntityReference("XWikiPreferences",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private XWikiContext context;
    private XWikiRightServiceImpl rightService;
    private XWikiGroupService groupService;
    private XWiki xwiki;

    private XWikiDocument user;

    private XWikiDocument group;

    private XWikiDocument group2;

    @BeforeEach
    void setUp(MockitoOldcore mockitoOldcore) throws Exception
    {
        this.context = mockitoOldcore.getXWikiContext();
        this.rightService = new XWikiRightServiceImpl();
        this.groupService = mockitoOldcore.getMockGroupService();

        this.xwiki = mockitoOldcore.getSpyXWiki();
        doAnswer(invocationOnMock -> new XWikiDocument(new DocumentReference("xwiki",
            (String) invocationOnMock.getArgument(0), "WebPreferences")))
            .when(this.xwiki).getDocument(any(String.class), eq("WebPreferences"), eq(this.context));
        doReturn(null).when(this.xwiki).getWikiOwner(any(), any());

        this.user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        this.user.setNew(false);
        this.context.setWikiId(this.user.getWikiName());
        BaseObject userObject = new BaseObject();
        userObject.setClassName("XWiki.XWikiUser");
        this.user.addXObject(userObject);
        when(this.xwiki.getDocument(this.user.getPrefixedFullName(), context)).thenReturn(this.user);
        

        this.group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));
        this.group.setNew(false);
        this.context.setWikiId(this.group.getWikiName());
        BaseObject groupObject = new BaseObject();
        groupObject.setClassName("XWiki.XWikiGroups");
        groupObject.setStringValue("member", this.user.getFullName());
        this.group.addXObject(groupObject);
        when(this.xwiki.getDocument(this.group.getPrefixedFullName(), context)).thenReturn(this.group);
        

        this.group2 = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "group2"));
        this.group2.setNew(false);
        this.context.setWikiId(this.group2.getWikiName());
        BaseObject group2Object = new BaseObject();
        group2Object.setClassName("XWiki.XWikiGroups");
        group2Object.setStringValue("member", this.user.getPrefixedFullName());
        this.group2.addXObject(groupObject);
        when(this.xwiki.getDocument(this.group2.getPrefixedFullName(), context)).thenReturn(this.group2);

        when(this.groupService.getAllGroupsReferencesForMember(eq(this.user.getDocumentReference()), anyInt(), 
            anyInt(), eq(this.context)))
            .thenAnswer(invocationOnMock -> {
            XWikiContext context = invocationOnMock.getArgument(3);

            if (context.getWikiId().equals(group.getWikiName())) {
                return Collections.singleton(group.getDocumentReference());
            } else if (context.getWikiId().equals(group2.getWikiName())) {
                return Collections.singleton(group2.getDocumentReference());
            } else {
                return Collections.emptyList();
            }
        });
    }

    /**
     * Test if checkRight() take care of users's groups from other wikis.
     */
    @Test
    void checkRight() throws XWikiRightNotFoundException, XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki2", "Space", "Page"));

        BaseObject globalRightObj = mock(BaseObject.class);
        when(globalRightObj.getStringValue("levels")).thenReturn("view");
        when(globalRightObj.getStringValue("groups")).thenReturn(this.group.getPrefixedFullName());
        when(globalRightObj.getIntValue("allow")).thenReturn(1);

        doc.addObject("XWiki.XWikiGlobalRights", globalRightObj);

        context.setWikiId("wiki2");

        boolean result =
            this.rightService.checkRight(this.user.getPrefixedFullName(), doc, "view", true, true, true, context);

        assertTrue(result, this.user.getPrefixedFullName() + " does not have global view right on wiki2");
    }

    @Test
    void hasAccessLevelAdminOnDocument() throws Exception
    {
        final String wikiName = this.user.getWikiName();

        final XWikiDocument doc = new XWikiDocument(new DocumentReference(wikiName,
                                                                          "Space", "Page"));

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference(wikiName,
                                                                                  "XWiki", "XWikiPreference"));

        BaseObject rightsObject = new BaseObject();
        rightsObject.setClassName("XWiki.XWikiRights");
        rightsObject.setStringValue("levels", "admin");
        rightsObject.setStringValue("users", this.user.getPrefixedFullName());
        rightsObject.setIntValue("allow", 1);
        doc.addXObject(rightsObject);

        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 0);
        preferencesObject.setStringValue("users", this.user.getPrefixedFullName());
        preferences.addXObject(preferencesObject);
        preferences.setNew(false);

        doAnswer(invocationOnMock -> {
            if (!this.context.getWikiId().equals(wikiName)) {
                new XWikiDocument(new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreference"));
            }

            return preferences;
        }).when(this.xwiki).getDocument(XWIKIPREFERENCES_REFERENCE, this.context);
        doReturn(doc).when(this.xwiki).getDocument(doc.getPrefixedFullName(), this.context);
        this.context.setWikiId(wikiName);

        assertFalse(this.rightService.hasAccessLevel("admin", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true, this.context),
            "Admin rights must not be considered when set on document level.");
    }

    @Test
    void hasAccessLevelWhithUserFromAnotherWiki() throws XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference(this.group2.getWikiName(), "Space", "Page"));

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "XWikiPreference"));
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "view");
        preferencesObject.setIntValue("allow", 1);
        preferences.addXObject(preferencesObject);
        preferences.setNew(false);

        doAnswer(invocationOnMock -> {
            if (!this.context.getWikiId().equals("wiki2")) {
                new XWikiDocument(new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreference"));
            }

            return preferences;
        }).when(this.xwiki).getDocument(XWIKIPREFERENCES_REFERENCE, this.context);
        doReturn(doc).when(this.xwiki).getDocument(doc.getPrefixedFullName(), this.context);
        this.context.setWikiId("wiki");

        assertFalse(this.rightService.hasAccessLevel("view",
            this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true, this.context),
            "User from another wiki has right on a local wiki");

        // direct user rights

        preferencesObject.setStringValue("users", this.user.getPrefixedFullName());

        this.context.setWikiId(this.user.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "User from another wiki does not have right on a local wiki when tested from user wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
            this.context), "User from another wiki does not have right on a local wiki when tested from user wiki");

        this.context.setWikiId(doc.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "User from another wiki does not have right on a local wiki when tested from local wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
            this.context), "User from another wiki does not have right on a local wiki when tested from local wiki");

        // user group rights

        preferencesObject.removeField("users");

        // group from user's wiki

        preferencesObject.setStringValue("groups", this.group.getPrefixedFullName());

        this.context.setWikiId(this.user.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "User group from another wiki does not have right on a local wiki when tested from user wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
            this.context),
            "User group from another wiki does not have right on a local wiki when tested from user wiki");

        this.context.setWikiId(doc.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "User group from another wiki does not have right on a local wiki when tested from local wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
            this.context),
            "User group from another wiki does not have right on a local wiki when tested from local wiki");

        // group from document's wiki

        preferencesObject.setStringValue("groups", this.group2.getFullName());

        this.context.setWikiId(this.user.getWikiName());
        
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "User group from another wiki does not have right on a local wiki when tested from user wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
            this.context),
            "User group from another wiki does not have right on a local wiki when tested from user wiki");

        this.context.setWikiId(doc.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "User group from another wiki does not have right on a local wiki when tested from local wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
            this.context),
            "User group from another wiki does not have right on a local wiki when tested from local wiki");

        // user is wiki owner

        preferencesObject.removeField("groups");
        when(this.xwiki.getWikiOwner(doc.getWikiName(), this.context)).thenReturn(this.user.getPrefixedFullName());
        this.context.setWikiId(this.user.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "Wiki owner from another wiki does not have right on a local wiki when tested from user wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(), true,
            this.context),
            "Wiki owner group from another wiki does not have right on a local wiki when tested from user wiki");

        this.context.setWikiId(doc.getWikiName());

        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getPrefixedFullName(),
            true, this.context),
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki");
        assertTrue(this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(), true,
            this.context),
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki");
    }

    @Test
    void hasAccessLevelWhithOnlyPageAsReference() throws XWikiException
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

        when(this.xwiki.getDocument(preferences.getSpaceName(), preferences.getPageName(), this.context))
            .thenReturn(preferences);
        when(this.xwiki.getDocument(XWIKIPREFERENCES_REFERENCE, this.context)).thenReturn(new XWikiDocument(
            new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreferences")));
        when(this.xwiki.getDocument(doc.getPrefixedFullName(), this.context)).thenReturn(doc);

        this.context.setWikiId("wiki");
        this.context.setDoc(doc);

        assertFalse(this.rightService.hasAccessLevel("view", this.user.getPageName(), doc.getPageName(), true,
            this.context), "Failed to check right with only page name");
    }

    /**
     * Test that programming rights are checked on the context user when no context document is set.
     */
    @Test
    void programmingRightsWhenNoContextDocumentIsSet() throws XWikiException
    {
        // Setup an XWikiPreferences document granting programming rights to XWiki.Programmer
        XWikiDocument prefs = new XWikiDocument(new DocumentReference(this.context.getMainXWiki(), "XWiki",
            "XWikiPreferences"));
        BaseObject globalRightObj = mock(BaseObject.class);
        when(globalRightObj.getStringValue("levels")).thenReturn("programming");
        when(globalRightObj.getStringValue("users")).thenReturn("XWiki.Programmer");
        when(globalRightObj.getIntValue("allow")).thenReturn(1);
        prefs.addObject("XWiki.XWikiGlobalRights", globalRightObj);
        when(this.xwiki.getDocument(XWIKIPREFERENCES_REFERENCE, this.context)).thenReturn(prefs);

        // Setup the context (no context document)
        when(this.xwiki.getDatabase()).thenReturn("xwiki");
        this.context.remove("doc");
        this.context.remove("sdoc");

        this.context.setWikiId(this.context.getMainXWiki());

        // XWiki.Programmer should have PR, as per the global rights.
        this.context.setUser("XWiki.Programmer");
        assertTrue(this.rightService.hasProgrammingRights(this.context));

        // Guests should not have PR
        this.context.setUser(XWikiRightService.GUEST_USER_FULLNAME);
        assertFalse(this.rightService.hasProgrammingRights(this.context));

        // superadmin should always have PR
        this.context.setUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        assertTrue(this.rightService.hasProgrammingRights(this.context));
    }

    @Test
    void hasAccessLevelWhithGuestUser() throws XWikiException
    {
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki2", "Space", "Page"));

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "XWikiPreference"));
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "view");
        preferencesObject.setIntValue("allow", 1);
        preferences.addXObject(preferencesObject);

        when(this.xwiki.getDocument(XWIKIPREFERENCES_REFERENCE, this.context)).thenAnswer(invocationOnMock -> {
            if (!this.context.getWikiId().equals("wiki2")) {
                new XWikiDocument(new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreference"));
            }

            return preferences;
        });
        when(this.xwiki.getDocument(doc.getPrefixedFullName(), this.context)).thenReturn(doc);

        this.context.setWikiId("wiki");

        assertFalse(this.rightService.hasAccessLevel("view",
            XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true, this.context),
            "Guest has wiew right on the document");

        // direct user rights

        preferencesObject.setStringValue("users", XWikiRightService.GUEST_USER_FULLNAME);

        this.context.setWikiId("wiki");

        assertTrue(this.rightService
            .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true,
                this.context),
            "Guest does not have right on the document when tested from another wiki");

        this.context.setWikiId(doc.getDatabase());

        assertTrue(this.rightService
            .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true,
                this.context),
            "Guest does not have right on the document when tested from the document wiki");
    }

    /**
     * This test will fail unless:
     * SuperAdmin has programming permission before calling dropPermissions().
     * SuperAdmin does not have programming permission after calling dropPermissions().
     */
    @Test
    void programmingRightsAfterDropPermissions()
    {
        // Nobody even superadmin gets PR after they have given it up.
        this.context.setUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);

        assertTrue(this.rightService.hasProgrammingRights(this.context),
            "User does not have programming right prior to calling dropPermissions()");
        this.context.dropPermissions();
        assertFalse(this.rightService.hasProgrammingRights(this.context),
            "Author retains programming right after calling dropPermissions()");
    }

    /**
     * 
     * This test will fail unless:
     * SuperAdmin has programming permission before calling Document#dropPermissions().
     * SuperAdmin does not have programming permission after calling dropPermissions().
     */
    @Test
    void programmingRightsAfterDropPermissionsForRenderingCycle(MockitoComponentManager componentManager)
        throws Exception
    {
        final Document doc =
            new Document(new XWikiDocument(new DocumentReference("XWiki", "Test", "Permissions")), this.context);

       // doc.setContentAuthor(XWikiRightService.SUPERADMIN_USER_FULLNAME);

        //this.context.setDoc(doc);
        this.context.setUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);

        assertTrue(this.rightService.hasProgrammingRights(this.context),
            "User does not have programming right prior to calling doc.dropPermissions()");

        final Map<String, Object> backup = new HashMap<String, Object>();
        XWikiDocument.backupContext(backup, this.context);

        doc.dropPermissions();

        assertFalse(this.rightService.hasProgrammingRights(this.context), 
            "Author retains programming right after calling doc.dropPermissions()");

        final Map<String, Object> backup2 = new HashMap<String, Object>();
        XWikiDocument.backupContext(backup2, this.context);

        assertTrue(this.rightService.hasProgrammingRights(this.context),
            "User does not have programming right after switching contexts.");

        XWikiDocument.restoreContext(backup2, this.context);

        assertFalse(this.rightService.hasProgrammingRights(this.context),
            "Author did not lose programming right after switching contexts back.");

        XWikiDocument.restoreContext(backup, this.context);

        assertTrue(this.rightService.hasProgrammingRights(this.context),
            "Author did not regain programming right after switching contexts back.");
    }

    @Test
    void hasAccessLevelForDeleteRightWhenUserIsDocumentCreator() throws Exception
    {
        this.context.setWikiId(this.user.getWikiName());
        final XWikiDocument doc = new XWikiDocument(new DocumentReference(this.user.getWikiName(), "Space", "Page"));

        // Set the creator to be the user we test against since creator should get delete rights
        doc.setCreatorReference(this.user.getDocumentReference());

        when(this.xwiki.getDocument(doc.getPrefixedFullName(), this.context)).thenReturn(doc);
        final XWikiDocument xwikiPreferences = new XWikiDocument(
            new DocumentReference(this.user.getWikiName(), "XWiki", "XWikiPreferences"));
        when(this.xwiki.getDocument("XWiki.XWikiPreferences", this.context)).thenReturn(xwikiPreferences);

        assertTrue(this.rightService.hasAccessLevel("delete", this.user.getFullName(), doc.getFullName(), true,
            this.context), "Should allow delete rights for page creator");
    }

    private void assertAccessLevelForGuestUser(String level, XWikiDocument doc, boolean shouldAllow) throws Exception
    {

        if (shouldAllow) {
            assertTrue(this.rightService.hasAccessLevel(level, XWikiRightService.GUEST_USER_FULLNAME,
                doc.getFullName(), this.context),
                "Empty wiki should allow " + level + " for guest.");
        } else {
            assertFalse(this.rightService.hasAccessLevel(level, XWikiRightService.GUEST_USER_FULLNAME,
                doc.getFullName(), this.context), "Empty wiki should deny " + level + " for guest.");
        }
    }

    @Test
    void hasAccessLevelOnEmptyWiki() throws Exception
    {
        this.context.setWikiId("xwiki");

        final XWikiDocument doc
            = new XWikiDocument(new DocumentReference(this.context.getWikiId(), "Space", "Page"));

        final XWikiDocument xwikiPreferences
            = new XWikiDocument(new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreference"));

        when(this.xwiki.getDocument(doc.getFullName(), this.context)).thenReturn(doc);
        when(this.xwiki.getDocument(XWIKIPREFERENCES_REFERENCE, this.context)).thenReturn(xwikiPreferences);

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
            assertTrue(this.rightService.hasAccessLevel(level, this.context.getWikiId() + ":XWiki.Admin",
                doc.getFullName(), this.context), level + " for admin should be allowed.");
        } else {
            assertFalse(this.rightService.hasAccessLevel(level, this.context.getWikiId() + ":XWiki.Admin",
                doc.getFullName(), this.context), level + " for admin should be denied.");
        }
    }

    @Test
    void adminAccessLevels() throws Exception
    {
        this.context.setWikiId("xwiki");
        
        final XWikiDocument doc
            = new XWikiDocument(new DocumentReference(this.context.getWikiId(), "Space", "Page"));

        final XWikiDocument xwikiPreferences
            = new XWikiDocument(new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreference"));

        when(this.xwiki.getDocument(doc.getFullName(), this.context)).thenReturn(doc);
        when(this.xwiki.getDocument(doc.getPrefixedFullName(), this.context)).thenReturn(doc);
        when(this.xwiki.getDocument(XWIKIPREFERENCES_REFERENCE, this.context)).thenReturn(xwikiPreferences);
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 1);
        preferencesObject.setStringValue("users", this.context.getWikiId() + ":XWiki.Admin");
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
    @Test
    void editRightsOnWebPreferencesDocuments() throws Exception
    {
        this.user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        this.user.setNew(false);
        this.context.setWikiId(this.user.getWikiName());
        BaseObject userObject = new BaseObject();
        userObject.setClassName("XWiki.XWikiUser");
        this.user.addXObject(userObject);
        when(this.xwiki.getDocument(user.getPrefixedFullName(), this.context)).thenReturn(user);

        this.context.setWikiId(this.user.getWikiName());
        final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki", "Space", "Document"));

        when(this.xwiki.getDocument(doc.getPrefixedFullName(), this.context)).thenReturn(doc);

        final XWikiDocument preferences = new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiPreference"));

        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 0);
        preferencesObject.setStringValue("users", "xwiki:XWiki.UserA");
        preferences.addXObject(preferencesObject);

        when(this.xwiki.getDocument("wiki:Space.WebPreferences", this.context)).thenReturn(
            new XWikiDocument(new DocumentReference("wiki", "Space", "WebPreferences")));
        when(this.xwiki.getDocument("wiki:XWiki.XWikiPreferences", this.context)).thenReturn(
            new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiPreferences")));
        when(this.xwiki.getDocument("wiki:Space.XWikiPreferences", this.context)).thenReturn(
            new XWikiDocument(new DocumentReference("wiki", "Space", "XWikiPreferences")));
        when(this.xwiki.getDocument(XWIKIPREFERENCES_REFERENCE, this.context)).thenAnswer(invocationOnMock -> {
            if (!this.context.getWikiId().equals("wiki")) {
                new XWikiDocument(new DocumentReference(this.context.getWikiId(), "XWiki", "XWikiPreference"));
            }

            return preferences;
        });

        assertFalse(rightService.hasAccessLevel("programming", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences",
            this.context), "Programming rights have not been configured.");

        assertFalse(rightService.hasAccessLevel("admin", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences",
            this.context), "Admin rights have not been configured.");

        assertFalse(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences",
            this.context), "Shouldn't allow edit rights by default on WebPreferences documents.");

        assertFalse(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:XWiki.XWikiPreferences",
            this.context), "Edit rights should be denied by default on XWiki.XWikiPreferences");

        assertTrue(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.XWikiPreferences",
            this.context), "Other documents named XWikiPreferences should be unaffected.");

        preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "edit");
        preferencesObject.setIntValue("allow", 1);
        preferencesObject.setStringValue("users", "xwiki:XWiki.UserA");
        preferences.addXObject(preferencesObject);

        assertTrue(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.Document", this.context),
            "Edit rights have been configured.");

        assertFalse(rightService.hasAccessLevel("admin", "xwiki:XWiki.UserA", "wiki:Space.Document", this.context),
            "No admin rights have been configured.");

        assertFalse(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences",
            this.context), "Edit rights should be denied WebPreferences document for non-admin users.");

        assertFalse(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:XWiki.XWikiPreferences",
            this.context), "Edit rights should be denied XWiki.XWikiPreferences document for non-admin users.");

        preferencesObject = new BaseObject();
        preferencesObject.setClassName("XWiki.XWikiGlobalRights");
        preferencesObject.setStringValue("levels", "admin");
        preferencesObject.setIntValue("allow", 1);
        preferencesObject.setStringValue("users", "xwiki:XWiki.UserA");
        preferences.addXObject(preferencesObject);

        assertTrue(rightService.hasAccessLevel("admin", "xwiki:XWiki.UserA", "wiki:Space.Document", this.context),
            "Admin rights have been configured.");

        assertTrue(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:Space.WebPreferences", this.context),
            "Edit rights should be granted on WebPreferences document for admin users.");

        assertTrue(rightService.hasAccessLevel("edit", "xwiki:XWiki.UserA", "wiki:XWiki.XWikiPreferences",
            this.context), "Edit rights should be granted on XWiki.XWikiPreferences document for non-admin users.");


    }

    // This is currently a proof-of-behavior test to show that if a document prevents you from editing
    // it, calling hasAccessLevel('create') on that document will also fail.
    // Changing this behavior is proposed here: http://lists.xwiki.org/pipermail/devs/2013-March/053802.html
    // See also: https://jira.xwiki.org/browse/XWIKI-8892
    @Test
    void deniesAccessLevelForCreateIfDocumentDeniesEdit() throws Exception
    {
        this.context.setWikiId(this.user.getWikiName());
        final XWikiDocument doc = new XWikiDocument(new DocumentReference(this.user.getWikiName(), "Space", "Page"));

        // Set the creator to be the user we test against since creator should get delete rights
        BaseObject xo = new BaseObject();
        xo.setClassName("XWiki.XWikiRights");
        xo.setStringValue("levels", "edit");
        xo.setStringValue("users", user.getFullName());
        xo.setIntValue("allow", 0);
        doc.addXObject(xo);

        DocumentReference dr = new DocumentReference(this.user.getWikiName(), "XWiki", "XWikiPreferences");
        doReturn(new XWikiDocument(new DocumentReference(dr)))
            .when(this.xwiki).getDocument(any(EntityReference.class), eq(this.context));
        when(this.xwiki.getDocument(doc.getPrefixedFullName(), this.context)).thenReturn(doc);

        when(this.xwiki.checkAuth(this.context)).thenReturn(new XWikiUser(this.user.getFullName()));
        when(this.xwiki.getRightService()).thenReturn(this.rightService);

        assertFalse(this.rightService.checkAccess("edit", doc, this.context),
            "Should not have edit permission on document if it is denied at a document level");
        assertFalse(this.rightService.checkAccess("create", doc, this.context),
            "Should not have create permission on document if it is denied at a document level");
    }
}

