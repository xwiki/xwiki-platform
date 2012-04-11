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
import org.xwiki.environment.Environment;
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
 * Test case for {@link DefaultOfficeViewer}.
 * 
 * @version $Id$
 */
public class DefaultOfficeViewerTest extends AbstractMockingComponentTestCase
{
    /**
     * An attachment reference to be used in tests.
     */
    private static final AttachmentReference ATTACHMENT_REFERENCE =
        new AttachmentReference("Test file.doc", new DocumentReference("xwiki", "Main", "Test"));

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
     * The {@link DefaultOfficeViewer} instance being tested.
     */
    @MockingRequirement
    private DefaultOfficeViewer defaultOfficeViewer;

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
    private Cache<OfficeDocumentView> cache;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        documentAccessBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        entityReferenceSerializer = getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        officeDocumentBuilder = getComponentManager().getInstance(XDOMOfficeDocumentBuilder.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception
    {
        super.configure();

        final CacheManager cacheManager = getComponentManager().getInstance(CacheManager.class);
        cache = getMockery().mock(Cache.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(cacheManager).createNewCache(with(aNonNull(CacheConfiguration.class)));
                will(returnValue(cache));
            }
        });
    }

    /**
     * Test creating a view for a non-existing attachment.
     * 
     * @throws Exception if an error occurs
     */
    @Test
    public void testViewNonExistingOfficeAttachment() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(cache).get(CACHE_KEY);
                will(returnValue(null));

                oneOf(documentAccessBridge).getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference());
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        try {
            defaultOfficeViewer.createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS);
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
        final ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        final XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), new HashMap<String, byte[]>(),
                getComponentManager());

        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(cache).get(CACHE_KEY);
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

                oneOf(cache).set(with(CACHE_KEY), with(aNonNull(OfficeDocumentView.class)));
            }
        });

        defaultOfficeViewer.createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS);
    }

    /**
     * Tests creating a view for an office attachment which has already been viewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testViewExistingOfficeAttachmentWithCacheHit() throws Exception
    {
        final OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(ATTACHMENT_REFERENCE, ATTACHMENT_VERSION, new XDOM(new ArrayList<Block>()),
                new HashSet<File>());

        getMockery().checking(new Expectations()
        {
            {
                oneOf(entityReferenceSerializer).serialize(ATTACHMENT_REFERENCE);
                will(returnValue(STRING_ATTACHMENT_REFERENCE));

                oneOf(cache).get(CACHE_KEY);
                will(returnValue(officeDocumentView));

                oneOf(documentAccessBridge).getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference());
                will(returnValue(Arrays.asList(ATTACHMENT_REFERENCE)));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(ATTACHMENT_VERSION));
            }
        });

        Assert.assertNotNull(defaultOfficeViewer.createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS));
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
        final OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(ATTACHMENT_REFERENCE, ATTACHMENT_VERSION, new XDOM(new ArrayList<Block>()),
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

                oneOf(cache).get(CACHE_KEY);
                will(returnValue(officeDocumentView));

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
                oneOf(cache).remove(CACHE_KEY);

                oneOf(documentAccessBridge).getAttachmentContent(ATTACHMENT_REFERENCE);
                will(returnValue(attachmentContent));

                oneOf(officeDocumentBuilder).build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                    ATTACHMENT_REFERENCE.getDocumentReference(), false);
                will(returnValue(xdomOfficeDocument));

                oneOf(documentAccessBridge).getAttachmentVersion(ATTACHMENT_REFERENCE);
                will(returnValue(attachmentVersion));

                oneOf(cache).set(with(CACHE_KEY), with(aNonNull(OfficeDocumentView.class)));
            }
        });

        Assert.assertNotNull(defaultOfficeViewer.createView(ATTACHMENT_REFERENCE, DEFAULT_VIEW_PARAMETERS));
    }

    /**
     * A test case for testing the {@link AbstractOfficeViewer#getTemporaryFile(AttachmentReference, String)} method.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    public void testGetTemporaryFile() throws Exception
    {
        final Environment environment = getComponentManager().getInstance(Environment.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(environment).getTemporaryDirectory();
                will(returnValue(new File(System.getProperty("java.io.tmpdir"))));
            }
        });

        File tempFile = defaultOfficeViewer.getTemporaryFile(ATTACHMENT_REFERENCE, "some image.png");
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
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getDocumentURL(ATTACHMENT_REFERENCE.getDocumentReference(), "temp", null,
                    null, true);
                will(returnValue("/xwiki/bin/temp/Main/Test"));
            }
        });

        String url = defaultOfficeViewer.buildURL(ATTACHMENT_REFERENCE, "some temporary artifact.gif");
        Assert.assertEquals("/xwiki/bin/temp/Main/Test/officeviewer/Test+file.doc/some+temporary+artifact.gif", url);
    }
}
