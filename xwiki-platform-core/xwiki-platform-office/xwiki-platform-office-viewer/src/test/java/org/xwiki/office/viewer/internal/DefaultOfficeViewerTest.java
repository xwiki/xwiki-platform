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
package org.xwiki.office.viewer.internal;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Test case for {@link DefaultOfficeViewer}.
 * 
 * @version $Id$
 */
public class DefaultOfficeViewerTest
{
    /**
     * An attachment reference to be used in tests.
     */
    private static final AttachmentReference ATTACHMENT_REFERENCE = new AttachmentReference("Test file.doc",
        new DocumentReference("xwiki", "Main", "Test"));

    /**
     * String attachment reference to be used in tests.
     */
    private static final String STRING_ATTACHMENT_REFERENCE = "xwiki:Main.Test@Test file.doc";

    /**
     * The cache key corresponding to {@link #STRING_ATTACHMENT_REFERENCE} and {@link #DEFAULT_VIEW_PARAMETERS}.
     */
    private static final String CACHE_KEY = STRING_ATTACHMENT_REFERENCE + "/0";

    /**
     * Attachment version to be used in tests.
     */
    private static final String ATTACHMENT_VERSION = "1.1";

    /**
     * Default view parameters.
     */
    private static final Map<String, String> DEFAULT_VIEW_PARAMETERS = Collections.emptyMap();

    /**
     * A component manager that automatically mocks all dependencies of {@link DefaultOfficeViewer}.
     */
    @Rule
    public MockitoComponentMockingRule<OfficeViewer> mocker = new MockitoComponentMockingRule<OfficeViewer>(
        DefaultOfficeViewer.class);

    /**
     * The mock {@link DocumentAccessBridge} instance used in tests.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The mock {@link XDOMOfficeDocumentBuilder} instance used in tests.
     */
    private XDOMOfficeDocumentBuilder officeDocumentBuilder;

    /**
     * The mock {@link Cache} instance used in tests.
     */
    private Cache<OfficeDocumentView> cache;

    /**
     * Test fixture.
     * 
     * @throws Exception in case of an exception raised during the fixture preparation
     */
    @Before
    public void configure() throws Exception
    {
        final CacheManager cacheManager = mocker.getInstance(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.<OfficeDocumentView> createNewCache(notNull(CacheConfiguration.class))).thenReturn(cache);

        EntityReferenceSerializer<String> entityReferenceSerializer =
            mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(entityReferenceSerializer.serialize(ATTACHMENT_REFERENCE)).thenReturn(STRING_ATTACHMENT_REFERENCE);

        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        officeDocumentBuilder = mocker.getInstance(XDOMOfficeDocumentBuilder.class);
    }

    /**
     * Test creating a view for a non-existing attachment.
     * 
     * @throws Exception if an error occurs
     */
    @Test
    public void testViewNonExistingOfficeAttachment() throws Exception
    {
        when(cache.get(CACHE_KEY)).thenReturn(null);
        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Collections.<AttachmentReference> emptyList());

        try {
            mocker.getComponentUnderTest().createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS);
            Assert.fail("Expected exception.");
        } catch (Exception e) {
            Assert.assertEquals(String.format("Attachment [%s] does not exist.", ATTACHMENT_REFERENCE), e.getMessage());
        }
    }

    /**
     * Tests creating a view for an existing office attachment.
     * 
     * @throws Exception if an error occurs
     */
    @Test
    public void testViewExistingOfficeAttachmentWithCacheMiss() throws Exception
    {
        when(cache.get(CACHE_KEY)).thenReturn(null);
        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Arrays.asList(ATTACHMENT_REFERENCE));
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_VERSION);

        ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        when(documentAccessBridge.getAttachmentContent(ATTACHMENT_REFERENCE)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), new HashMap<String, byte[]>(), mocker);
        when(
            officeDocumentBuilder.build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                ATTACHMENT_REFERENCE.getDocumentReference(), false)).thenReturn(xdomOfficeDocument);

        mocker.getComponentUnderTest().createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS);

        verify(cache).set(eq(CACHE_KEY), notNull(OfficeDocumentView.class));
    }

    /**
     * Tests creating a view for an office attachment which has already been viewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testViewExistingOfficeAttachmentWithCacheHit() throws Exception
    {
        OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(ATTACHMENT_REFERENCE, ATTACHMENT_VERSION, new XDOM(new ArrayList<Block>()),
                new HashSet<File>());
        when(cache.get(CACHE_KEY)).thenReturn(officeDocumentView);

        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Arrays.asList(ATTACHMENT_REFERENCE));
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_VERSION);

        Assert.assertNotNull(mocker.getComponentUnderTest().createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS));
    }

    /**
     * Tests creating a view for an office attachment that has been viewed in past and whose version has been
     * incremented.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testViewANewVersionOfAnExistingOfficeAttachment() throws Exception
    {
        OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(ATTACHMENT_REFERENCE, ATTACHMENT_VERSION, new XDOM(new ArrayList<Block>()),
                new HashSet<File>());
        when(cache.get(CACHE_KEY)).thenReturn(officeDocumentView);

        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Arrays.asList(ATTACHMENT_REFERENCE));
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn("2.1");

        ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        when(documentAccessBridge.getAttachmentContent(ATTACHMENT_REFERENCE)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), new HashMap<String, byte[]>(), mocker);
        when(
            officeDocumentBuilder.build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                ATTACHMENT_REFERENCE.getDocumentReference(), false)).thenReturn(xdomOfficeDocument);

        Assert.assertNotNull(mocker.getComponentUnderTest().createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS));

        verify(cache).remove(CACHE_KEY);
        verify(cache).set(eq(CACHE_KEY), notNull(OfficeDocumentView.class));
    }

    /**
     * A test case for testing the {@link AbstractOfficeViewer#getTemporaryFile(AttachmentReference, String)} method.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testGetTemporaryFile() throws Exception
    {
        Environment environment = mocker.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(new File(System.getProperty("java.io.tmpdir")));

        DefaultOfficeViewer implementation = (DefaultOfficeViewer) mocker.getComponentUnderTest();
        File tempFile = implementation.getTemporaryFile(ATTACHMENT_REFERENCE, "some image.png");
        Assert.assertTrue(tempFile.getAbsolutePath().endsWith(
            "/temp/officeviewer/xwiki/Main/Test/Test+file.doc/some+image.png"));
    }

    /**
     * A test case for testing the {@link AbstractOfficeViewer#buildURL(AttachmentReference, String)} method.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testBuildURL() throws Exception
    {
        when(documentAccessBridge.getDocumentURL(ATTACHMENT_REFERENCE.getDocumentReference(), "temp", null, null, true))
            .thenReturn("/xwiki/bin/temp/Main/Test");

        DefaultOfficeViewer implementation = (DefaultOfficeViewer) mocker.getComponentUnderTest();
        String url = implementation.buildURL(ATTACHMENT_REFERENCE, "some temporary artifact.gif");
        Assert.assertEquals("/xwiki/bin/temp/Main/Test/officeviewer/Test+file.doc/some+temporary+artifact.gif", url);
    }
}
