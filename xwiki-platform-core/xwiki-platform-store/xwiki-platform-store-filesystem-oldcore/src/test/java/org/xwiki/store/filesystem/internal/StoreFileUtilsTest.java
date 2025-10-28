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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Validate {@link StoreFileUtils}.
 * 
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
class StoreFileUtilsTest
{
    @XWikiTempDir
    private File tempDir;

    private BlobStore blobStore;

    @BeforeEach
    void setUp()
    {
        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(this.tempDir.toPath());
        properties.setName("Test");
        properties.setType("filesystem");
        this.blobStore = new FileSystemBlobStore(properties);
    }

    @Test
    void resolveNotExistingFile() throws Exception
    {
        Blob blob = this.blobStore.getBlob(BlobPath.of(List.of("does not exist")));

        assertFalse(blob.exists());

        Blob foundBlob = StoreFileUtils.resolve(blob, true);

        assertSame(blob, foundBlob);
    }

    @Test
    void getLinkBlob() throws Exception
    {
        Blob blob = this.blobStore.getBlob(BlobPath.of(List.of("folder", "file.ext")));

        Blob linkFile = StoreFileUtils.getLinkBlob(blob);

        assertEquals("folder/file.ext.lnk", linkFile.getPath().toString());
    }
}
