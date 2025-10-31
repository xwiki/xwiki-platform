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
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.store.blob.BlobStorePropertiesBuilder;
import org.xwiki.store.blob.BlobStorePropertiesCustomizer;
import org.xwiki.store.internal.FileSystemStoreUtils;

/**
 * Customizes filesystem blob store profiles using legacy XWiki filesystem attachment config.
 * Applies only when the profile type is "filesystem" and store name equals the store/file name.
 * Cleans up empty directories on startup if configured to do so.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component
@Singleton
@Named("org.xwiki.store.filesystem.internal.XWikiFilesystemBlobStorePropertiesCustomizer")
public class XWikiFilesystemBlobStorePropertiesCustomizer implements BlobStorePropertiesCustomizer
{
    @Inject
    private FilesystemAttachmentsConfiguration config;

    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    @Override
    public void customize(BlobStorePropertiesBuilder propertiesBuilder)
    {
        // Only adjust for filesystem stores
        if (!"filesystem".equals(propertiesBuilder.getHint())) {
            return;
        }

        // If the name matches the attachments store, map configured directory.
        if (("store/" + FileSystemStoreUtils.HINT).equals(propertiesBuilder.getName())) {
            File fileStorageDirectory = this.config.getDirectory();

            if (fileStorageDirectory == null) {
                File storeDirectory = new File(this.environment.getPermanentDirectory(), "store");
                fileStorageDirectory = new File(storeDirectory, FileSystemStoreUtils.HINT);
            }

            Path path = fileStorageDirectory.toPath();
            this.logger.debug("Customizing filesystem store root directory to [{}] for attachments store", path);
            // Constant copied from FileSystemBlobStoreProperties to avoid a direct dependency.
            propertiesBuilder.set("filesystem.rootDirectory", path);

            if (this.config.cleanOnStartup()) {
                File finalFileStorageDirectory = fileStorageDirectory;
                new Thread(() -> deleteEmptyDirs(finalFileStorageDirectory, 0)).start();
            }
        }
    }

    private static boolean deleteEmptyDirs(final File location, int depth)
    {
        if (location != null && location.exists() && location.isDirectory()) {
            final File[] files = location.listFiles();
            boolean empty = true;
            if (files != null) {
                for (File file : files) {
                    if (!deleteEmptyDirs(file, depth + 1)) {
                        empty = false;
                    }
                }
            }

            if (empty && depth != 0) {
                try {
                    Files.delete(location.toPath());
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }

        return false;
    }
}
