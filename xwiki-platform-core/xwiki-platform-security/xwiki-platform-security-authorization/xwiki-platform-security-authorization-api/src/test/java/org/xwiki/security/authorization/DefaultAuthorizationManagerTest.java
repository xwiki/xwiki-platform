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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.security.authorization.AuthorizationManager.SUPERADMIN_USER;

/**
 * Test of {@link DefaultAuthorizationManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultAuthorizationManagerTest
{
    @InjectMockComponents
    private DefaultAuthorizationManager defaultAuthorizationManager;

    @Test
    void isSuperAdminExpectTrue()
    {
        assertTrue(
            this.defaultAuthorizationManager.isSuperAdmin(
                new DocumentReference("s1", "Space", SUPERADMIN_USER)));
    }

    @Test
    void isSuperAdminExpectFalse()
    {
        assertFalse(
            this.defaultAuthorizationManager.isSuperAdmin(new DocumentReference("xwiki", "XWiki", "Admin")));
    }
}
