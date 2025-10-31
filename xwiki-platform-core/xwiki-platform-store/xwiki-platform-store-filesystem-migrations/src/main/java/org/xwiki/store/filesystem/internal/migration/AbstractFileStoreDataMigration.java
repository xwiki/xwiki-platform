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

package org.xwiki.store.filesystem.internal.migration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Base class used by migrations on file store.
 *
 * @version $Id$
 * @since 11.0
 */
public abstract class AbstractFileStoreDataMigration extends AbstractHibernateDataMigration implements Initializable
{
    /**
     * The name of the directory where document information is stored. This must have a URL illegal character in it,
     * otherwise it will be confused if/when nested spaces are implemented.
     */
    protected static final String THIS_DIR_NAME = "~this";

    /**
     * The folder name of {@link Locale#ROOT}.
     */
    protected static final String DOCUMENT_LOCALE_ROOT_NAME = "~";

    @Inject
    protected Environment environment;

    @Inject
    protected FilesystemStoreTools fstools;

    @Inject
    protected Logger logger;

    protected FileSystemBlobStore pre11BlobStore;

    protected File pre11StoreRootDirectory;

    protected BlobStore blobStore;

    @Override
    public void initialize() throws InitializationException
    {
        String pre11StoreName = "storage";
        this.pre11StoreRootDirectory = new File(this.environment.getPermanentDirectory(), pre11StoreName);

        // TODO: not sure if this is the best way to get the pre-11 blob store. Maybe use the factory instead?
        FileSystemBlobStoreProperties pre11Properties = new FileSystemBlobStoreProperties();
        pre11Properties.setRootDirectory(this.pre11StoreRootDirectory.toPath());
        this.pre11BlobStore = new FileSystemBlobStore(pre11StoreName, pre11Properties);

        if (getVersion().getVersion() < R1100000XWIKI15620DataMigration.VERSION) {
            this.blobStore = this.pre11BlobStore;
        } else {
            this.blobStore = this.fstools.getStore();
        }
    }

    protected File getPre11StoreRootDirectory()
    {
        return this.pre11StoreRootDirectory;
    }

    protected File getStoreRootDirectory()
    {
        if (this.blobStore instanceof FileSystemBlobStore fileSystemBlobStore) {
            return fileSystemBlobStore.getBlobFilePath(BlobPath.of(List.of())).toFile();
        } else {
            throw new IllegalStateException("The current blob store is not a file system blob store");
        }
    }

    protected BlobStore getBlobStore()
    {
        return this.blobStore;
    }

    protected void setBlobStore(BlobStore blobStore)
    {
        this.blobStore = blobStore;
    }

    protected File getPre11WikiDir(String wikiId)
    {
        return new File(getPre11StoreRootDirectory(), wikiId);
    }

    private EntityReference getPre11EntityReference(File directory) throws IOException
    {
        String name = FileSystemStoreUtils.decode(directory.getName());

        File parent = directory.getParentFile();

        if (parent.getCanonicalPath().equals(getStoreRootDirectory().getCanonicalPath())) {
            return new WikiReference(name);
        } else {
            return new SpaceReference(name, getPre11EntityReference(parent));
        }
    }

    protected DocumentReference getPre11DocumentReference(File directory) throws DataMigrationException
    {
        try {
            String name = FileSystemStoreUtils.decode(directory.getName());

            return new DocumentReference(name, (SpaceReference) getPre11EntityReference(directory.getParentFile()));
        } catch (Exception e) {
            throw new DataMigrationException("Failed to get document reference for filesystem path [" + directory
                + "] (root=" + getStoreRootDirectory() + ")", e);
        }
    }

    protected void moveFolderContent(File sourceFolder, File toFolder) throws DataMigrationException
    {
        this.logger.info("Moving content of folder [{}] to new location [{}]", sourceFolder, toFolder);

        for (File child : sourceFolder.listFiles()) {
            try {
                FileUtils.moveToDirectory(child, toFolder, true);
            } catch (IOException e) {
                throw new DataMigrationException(
                    "Failed to move content of folder [" + sourceFolder + "] the new location [" + toFolder + "]", e);
            }
        }
    }
}
