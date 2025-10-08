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

import javax.inject.Inject;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.store.internal.FileSystemStoreUtils;

/**
 * Specialized implementation of {@link BlobStoreManager} to consider the configured storage directory for attachments.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component
@Singleton
@Named("filesystem/" + XWikiFileSystemBlobStoreManager.NAME)
public class XWikiFileSystemBlobStoreManager implements BlobStoreManager
{
    /**
     * The name of the XWiki file system store.
     */
    public static final String NAME = "store/" + FileSystemStoreUtils.HINT;

    @Inject
    private FilesystemAttachmentsConfiguration config;

    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    @Override
    public FileSystemBlobStore getBlobStore(String name) throws BlobStoreException
    {
        File fileStorageDirectory = this.config.getDirectory();

        if (fileStorageDirectory == null) {
            // General location when filesystem based stored put data
            File storeDirectory = new File(this.environment.getPermanentDirectory(), "store");
            // Specific location for file component
            fileStorageDirectory = new File(storeDirectory, FileSystemStoreUtils.HINT);
        }

        try {
            fileStorageDirectory = fileStorageDirectory.getCanonicalFile();
        } catch (IOException e) {
            throw new BlobStoreException("Invalid permanent directory", e);
        }

        this.logger.info("Using filesystem store directory [{}]", fileStorageDirectory);

        // TODO: make this useless (by cleaning empty directories as soon as they appear)
        // TODO: this could actually be handled by the blob store itself.
        if (this.config.cleanOnStartup()) {
            final File dir = fileStorageDirectory;

            new Thread(() -> deleteEmptyDirs(dir, 0)).start();
        }

        return new FileSystemBlobStore(NAME, fileStorageDirectory.toPath());
    }

    /**
     * Delete all empty directories under the given directory. A directory which contains only empty directories is also
     * considered an empty ditectory. This function will not delete *location* unless depth is non-zero.
     *
     * @param location a directory to delete.
     * @param depth used for recursion, should always be zero.
     * @return true if the directory existed, was empty and was deleted.
     */
    private static boolean deleteEmptyDirs(final File location, int depth)
    {
        if (location != null && location.exists() && location.isDirectory()) {
            final File[] dirs = location.listFiles();
            boolean empty = true;
            for (File dir : dirs) {
                if (!deleteEmptyDirs(dir, depth + 1)) {
                    empty = false;
                }
            }

            if (empty && depth != 0) {
                location.delete();

                return true;
            }
        }

        return false;
    }
}
