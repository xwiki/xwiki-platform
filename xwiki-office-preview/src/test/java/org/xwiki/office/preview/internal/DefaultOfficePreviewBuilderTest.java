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
package org.xwiki.office.preview.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Test case for {@link DefaultOfficePreviewBuilder}.
 * 
 * @version $Id$
 */
public class DefaultOfficePreviewBuilderTest extends AbstractMockingComponentTestCase
{
    /**
     * An attachment reference to be used in tests.
     */
    private static final AttachmentReference ATTACHMENT_REFERENCE =
        new AttachmentReference("Test.doc", new DocumentReference("xwiki", "Main", "Test"));

    /**
     * String attachment reference to be used in tests.
     */
    private static final String STRING_ATTACHMENT_REFERENCE = "xwiki:Main.Test@Test.doc";

    /**
     * The cache key corresponding to {@link #STRING_ATTACHMENT_REFERENCE} and {@link #DEFAULT_PREVIEW_PARAMETERS}.
     */
    private static final String PREVIEW_CACHE_KEY = STRING_ATTACHMENT_REFERENCE + "/0";

    /**
     * Attachment version to be used in tests.
     */
    private static final String ATTACHMENT_VERSION = "1.1";

    /**
     * Default preview parameters.
     */
    private static final Map<String, String> DEFAULT_PREVIEW_PARAMETERS = Collections.emptyMap();

    /**
     * The {@link DefaultOfficePreviewBuilder} instance being tested.
     */
    @MockingRequirement
    private DefaultOfficePreviewBuilder defaultOfficePreviewBuilder;

    /**
     * The mock {@link DocumentAccessBridge} instance used in tests.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The mock {@link EntityReferenceSerializer} instance used in tests.
     */
    private EntityReferenceSerializer< ? > entityReferenceSerializer;

    /**
     * The mock {@link XDOMOfficeDocumentBuilder} instance used in tests.
     */
    private XDOMOfficeDocumentBuilder officeDocumentBuilder;

