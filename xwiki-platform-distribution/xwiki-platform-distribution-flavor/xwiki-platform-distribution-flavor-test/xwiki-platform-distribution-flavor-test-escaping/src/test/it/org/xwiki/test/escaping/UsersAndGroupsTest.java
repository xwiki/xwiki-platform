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
package org.xwiki.test.escaping;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.test.escaping.framework.AbstractEscapingTest;
import org.xwiki.test.escaping.framework.AbstractManualTest;
import org.xwiki.test.escaping.framework.XMLEscapingValidator;


/**
 * Manual tests for user and group name escaping. Creates a test group and user for the test run.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class UsersAndGroupsTest extends AbstractManualTest
{
    /** Name of the test user. */
    private static final String TEST_USER = "User" + XMLEscapingValidator.getTestString();

    /** Name of the test group. */
    private static final String TEST_GROUP = "Group" + XMLEscapingValidator.getTestString();

    /**
     * Set up the tests. Creates a test group and adds a test user to that group.
     * @throws InterruptedException 
     */
    @BeforeClass
    public static void init() throws InterruptedException
    {
        // create a test group
        AbstractEscapingTest.getUrlContent(createUrl("save", "XWiki", TEST_GROUP,
            params(kv("template", "XWiki.XWikiGroupTemplate"))));
        // create a test user
        AbstractEscapingTest.getUrlContent(createUrl("register", "XWiki", "XWikiPreferences",
            params(template("registerinline"), kv("xwikiname", "TEST_USER"),
                test("register_first_name"), test("register_last_name"), kv("register_email", ""),
                test("register_password"), test("register2_password"),
                kv("template", "XWiki.XWikiUserTemplate"), kv("xredirect", ""))));
        // create an even more evil user by renaming the user page
        AbstractEscapingTest.getUrlContent(createUrl("view", "XWiki", "TEST_USER",
            params(template("rename"), kv("step", "2"), kv("newSpaceName", "XWiki"), kv("newPageName", TEST_USER))));
        // add the test user to the test group
        AbstractEscapingTest.getUrlContent(createUrl("view", "XWiki", TEST_GROUP,
            params(template("adduorg"), kv("uorg", "user"), kv("name", "XWiki." + TEST_USER))));
    }

    /**
     * Clean up after the tests.
     */
    @AfterClass
    public static void shutdown()
    {
        // remove the test user from the test group
        AbstractEscapingTest.getUrlContent(createUrl("view", "XWiki", TEST_GROUP,
            params(template("deletegroupmember"), kv("fullname", "XWiki." + TEST_USER))));
        // delete the test user
        AbstractEscapingTest.getUrlContent(createUrl("admin", "XWiki", "XWikiPreferences",
            params(template("deleteuorg"), kv("docname", "XWiki." + TEST_USER))));
        // delete the test group
        AbstractEscapingTest.getUrlContent(createUrl("admin", "XWiki", "XWikiPreferences",
            params(template("deleteuorg"), kv("docname", "XWiki." + TEST_GROUP))));
    }

    @Test
    public void testGetUsers()
    {
        skipIfIgnored("templates/getusers.vm");
        Map<String, String> parameters = params(template("getusers"),
            test("offset"), test("limit"), test("wiki"), test("reqNo"), test("sort"), test("dir"));
        // language=en is seen as a filter, don't add it
        checkUnderEscaping(createUrl("view", null, null, parameters, false), "XWIKI-5244 (get users)");
    }

    @Test
    public void testGetGroups()
    {
        skipIfIgnored("templates/getgroups.vm");
        Map<String, String> parameters = params(template("getgroups"),
            test("offset"), test("limit"), test("wiki"), test("reqNo"), test("sort"), test("dir"));
        // language=en is seen as a filter, don't add it
        checkUnderEscaping(createUrl("view", null, null, parameters, false), "XWIKI-5244 (get groups)");
    }

    @Test
    public void testGetUsersAndGroups()
    {
        skipIfIgnored("templates/getusersandgroups.vm");
        Map<String, String> parameters = params(template("getusersandgroups"),
            test("offset"), test("limit"), test("wiki"), test("reqNo"), test("sort"), test("dir"),
            test("clsname"), test("space"), test("uorg"));
        // language=en is seen as a filter, don't add it
        checkUnderEscaping(createUrl("view", null, null, parameters, false), "XWIKI-5244 (get users and groups)");
    }

    @Test
    public void testGetUsersAndGroupsUsers()
    {
        skipIfIgnored("templates/getusersandgroups.vm");
        Map<String, String> parameters = params(template("getusersandgroups"),
            test("offset"), test("limit"), test("wiki"), test("reqNo"), test("sort"), test("dir"),
            test("clsname"), test("space"), kv("uorg", "users"));
        // language=en is seen as a filter, don't add it
        checkUnderEscaping(createUrl("view", null, null, parameters, false), "XWIKI-5244 (get users and groups: uorg=users)");
    }

    @Test
    public void testGetUsersAndGroupsGroups()
    {
        skipIfIgnored("templates/getusersandgroups.vm");
        Map<String, String> parameters = params(template("getusersandgroups"),
            test("offset"), test("limit"), test("wiki"), test("reqNo"), test("sort"), test("dir"),
            test("clsname"), test("space"), kv("uorg", "groups"));
        // language=en is seen as a filter, don't add it
        checkUnderEscaping(createUrl("view", null, null, parameters, false), "XWIKI-5244 (get users and groups: uorg=groups)");
    }
}

