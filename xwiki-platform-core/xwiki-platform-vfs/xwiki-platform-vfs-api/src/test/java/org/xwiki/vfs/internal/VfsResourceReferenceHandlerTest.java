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
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;
import org.xwiki.vfs.internal.attach.AttachDriver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VfsResourceReferenceHandler}.
 * <p>
 * Note: We use a different URI for the various unit tests in this class since otherwise they're cached by
 * TrueVFS. TODO: Find a way to flush TrueVFS caches.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class VfsResourceReferenceHandlerTest
{
    @Rule
    public MockitoComponentMockingRule<VfsResourceReferenceHandler> mocker =
        new MockitoComponentMockingRule<>(VfsResourceReferenceHandler.class);

    private ByteArrayOutputStream baos;

    private DocumentReference documentReference;

    private VfsResourceReference reference;

    private Response response = mock(Response.class);

    private void setUp(String scheme, String wikiName, String spaceName, String pageName, String attachmentName,
        List<String> path) throws Exception
    {
        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);

        String attachmentReferenceAsString =
            String.format("%s:%s:%s.%s@%s", scheme, wikiName, spaceName, pageName, attachmentName);
        this.reference = new VfsResourceReference(URI.create(attachmentReferenceAsString), path);

        ResourceReferenceSerializer<VfsResourceReference, URI> trueVfsResourceReferenceSerializer =
            this.mocker.getInstance(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
                VfsResourceReference.class, URI.class), "truevfs");
        String truevfsURIFragment = String.format("%s://%s:%s.%s/%s/%s", scheme, wikiName, spaceName, pageName,
            attachmentName, StringUtils.join(path, '/'));
        when(trueVfsResourceReferenceSerializer.serialize(this.reference)).thenReturn(URI.create(truevfsURIFragment));

        Provider<XWikiContext> xwikiContextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xwikiContextProvider.get()).thenReturn(xcontext);

        XWiki xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        DocumentReferenceResolver<String> documentReferenceResolver =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING);
        this.documentReference = new DocumentReference(wikiName, Arrays.asList(spaceName), pageName);
        String documentReferenceAsString = String.format("%s:%s.%s", wikiName, spaceName, pageName);
        when(documentReferenceResolver.resolve(documentReferenceAsString)).thenReturn(this.documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(this.documentReference, xcontext)).thenReturn(document);

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(document.getAttachment(attachmentName)).thenReturn(attachment);

        when(attachment.getDate()).thenReturn(new Date());
        when(attachment.getContentLongSize(xcontext)).thenReturn(1000L);

        when(attachment.getContentInputStream(xcontext)).thenReturn(
            createZipInputStream(StringUtils.join(path, '/'), "success!"));

        Container container = this.mocker.getInstance(Container.class);
        when(container.getResponse()).thenReturn(response);

        this.baos = new ByteArrayOutputStream();
        when(this.response.getOutputStream()).thenReturn(this.baos);

        // Register our custom Attach Driver in TrueVFS
        TConfig config = TConfig.current();
        // Note: Make sure we add our own Archive Detector to the existing Detector so that all archive formats
        // supported by TrueVFS are handled properly.
        config.setArchiveDetector(new TArchiveDetector(config.getArchiveDetector(), "attach",
            new AttachDriver(this.mocker)));
    }

    @Test
    public void handleOk() throws Exception
    {
        setUp("attach", "wiki1", "space1", "page1", "test.zip", Arrays.asList("test.txt"));

        assertEquals(Arrays.asList(VfsResourceReference.TYPE),
            this.mocker.getComponentUnderTest().getSupportedResourceReferences());
        this.mocker.getComponentUnderTest().handle(this.reference, mock(ResourceReferenceHandlerChain.class));

        assertEquals("success!", this.baos.toString());
    }

    @Test
    public void handlecustomContentType() throws Exception
    {
        setUp("attach", "wiki1", "space1", "page1", "test.zip", Arrays.asList("test.txt"));

        this.reference.addParameter("content-type", "custom content type");

        assertEquals(Arrays.asList(VfsResourceReference.TYPE),
            this.mocker.getComponentUnderTest().getSupportedResourceReferences());
        this.mocker.getComponentUnderTest().handle(this.reference, mock(ResourceReferenceHandlerChain.class));

        assertEquals("success!", this.baos.toString());

        verify(this.response).setContentType("custom content type");
    }

    @Test
    public void handleWhenNoGenericPermissionForScheme() throws Exception
    {
        setUp("customscheme", "wiki3", "space3", "page3", "test.zip", Arrays.asList("test.txt"));

        // Don't allow permission for "customscheme"
        VfsPermissionChecker checker = this.mocker.getInstance(VfsPermissionChecker.class, "cascading");
        doThrow(new VfsException("no permission")).when(checker).checkPermission(this.reference);

        try {
            this.mocker.getComponentUnderTest().handle(this.reference, mock(ResourceReferenceHandlerChain.class));
            fail("Should have thrown exception here");
        } catch (ResourceReferenceHandlerException expected) {
            assertEquals("VfsException: no permission", ExceptionUtils.getRootCauseMessage(expected));
        }
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
}
