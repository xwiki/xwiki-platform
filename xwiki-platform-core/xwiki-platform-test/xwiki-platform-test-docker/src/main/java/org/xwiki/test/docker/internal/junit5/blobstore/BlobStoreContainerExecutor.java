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
package org.xwiki.test.docker.internal.junit5.blobstore;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.xwiki.test.docker.internal.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.blobstore.BlobStore;

/**
 * Create and execute the Docker blob store container for the tests.
 *
 * @version $Id$
 * @since 16.9.0RC1
 */
public class BlobStoreContainerExecutor extends AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreContainerExecutor.class);

    private static final String BUCKET_NAME = "xwiki";

    private static final String NETWORK_ALIAS = "xwikiminio";

    private static final String USERNAME = "minioadmin";

    private static final String PASSWORD = "miniopassword";

    private static final String MINIO_CLIENT = "mc";

    /**
     * @param testConfiguration the configuration to build (blob store, debug mode, etc)
     * @throws Exception if the container fails to start
     */
    public void start(TestConfiguration testConfiguration) throws Exception
    {
        BlobStore blobStore = testConfiguration.getBlobStore();
        if (blobStore == null) {
            // Default to filesystem
            return;
        }

        switch (blobStore) {
            case FILESYSTEM:
                // No container needed for filesystem blob store
                break;
            case S3:
                startMinIOContainer(testConfiguration);
                break;
            default:
                throw new DockerTestException(String.format("Blob store [%s] is not yet supported!",
                    testConfiguration.getBlobStore()));
        }
    }

    /**
     * @param testConfiguration the configuration to build (blob store, debug mode, etc)
     */
    public void stop(TestConfiguration testConfiguration)
    {
        // Note that we don't need to stop the container as this is taken care of by TestContainers
    }

    private void startMinIOContainer(TestConfiguration testConfiguration) throws Exception
    {
        MinIOContainer minioContainer;
        if (StringUtils.isNotBlank(testConfiguration.getBlobStoreTag())) {
            minioContainer = new MinIOContainer(String.format("minio/minio:%s", testConfiguration.getBlobStoreTag()));
        } else {
            // No tag specified, use "latest"
            minioContainer = new MinIOContainer("minio/minio:latest");
        }

        minioContainer
            .withUserName(USERNAME)
            .withPassword(PASSWORD)
            .withNetwork(Network.SHARED)
            .withNetworkAliases(NETWORK_ALIAS);

        start(minioContainer, testConfiguration);

        String endpoint;
        if (testConfiguration.getServletEngine().isOutsideDocker()) {
            endpoint = minioContainer.getS3URL();
        } else {
            endpoint = String.format("http://%s:9000", NETWORK_ALIAS);
        }

        testConfiguration.getBlobStore().setEndpoint(endpoint);

        // Create the bucket
        createBucket(minioContainer);
    }

    private void createBucket(MinIOContainer minioContainer) throws Exception
    {
        // Execute the MinIO client command inside the container to create the bucket
        LOGGER.info("Creating S3 bucket [{}]", BUCKET_NAME);

        minioContainer.execInContainer(MINIO_CLIENT, "alias", "set", "local", "http://localhost:9000",
            USERNAME, PASSWORD);
        minioContainer.execInContainer(MINIO_CLIENT, "mb", "local/" + BUCKET_NAME);

        LOGGER.info("Successfully created S3 bucket [{}]", BUCKET_NAME);
    }

    /**
     * @return the bucket name used for S3 blob store
     */
    public static String getBucketName()
    {
        return BUCKET_NAME;
    }

    /**
     * @return the access key used for S3 blob store
     */
    public static String getAccessKey()
    {
        return USERNAME;
    }

    /**
     * @return the secret key used for S3 blob store
     */
    public static String getSecretKey()
    {
        return PASSWORD;
    }
}
