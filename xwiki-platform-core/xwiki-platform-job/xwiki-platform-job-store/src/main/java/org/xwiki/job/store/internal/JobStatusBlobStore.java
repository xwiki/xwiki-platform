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
package org.xwiki.job.store.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.internal.JobStatusSerializer;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.BlobWriteMode;

/**
 * Stores and loads job status blobs from the configured blob store.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component(roles = JobStatusBlobStore.class)
@Singleton
public class JobStatusBlobStore implements Initializable
{
    private static final String BLOB_STORE_NAME = "jobstatus-db";

    @Inject
    private BlobStoreManager blobStoreManager;

    @Inject
    private JobStatusSerializer serializer;

    @Inject
    private Logger logger;

    private BlobStore blobStore;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.blobStore = this.blobStoreManager.getBlobStore(BLOB_STORE_NAME);
        } catch (BlobStoreException e) {
            throw new InitializationException(
                "Failed to initialize JobStatusBlobStore with blob store [%s].".formatted(BLOB_STORE_NAME), e);
        }
    }

    /**
     * Store the given job status in the blob store at the location specified by the blob locator. The blob locator
     * is expected to be a valid blob path for the configured blob store.
     *
     * @param status the job status to store
     * @param blobLocator the path in the blob store where the job status should be stored
     * @throws BlobStoreException if an error occurs while accessing the blob store
     * @throws IOException if an error occurs while writing the job status to the blob store
     */
    public void store(JobStatus status, String blobLocator) throws BlobStoreException, IOException
    {
        Blob blob = this.blobStore.getBlob(BlobPath.parse(blobLocator));
        try (OutputStream outputStream = blob.getOutputStream(BlobWriteMode.REPLACE_EXISTING)) {
            this.serializer.write(status, outputStream, isZipFile(blobLocator));
        }
    }

    /**
     * Delete the job status blob at the location specified by the blob locator. The blob locator is expected to be a
     * valid blob path for the configured blob store.
     *
     * @param blobLocator the path in the blob store where the job status is stored
     */
    public void delete(String blobLocator)
    {
        if (StringUtils.isBlank(blobLocator)) {
            return;
        }

        try {
            this.blobStore.deleteBlob(BlobPath.parse(blobLocator));
        } catch (BlobStoreException e) {
            this.logger.warn("Failed to delete job status blob [{}].", blobLocator, e);
        }
    }

    /**
     * Load the job status from the blob store at the location specified by the blob locator. The blob locator is
     * expected to be a valid blob path for the configured blob store.
     *
     * @param blobLocator the path in the blob store where the job status is stored
     * @return the loaded job status
     * @throws BlobStoreException if an error occurs while accessing the blob store or deserializing the job status
     */
    public JobStatus load(String blobLocator) throws BlobStoreException
    {
        Blob blob = this.blobStore.getBlob(BlobPath.parse(blobLocator));

        try (InputStream stream = blob.getStream()) {
            return this.serializer.read(stream, isZipFile(blobLocator));
        } catch (Exception e) {
            throw new BlobStoreException("Failed to deserialize job status blob at [%s].".formatted(blobLocator), e);
        }
    }

    private static boolean isZipFile(String blobLocator)
    {
        return blobLocator.endsWith(".zip");
    }
}
