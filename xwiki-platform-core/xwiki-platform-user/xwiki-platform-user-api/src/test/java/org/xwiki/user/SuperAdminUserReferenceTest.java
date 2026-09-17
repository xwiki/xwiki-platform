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
package org.xwiki.user;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SuperAdminUserReference}.
 *
 * @version $Id$
 */
class SuperAdminUserReferenceTest
{
    @Test
    void isGlobal()
    {
        assertTrue(SuperAdminUserReference.INSTANCE.isGlobal());
    }

    @Test
    void isSuperAdmin()
    {
        assertTrue(SuperAdminUserReference.isSuperAdmin(SuperAdminUserReference.INSTANCE));
    }

    @Test
    void isSuperAdminWhenOtherReference()
    {
        assertFalse(SuperAdminUserReference.isSuperAdmin(null));
        assertFalse(SuperAdminUserReference.isSuperAdmin(GuestUserReference.INSTANCE));
        // The current user reference is not resolved, so it is never the Super Admin user, even when the current user
        // is the Super Admin one.
        assertFalse(SuperAdminUserReference.isSuperAdmin(CurrentUserReference.INSTANCE));
    }

    @ParameterizedTest
    @ValueSource(strings = { "superadmin", "SuperAdmin", "SUPERADMIN" })
    void isSuperAdminName(String userName)
    {
        assertTrue(SuperAdminUserReference.isSuperAdminName(userName));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "superadmi", "superadmins", "XWiki.superadmin", "JohnDoe" })
    void isSuperAdminNameWhenOtherName(String userName)
    {
        assertFalse(SuperAdminUserReference.isSuperAdminName(userName));
    }

    @ParameterizedTest
    @ValueSource(strings = { "superadmin", "SuperAdmin", "SUPERADMIN" })
    void isSuperAdminReference(String userName)
    {
        assertTrue(SuperAdminUserReference.isSuperAdminReference(new DocumentReference("xwiki", "XWiki", userName)));
        // The Super Admin user of any wiki is the Super Admin user.
        assertTrue(SuperAdminUserReference.isSuperAdminReference(new DocumentReference("subwiki", "XWiki", userName)));
        assertTrue(SuperAdminUserReference.isSuperAdminReference(
            new LocalDocumentReference("XWiki", userName)));
    }

    @Test
    void isSuperAdminReferenceWhenNull()
    {
        assertFalse(SuperAdminUserReference.isSuperAdminReference(null));
    }

    @Test
    void isSuperAdminReferenceWhenOtherSpace()
    {
        assertFalse(SuperAdminUserReference.isSuperAdminReference(
            new DocumentReference("xwiki", "Main", "superadmin")));
        assertFalse(SuperAdminUserReference.isSuperAdminReference(
            new DocumentReference("xwiki", "xwiki", "superadmin")));
        // A nested space whose last level is named XWiki is not the XWiki space.
        assertFalse(SuperAdminUserReference.isSuperAdminReference(
            new DocumentReference("xwiki", List.of("Sandbox", "XWiki"), "superadmin")));
    }

    @Test
    void isSuperAdminReferenceWhenOtherName()
    {
        assertFalse(SuperAdminUserReference.isSuperAdminReference(
            new DocumentReference("xwiki", "XWiki", "JohnDoe")));
    }

    @Test
    void isSuperAdminReferenceWhenNotADocument()
    {
        assertFalse(SuperAdminUserReference.isSuperAdminReference(new SpaceReference("xwiki", "XWiki")));
    }

    @Test
    void isSuperAdminReferenceWhenSubReference()
    {
        // A reference below the Super Admin user document still denotes the Super Admin user document.
        assertTrue(SuperAdminUserReference.isSuperAdminReference(
            new ObjectReference("XWiki.XWikiUsers[0]", new DocumentReference("xwiki", "XWiki", "superadmin"))));
    }

    @Test
    void superAdminLocalReference()
    {
        assertEquals(SuperAdminUserReference.SUPERADMIN_USER_NAME,
            SuperAdminUserReference.SUPERADMIN_LOCAL_REFERENCE.getName());
        assertEquals(SuperAdminUserReference.SUPERADMIN_USER_SPACE,
            SuperAdminUserReference.SUPERADMIN_LOCAL_REFERENCE.getParent().getName());
    }
}
