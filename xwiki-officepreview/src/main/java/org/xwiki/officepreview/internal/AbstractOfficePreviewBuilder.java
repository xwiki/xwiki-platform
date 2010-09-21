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
package org.xwiki.officepreview.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.Container;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officepreview.OfficePreviewBuilder;
import org.xwiki.officepreview.OfficePreviewConfiguration;
import org.xwiki.rendering.block.XDOM;

/**
 * An abstract implementation of {@link OfficePreviewBuilder} which provides caching and other utility functions.
 * 
 * @since 2.5M2
 * @version $Id$
 */
public abstract class AbstractOfficePreviewBuilder extends AbstractLogEnabled implements OfficePreviewBuilder,
    Initializable
{
    /**
     * Default encoding used for encoding wiki, space, page and attachment names.
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Used to read configuration details.
     */
    @Requirement
    protected OfficePreviewConfiguration conf;

    /**
     * Used to access the temporary directory.
     */
    @Requirement
    private Container container;

    /**
     * Used to access attachment content.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * Used for serializing {@link AttachmentReference}s.
     */
    @Requirement
    private EntityReferenceSerializer<String> serializer;

    /**
     * Used to initialize the previews cache.
     */
    @Requirement
    private CacheManager cacheManager;

    /**
     * Office document previews cache.
     */
    private Cache<OfficeDocumentPreview> previewsCache;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        CacheConfiguration config = new CacheConfiguration();
        LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
        lec.setMaxEntries(conf.getCacheSize());
        config.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);
        try {
            previewsCache = cacheManager.createNewCache(config);
        } catch (CacheException ex) {
            throw new InitializationException("Error while initializing previews cache.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public XDOM build(AttachmentReference attachmentReference, boolean filterStyles) throws Exception
    {
        String strAttachRef = serializer.serialize(attachmentReference);
        DocumentReference documentReference = attachmentReference.getDocumentReference();

        // Search the cache.
        OfficeDocumentPreview preview = previewsCache.get(strAttachRef);

        // It's possible that the attachment has been deleted. We need to catch such events and cleanup the cache.
        if (!docBridge.getAttachmentReferences(documentReference).contains(attachmentReference)) {
            // If a cached preview exists, flush it.
            if (null != preview) {
                previewsCache.remove(strAttachRef);
            }
            throw new Exception(String.format("Attachment [%s] does not exist.", strAttachRef));
        }

        // Query the current version of the attachment.
        String currentVersion = docBridge.getAttachmentVersion(attachmentReference);

        // Check if the preview has been expired.
        if (null != preview && !currentVersion.equals(preview.getVersion())) {
            // Flush the cached preview.
            previewsCache.remove(strAttachRef);
            preview = null;
        }

        // If a preview in not available, build one.
        if (null == preview) {
            // Build preview.
            preview =
                build(attachmentReference, currentVersion, docBridge.getAttachmentContent(attachmentReference),
                    filterStyles);

            // Cache the preview.
            previewsCache.set(strAttachRef, preview);
        }

        // Done.
        return preview.getXDOM();
    }

    /**
     * Builds a preview of the specified attachment.
     * 
     * @param attachmentReference reference to the attachment to be previewed.
     * @param version version of the attachment for which the preview should be generated for.
     * @param data content of the attachment.
     * @param filterStyles whether office document styles should be filtered.
     * @return {@link OfficeDocumentPreview} corresponding to the preview of the specified attachment.
     * @throws Exception if an error occurs while building the preview.
     */
    protected abstract OfficeDocumentPreview build(AttachmentReference attachmentReference, String version,
        InputStream data, boolean filterStyles) throws Exception;

    /**
     * Saves a temporary file associated with the given attachment.
     * 
     * @param attachmentReference reference to the attachment to which this temporary file belongs.
     * @param fileName name of the temporary file.
     * @param fileData file data.
     * @return file that was just written.
     * @throws Exception if an error occurs while writing the temporary file.
     */
    protected File saveTemporaryFile(AttachmentReference attachmentReference, String fileName, byte[] fileData)
        throws Exception
    {
        File tempFile = new File(getTemporaryDirectory(attachmentReference), fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);
            IOUtils.write(fileData, fos);
            tempFile.deleteOnExit();
            return tempFile;
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    /**
     * Utility method for obtaining a temporary storage directory for an attachment.
     * 
     * @param attachmentReference reference to the attachment.
     * @return temporary directory for the specified attachment.
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
        String path = String.format("temp/officepreview/%s/%s/%s/%s/", wiki, space, page, attachmentName);
        File rootDir = container.getApplicationContext().getTemporaryDirectory();
        File tempDir = new File(rootDir, path);
        boolean success = tempDir.exists() ? true : tempDir.mkdirs();
        success = success && tempDir.isDirectory();
        success = success && tempDir.canWrite();
        if (!success) {
            String message = "Error while creating temporary directory for attachment [%s].";
            throw new Exception(String.format(message, attachmentName));
        }
        return tempDir;
    }

    /**
     * Utility method for building a URL to the specified temporary file.
     * 
     * @param attachmentReference attachment to which the temporary file is associated.
     * @param fileName name of the temporary file.
     * @return URL string that refers the specified temporary file.
     */
    protected String buildURL(AttachmentReference attachmentReference, String fileName)
    {
        String prefix = docBridge.getDocumentURL(attachmentReference.getDocumentReference(), "temp", null, null);
        String attachmentName = attachmentReference.getName();
        return String.format("%s/officepreview/%s/%s", prefix, attachmentName, fileName);
    }
}
