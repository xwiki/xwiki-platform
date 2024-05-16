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

import org.apache.commons.io.FileUtils;

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
     * A helper which return either the file if it exist or the linked file if there is a link instead.
     * 
     * @param targetFile the target file
     * @param followLinks true if links should be followed
     * @return the resolved file
     * @throws IOException when failing to resolve the link
     * @since 16.4.0RC1
     */
    public static File resolve(File targetFile, boolean followLinks) throws IOException
    {
        // Return the target file by default
        File file = targetFile;

        while (!file.exists()) {
            // If the file does not exist, check if there is a link instead
            File linkFile = getLinkFile(file);

            if (linkFile.exists()) {
                if (followLinks) {
                    // Move the target file to the link's target file
                    file = new File(linkFile.getParent(), FileUtils.readFileToString(linkFile, StandardCharsets.UTF_8));
                } else {
                    // Stop at the link file if we don't follow it
                    file = linkFile;
                }
            } else {
                // Stop the loop since no file or link could be found
                break;
            }
        }

        return file;
    }

    /**
     * @param originalfile the location for which to create a link
     * @return the File representing the link for the passed location
     * @since 16.4.0RC1
     */
    public static File getLinkFile(File originalfile)
    {
        return new File(originalfile.getParent(),
            StoreFileUtils.getStoredFilename(originalfile.getName() + ".lnk", null));
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
