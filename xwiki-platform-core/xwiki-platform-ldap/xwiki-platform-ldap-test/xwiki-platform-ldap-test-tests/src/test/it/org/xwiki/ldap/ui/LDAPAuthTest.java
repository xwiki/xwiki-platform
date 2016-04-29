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
package org.xwiki.ldap.ui;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.ldap.framework.LDAPRunner;
import org.xwiki.ldap.framework.LDAPTestSetup;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.AbstractGuestTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.object;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * Verify the LDAP login and logout features.
 * 
 * @version $Id$
 */
public class LDAPAuthTest extends AbstractGuestTest
{
    private ViewPage vp;

    /**
     * Tool to start and stop embedded LDAP server.
     */
    private static LDAPRunner ldap;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        if (System.getProperty(LDAPTestSetup.SYSPROPNAME_LDAPPORT) == null) {
            ldap = new LDAPRunner();
            ldap.start();
        }
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        if (ldap != null) {
            ldap.stop();
        }
    }

    /**
     * Validate that it tries to log as "common" XWiki login if user is not found in LDAP.
     */
    @Test
    public void testLogAsXWikiUser()
    {
        getUtil().loginAsSuperAdmin();
    }

    /**
     * Validate that it success to authenticate with LDAP user. Also the user id contains space character.
     */
    @Test
    public void testLogAsLDAPUser() throws Exception
    {
        // ///////////////////
        // Validate normal login
        getUtil().login(LDAPTestSetup.HORATIOHORNBLOWER_CN, LDAPTestSetup.HORATIOHORNBLOWER_PWD);
        this.vp = getUtil().gotoPage("Main", "WebHome");

        // Logout
        this.vp.logout();

        // ///////////////////
        // Validate exclusion group
        LoginPage loginPage = LoginPage.gotoPage();
        loginPage.loginAs(LDAPTestSetup.THOMASQUIST_CN, LDAPTestSetup.THOMASQUIST_PWD, true);
        assertFalse(LDAPTestSetup.THOMASQUIST_CN + " user has been authenticated", this.vp.isAuthenticated());

        // ///////////////////
        // Validate XE-136: log with LDAP user then search for provided user uid/pass
        getUtil().setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        Page page = getUtil().rest().page(new LocalDocumentReference("XWiki", "XWikiPreferences"));
        org.xwiki.rest.model.jaxb.Object obj = object("XWiki.XWikiPreferences");
        obj.getProperties().add(property("ldap_bind_DN", LDAPTestSetup.HORATIOHORNBLOWER_DN));
        obj.getProperties().add(property("ldap_bind_pass", LDAPTestSetup.HORATIOHORNBLOWER_PWD));
        obj.getProperties().add(property("ldap_UID_attr", LDAPTestSetup.LDAP_USERUID_FIELD_UID));
        obj.getProperties().add(property("fields_mapping", "name=" + LDAPTestSetup.LDAP_USERUID_FIELD_UID
            + ",last_name=sn,first_name=givenname,fullname=description,email=mail"));
        obj.getProperties()
            .add(property("ldap_group_mapping", "XWiki.XWikiAdminGroup=cn=HMS Lydia,ou=crews,ou=groups,o=sevenSeas"));
        page.setObjects(new Objects());
        page.getObjects().getObjectSummaries().add(obj);
        getUtil().rest().save(page);
        // Wait for group cache invalidation
        Thread.sleep(1000);
        getUtil().login(LDAPTestSetup.WILLIAMBUSH_UID, LDAPTestSetup.WILLIAMBUSH_PWD);

        // ///////////////////
        // Validate
        // - XWIKI-2205: case insensitive user uid
        // - XWIKI-2202: LDAP user update corrupt XWiki user page
        getUtil().login(LDAPTestSetup.WILLIAMBUSH_UID_MIXED, LDAPTestSetup.WILLIAMBUSH_PWD);

        // ///////////////////
        // Validate XWIKI-2201: LDAP group mapping defined in XWikiPreferences is not working
        getUtil().setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        org.xwiki.rest.model.jaxb.Object groupMember = getUtil().rest().get(
            new ObjectReference("XWiki.XWikiGroups[0]", new DocumentReference("xwiki", "XWiki", "XWikiAdminGroup")));
        // Make sure the user is only added once to the group
        assertEquals(1, groupMember.getProperties().size());
        // Make sure the added with the right reference
        assertEquals("XWiki." + LDAPTestSetup.WILLIAMBUSH_UID, groupMember.getProperties().get(0).getValue());

        // ///////////////////
        // Validate
        // - XWIKI-2264: LDAP authentication does not support "." in login names
        getUtil().login(LDAPTestSetup.USERWITHPOINTS_UID, LDAPTestSetup.USERWITHPOINTS_PWD);
    }
}
