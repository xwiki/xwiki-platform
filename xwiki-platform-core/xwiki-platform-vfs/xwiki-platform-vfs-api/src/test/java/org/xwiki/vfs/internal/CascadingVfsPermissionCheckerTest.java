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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsResourceReference;
import org.xwiki.vfs.internal.attach.AttachVfsPermissionChecker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CascadingVfsPermissionChecker}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@ComponentList({
    DefaultVfsPermissionChecker.class,
    AttachVfsPermissionChecker.class
})
public class CascadingVfsPermissionCheckerTest
{
    @Rule
    public MockitoComponentMockingRule<CascadingVfsPermissionChecker> mocker =
        new MockitoComponentMockingRule<>(CascadingVfsPermissionChecker.class);

    @Test
    public void checkPermissionWhenReservedScheme() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("cascading:whatever"), "whatever");

        try {
            this.mocker.getComponentUnderTest().checkPermission(reference);
            fail("Should have raised exception");
        } catch (VfsException expected) {
            assertEquals("[cascading] is a reserved VFS URI scheme and cannot be used.", expected.getMessage());
        }
    }

    @Test
    public void checkPermissionWithAttachSchemeChecker() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:whatever"), "whatever");

        AttachmentReferenceResolver<String> resolver =
            this.mocker.registerMockComponent(AttachmentReferenceResolver.TYPE_STRING);
        DocumentReference attachmentDocumentReference = new DocumentReference("wiki", "space", "page");
        when(resolver.resolve("whatever")).thenReturn(new AttachmentReference("file", attachmentDocumentReference));

        ContextualAuthorizationManager authorizationManager =
            this.mocker.registerMockComponent(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.VIEW, attachmentDocumentReference)).thenReturn(true);

        this.mocker.getComponentUnderTest().checkPermission(reference);
    }

    @Test
    public void checkPermissionWhenNoSpecificSchemeCheckerAndAllowed() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("customscheme:whatever"), "whatever");

        ContextualAuthorizationManager authorizationManager =
            this.mocker.registerMockComponent(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);

        this.mocker.getComponentUnderTest().checkPermission(reference);
    }

    @Test
    public void checkPermissionWhenNoSpecificSchemeCheckerAndNotAllowed() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("customscheme:whatever"), "whatever");

        ContextualAuthorizationManager authorizationManager =
            this.mocker.registerMockComponent(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);

        try {
            this.mocker.getComponentUnderTest().checkPermission(reference);
            fail("Should have raised exception");
        } catch (VfsException expected) {
            assertEquals("Current logged-in user needs to have Programming Rights to use the [customscheme] VFS",
                expected.getMessage());
        }
    }
}
