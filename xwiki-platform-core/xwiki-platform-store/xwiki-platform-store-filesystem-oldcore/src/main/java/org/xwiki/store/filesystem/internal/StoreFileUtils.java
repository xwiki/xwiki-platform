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
package org.xwiki.store.filesystem.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStoreException;

/**
 * Internal class for providing static utilities used by multiple classes in this package.
 *
 * @version $Id$
 * @since 11.0
 */
public final class StoreFileUtils
{
    private static final String FILE_VERSION_PREFIX = "v";

    private static final String FILE_NAME = "f";

    /**
     * Private constructor for utility class.
     */
    private StoreFileUtils()
    {
    }

    /**
     * Get the stored (optionally versionned) name of a file name assuming it's in an associated unique folder.
     *
     * @param filename the name of the file to save. This will be URL encoded.
     * @param versionName the name of the version of the file. This will also be URL encoded.
     * @return a string representing the filename and version which is guaranteed not to collide with any other file
     *         gotten through DefaultFilesystemStoreTools.
     * @since 11.0
     */
    public static String getStoredFilename(final String filename, final String versionName)
    {
        StringBuilder storedFileNameBuilder = new StringBuilder(FILE_NAME);

        if (versionName != null) {
            storedFileNameBuilder.append(FILE_VERSION_PREFIX);
            storedFileNameBuilder.append(versionName);
        }

        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex != -1) {
            storedFileNameBuilder.append(filename.substring(extensionIndex));
        }

        return storedFileNameBuilder.toString();
    }

    /**
     * A helper that returns either the blob if it exists or the linked blob if there is a link instead.
     * 
     * @param targetBlob the target blob
     * @param followLinks true if links should be followed
     * @return the resolved blob
     * @throws BlobStoreException when failing to resolve the link
     * @throws IOException when failing to read the link blob
     * @since 16.4.0RC1
     */
    public static Blob resolve(Blob targetBlob, boolean followLinks) throws BlobStoreException, IOException
    {
        // Return the target blob by default
        Blob blob = targetBlob;

        while (!blob.exists()) {
            // If the blob does not exist, check if there is a link instead
            Blob linkBlob = getLinkBlob(blob);

            if (linkBlob.exists()) {
                if (followLinks) {
                    // Move the target blob to the link's target blob
                    String linkContent = IOUtils.toString(linkBlob.getStream(), StandardCharsets.UTF_8).trim();
                    blob = linkBlob.getStore().getBlob(linkBlob.getPath().resolveSibling(linkContent));
                } else {
                    // Stop at the link blob if we don't follow it
                    blob = linkBlob;
                }
            } else {
                // Stop the loop since no blob or link could be found
                break;
            }
        }

        return blob;
    }

    /**
     * @param originalfile the location for which to create a link
     * @return the File representing the link for the passed location
     * @since 17.10.0RC1
     */
    public static Blob getLinkBlob(Blob originalfile) throws BlobStoreException
    {
        BlobPath originalPath = originalfile.getPath();
        // The file name cannot be null here as the storage file cannot be the root.
        String fileName = Objects.requireNonNull(originalPath.getFileName()).toString();
        BlobPath linkPath = originalPath.resolveSibling(fileName + ".lnk");
        return originalfile.getStore().getBlob(linkPath);
    }

    /**
     * @param folder the folder where the link is located
     * @param targetFile the target file
     * @return the content of the link file
     * @since 16.4.0RC1
     */
    public static String getLinkContent(File folder, File targetFile)
    {
        return folder.toPath().relativize(targetFile.toPath()).toString();
    }
}
