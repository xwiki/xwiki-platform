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

import org.xwiki.store.internal.FileSystemStoreUtils;

/**
 * Internal class for providing static utilities used by multiple classes in this package.
 *
 * @version $Id$
 * @since 3.0M2
 */
public final class GenericFileUtils
{
    /**
     * This must precede the version of a file. It has to be URL invalid so that it cannot collide with the name of
     * another file. Also no other key can start with ~v because the name of the version might be anything. If the
     * prefix was "~b" and a version was made called "ak" then it would collide with the
     * DefaultFilesystemStoreTools.BACKUP_FILE_SUFFIX.
     */
    private static final String FILE_VERSION_PREFIX = "~v";

    /**
     * Private constructor for utility class.
     */
    private GenericFileUtils()
    {
    }

    /**
     * Get a version of a filename. The filename is URL encoded and the version has "~v" prepended so that it cannot be
     * mistaken for part of the filename. If the filename contains one or more '.' characters then the version is
     * inserted before the last '.' character. Otherwise it is appended to the end. This means a file such as: file.txt
     * version 1.1 will become file~v1.1.txt and will still be recognized by a text editor A file with no extension such
     * as myUnknownFile version 1.1 will become myUnknownFile~v1.1 Because of URL encoding, a file named file~v1.3.txt
     * of version 1.1 will become file%7Ev1.3~1.1.txt and thus will not collide with file.txt version 1.1.
     *
     * @param filename the name of the file to save. This will be URL encoded.
     * @param versionName the name of the version of the file. This will also be URL encoded.
     * @return a string representing the filename and version which is guaranteed not to collide with any other file
     *         gotten through DefaultFilesystemStoreTools.
     */
    public static String getVersionedFilename(final String filename, final String versionName)
    {
        final String attachFilename = FileSystemStoreUtils.encode(filename, false);
        final String version = FileSystemStoreUtils.encode(versionName, false);
        if (attachFilename.contains(".")) {
            // file.txt version 1.1 --> file~v1.1.txt
            return attachFilename.substring(0, attachFilename.lastIndexOf('.')) + FILE_VERSION_PREFIX + version
                + attachFilename.substring(attachFilename.lastIndexOf('.'));
        }
        // someFile version 2.2 --> someFile~v2.2
        return attachFilename + FILE_VERSION_PREFIX + version;
    }
}
