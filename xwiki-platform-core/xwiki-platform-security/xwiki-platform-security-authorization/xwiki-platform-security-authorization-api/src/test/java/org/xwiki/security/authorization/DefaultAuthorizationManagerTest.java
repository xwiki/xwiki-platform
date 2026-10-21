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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.AuthorizationManager.SUPERADMIN_USER;

/**
 * Test of {@link DefaultAuthorizationManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultAuthorizationManagerTest
{
    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final DocumentReference DOCUMENT = new DocumentReference("xwiki", "Space", "Page");

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private SecurityCacheLoader securityCacheLoader;

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

    @Test
    void evaluateAccessAllowed()
    {
        assertEquals(Boolean.TRUE, this.defaultAuthorizationManager.evaluateAccess(Right.VIEW,
            new DocumentReference("xwiki", "XWiki", SUPERADMIN_USER), DOCUMENT));
    }

    @Test
    void evaluateAccessDenied()
    {
        assertEquals(Boolean.FALSE, this.defaultAuthorizationManager.evaluateAccess(Right.ILLEGAL, USER, DOCUMENT));
    }

    @Test
    void evaluateAccessFailure() throws Exception
    {
        when(this.securityCacheLoader.load(any(), any())).thenThrow(new AuthorizationException("error"));

        assertNull(this.defaultAuthorizationManager.evaluateAccess(Right.VIEW, USER, null));
        assertFalse(this.defaultAuthorizationManager.hasAccess(Right.VIEW, USER, null));

        assertEquals("Failed to load rights for user [xwiki:XWiki.User] on [Main Wiki].",
            this.logCapture.getMessage(0));
        assertEquals("Failed to load rights for user [xwiki:XWiki.User] on [Main Wiki].",
            this.logCapture.getMessage(1));
    }
}
