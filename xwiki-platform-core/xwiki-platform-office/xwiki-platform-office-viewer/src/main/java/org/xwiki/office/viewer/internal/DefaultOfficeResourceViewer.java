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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageAttachmentReference;
import org.xwiki.model.reference.PageAttachmentReferenceResolver;
import org.xwiki.office.viewer.OfficeResourceViewer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLSecurityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Default implementation of {@link org.xwiki.office.viewer.OfficeResourceViewer}.
 * 
 * @since 5.4.6
 * @since 6.2.2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultOfficeResourceViewer implements OfficeResourceViewer, Initializable
{
    /**
     * The module name used when creating temporary files. This is the module used by the temporary resource action to
     * retrieve the temporary file.
     */
    private static final String MODULE_NAME = "officeviewer";

    /**
     * List of protocols that can be used in URLs to retrieve documents.
     */
    private static final List<String> ALLOWED_PROTOCOLS = Arrays.asList(
        "http",
        "https",
        "ftp",
        "ftps"
    );

    /**
     * Used to access attachment content.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    @Inject
    private ResourceReferenceSerializer<org.xwiki.resource.ResourceReference, ExtendedURL> resourceReferenceSerializer;

    /**
     * Used for serializing {@link AttachmentReference}s.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ResourceReferenceTypeSerializer resourceReferenceTypeSerializer;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    @Named("current")
    private PageAttachmentReferenceResolver<String> pageAttachmentResolver;

    @Inject
    private AttachmentReferenceResolver<EntityReference> attachmentConverter;

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    /**
     * Used to initialize the view cache.
     */
    @Inject
    private CacheManager cacheManager;

    /**
     * Attachment based office document view cache.
     */
    private Cache<AttachmentOfficeDocumentView> attachmentCache;

    /**
     * External file based office document view cache.
     */
    private Cache<OfficeDocumentView> externalCache;

    /**
     * Used to build XDOM documents from office documents.
     */
    @Inject
    private XDOMOfficeDocumentBuilder documentBuilder;

    /**
     * Used to build XDOM documents from office presentations.
     */
    @Inject
    private PresentationBuilder presentationBuilder;

    /**
     * Used to access the document converter.
     */
    @Inject
    private OfficeServer officeServer;

    @Inject
    private ConverterManager converter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private URLSecurityManager urlSecurityManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Processes all the image blocks in the given XDOM and changes image URL to point to a temporary file for those
     * images that are view artifacts.
     * 
     * @param xdom the XDOM whose image blocks are to be processed
     * @param artifactMap specify which of the image blocks should be processed; only the image blocks
     *          that were generated during the office import process should be processed
     * @param ownerDocumentReference specifies the document that owns the office file
     * @param parameters the build parameters. Note that currently only {@code filterStyles} is supported and if "true"
     *            it means that styles will be filtered to the maximum and the focus will be put on importing only the
     * @return the set of temporary files corresponding to image artifacts
     */
    private Set<File> processImages(XDOM xdom, Map<String, OfficeDocumentArtifact> artifactMap,
        DocumentReference ownerDocumentReference, Map<String, ?> parameters)
    {
        // Process all image blocks.
        Set<File> temporaryFiles = new HashSet<>();
        List<ImageBlock> imgBlocks = xdom.getBlocks(new ClassBlockMatcher(ImageBlock.class), Block.Axes.DESCENDANT);
        if (!imgBlocks.isEmpty()) {
            for (ImageBlock imgBlock : imgBlocks) {
                String imageReference = imgBlock.getReference().getReference();

                // Check whether there is a corresponding artifact.
                if (artifactMap.containsKey(imageReference)) {
                    try {
                        List<String> resourcePath =
                            Arrays.asList(String.valueOf(parameters.hashCode()), imageReference);
                        TemporaryResourceReference temporaryResourceReference =
                            new TemporaryResourceReference(MODULE_NAME, resourcePath, ownerDocumentReference);

                        // Write the image into a temporary file.
                        OfficeDocumentArtifact artifact = artifactMap.get(imageReference);

                        File tempFile;
                        try (InputStream inputStream = artifact.getContentInputStream()) {
                            tempFile = this.temporaryResourceStore.createTemporaryFile(temporaryResourceReference,
                                inputStream);
                        }

                        // Create a URL image reference which links to above temporary image file.
                        String temporaryResourceURL =
                            this.resourceReferenceSerializer.serialize(temporaryResourceReference).serialize();
                        ResourceReference urlImageReference =
                            new ResourceReference(temporaryResourceURL, ResourceType.PATH);
                        urlImageReference.setTyped(true);

                        // Replace the old image block with a new one that uses the above URL image reference.
                        Block newImgBlock = new ImageBlock(urlImageReference, false, imgBlock.getParameters());
                        imgBlock.getParent().replaceChild(Arrays.asList(newImgBlock), imgBlock);

                        // Make sure the new image block is not inside an ExpandedMacroBlock whose's content syntax
                        // doesn't support relative path resource references (we use relative paths to refer the
                        // temporary files).
                        maybeFixExpandedMacroAncestor(newImgBlock);

                        // Collect the temporary file so that it can be cleaned up when the view is disposed from cache.
                        temporaryFiles.add(tempFile);
                    } catch (Exception ex) {
                        String message = "Error while processing artifact image [%s].";
                        this.logger.error(String.format(message, imageReference), ex);
                    }
                }
            }
        }
        return temporaryFiles;
    }

    private void maybeFixExpandedMacroAncestor(Block block)
    {
        ExpandedMacroBlock expandedMacro =
            block.getFirstBlock(new ClassBlockMatcher(ExpandedMacroBlock.class), Block.Axes.ANCESTOR_OR_SELF);
        if (expandedMacro != null) {
            Block parent = expandedMacro.getParent();
            if (!(parent instanceof MetaDataBlock) || !((MetaDataBlock) parent).getMetaData().contains(MODULE_NAME)) {
                MetaDataBlock metaData = new MetaDataBlock(Collections.emptyList());
                // Use a syntax that supports relative path resource references (we use relative paths to include the
                // temporary files).
                metaData.getMetaData().addMetaData(MetaData.SYNTAX, Syntax.XWIKI_2_1);
                metaData.getMetaData().addMetaData(MODULE_NAME, true);
                parent.replaceChild(metaData, expandedMacro);
                metaData.addChild(expandedMacro);
            }
        }
    }

    /**
     * Creates a {@link XDOM} representation of the specified office attachment.
     * 
     * @param attachmentReference a reference to the office file to be parsed into XDOM
     * @param parameters the build parameters. Note that currently only {@code filterStyles} is supported and if "true"
     *            it means that styles will be filtered to the maximum and the focus will be put on importing only the
     *            content
     * @return the {@link XDOMOfficeDocument} corresponding to the specified office file
     * @throws Exception if building the XDOM fails
     */
    private XDOMOfficeDocument createXDOM(AttachmentReference attachmentReference, Map<String, ?> parameters)
        throws Exception
    {
        InputStream officeFileStream;
        if (this.documentAccessBridge.getAttachmentVersion(attachmentReference) != null) {
            officeFileStream = this.documentAccessBridge.getAttachmentContent(attachmentReference);
        } else {
            Optional<XWikiAttachment> uploadedAttachment =
                this.temporaryAttachmentSessionsManager.getUploadedAttachment(attachmentReference);
            if (uploadedAttachment.isPresent()) {
                officeFileStream = uploadedAttachment.get().getContentInputStream(this.contextProvider.get());
            } else {
                throw new OfficeImporterException(
                    String.format("Error when loading temporary attachment [%s]", attachmentReference));
            }
        }
        String officeFileName = attachmentReference.getName();

        return createXDOM(attachmentReference.getDocumentReference(), officeFileStream, officeFileName, parameters);
    }

    private XDOMOfficeDocument createXDOM(DocumentReference ownerDocument, ResourceReference resourceReference,
        Map<String, ?> parameters) throws Exception
    {
        InputStream officeFileStream;
        String officeFileName;
        XDOMOfficeDocument result = null;

        if (resourceReference.getType().equals(ResourceType.URL)) {
            URL url = new URL(resourceReference.getReference());
            if (!ALLOWED_PROTOCOLS.contains(url.getProtocol())) {
                throw new Exception(
                    String.format("The requested resource [%s] uses a protocol [%s] that is not supported.", url,
                        url.getProtocol()));
            } else if (!this.urlSecurityManager.isDomainTrusted(url)) {
                throw new Exception(
                    String.format("The requested resource [%s] does not belong to the list of trusted domains. "
                        + "Please ask your administrator to add it to the list of trusted domains to access it.", url));
            } else {
                officeFileStream = url.openStream();
                officeFileName = StringUtils.substringAfterLast(url.getPath(), "/");
                result = createXDOM(ownerDocument, officeFileStream, officeFileName, parameters);
            }
        } else {
            throw new Exception(String.format("Unsupported resource type [%s].", resourceReference.getType()));
        }
        return result;
    }

    private XDOMOfficeDocument createXDOM(DocumentReference ownerDocument, InputStream officeFileStream,
        String officeFileName, Map<String, ?> parameters) throws Exception
    {
        try {
            if (isPresentation(officeFileName)) {
                return this.presentationBuilder.build(officeFileStream, officeFileName, ownerDocument);
            } else {
                boolean filterStyles = this.converter.convert(boolean.class, parameters.get("filterStyles"));
                return this.documentBuilder.build(officeFileStream, officeFileName, ownerDocument, filterStyles);
            }
        } finally {
            IOUtils.closeQuietly(officeFileStream);
        }
    }

    /**
     * Utility method for checking if a file name corresponds to an office presentation.
     * 
     * @param fileName attachment file name
     * @return {@code true} if the file extension represents an office presentation format, {@code false} otherwise
     */
    private boolean isPresentation(String fileName)
    {
        OfficeConverter officeConverter = this.officeServer.getConverter();
        if (officeConverter != null) {
            return officeConverter.isPresentation(fileName);
        }

        return false;
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            LRUCacheConfiguration attachmentConfig = new LRUCacheConfiguration(MODULE_NAME + ".attachment", 50);
            this.attachmentCache = this.cacheManager.createNewCache(attachmentConfig);

            // We have no idea when to invalidate the cache so lets at least put a time to live
            LRUCacheConfiguration exteralConfig = new LRUCacheConfiguration(MODULE_NAME + ".external", 50, 3600);
            this.externalCache = this.cacheManager.createNewCache(exteralConfig);
        } catch (CacheException e) {
            throw new InitializationException("Failed to create caches.", e);
        }
    }

    private OfficeDocumentView getView(ResourceReference reference, AttachmentReference attachmentReference,
        Map<String, ?> parameters) throws Exception
    {
        // Search the cache.
        String cacheKey =
            getCacheKey(attachmentReference.getDocumentReference(), attachmentReference.getName(), parameters);
        AttachmentOfficeDocumentView view = this.attachmentCache.get(cacheKey);
        Optional<XWikiAttachment> uploadedAttachment =
            this.temporaryAttachmentSessionsManager.getUploadedAttachment(attachmentReference);

        // It's possible that the attachment has been deleted. We need to catch such events and cleanup the cache.
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        if (!this.documentAccessBridge.getAttachmentReferences(documentReference).contains(attachmentReference)
            && !uploadedAttachment.isPresent())
        {
            // If a cached view exists, flush it.
            if (view != null) {
                this.attachmentCache.remove(cacheKey);
            }
            throw new Exception(String.format("Attachment [%s] does not exist.", attachmentReference));
        }

        // Check if the view has expired.
        String currentVersion = this.documentAccessBridge.getAttachmentVersion(attachmentReference);
        if (view != null && !StringUtils.equals(currentVersion, view.getVersion())) {
            // Flush the cached view.
            this.attachmentCache.remove(cacheKey);
            view = null;
        }

        // If a view in not available, build one and cache it.
        if (view == null) {
            try (XDOMOfficeDocument xdomOfficeDocument = createXDOM(attachmentReference, parameters)) {
                String attachmentVersion = this.documentAccessBridge.getAttachmentVersion(attachmentReference);
                if (attachmentVersion == null && uploadedAttachment.isPresent()) {
                    attachmentVersion = "temp";
                }
                XDOM xdom = xdomOfficeDocument.getContentDocument();
                // We use only the file name from the resource reference because the rest of the information is
                // specified by the owner document reference. This way we ensure the path to the temporary files
                // doesn't contain redundant information and so it remains as small as possible (considering that the
                // path length is limited on some environments).
                Set<File> temporaryFiles = processImages(xdom, xdomOfficeDocument.getArtifactsMap(),
                    attachmentReference.getDocumentReference(), parameters);
                view = new AttachmentOfficeDocumentView(reference, attachmentReference, attachmentVersion, xdom,
                    temporaryFiles);

                this.attachmentCache.set(cacheKey, view);
            }
        }

        // We have to clone the cached XDOM to protect it from the rendering transformations. For instance, macro
        // transformations must be executed even when the XDOM is taken from the cache.
        return view;
    }

    private OfficeDocumentView getView(ResourceReference resourceReference, Map<String, ?> parameters) throws Exception
    {
        DocumentReference ownerDocument = getOwnerDocument(parameters);
        String serializedResourceReference = this.resourceReferenceTypeSerializer.serialize(resourceReference);

        // Search the cache.
        String cacheKey = getCacheKey(ownerDocument, serializedResourceReference, parameters);
        OfficeDocumentView view = this.externalCache.get(cacheKey);

        // If a view in not available, build one and cache it.
        if (view == null) {
            try (XDOMOfficeDocument xdomOfficeDocument = createXDOM(ownerDocument, resourceReference, parameters))
            {
                XDOM xdom = xdomOfficeDocument.getContentDocument();
                Set<File> temporaryFiles = processImages(xdom, xdomOfficeDocument.getArtifactsMap(), ownerDocument,
                    parameters);
                view = new OfficeDocumentView(resourceReference, xdom, temporaryFiles);

                this.externalCache.set(cacheKey, view);
            }
        }

        return view;
    }

    @Override
    public XDOM createView(ResourceReference reference, Map<String, ?> parameters) throws Exception
    {
        OfficeDocumentView view;

        if (reference.getType().equals(ResourceType.ATTACHMENT) || reference.getType().equals(ResourceType.UNKNOWN)) {
            AttachmentReference attachmentReference = this.attachmentResolver.resolve(reference.getReference());

            view = getView(reference, attachmentReference, parameters);
        } else if (reference.getType().equals(ResourceType.PAGE_ATTACHMENT)) {
            PageAttachmentReference attachmentReference = this.pageAttachmentResolver.resolve(reference.getReference());

            view = getView(reference, this.attachmentConverter.resolve(attachmentReference), parameters);
        } else {
            view = getView(reference, parameters);
        }

        // We have to clone the cached XDOM to protect it from the rendering transformations. For instance, macro
        // transformations must be executed even when the XDOM is taken from the cache.
        return view.getXDOM().clone();
    }

    private String getCacheKey(DocumentReference ownerDocument, String resource, Map<String, ?> parameters)
    {
        return this.serializer.serialize(ownerDocument) + '/' + resource + '/' + parameters.hashCode();
    }

    private DocumentReference getOwnerDocument(Map<String, ?> parameters)
    {
        DocumentReference ownerDocument =
            this.converter.convert(DocumentReference.class, parameters.get("ownerDocument"));
        if (ownerDocument == null) {
            ownerDocument = this.documentAccessBridge.getCurrentDocumentReference();
        }

        return ownerDocument;
    }
}
