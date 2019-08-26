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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsResourceReference;
import org.xwiki.vfs.internal.attach.AttachVfsPermissionChecker;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CascadingVfsPermissionChecker}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@ComponentList({ DefaultVfsPermissionChecker.class, AttachVfsPermissionChecker.class })
public class CascadingVfsPermissionCheckerTest
{
    @Rule
    public MockitoComponentMockingRule<CascadingVfsPermissionChecker> mocker =
        new MockitoComponentMockingRule<>(CascadingVfsPermissionChecker.class);

    private DocumentReference contextUser = new DocumentReference("wiki", "space", "page");

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        Provider<XWikiContext> provider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(provider.get()).thenReturn(xcontext);
        when(xcontext.getUserReference()).thenReturn(contextUser);
    }

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
        AttachmentReference attachmentReference = new AttachmentReference("file", attachmentDocumentReference);
        when(resolver.resolve("whatever")).thenReturn(attachmentReference);

        ContextualAuthorizationManager authorizationManager =
            this.mocker.registerMockComponent(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.VIEW, attachmentReference)).thenReturn(true);

        this.mocker.getComponentUnderTest().checkPermission(reference);

        when(authorizationManager.hasAccess(Right.VIEW, attachmentReference)).thenReturn(false);

        assertThrows(VfsException.class, () -> this.mocker.getComponentUnderTest().checkPermission(reference));
    }

    @Test
    public void checkPermissionWhenNoSpecificSchemeCheckerAndAllowed() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("customscheme:whatever"), "whatever");

        AuthorizationManager authorizationManager = this.mocker.registerMockComponent(AuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.PROGRAM, this.contextUser, null)).thenReturn(true);

        this.mocker.getComponentUnderTest().checkPermission(reference);
    }

    @Test
    public void checkPermissionWhenNoSpecificSchemeCheckerAndNotAllowed() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("customscheme:whatever"), "whatever");

        AuthorizationManager authorizationManager = this.mocker.registerMockComponent(AuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.PROGRAM, this.contextUser, null)).thenReturn(false);

        try {
            this.mocker.getComponentUnderTest().checkPermission(reference);
            fail("Should have raised exception");
        } catch (VfsException expected) {
            assertEquals("Current logged-in user ([" + this.contextUser
                + "]) needs to have Programming Rights to use the [customscheme] VFS", expected.getMessage());
        }
    }
}
