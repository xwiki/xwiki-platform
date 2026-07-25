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
package com.xpn.xwiki.api;

import org.junit.jupiter.api.Test;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiUser;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for the deprecated methods of {@link User} that are re-added by
 * {@code UserCompatibilityAspect}.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@OldcoreTest
@AllComponents
class UserTest
{
    /**
     * Checks that XWIKI-2040 remains fixed.
     */
    @Test
    void isUserInGroupDoesNotThrowNPE()
    {
        User u = new User(null, null);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

        XWikiUser xu = new XWikiUser((String) null);
        u = new User(xu, null);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

        XWikiContext c = new XWikiContext();
        u = new User(xu, c);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));
    }
}