    /**
     * The mock {@link Cache} instance used in tests.
     */
    private Cache<OfficeDocumentPreview> previewCache;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMockingComponentTestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        entityReferenceSerializer = getComponentManager().lookup(EntityReferenceSerializer.class);
        officeDocumentBuilder = getComponentManager().lookup(XDOMOfficeDocumentBuilder.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMockingComponentTestCase#configure()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception
    {
        super.configure();

        final CacheManager cacheManager = getComponentManager().lookup(CacheManager.class);
        previewCache = getMockery().mock(Cache.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(cacheManager).createNewCache(with(aNonNull(CacheConfiguration.class)));
                will(returnValue(previewCache));
            }
        });
    }

    /**
     * Test the previewing of a non-existing attachment.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testOfficePreviewWithNonExistingAttachment() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(previewCache).get(PREVIEW_CACHE_KEY);
                will(returnValue(null));

                oneOf(documentAccessBridge).getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference());
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        try {
            defaultOfficePreviewBuilder.build(ATTACHMENT_REFERENCE, DEFAULT_PREVIEW_PARAMETERS);
            Assert.fail("Expected exception.");
        } catch (Exception e) {
            Assert.assertEquals(String.format("Attachment [%s] does not exist.", ATTACHMENT_REFERENCE), e.getMessage());
        }
    }

    /**
     * Tests the normal office preview function.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testOfficePreviewWithCacheMiss() throws Exception
    {
        final ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        final XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), new HashMap<String, byte[]>(),
                getComponentManager());

        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(previewCache).get(PREVIEW_CACHE_KEY);
                will(returnValue(null));

                oneOf(documentAccessBridge).getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference());
                will(returnValue(Arrays.asList(ATTACHMENT_REFERENCE)));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(ATTACHMENT_VERSION));
            }
        });
        // Note: We're using two expectation groups to limit the length of the anonymous inner class (max allowed 20)
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentContent(ATTACHMENT_REFERENCE);
                will(returnValue(attachmentContent));

                oneOf(officeDocumentBuilder).build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                    ATTACHMENT_REFERENCE.getDocumentReference(), false);
                will(returnValue(xdomOfficeDocument));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(ATTACHMENT_VERSION));

                oneOf(previewCache).set(with(PREVIEW_CACHE_KEY), with(aNonNull(OfficeDocumentPreview.class)));
            }
        });

        defaultOfficePreviewBuilder.build(ATTACHMENT_REFERENCE, DEFAULT_PREVIEW_PARAMETERS);
    }

    /**
     * Tests the previewing of an office document which has already been previewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testOfficePreviewWithCacheHit() throws Exception
    {
        final OfficeDocumentPreview officeDocumentPreview =
            new OfficeDocumentPreview(ATTACHMENT_REFERENCE, ATTACHMENT_VERSION, new XDOM(new ArrayList<Block>()),
                new HashSet<File>());

        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(previewCache).get(PREVIEW_CACHE_KEY);
                will(returnValue(officeDocumentPreview));

                oneOf(documentAccessBridge).getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference());
                will(returnValue(Arrays.asList(ATTACHMENT_REFERENCE)));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(ATTACHMENT_VERSION));
            }
        });

        Assert.assertNotNull(defaultOfficePreviewBuilder.build(ATTACHMENT_REFERENCE, DEFAULT_PREVIEW_PARAMETERS));
    }

    /**
     * Tests office attachment previewing where a cached preview exists for an older version of the attachment.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testOfficePreviewWithExpiredCachedAttachmentPreview() throws Exception
    {
        final OfficeDocumentPreview officeDocumentPreview =
            new OfficeDocumentPreview(ATTACHMENT_REFERENCE, ATTACHMENT_VERSION, new XDOM(new ArrayList<Block>()),
                new HashSet<File>());
        final ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        final XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), new HashMap<String, byte[]>(),
                getComponentManager());
        final String attachmentVersion = "2.1";

        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(previewCache).get(PREVIEW_CACHE_KEY);
                will(returnValue(officeDocumentPreview));

                oneOf(documentAccessBridge).getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference());
                will(returnValue(Arrays.asList(ATTACHMENT_REFERENCE)));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(attachmentVersion));
            }
        });
        // Note: We're using two expectation groups to limit the length of the anonymous inner class (max allowed 20)
        getMockery().checking(new Expectations()
        {
            {
                oneOf(previewCache).remove(PREVIEW_CACHE_KEY);

                oneOf(documentAccessBridge).getAttachmentContent(ATTACHMENT_REFERENCE);
                will(returnValue(attachmentContent));

                oneOf(officeDocumentBuilder).build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                    ATTACHMENT_REFERENCE.getDocumentReference(), false);
                will(returnValue(xdomOfficeDocument));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(attachmentVersion));

                oneOf(previewCache).set(with(PREVIEW_CACHE_KEY), with(aNonNull(OfficeDocumentPreview.class)));
            }
        });

        Assert.assertNotNull(defaultOfficePreviewBuilder.build(ATTACHMENT_REFERENCE, DEFAULT_PREVIEW_PARAMETERS));
    }

    /**
     * A test case for testing the {@link AbstractOfficePreviewBuilder#getTemporaryDirectory(AttachmentReference)}
     * method.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testGetTemporaryDirectory() throws Exception
    {
        final Container container = getComponentManager().lookup(Container.class);
        final ApplicationContext applicationContext = getMockery().mock(ApplicationContext.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(container).getApplicationContext();
                will(returnValue(applicationContext));

                oneOf(applicationContext).getTemporaryDirectory();
                will(returnValue(new File(System.getProperty("java.io.tmpdir"))));
            }
        });

        File tempFile = defaultOfficePreviewBuilder.getTemporaryDirectory(ATTACHMENT_REFERENCE);
        Assert.assertTrue(tempFile.getAbsolutePath().endsWith("/temp/officepreview/xwiki/Main/Test/Test.doc"));
    }

    /**
     * A test case for testing the {@link AbstractOfficePreviewBuilder#buildURL(AttachmentReference, String)} method.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testBuildURL() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getDocumentURL(ATTACHMENT_REFERENCE.getDocumentReference(), "temp", null,
                    null);
                will(returnValue("/xwiki/bin/temp/Main/Test"));
            }
        });

        String url = defaultOfficePreviewBuilder.buildURL(ATTACHMENT_REFERENCE, "some_temporary_artifact.gif");
        Assert.assertEquals("/xwiki/bin/temp/Main/Test/officepreview/Test.doc/some_temporary_artifact.gif", url);
    }
}
