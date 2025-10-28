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
package org.xwiki.test.docker.junit5.blobstore;

/**
 * The blob store backend to use for the UI tests.
 *
 * @version $Id$
 * @since 16.9.0RC1
 */
public enum BlobStore
{
    /**
     * Represents the filesystem blob store (default).
     */
    FILESYSTEM("filesystem"),

    /**
     * Represents the S3 blob store using MinIO.
     */
    S3("s3");

    private String endpoint;

    private final String hint;

    /**
     * @param hint the hint to use in xwiki.properties for store.blobStoreHint
     */
    BlobStore(String hint)
    {
        this.hint = hint;
    }

    /**
     * @param endpoint see {@link #getEndpoint()}
     */
    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    /**
     * @return the endpoint URL to use to connect to the blob store (only applicable for S3)
     */
    public String getEndpoint()
    {
        return this.endpoint;
    }

    /**
     * @return the hint to use in xwiki.properties for store.blobStoreHint
     */
    public String getHint()
    {
        return this.hint;
    }
}
