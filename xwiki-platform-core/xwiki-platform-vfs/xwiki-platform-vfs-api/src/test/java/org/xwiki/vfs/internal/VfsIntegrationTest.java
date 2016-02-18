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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.vfs.internal.attach.AttachDriver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.access.TPath;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Some VFS Integration Tests.
 *
 * @version $Id$
 * @since 7.4.1, 8.0M1
 */
public class VfsIntegrationTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    /**
     * Verify that access is refused to an arhive path when a user doesn't have permission to access an archive, even
     * though that archive was previously accessed by a user with permission. This verifies that the caching behavior is
     * correct.
     */
    @Test
    @Ignore("Remove ignore when http://jira.xwiki.org/browse/XWIKI-12912 is fixed")
    public void verifyAccessRefusedWhenNoPermissionEvenWhenOtherUserSucceeded() throws Exception
    {
        setUp("wiki", "space", "page", "test.zip", Arrays.asList("some", "file"));

        // First user has permission (no IOException is raised)
        setupAttachmentViewAccess(true);
        TPath path = new TPath(URI.create("attach://wiki:space.page/test.zip/some/file"));
        InputStream is = Files.newInputStream(path);
        assertNotNull(is);

        // Second user doesn't have permission (IOException should be raised)
        setupAttachmentViewAccess(false);
        try {
            is = Files.newInputStream(path);
            fail("IOException should have been raised here");
        } catch (IOException expected) {
            assertEquals("Failed to get attachment content for attachment [test.zip] in URI "
                + "[attach://wiki:space.page/test.zip]", expected.getMessage());
            assertEquals("IOException: No View permission for document [wiki:space.page]",
                ExceptionUtils.getRootCauseMessage(expected));
        }
        assertNotNull(is);
    }

    private void setUp(String wikiName, String spaceName, String pageName, String attachmentName,
        List<String> path) throws Exception
    {
        Provider<XWikiContext> xwikiContextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xwikiContextProvider.get()).thenReturn(xcontext);

        XWiki xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        DocumentReferenceResolver<String> documentReferenceResolver =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING);
        DocumentReference documentReference = new DocumentReference(wikiName, Arrays.asList(spaceName), pageName);
        String documentReferenceAsString = String.format("%s:%s.%s", wikiName, spaceName, pageName);
        when(documentReferenceResolver.resolve(documentReferenceAsString)).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(documentReference, xcontext)).thenReturn(document);

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(document.getAttachment(attachmentName)).thenReturn(attachment);

        when(attachment.getDate()).thenReturn(new Date());
        when(attachment.getContentSize(xcontext)).thenReturn(1000);

        when(attachment.getContentInputStream(xcontext)).thenReturn(
            createZipInputStream(StringUtils.join(path, '/'), "success!"));

        // Register our custom Attach Driver in TrueVFS
        TConfig config = TConfig.current();
        // Note: Make sure we add our own Archive Detector to the existing Detector so that all archive formats
        // supported by TrueVFS are handled properly.
        config.setArchiveDetector(new TArchiveDetector(config.getArchiveDetector(), "attach",
            new AttachDriver(this.mocker)));
    }

    private InputStream createZipInputStream(String fileName, String content) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private void setupAttachmentViewAccess(boolean isPermitted) throws Exception
    {
        ContextualAuthorizationManager authorizationManager;
        if (this.mocker.hasComponent(ContextualAuthorizationManager.class)) {
            authorizationManager = this.mocker.getInstance(ContextualAuthorizationManager.class);
        } else {
            authorizationManager = this.mocker.registerMockComponent(ContextualAuthorizationManager.class);
        }
        when(authorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class))).thenReturn(isPermitted);
    }
}
