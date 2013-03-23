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
    public void testInheritancePolicyInMainWiki() throws Exception
    {
        initialiseWikiMock("inheritancePolicy");

        //
        // Main Wiki
        //

        assertAccess(ALL_RIGHTS,           getXUser("userA"), getXDoc("any document", "any space"));
        assertAccess(ALL_RIGHTS_BUT_ADMIN, getXUser("userB"), getXDoc("any document", "any space"));
        assertAccess(null,                 getXUser("userC"), getXDoc("any document", "any space"));
        assertAccess(null,                 getXUser("userD"), getXDoc("any document", "any space"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getXDoc("any document", "spaceCDnoAB"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userB"), getXDoc("any document", "spaceCDnoAB"));
        assertAccess(ALL_SPACE_RIGHTS,              getXUser("userC"), getXDoc("any document", "spaceCDnoAB"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userD"), getXDoc("any document", "spaceCDnoAB"));

        assertAccess(ALL_RIGHTS,           getXUser("userA"), getXDoc("docABnoCD", "spaceCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_ADMIN, getXUser("userB"), getXDoc("docABnoCD", "spaceCDnoAB"));
        assertAccess(ALL_SPACE_RIGHTS,     getXUser("userC"), getXDoc("docABnoCD", "spaceCDnoAB"));
        assertAccess(null,                 getXUser("userD"), getXDoc("docABnoCD", "spaceCDnoAB"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getXDoc("docCDnoAB", "any space"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userB"), getXDoc("docCDnoAB", "any space"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userC"), getXDoc("docCDnoAB", "any space"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userD"), getXDoc("docCDnoAB", "any space"));

        //
        // Subwiki with no rules
        //

        assertAccess(ALL_RIGHTS,           getXUser("userA"), getDoc("any document", "any space", "wikinorules"));
        assertAccess(ALL_RIGHTS_BUT_ADMIN, getXUser("userB"), getDoc("any document", "any space", "wikinorules"));
        assertAccess(null,                 getXUser("userC"), getDoc("any document", "any space", "wikinorules"));
        assertAccess(null,                 getXUser("userD"), getDoc("any document", "any space", "wikinorules"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getDoc("any document", "spaceCDnoAB", "wikinorules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userB"), getDoc("any document", "spaceCDnoAB", "wikinorules"));
        assertAccess(ALL_SPACE_RIGHTS,              getXUser("userC"), getDoc("any document", "spaceCDnoAB", "wikinorules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userD"), getDoc("any document", "spaceCDnoAB", "wikinorules"));

        assertAccess(ALL_RIGHTS,           getXUser("userA"), getDoc("docABnoCD", "spaceCDnoAB", "wikinorules"));
        assertAccess(ALL_RIGHTS_BUT_ADMIN, getXUser("userB"), getDoc("docABnoCD", "spaceCDnoAB", "wikinorules"));
        assertAccess(ALL_SPACE_RIGHTS,     getXUser("userC"), getDoc("docABnoCD", "spaceCDnoAB", "wikinorules"));
        assertAccess(null,                 getXUser("userD"), getDoc("docABnoCD", "spaceCDnoAB", "wikinorules"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getDoc("docCDnoAB", "any space", "wikinorules"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userB"), getDoc("docCDnoAB", "any space", "wikinorules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userC"), getDoc("docCDnoAB", "any space", "wikinorules"));
        assertAccess(ALL_DOCUMENT_RIGHTS,           getXUser("userD"), getDoc("docCDnoAB", "any space", "wikinorules"));

        //
        // Subwiki with inverted rules
        //

        assertAccess(ALL_RIGHTS,                getXUser("userA"), getDoc("any document", "any space", "wikiCDnoAB"));
        assertAccess(new RightSet(REGISTER),    getXUser("userB"), getDoc("any document", "any space", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_PROGRAMING, getXUser("userC"), getDoc("any document", "any space", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_ADMIN,      getXUser("userD"), getDoc("any document", "any space", "wikiCDnoAB"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getDoc("any document", "spaceABnoCD", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_LOGIN,          getXUser("userB"), getDoc("any document", "spaceABnoCD", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_PROGRAMING,     getXUser("userC"), getDoc("any document", "spaceABnoCD", "wikiCDnoAB"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userD"), getDoc("any document", "spaceABnoCD", "wikiCDnoAB"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getDoc("docCDnoAB", "spaceABnoCD", "wikiCDnoAB"));
        assertAccess(new RightSet(REGISTER),        getXUser("userB"), getDoc("docCDnoAB", "spaceABnoCD", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_PROGRAMING,     getXUser("userC"), getDoc("docCDnoAB", "spaceABnoCD", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_ADMIN,          getXUser("userD"), getDoc("docCDnoAB", "spaceABnoCD", "wikiCDnoAB"));

        assertAccess(ALL_RIGHTS,                    getXUser("userA"), getDoc("docABnoCD", "any space", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_LOGIN,          getXUser("userB"), getDoc("docABnoCD", "any space", "wikiCDnoAB"));
        assertAccess(ALL_RIGHTS_BUT_PROGRAMING,     getXUser("userC"), getDoc("docABnoCD", "any space", "wikiCDnoAB"));
        assertAccess(new RightSet(LOGIN, REGISTER), getXUser("userD"), getDoc("docABnoCD", "any space", "wikiCDnoAB"));
    }
}
