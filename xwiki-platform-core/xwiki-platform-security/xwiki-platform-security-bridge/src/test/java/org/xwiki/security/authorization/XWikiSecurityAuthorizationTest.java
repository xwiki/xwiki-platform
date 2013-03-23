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

package org.xwiki.security.authorization;

import org.junit.Test;

import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.security.authorization.Right.COMMENT;
import static org.xwiki.security.authorization.Right.DELETE;
import static org.xwiki.security.authorization.Right.EDIT;
import static org.xwiki.security.authorization.Right.ILLEGAL;
import static org.xwiki.security.authorization.Right.LOGIN;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.REGISTER;
import static org.xwiki.security.authorization.Right.VIEW;

/**
 * Test XWiki Authorization policy against the authentication module.
 * @since 5.0M2
 */
public class XWikiSecurityAuthorizationTest extends AbstractAuthorizationTestCase
{
    @Test
    public void testDefaultAccessOnEmptyWikis() throws Exception
    {
        initialiseWikiMock("emptyWikis");

        // Public access on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            null, getXDoc("an empty main wiki", "anySpace"));

        // SuperAdmin access on main wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, ADMIN, PROGRAM, ILLEGAL),
            SUPERADMIN, getXDoc("an empty main wiki", "anySpace"));

        // Public access on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, REGISTER, LOGIN),
            null, getDoc("an empty sub wiki", "anySpace", "any SubWiki"));

        // SuperAdmin access on sub wiki
        assertAccess(new RightSet(VIEW, EDIT, COMMENT, DELETE, REGISTER, LOGIN, ADMIN, PROGRAM, ILLEGAL),
            SUPERADMIN, getDoc("an empty sub wiki", "anySpace", "any SubWiki"));
    }

    @Test
    public void testInheritancePolicyForFullFarmAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyFullFarmAccess");

        // Main wiki allowing all access to A
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("any document", "spaceDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("docAllowA",    "spaceDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getXDoc("docDenyA",     "any space"));

        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "any space",  "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiNoRules"));

        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "any space",   "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS, getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiDenyA"));
    }

    @Test
    public void testInheritancePolicyForGlobalFullWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForGlobalFullWikiAccess");

        // Main wiki denying all access to A
        assertAccess(null,                         getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getXDoc("any document", "spaceAllowA"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getXDoc("docDenyA",     "spaceAllowA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getXUser("userA"), getXDoc("docAllowA",    "any space"));

        assertAccess(null,                         getXUser("userA"), getDoc("any document", "any space",   "wikiNoRules"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_SPACE_RIGHTS,             getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiNoRules"));

        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getXUser("userA"), getDoc("any document", "any space",  "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiAllowA"));
    }

    @Test
    public void testInheritancePolicyForLocalWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForLocalWikiAccess");

        // Main wiki denying all access to A
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiAllowA"), getDoc("any document", "any space",  "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiAllowA"), getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiAllowA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiAllowA"), getDoc("docDenyA",     "any space",  "wikiAllowA"));

        assertAccess(null,                         getUser("userA", "wikiDenyA"), getDoc("any document", "any space",   "wikiDenyA"));
        assertAccess(ALL_SPACE_RIGHTS,             getUser("userA", "wikiDenyA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_SPACE_RIGHTS,             getUser("userA", "wikiDenyA"), getDoc("docDenyA",     "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getUser("userA", "wikiDenyA"), getDoc("any document", "spaceAllowANoAdmin", "wikiDenyA"));
        assertAccess(null,                         getUser("userA", "wikiDenyA"), getDoc("docDenyA",     "spaceAllowANoAdmin", "wikiDenyA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,          getUser("userA", "wikiDenyA"), getDoc("docAllowA",    "any space",   "wikiDenyA"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getUser("userA", "wikiAllowNoAdminA"), getDoc("any document", "any space",  "wikiAllowNoAdminA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiAllowNoAdminA"), getDoc("any document", "spaceDenyA", "wikiAllowNoAdminA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getUser("userA", "wikiAllowNoAdminA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowNoAdminA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getUser("userA", "wikiAllowNoAdminA"), getDoc("docDenyA",     "any space",  "wikiAllowNoAdminA"));
    }

    @Test
    public void testInheritancePolicyForNoAdminFarmAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForNoAdminFarmAccess");

        // Main wiki allowing all but admin access to A
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("any document", "spaceDenyA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getXUser("userA"), getXDoc("docAllowA",    "spaceDenyA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getXDoc("docDenyA",     "any space"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getXUser("userA"), getDoc("any document", "any space",  "wikiNoRules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiNoRules"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiNoRules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiNoRules"));

        assertAccess(new RightSet(REGISTER),        getXUser("userA"), getDoc("any document", "any space",   "wikiDenyA"));
        assertAccess(ALL_RIGHTS_EXCEPT_LOGIN,       getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiDenyA"));
        assertAccess(new RightSet(REGISTER),        getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiDenyA"));
        assertAccess(ALL_RIGHTS_EXCEPT_LOGIN,       getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiDenyA"));
    }

    @Test
    public void testInheritancePolicyForNoAdminWikiAccess() throws Exception
    {
        initialiseWikiMock("inheritancePolicyForNoAdminWikiAccess");

        // Main wiki denying all access to A
        assertAccess(null,                          getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getXDoc("any document", "spaceAllowA"));
        assertAccess(null,                          getXUser("userA"), getXDoc("docDenyA",     "spaceAllowA"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getXDoc("docAllowA",    "any space"));

        assertAccess(null,                          getXUser("userA"), getDoc("any document", "any space",   "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getDoc("any document", "spaceAllowA", "wikiNoRules"));
        assertAccess(null,                          getXUser("userA"), getDoc("docDenyA",     "spaceAllowA", "wikiNoRules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userA"), getDoc("docAllowA",    "any space",   "wikiNoRules"));

        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getXUser("userA"), getDoc("any document", "any space",  "wikiAllowA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("any document", "spaceDenyA", "wikiAllowA"));
        assertAccess(ALL_RIGHTS_EXCEPT_ADMIN,       getXUser("userA"), getDoc("docAllowA",    "spaceDenyA", "wikiAllowA"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userA"), getDoc("docDenyA",     "any space",  "wikiAllowA"));
    }

    @Test
    public void testTieResolutionPolicy() throws Exception
    {
        initialiseWikiMock("tieResolutionPolicy");

        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiUserAllowDeny"), getWiki("wikiUserAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiUserDenyAllow"), getWiki("wikiUserDenyAllow"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiGroupAllowDeny"), getWiki("wikiGroupAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiGroupDenyAllow"), getWiki("wikiGroupDenyAllow"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiUserGroupAllowDeny"), getWiki("wikiUserGroupAllowDeny"));
        assertAccess(null,                         getUser("userA", "wikiUserGroupDenyAllow"), getWiki("wikiUserGroupDenyAllow"));
        assertAccess(null,                         getUser("userA", "wikiGroupUserAllowDeny"), getWiki("wikiGroupUserAllowDeny"));
        assertAccess(ALL_RIGHTS_EXCEPT_PROGRAMING, getUser("userA", "wikiGroupUserDenyAllow"), getWiki("wikiGroupUserDenyAllow"));
    }
}
