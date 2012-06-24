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
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.rendering.block.XDOM;

/**
 * An abstract implementation of {@link OfficeViewer} which provides caching and other utility functions.
 * 
 * @since 2.5M2
 * @version $Id$
 */
public abstract class AbstractOfficeViewer implements OfficeViewer, Initializable
{
    /**
     * Default encoding used for encoding wiki, space, page and attachment names.
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * The module name used when creating temporary files. This is the module used by the temporary resource action to
     * retrieve the temporary file.
     */
    private static final String MODULE_NAME = "officeviewer";

    /**
     * Used to access attachment content.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /**
     * Used to access the temporary directory.
     */
    @Inject
    private Environment environment;

    /**
     * Used for serializing {@link AttachmentReference}s.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Used to initialize the view cache.
     */
    @Inject
    private CacheManager cacheManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Office document view cache.
     */
    private Cache<OfficeDocumentView> cache;

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration config = new CacheConfiguration();
        config.setConfigurationId(MODULE_NAME);
        try {
            cache = cacheManager.createNewCache(config);
        } catch (CacheException e) {
            throw new InitializationException("Failed to create cache.", e);
        }
    }

    @Override
    public XDOM createView(AttachmentReference attachmentReference, Map<String, String> parameters) throws Exception
    {
        // Search the cache.
        String cacheKey = getCacheKey(attachmentReference, parameters);
        OfficeDocumentView view = cache.get(cacheKey);

        // It's possible that the attachment has been deleted. We need to catch such events and cleanup the cache.
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        if (!documentAccessBridge.getAttachmentReferences(documentReference).contains(attachmentReference)) {
            // If a cached view exists, flush it.
            if (view != null) {
                cache.remove(cacheKey);
            }
            throw new Exception(String.format("Attachment [%s] does not exist.", attachmentReference));
        }

        // Check if the view has expired.
        String currentVersion = documentAccessBridge.getAttachmentVersion(attachmentReference);
        if (view != null && !currentVersion.equals(view.getVersion())) {
            // Flush the cached view.
            cache.remove(cacheKey);
            view = null;
        }

        // If a view in not available, build one and cache it.
        if (view == null) {
            view = createOfficeDocumentView(attachmentReference, parameters);
            cache.set(cacheKey, view);
        }

        // We have to clone the cached XDOM to protect it from the rendering transformations. For instance, macro
        // transformations must be executed even when the XDOM is taken from the cache.
        return view.getXDOM().clone();
    }

    /**
     * @param attachmentReference reference to the attachment to be viewed
     * @param viewParameters implementation specific view parameters
     * @return a key to cache the view of the specified attachment
     */
    private String getCacheKey(AttachmentReference attachmentReference, Map<String, String> viewParameters)
    {
        return serializer.serialize(attachmentReference) + '/' + viewParameters.hashCode();
    }

    /**
     * Creates an {@link OfficeDocumentView} of the specified attachment.
     * 
     * @param attachmentReference reference to the attachment to be viewed
     * @param parameters implementation specific view parameters
     * @return {@link OfficeDocumentView} that can be used to cache the XDOM representation of the specified attachment
     * @throws Exception if an error occurs while creating the view
     */
    protected abstract OfficeDocumentView createOfficeDocumentView(AttachmentReference attachmentReference,
        Map<String, String> parameters) throws Exception;

    /**
     * Creates a temporary file that stores the given data.
     * 
     * @param file the temporary file to be created
     * @param fileData file data to be written
     * @throws Exception if an error occurs while creating the temporary file
     */
    protected void createTemporaryFile(File file, byte[] fileData) throws Exception
    {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            IOUtils.write(fileData, fos);
            file.deleteOnExit();
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    /**
     * Utility method for obtaining a temporary storage directory for an attachment.
     * 
     * @param attachmentReference reference to the attachment
     * @return temporary directory for the specified attachment
     * @throws Exception if creating or accessing the temporary directory fails
     */
    protected File getTemporaryDirectory(AttachmentReference attachmentReference) throws Exception
    {
        // Extract wiki, space, page and attachment name.
        String wiki = attachmentReference.getDocumentReference().getWikiReference().getName();
        String space = attachmentReference.getDocumentReference().getParent().getName();
        String page = attachmentReference.getDocumentReference().getName();
        String attachmentName = attachmentReference.getName();

        // Encode to avoid illegal characters in file paths.
        wiki = URLEncoder.encode(wiki, DEFAULT_ENCODING);
        space = URLEncoder.encode(space, DEFAULT_ENCODING);
        page = URLEncoder.encode(page, DEFAULT_ENCODING);
        attachmentName = URLEncoder.encode(attachmentName, DEFAULT_ENCODING);

        // Create temporary directory.
        String path = String.format("temp/%s/%s/%s/%s/%s/", MODULE_NAME, wiki, space, page, attachmentName);
        File rootDir = this.environment.getTemporaryDirectory();
        File tempDir = new File(rootDir, path);
        boolean success = (tempDir.exists() || tempDir.mkdirs()) && tempDir.isDirectory() && tempDir.canWrite();
        if (!success) {
            String message = "Error while creating temporary directory [%s] for attachment [%s].";
            throw new Exception(String.format(message, tempDir, attachmentName));
        }
        return tempDir;
    }

    /**
     * Utility method for obtaining a temporary file to store an artifact produced by the office viewer.
     * 
     * @param attachmentReference a reference to the attachment that produced the artifact
     * @param artifactName the name of the artifact to be saved in the temporary file
     * @return a temporary file to store the specified artifact
     * @throws Exception if {@link #getTemporaryDirectory(AttachmentReference)} throws an exception
     */
    protected File getTemporaryFile(AttachmentReference attachmentReference, String artifactName) throws Exception
    {
        // Encode to avoid illegal characters in the artifact name.
        String encodedArtifactName = URLEncoder.encode(artifactName, DEFAULT_ENCODING);
        return new File(getTemporaryDirectory(attachmentReference), encodedArtifactName);
    }

    /**
     * Utility method for building a URL to the specified temporary file.
     * 
     * @param attachmentReference attachment to which the temporary file is associated
     * @param fileName name of the temporary file
     * @return URL string that refers the specified temporary file
     */
    protected String buildURL(AttachmentReference attachmentReference, String fileName)
    {
        // We need the absolute URL because the gallery macro, which is used when viewing an office presentation, uses
        // the syntax of the target document to parse its content and XWiki 2.0 syntax (unlike XWiki 2.1) doesn't
        // support path image references (e.g. image:path:/one/two/three.png).
        String prefix =
            documentAccessBridge.getDocumentURL(attachmentReference.getDocumentReference(), "temp", null, null, true);
        try {
            String encodedAttachmentName = URLEncoder.encode(attachmentReference.getName(), DEFAULT_ENCODING);
            String encodedFileName = URLEncoder.encode(fileName, DEFAULT_ENCODING);
            return String.format("%s/%s/%s/%s", prefix, MODULE_NAME, encodedAttachmentName, encodedFileName);
        } catch (UnsupportedEncodingException e) {
            // This should never happen.
            this.logger.error("Failed to encode URL using " + DEFAULT_ENCODING, e);
            return null;
        }
    }
}
