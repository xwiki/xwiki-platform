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

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.office.viewer.OfficeResourceViewer;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Test case for {@link DefaultOfficeResourceViewer}.
 *
 * @version $Id$
 */
public class DefaultOfficeResourceViewerTest
{
    private static final String ATTACHEMENT_NAME = "Test file.doc";

    /**
     * An attachment reference to be used in tests.
     */
    private static final AttachmentReference ATTACHMENT_REFERENCE = new AttachmentReference(ATTACHEMENT_NAME,
        new DocumentReference("xwiki", "Main", "Test"));

    /**
     * String document reference to be used in tests.
     */
    private static final String STRING_DOCUMENT_REFERENCE = "xwiki:Main.Test";

    /**
     * String attachment reference to be used in tests.
     */
    private static final String STRING_ATTACHMENT_REFERENCE = STRING_DOCUMENT_REFERENCE + '@' + ATTACHEMENT_NAME;

    private static final String STRING_ATTACHMENT_RESOURCE_REFERENCE = "attach:" + STRING_ATTACHMENT_REFERENCE;

    /**
     * The attachment as {@link ResourceReference}.
     */
    private static final AttachmentResourceReference ATTACHMENT_RESOURCE_REFERENCE = new AttachmentResourceReference(
        STRING_ATTACHMENT_REFERENCE);

    /**
     * Default view parameters.
     */
    private static final Map<String, ?> DEFAULT_VIEW_PARAMETERS = Collections.singletonMap("ownerDocument",
        ATTACHMENT_REFERENCE.getDocumentReference());

    /**
     * The cache key corresponding to {@link #STRING_ATTACHMENT_REFERENCE} and {@link #DEFAULT_VIEW_PARAMETERS}.
     */
    private static final String CACHE_KEY = STRING_DOCUMENT_REFERENCE + '/' + ATTACHEMENT_NAME + '/'
        + DEFAULT_VIEW_PARAMETERS.hashCode();

    /**
     * Attachment version to be used in tests.
     */
    private static final String ATTACHMENT_VERSION = "1.1";

    /**
     * A component manager that automatically mocks all dependencies of {@link DefaultOfficeResourceViewer}.
     */
    @Rule
    public MockitoComponentMockingRule<OfficeResourceViewer> mocker =
        new MockitoComponentMockingRule<OfficeResourceViewer>(DefaultOfficeResourceViewer.class);

    /**
     * The mock {@link DocumentAccessBridge} instance used in tests.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The mock {@link XDOMOfficeDocumentBuilder} instance used in tests.
     */
    private XDOMOfficeDocumentBuilder officeDocumentBuilder;

    private ResourceReferenceTypeSerializer resourceReferenceSerializer;

    /**
     * The mock {@link Cache} instance used in tests.
     */
    private Cache<OfficeDocumentView> attachmentCache;

    /**
     * The mock {@link Cache} instance used in tests.
     */
    private Cache<OfficeDocumentView> externalCache;

    /**
     * Test fixture.
     * 
     * @throws Exception in case of an exception raised during the fixture preparation
     */
    @Before
    public void configure() throws Exception
    {
        final CacheManager cacheManager = mocker.getInstance(CacheManager.class);
        attachmentCache = mock(Cache.class);
        externalCache = mock(Cache.class);
        when(cacheManager.<OfficeDocumentView>createNewCache(notNull(CacheConfiguration.class))).thenReturn(
            attachmentCache, externalCache);

        EntityReferenceSerializer<String> entityReferenceSerializer =
            mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(entityReferenceSerializer.serialize(ATTACHMENT_REFERENCE)).thenReturn(STRING_ATTACHMENT_REFERENCE);
        when(entityReferenceSerializer.serialize(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            STRING_DOCUMENT_REFERENCE);

        AttachmentReferenceResolver<String> attachmentReferenceResolver =
            mocker.getInstance(AttachmentReferenceResolver.TYPE_STRING, "current");
        when(attachmentReferenceResolver.resolve(STRING_ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_REFERENCE);

        this.resourceReferenceSerializer = mocker.getInstance(ResourceReferenceTypeSerializer.class);
        when(this.resourceReferenceSerializer.serialize(ATTACHMENT_RESOURCE_REFERENCE)).thenReturn(
            STRING_ATTACHMENT_RESOURCE_REFERENCE);

        ConverterManager converterManager = mocker.getInstance(ConverterManager.class);
        when(converterManager.convert(boolean.class, null)).thenReturn(false);
        when(converterManager.convert(DocumentReference.class, ATTACHMENT_REFERENCE.getDocumentReference()))
            .thenReturn(ATTACHMENT_REFERENCE.getDocumentReference());

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
        when(attachmentCache.get(CACHE_KEY)).thenReturn(null);
        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Collections.<AttachmentReference>emptyList());

        try {
            mocker.getComponentUnderTest().createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS);
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
        when(attachmentCache.get(CACHE_KEY)).thenReturn(null);
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

        mocker.getComponentUnderTest().createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS);

        verify(attachmentCache).set(eq(CACHE_KEY), notNull(AttachmentOfficeDocumentView.class));
    }

