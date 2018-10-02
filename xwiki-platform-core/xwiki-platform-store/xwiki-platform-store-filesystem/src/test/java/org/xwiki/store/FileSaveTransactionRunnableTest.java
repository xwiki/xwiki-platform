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
package org.xwiki.store;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FileDeleteTransactionRunnable
 *
 * @version $Id$
 * @since 3.0M2
 */
public class FileSaveTransactionRunnableTest
{
    private static final String[] FILE_PATH = { "path", "to", "file" };

    private static final String CONTENT_VERSION1 = "Version1";

    private static final String CONTENT_VERSION2 = "Version2";

    private File toSave;

    private File temp;

    private File backup;

    private StreamProvider provider;

    private ReadWriteLock lock;

    private FileSaveTransactionRunnable runnable;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        File testDirectory = new File("target/test-" + new Date().getTime()).getAbsoluteFile();
        File storageLocation = new File(testDirectory, "test-storage" + System.identityHashCode(this.getClass()));

        this.toSave = storageLocation;
        for (int i = 0; i < FILE_PATH.length; i++) {
            this.toSave = new File(this.toSave, FILE_PATH[i]);
        }
        this.temp = new File(this.toSave.getParentFile(), FILE_PATH[FILE_PATH.length - 1] + "~tmp");
        this.backup = new File(this.toSave.getParentFile(), FILE_PATH[FILE_PATH.length - 1] + "~bak");

        this.toSave.getParentFile().mkdirs();
        FileUtils.write(this.toSave, CONTENT_VERSION1, StandardCharsets.UTF_8);
        FileUtils.write(this.temp, "HAHA I am here to trip you up!", StandardCharsets.UTF_8);
        FileUtils.write(this.backup, "I am also here to trip you up!", StandardCharsets.UTF_8);

        this.lock = new ReentrantReadWriteLock();

        this.provider = () -> new ByteArrayInputStream(CONTENT_VERSION2.getBytes());

        this.runnable = new FileSaveTransactionRunnable(this.toSave, this.temp, this.backup, this.lock, this.provider);
    }

    @Test
    public void simpleTest() throws Exception
    {
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION1);

        this.runnable.start();

        assertFalse(this.backup.exists());
        assertFalse(this.temp.exists());
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION2);
    }

    @Test
    public void rollbackAfterPreRunTest() throws Exception
    {
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION1);

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

        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION1);
    }

    @Test
    public void rollbackAfterRunTest() throws Exception
    {
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION1);

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
    public void rollbackAfterFailedCommit() throws Exception
    {
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION1);

        // Make the temp rename fail
        this.temp = new File(this.temp.getPath())
        {
            @Override
            public boolean renameTo(File dest)
            {
                throw new SecurityException("Simulate failing rename");
            }
        };
        this.runnable = new FileSaveTransactionRunnable(this.toSave, this.temp, this.backup, this.lock, this.provider);

        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        validateRollback(str);
    }

    @Test
    public void saveWithNonexistantOriginalTest() throws Exception
    {
        this.toSave.delete();
        assertFalse(this.toSave.exists());

        this.runnable.start();

        assertTrue(this.toSave.exists());
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION2);

        assertFalse(this.temp.exists());
        assertFalse(this.backup.exists());
    }

    @Test
    public void rollbackWithNonexistantOriginalTest() throws Exception
    {
        this.toSave.delete();
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
        Exception exception = assertThrows(Exception.class, () -> str.start());

        assertFalse(this.toSave.exists());
        assertFalse(this.temp.exists());
        assertFalse(this.backup.exists());
    }

    private void validateRollback(final StartableTransactionRunnable tr) throws Exception
    {
        assertThrows(Exception.class, () -> tr.start(),
            "TransactionRunnable#start() did not throw the exception thrown by run.");

        assertTrue(this.toSave.exists());
        assertEquals(FileUtils.readFileToString(this.toSave, StandardCharsets.UTF_8), CONTENT_VERSION1);
        assertFalse(this.temp.exists());
        assertFalse(this.backup.exists());
    }
}
