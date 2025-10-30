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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link BlobSaveTransactionRunnable}.
 *
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
class BlobSaveTransactionRunnableTest
{
    private static final String[] FILE_PATH = { "path", "to", "file" };

    private static final String CONTENT_VERSION1 = "Version1";

    private static final String CONTENT_VERSION2 = "Version2";

    private Blob toSave;

    private Blob temp;

    private Blob backup;

    private StreamProvider provider;

    private ReadWriteLock lock;

    private BlobSaveTransactionRunnable runnable;

    private BlobStore blobStore;

    @XWikiTempDir
    private File testDirectory;

    @BeforeEach
    void beforeEach() throws Exception
    {
        File storageLocation = new File(this.testDirectory, "test-storage" + System.identityHashCode(this.getClass()));
        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(storageLocation.toPath());
        this.blobStore = spy(new FileSystemBlobStore("Test", properties));

        BlobPath blobPath = BlobPath.of(Arrays.asList(FILE_PATH));
        this.toSave = this.blobStore.getBlob(blobPath);

        this.temp = this.blobStore.getBlob(blobPath.appendSuffix("~tmp"));
        this.backup = this.blobStore.getBlob(blobPath.appendSuffix("~bak"));

        IOUtils.write(CONTENT_VERSION1, this.toSave.getOutputStream(), StandardCharsets.UTF_8);
        IOUtils.write("HAHA I am here to trip you up!", this.temp.getOutputStream(), StandardCharsets.UTF_8);
        IOUtils.write("I am also here to trip you up!", this.backup.getOutputStream(), StandardCharsets.UTF_8);

        this.lock = new ReentrantReadWriteLock();

        this.provider = () -> new ByteArrayInputStream(CONTENT_VERSION2.getBytes());

        this.runnable = new BlobSaveTransactionRunnable(this.toSave, this.temp, this.backup, this.lock, this.provider);
    }

    @Test
    void simpleTest() throws Exception
    {
        assertEquals(CONTENT_VERSION1, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));

        this.runnable.start();

        assertFalse(this.backup.exists());
        assertFalse(this.temp.exists());
        assertEquals(CONTENT_VERSION2, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));
    }

    @Test
    void rollbackAfterPreRunTest() throws Exception
    {
        assertEquals(CONTENT_VERSION1, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));

        // After preRun(), before run.
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            @Override
            public void onRun() throws Exception
            {
                assertFalse(temp.exists(), "Temp file was not cleared in preRun.");
                assertFalse(backup.exists(), "Backup file was not cleared in preRun.");

                throw new Exception("Simulate something going wrong.");
            }
        };

        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        failRunnable.runIn(str);
        runnable.runIn(str);
        this.validateRollback(str);

        assertEquals(CONTENT_VERSION1, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));
    }

    @Test
    void rollbackAfterRunTest() throws Exception
    {
        assertEquals(CONTENT_VERSION1, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));

        // After run() before onCommit()
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            @Override
            public void onRun() throws Exception
            {
                assertTrue(temp.exists(), "Content was not saved to temp file.");

                throw new Exception("Simulate something going wrong.");
            }
        };

        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        failRunnable.runIn(str);
        validateRollback(str);
    }

    @Test
    void rollbackAfterFailedCommit() throws Exception
    {
        assertEquals(CONTENT_VERSION1, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));

        // Make the temp rename fail
        doThrow(new SecurityException("Simulate failing rename")).when(this.blobStore)
            .moveBlob(this.temp.getPath(), this.toSave.getPath());

        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        validateRollback(str);
    }

    @Test
    void saveWithNonexistantOriginalTest() throws Exception
    {
        this.toSave.getStore().deleteBlob(this.toSave.getPath());
        assertFalse(this.toSave.exists());

        this.runnable.start();

        assertTrue(this.toSave.exists());
        assertEquals(CONTENT_VERSION2, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));

        assertFalse(this.temp.exists());
        assertFalse(this.backup.exists());
    }

    @Test
    void rollbackWithNonexistantOriginalTest() throws Exception
    {
        this.toSave.getStore().deleteBlob(this.toSave.getPath());
        assertFalse(this.toSave.exists());

        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            @Override
            public void onRun() throws Exception
            {
                assertFalse(backup.exists());
                assertTrue(temp.exists());
                assertFalse(toSave.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };

        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        failRunnable.runIn(str);
        Exception exception = assertThrows(Exception.class, str::start);
        assertThat(exception.getMessage(), containsString("Simulate something going wrong."));

        assertFalse(this.toSave.exists());
        assertFalse(this.temp.exists());
        assertFalse(this.backup.exists());
    }

    private void validateRollback(final StartableTransactionRunnable tr) throws Exception
    {
        assertThrows(Exception.class, tr::start,
            "TransactionRunnable#start() did not throw the exception thrown by run.");

        assertTrue(this.toSave.exists());
        assertEquals(CONTENT_VERSION1, IOUtils.toString(this.toSave.getStream(), StandardCharsets.UTF_8));
        assertFalse(this.temp.exists());
        assertFalse(this.backup.exists());
    }
}
