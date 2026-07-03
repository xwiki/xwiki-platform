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
package org.xwiki.store.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BlobDeleteTransactionRunnable}.
 *
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
class BlobDeleteTransactionRunnableTest
{
    private static final BlobPath FILE_PATH = BlobPath.absolute("path", "to", "file");

    private Blob location;

    private Blob temp;

    private ReadWriteLock lock;

    private BlobDeleteTransactionRunnable runnable;

    @XWikiTempDir
    private File tmpDir;

    @BeforeEach
    void setUp() throws Exception
    {
        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(this.tmpDir.toPath());
        BlobStore blobStore = new FileSystemBlobStore("Test", properties);

        String tmpName = Objects.requireNonNull(FILE_PATH.getFileName()) + "~tmp";

        this.location = blobStore.getBlob(FILE_PATH);
        this.temp = blobStore.getBlob(FILE_PATH.resolveSibling(tmpName));
        IOUtils.write("Delete me!", this.location.getOutputStream(), StandardCharsets.UTF_8);
        IOUtils.write("HAHA I am here to trip you up!", this.temp.getOutputStream(), StandardCharsets.UTF_8);

        this.lock = new ReentrantReadWriteLock();

        this.runnable = new BlobDeleteTransactionRunnable(this.location, this.temp, this.lock);
    }

    @Test
    void simpleTest() throws Exception
    {
        assertTrue(this.location.exists());
        this.runnable.start();
        assertFalse(this.location.exists());
        assertFalse(this.temp.exists());
    }

    @Test
    void rollbackAfterPreRunTest() throws BlobStoreException
    {
        assertTrue(this.location.exists());

        // After preRun(), before run.
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                assertFalse(temp.exists());
                assertTrue(location.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        failRunnable.runIn(str);
        runnable.runIn(str);
        this.validateRollback(str);
    }

    @Test
    void rollbackAfterRunTest() throws BlobStoreException
    {
        assertTrue(this.location.exists());

        // After run() before onCommit()
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                assertTrue(temp.exists());
                assertFalse(location.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        failRunnable.runIn(str);
        this.validateRollback(str);
    }

    @Test
    void deleteNonexistantTest() throws Exception
    {
        this.location.getStore().deleteBlob(this.location.getPath());
        assertFalse(this.location.exists());
        this.runnable.start();
        assertFalse(this.location.exists());
        assertFalse(this.temp.exists());
    }

    @Test
    void rollbackDeleteNonexistantTest() throws BlobStoreException
    {
        this.location.getStore().deleteBlob(this.location.getPath());
        assertFalse(this.location.exists());

        final TransactionRunnable<TransactionRunnable<?>> failRunnable = new TransactionRunnable<>()
        {
            @Override
            public void onRun() throws Exception
            {
                assertFalse(BlobDeleteTransactionRunnableTest.this.temp.exists());
                assertFalse(BlobDeleteTransactionRunnableTest.this.location.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };

        final StartableTransactionRunnable<TransactionRunnable<?>> str = new StartableTransactionRunnable<>();
        this.runnable.runIn(str);
        failRunnable.runIn(str);
        assertThrows(Exception.class, str::start);
        assertFalse(this.location.exists());
        assertFalse(this.temp.exists());
    }

    private void validateRollback(final StartableTransactionRunnable<?> str) throws BlobStoreException
    {
        assertThrows(Exception.class, str::start);

        assertTrue(this.location.exists());
        assertFalse(this.temp.exists());
    }
}
