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
package org.xwiki.security.authorization.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.AuthorizationManager.SUPERADMIN_USER;

/**
 * Unit tests for {@link BridgeAuthorizationManager}.
 *
 * @version $Id$
 */
@ComponentTest
class BridgeAuthorizationManagerTest
{
    private static final DocumentReference SUPERADMIN = new DocumentReference("xwiki", "XWiki", SUPERADMIN_USER);

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final String FAILURE_MESSAGE = "Failed to load rights for user [xwiki:XWiki.User] on [Main Wiki].";

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private AsyncContext asyncContext;

    @MockComponent
    private SecurityCacheLoader securityCacheLoader;

    @InjectMockComponents
    private BridgeAuthorizationManager authorizationManager;

    @Test
    void hasAccessAllowed()
    {
        assertTrue(this.authorizationManager.hasAccess(Right.VIEW, SUPERADMIN, null));

        verify(this.asyncContext).useRight(Right.VIEW, SUPERADMIN, null, Boolean.TRUE);
    }

    @Test
    void hasAccessDenied()
    {
        assertFalse(this.authorizationManager.hasAccess(Right.ILLEGAL, USER, null));

        verify(this.asyncContext).useRight(Right.ILLEGAL, USER, null, Boolean.FALSE);
    }

    @Test
    void hasAccessFailure() throws Exception
    {
        when(this.securityCacheLoader.load(any(), any())).thenThrow(new AuthorizationException("error"));

        assertFalse(this.authorizationManager.hasAccess(Right.VIEW, USER, null));

        verify(this.asyncContext).useRight(Right.VIEW, USER, null, (Boolean) null);
        assertEquals(FAILURE_MESSAGE, this.logCapture.getMessage(0));
    }

    @Test
    void checkAccessAllowed() throws Exception
    {
        this.authorizationManager.checkAccess(Right.VIEW, SUPERADMIN, null);

        verify(this.asyncContext).useRight(Right.VIEW, SUPERADMIN, null, Boolean.TRUE);
    }

    @Test
    void checkAccessDenied()
    {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> this.authorizationManager.checkAccess(Right.ILLEGAL, USER, null));

        assertNull(exception.getCause());
        verify(this.asyncContext).useRight(Right.ILLEGAL, USER, null, Boolean.FALSE);
    }

    @Test
    void checkAccessFailure() throws Exception
    {
        when(this.securityCacheLoader.load(any(), any())).thenThrow(new AuthorizationException("error"));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> this.authorizationManager.checkAccess(Right.VIEW, USER, null));

        assertNotNull(exception.getCause());
        verify(this.asyncContext).useRight(Right.VIEW, USER, null, (Boolean) null);
    }
}
