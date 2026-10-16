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
package org.xwiki.store.filesystem.internal;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentsLogoutListener}.
 *
 * @version $Id$
 */
@ComponentTest
class TemporaryAttachmentsLogoutListenerTest
{
    private static final BlobPath SCOPE_PATH = BlobPath.absolute("scope");

    @InjectMockComponents
    private TemporaryAttachmentsLogoutListener listener;

    @MockComponent
    private TemporaryAttachmentScopeManager scopeManager;

    @MockComponent
    private TemporaryAttachmentStore store;

    @Test
    void logout() throws Exception
    {
        when(this.scopeManager.getScopeId()).thenReturn(Optional.of("scopeId"));
        when(this.store.getScopePath("scopeId")).thenReturn(SCOPE_PATH);

        this.listener.onEvent(new ActionExecutingEvent("logout"), null, mock(XWikiContext.class));

        verify(this.store).deleteAll(SCOPE_PATH);
        verify(this.scopeManager).resetScope();
    }

    @Test
    void logoutNotPerformed() throws Exception
    {
        // The authenticator did not log the user out (for example a logout URL without form token).
        XWikiContext context = mock(XWikiContext.class);
        when(context.getUserReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "User"));

        this.listener.onEvent(new ActionExecutingEvent("logout"), null, context);

        verify(this.scopeManager, never()).getScopeId();
        verify(this.store, never()).deleteAll(any());
        verify(this.scopeManager, never()).resetScope();
    }

    @Test
    void logoutWithoutScope() throws Exception
    {
        when(this.scopeManager.getScopeId()).thenReturn(Optional.empty());

        this.listener.onEvent(new ActionExecutingEvent("logout"), null, mock(XWikiContext.class));
        // Not an XWiki context: nothing to check, nothing done.
        this.listener.onEvent(new ActionExecutingEvent("logout"), null, null);

        verify(this.scopeManager).getScopeId();
        verify(this.store, never()).deleteAll(any());
        verify(this.scopeManager, never()).resetScope();
    }

    @Test
    void logoutRefusedBecauseOfInvalidCSRFToken() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        when(context.get("core.logout.error.invalidCSRF")).thenReturn(true);

        this.listener.onEvent(new ActionExecutingEvent("logout"), null, context);

        verify(this.scopeManager, never()).getScopeId();
        verify(this.store, never()).deleteAll(any());
    }
}