    /**
     * Tests creating a view for an office attachment which has already been viewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testViewExistingOfficeAttachmentWithCacheHit() throws Exception
    {
        AttachmentOfficeDocumentView officeDocumentView =
            new AttachmentOfficeDocumentView(ATTACHMENT_RESOURCE_REFERENCE, ATTACHMENT_REFERENCE, ATTACHMENT_VERSION,
                new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(attachmentCache.get(CACHE_KEY)).thenReturn(officeDocumentView);

        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Arrays.asList(ATTACHMENT_REFERENCE));
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_VERSION);

        Assert.assertNotNull(mocker.getComponentUnderTest().createView(ATTACHMENT_RESOURCE_REFERENCE,
            DEFAULT_VIEW_PARAMETERS));
    }

    /**
     * Tests creating a view for an external office file which has already been viewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testViewExistingOfficeFileWithCacheHit() throws Exception
    {
        ResourceReference resourceReference = new ResourceReference("http://resource", ResourceType.URL);

        when(this.resourceReferenceSerializer.serialize(resourceReference)).thenReturn(
            "url:" + resourceReference.getReference());

        OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(resourceReference, new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(
            externalCache.get(STRING_DOCUMENT_REFERENCE + "/url:http://resource/" + DEFAULT_VIEW_PARAMETERS.hashCode()))
            .thenReturn(officeDocumentView);

        Assert.assertNotNull(mocker.getComponentUnderTest().createView(resourceReference, DEFAULT_VIEW_PARAMETERS));
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
        AttachmentOfficeDocumentView officeDocumentView =
            new AttachmentOfficeDocumentView(ATTACHMENT_RESOURCE_REFERENCE, ATTACHMENT_REFERENCE, ATTACHMENT_VERSION,
                new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(attachmentCache.get(CACHE_KEY)).thenReturn(officeDocumentView);

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

        Assert.assertNotNull(mocker.getComponentUnderTest().createView(ATTACHMENT_RESOURCE_REFERENCE,
            DEFAULT_VIEW_PARAMETERS));

        verify(attachmentCache).remove(CACHE_KEY);
        verify(attachmentCache).set(eq(CACHE_KEY), notNull(AttachmentOfficeDocumentView.class));
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

        DefaultOfficeResourceViewer implementation = (DefaultOfficeResourceViewer) mocker.getComponentUnderTest();
        File tempFile =
            implementation.getTemporaryFile(ATTACHMENT_REFERENCE.getDocumentReference(),
                "Test+file.doc/0/some+image.png");
        Assert.assertTrue(tempFile.getAbsolutePath().endsWith(
            "/temp/officeviewer/xwiki/Main/Test/Test+file.doc/0/some+image.png"));
    }

    /**
     * A test case for testing the {@link DefaultOfficeResourceViewer#getFilePath(String, String, Map)} method.
     * 
     * @throws Exception if an error occurs
     */
    @Test
    public void testGetFilePath() throws Exception
    {
        when(documentAccessBridge.getDocumentURL(ATTACHMENT_REFERENCE.getDocumentReference(), "temp", null, null, true))
            .thenReturn("/xwiki/bin/temp/Main/Test");

        DefaultOfficeResourceViewer implementation = (DefaultOfficeResourceViewer) mocker.getComponentUnderTest();
        String filePath =
            implementation.getFilePath(ATTACHMENT_REFERENCE.getName(), "some temporary artifact.gif",
                Collections.<String, Object>emptyMap());
        Assert.assertEquals("VGVzdCBmaWxlLmRvYw/0/some+temporary+artifact.gif", filePath);
    }
}
