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
package org.xwiki.vfs.internal;

import java.net.URI;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsResourceReference;
import org.xwiki.vfs.internal.attach.AttachVfsPermissionChecker;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CascadingVfsPermissionChecker}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@ComponentTest
// @formatter:off
@ComponentList({
    DefaultVfsPermissionChecker.class,
    AttachVfsPermissionChecker.class
})
// @formatter:on
class CascadingVfsPermissionCheckerTest
{
    @InjectMockComponents
    private CascadingVfsPermissionChecker checker;

    @MockComponent
    private Provider<XWikiContext> provider;

    @MockComponent
    private AttachmentReferenceResolver<String> resolver;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private DocumentReference contextUser = new DocumentReference("wiki", "space", "page");

    @BeforeComponent
    void beforeComponent()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(this.provider.get()).thenReturn(xcontext);
        when(xcontext.getUserReference()).thenReturn(this.contextUser);
    }

    @Test
    void checkPermissionWhenReservedScheme()
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("cascading:whatever"), "whatever");

        Throwable exception = assertThrows(VfsException.class, () -> this.checker.checkPermission(reference));
        assertEquals("[cascading] is a reserved VFS URI scheme and cannot be used.", exception.getMessage());
    }

    @Test
    void checkPermissionWithAttachSchemeChecker() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:whatever"), "whatever");

        DocumentReference attachmentDocumentReference = new DocumentReference("wiki", "space", "page");
        AttachmentReference attachmentReference = new AttachmentReference("file", attachmentDocumentReference);
        when(this.resolver.resolve("whatever")).thenReturn(attachmentReference);

        ContextualAuthorizationManager authorizationManager =
            this.componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.VIEW, attachmentReference)).thenReturn(true);

        this.checker.checkPermission(reference);

        when(authorizationManager.hasAccess(Right.VIEW, attachmentReference)).thenReturn(false);

        Throwable exception = assertThrows(VfsException.class, () -> this.checker.checkPermission(reference));
        assertEquals("No View permission for attachment [Attachment wiki:space.page@file]", exception.getMessage());
    }

    @Test
    void checkPermissionWhenNoSpecificSchemeCheckerAndAllowed() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("customscheme:whatever"), "whatever");

        AuthorizationManager authorizationManager =
            this.componentManager.registerMockComponent(AuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.PROGRAM, this.contextUser, null)).thenReturn(true);

        this.checker.checkPermission(reference);
    }

    @Test
    public void checkPermissionWhenNoSpecificSchemeCheckerAndNotAllowed() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("customscheme:whatever"), "whatever");

        AuthorizationManager authorizationManager =
            this.componentManager.registerMockComponent(AuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.PROGRAM, this.contextUser, null)).thenReturn(false);

        Throwable exception = assertThrows(VfsException.class, () -> this.checker.checkPermission(reference));
        assertEquals("Current logged-in user ([" + this.contextUser
            + "]) needs to have Programming Rights to use the [customscheme] VFS", exception.getMessage());
    }
}
