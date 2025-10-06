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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.document.ByteArrayOfficeDocumentArtifact;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLSecurityManager;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DefaultOfficeResourceViewer}.
 *
 * @version $Id$
 */
@WireMockTest
@ComponentTest
class DefaultOfficeResourceViewerTest
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
    @InjectMockComponents
    private DefaultOfficeResourceViewer officeResourceViewer;

    /**
     * The mock {@link DocumentAccessBridge} instance used in tests.
     */
    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    /**
     * The mock {@link XDOMOfficeDocumentBuilder} instance used in tests.
     */
    @MockComponent
    private XDOMOfficeDocumentBuilder officeDocumentBuilder;

    @MockComponent
    private ResourceReferenceTypeSerializer resourceReferenceTypeSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @MockComponent
    private ConverterManager converterManager;

    @MockComponent
    private OfficeServer officeServer;

    @MockComponent
    private PresentationBuilder presentationBuilder;

    @MockComponent
    private ResourceReferenceSerializer<org.xwiki.resource.ResourceReference, ExtendedURL> resourceReferenceSerializer;

    @MockComponent
    private TemporaryResourceStore temporaryResourceStore;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @MockComponent
    private URLSecurityManager urlSecurityManager;

    @XWikiTempDir
    private File tempDir;

    /**
     * The mock {@link Cache} instance used in tests.
     */
    private Cache<OfficeDocumentView> attachmentCache;

    /**
     * The mock {@link Cache} instance used in tests.
     */
    private Cache<OfficeDocumentView> externalCache;

    private XWikiContext context;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        final CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);
        attachmentCache = mock(Cache.class, "attachment");
        externalCache = mock(Cache.class, "external");
        when(cacheManager.<OfficeDocumentView>createNewCache(any(CacheConfiguration.class))).thenReturn(
            attachmentCache, externalCache);
    }

    /**
     * Test fixture.
     * 
     * @throws Exception in case of an exception raised during the fixture preparation
     */
    @BeforeEach
    void configure() throws Exception
    {
        when(entityReferenceSerializer.serialize(ATTACHMENT_REFERENCE)).thenReturn(STRING_ATTACHMENT_REFERENCE);
        when(entityReferenceSerializer.serialize(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            STRING_DOCUMENT_REFERENCE);

        when(attachmentReferenceResolver.resolve(STRING_ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_REFERENCE);
        when(this.resourceReferenceTypeSerializer.serialize(ATTACHMENT_RESOURCE_REFERENCE)).thenReturn(
            STRING_ATTACHMENT_RESOURCE_REFERENCE);

        when(converterManager.convert(boolean.class, null)).thenReturn(false);
        when(converterManager.convert(DocumentReference.class, ATTACHMENT_REFERENCE.getDocumentReference()))
            .thenReturn(ATTACHMENT_REFERENCE.getDocumentReference());

        OfficeConverter officeConverter = mock(OfficeConverter.class);
        when(this.officeServer.getConverter()).thenReturn(officeConverter);
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    /**
     * Test creating a view for a non-existing attachment.
     * 
     * @throws Exception if an error occurs
     */
    @Test
    void viewNonExistingOfficeAttachment() throws Exception
    {
        when(attachmentCache.get(CACHE_KEY)).thenReturn(null);
        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Collections.<AttachmentReference>emptyList());

        Exception exception = assertThrows(Exception.class,
            () -> this.officeResourceViewer.createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS));
        assertEquals(String.format("Attachment [%s] does not exist.", ATTACHMENT_REFERENCE), exception.getMessage());
    }

    /**
     * Tests creating a view for an existing office attachment.
     * 
     * @throws Exception if an error occurs
     */
    @Test
    void viewExistingOfficeAttachmentWithCacheMiss(MockitoComponentManager componentManager) throws Exception
    {
        when(attachmentCache.get(CACHE_KEY)).thenReturn(null);
        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Arrays.asList(ATTACHMENT_REFERENCE));
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_VERSION);

        ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        when(documentAccessBridge.getAttachmentContent(ATTACHMENT_REFERENCE)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), Collections.emptyMap(), componentManager, null);
        when(
            officeDocumentBuilder.build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                ATTACHMENT_REFERENCE.getDocumentReference(), false)).thenReturn(xdomOfficeDocument);

        this.officeResourceViewer.createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS);

        verify(attachmentCache).set(eq(CACHE_KEY), any(AttachmentOfficeDocumentView.class));
    }

    /**
     * Tests creating a view for an existing office attachment.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void viewTemporaryUploadedOfficeAttachmentWithCacheMiss(MockitoComponentManager componentManager) throws Exception
    {
        when(attachmentCache.get(CACHE_KEY)).thenReturn(null);
        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Collections.emptyList());
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(null);
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(temporaryAttachmentSessionsManager.getUploadedAttachment(ATTACHMENT_REFERENCE))
            .thenReturn(Optional.of(attachment));

        ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        when(attachment.getContentInputStream(this.context)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), Collections.emptyMap(), componentManager, null);
        when(
            officeDocumentBuilder.build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                ATTACHMENT_REFERENCE.getDocumentReference(), false)).thenReturn(xdomOfficeDocument);

        this.officeResourceViewer.createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS);

        verify(attachmentCache).set(eq(CACHE_KEY), any(AttachmentOfficeDocumentView.class));
    }

    /**
     * Tests creating a view for an office attachment which has already been viewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    void viewExistingOfficeAttachmentWithCacheHit() throws Exception
    {
        AttachmentOfficeDocumentView officeDocumentView =
            new AttachmentOfficeDocumentView(ATTACHMENT_RESOURCE_REFERENCE, ATTACHMENT_REFERENCE, ATTACHMENT_VERSION,
                new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(attachmentCache.get(CACHE_KEY)).thenReturn(officeDocumentView);

        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Arrays.asList(ATTACHMENT_REFERENCE));
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(ATTACHMENT_VERSION);

        assertNotNull(this.officeResourceViewer.createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS));
    }

    /**
     * Tests creating a view for an office attachment which has already been viewed and cached.
     *
     * @throws Exception if an error occurs.
     */
    @Test
    void viewTemporaryUploadedOfficeAttachmentWithCacheHit(MockitoComponentManager componentManager) throws Exception
    {
        AttachmentOfficeDocumentView officeDocumentView =
            new AttachmentOfficeDocumentView(ATTACHMENT_RESOURCE_REFERENCE, ATTACHMENT_REFERENCE, ATTACHMENT_VERSION,
                new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(attachmentCache.get(CACHE_KEY)).thenReturn(officeDocumentView);

        when(documentAccessBridge.getAttachmentReferences(ATTACHMENT_REFERENCE.getDocumentReference())).thenReturn(
            Collections.emptyList());
        when(documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(null);
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(temporaryAttachmentSessionsManager.getUploadedAttachment(ATTACHMENT_REFERENCE))
            .thenReturn(Optional.of(attachment));

        ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        when(attachment.getContentInputStream(this.context)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument =
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), Collections.emptyMap(), componentManager, null);
        when(
            officeDocumentBuilder.build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                ATTACHMENT_REFERENCE.getDocumentReference(), false)).thenReturn(xdomOfficeDocument);

        this.officeResourceViewer.createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS);

        verify(attachmentCache).set(eq(CACHE_KEY), any(AttachmentOfficeDocumentView.class));
    }

    /**
     * Tests creating a view for an external office file which has already been viewed and cached.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    void viewExistingOfficeFileWithCacheHit() throws Exception
    {
        ResourceReference resourceReference = new ResourceReference("http://resource", ResourceType.URL);

        when(this.resourceReferenceTypeSerializer.serialize(resourceReference)).thenReturn(
            "url:" + resourceReference.getReference());

        OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(resourceReference, new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(
            externalCache.get(STRING_DOCUMENT_REFERENCE + "/url:http://resource/" + DEFAULT_VIEW_PARAMETERS.hashCode()))
            .thenReturn(officeDocumentView);

        assertNotNull(this.officeResourceViewer.createView(resourceReference, DEFAULT_VIEW_PARAMETERS));
    }

    @Test
    void viewExistingOfficeFileWithCacheHitNoOwnerDocument() throws Exception
    {
        ResourceReference resourceReference = new ResourceReference("http://resource", ResourceType.URL);

        when(this.resourceReferenceTypeSerializer.serialize(resourceReference)).thenReturn(
            "url:" + resourceReference.getReference());

        Map<String, Object> parameters = Collections.emptyMap();
        when(this.documentAccessBridge.getCurrentDocumentReference())
            .thenReturn(ATTACHMENT_REFERENCE.getDocumentReference());
        OfficeDocumentView officeDocumentView =
            new OfficeDocumentView(resourceReference, new XDOM(new ArrayList<Block>()), new HashSet<File>());
        when(
            externalCache.get(STRING_DOCUMENT_REFERENCE + "/url:http://resource/" + parameters.hashCode()))
            .thenReturn(officeDocumentView);

        assertNotNull(this.officeResourceViewer.createView(resourceReference, parameters));
        verify(this.documentAccessBridge).getCurrentDocumentReference();
    }

    /**
     * Tests creating a view for an office attachment that has been viewed in past and whose version has been
     * incremented.
     * 
     * @throws Exception if an error occurs.
     */
    @Test
    void viewANewVersionOfAnExistingOfficeAttachment(MockitoComponentManager componentManager) throws Exception
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
            new XDOMOfficeDocument(new XDOM(new ArrayList<Block>()), Collections.emptyMap(), componentManager, null);
        when(
            officeDocumentBuilder.build(attachmentContent, ATTACHMENT_REFERENCE.getName(),
                ATTACHMENT_REFERENCE.getDocumentReference(), false)).thenReturn(xdomOfficeDocument);

        assertNotNull(this.officeResourceViewer.createView(ATTACHMENT_RESOURCE_REFERENCE, DEFAULT_VIEW_PARAMETERS));

        verify(attachmentCache).remove(CACHE_KEY);
        verify(attachmentCache).set(eq(CACHE_KEY), notNull());
    }

    @Test
    void viewPresentation(MockitoComponentManager componentManager) throws Exception
    {
        AttachmentResourceReference attachResourceRef =
            new AttachmentResourceReference("xwiki:Some.Page@presentation.odp");
        DocumentReference documentReference = new DocumentReference("wiki", "Some", "Page");
        AttachmentReference attachmentReference = new AttachmentReference("presentation.odp", documentReference);
        OfficeConverter officeConverter = mock(OfficeConverter.class);
        when(this.officeServer.getConverter()).thenReturn(officeConverter);
        when(officeConverter.isPresentation("presentation.odp")).thenReturn(true);

        when(attachmentReferenceResolver.resolve(attachResourceRef.getReference())).thenReturn(attachmentReference);

        when(documentAccessBridge.getAttachmentReferences(attachmentReference.getDocumentReference())).thenReturn(
            Arrays.asList(attachmentReference));
        when(documentAccessBridge.getAttachmentVersion(attachmentReference)).thenReturn("3.2");

        ByteArrayInputStream attachmentContent = new ByteArrayInputStream(new byte[256]);
        when(documentAccessBridge.getAttachmentContent(attachmentReference)).thenReturn(attachmentContent);

        String imageName = "slide0.png";
        ResourceReference imageReference = new ResourceReference(imageName, ResourceType.URL);
        ExpandedMacroBlock galleryMacro =
            new ExpandedMacroBlock("gallery", Collections.singletonMap("width", "300px"), null, false);
        galleryMacro.addChild(new ImageBlock(imageReference, true));
        XDOM xdom = new XDOM(Collections.<Block>singletonList(galleryMacro));

        OfficeConverterResult converterResult = mock(OfficeConverterResult.class);
        XDOMOfficeDocument xdomOfficeDocument = new XDOMOfficeDocument(xdom, Collections.singletonMap(imageName,
            new ByteArrayOfficeDocumentArtifact(imageName, new byte[8])), componentManager, converterResult);

        when(presentationBuilder.build(attachmentContent, attachmentReference.getName(), documentReference))
            .thenReturn(xdomOfficeDocument);

        Map<String, ?> viewParameters = Collections.singletonMap("ownerDocument", documentReference);
        TemporaryResourceReference temporaryResourceReference = new TemporaryResourceReference("officeviewer",
            Arrays.asList(String.valueOf(viewParameters.hashCode()), imageName), documentReference);

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("url", "to", imageName));
        when(this.resourceReferenceSerializer.serialize(temporaryResourceReference)).thenReturn(extendedURL);

        XDOM output = this.officeResourceViewer.createView(attachResourceRef, viewParameters);

        ImageBlock imageBlock =
            (ImageBlock) output.getBlocks(new ClassBlockMatcher(ImageBlock.class), Block.Axes.DESCENDANT).get(0);
        assertEquals("/url/to/slide0.png", imageBlock.getReference().getReference());

        galleryMacro = (ExpandedMacroBlock) output
            .getBlocks(new ClassBlockMatcher(ExpandedMacroBlock.class), Block.Axes.DESCENDANT).get(0);
        assertFalse(galleryMacro.getParent() instanceof XDOM);
        assertEquals(Syntax.XWIKI_2_1,
            ((MetaDataBlock) galleryMacro.getParent()).getMetaData().getMetaData(MetaData.SYNTAX));

        verify(this.temporaryResourceStore).createTemporaryFile(eq(temporaryResourceReference), any(InputStream.class));
        verify(converterResult).close();
    }

    @Test
    void viewURLWithLocalFile() throws AccessDeniedException
    {
        ResourceReference resourceReference = new ResourceReference("file://resource", ResourceType.URL);
        when(this.resourceReferenceTypeSerializer.serialize(resourceReference)).thenReturn(
            "url:" + resourceReference.getReference());

        Map<String, Object> parameters = Collections.emptyMap();
        DocumentReference ownerDocRef = new DocumentReference("xwiki", "Owner", "Document");
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(ownerDocRef);

        Exception expectedException = new Exception("The requested resource [file://resource] uses a protocol [file] "
            + "that is not supported.");

        Exception exception =
            assertThrows(Exception.class, () -> this.officeResourceViewer.createView(resourceReference, parameters));
        assertEquals(expectedException.getMessage(), exception.getMessage());
    }

    @Test
    void viewURLWithDistantFile(WireMockRuntimeInfo wmRuntimeInfo) throws Exception
    {
        int httpPort = wmRuntimeInfo.getHttpPort();
        String url = String.format("http://localhost:%s/myfile", httpPort);
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/myfile")).willReturn(WireMock.ok()));
        ResourceReference resourceReference = new ResourceReference(url, ResourceType.URL);
        when(this.resourceReferenceTypeSerializer.serialize(resourceReference)).thenReturn(
            "url:" + resourceReference.getReference());

        Map<String, Object> parameters = Collections.emptyMap();
        DocumentReference ownerDocRef = new DocumentReference("xwiki", "Owner", "Document");
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(ownerDocRef);

        URL resourceUrl = new URL(url);
        when(this.urlSecurityManager.isDomainTrusted(resourceUrl)).thenReturn(false);

        Exception expectedException = new Exception(String.format("The requested resource [%s] does not "
            + "belong to the list of trusted domains. "
            + "Please ask your administrator to add it to the list of trusted domains to access it.", url));

        Exception exception =
            assertThrows(Exception.class, () -> this.officeResourceViewer.createView(resourceReference, parameters));
        assertEquals(expectedException.getMessage(), exception.getMessage());

        when(this.urlSecurityManager.isDomainTrusted(resourceUrl)).thenReturn(true);

        XDOMOfficeDocument xdomOfficeDocument = mock(XDOMOfficeDocument.class);
        when(this.officeDocumentBuilder.build(any(InputStream.class), eq("myfile"), eq(ownerDocRef), eq(false)))
            .thenReturn(xdomOfficeDocument);

        XDOM expectedXDOM = mock(XDOM.class);
        when(xdomOfficeDocument.getContentDocument()).thenReturn(expectedXDOM);
        when(expectedXDOM.clone()).thenReturn(expectedXDOM);
        when(expectedXDOM.getBlocks(any(), any())).thenReturn(Collections.emptyList());
        XDOM xdom = this.officeResourceViewer.createView(resourceReference, parameters);
        assertSame(expectedXDOM, xdom);
    }
}
