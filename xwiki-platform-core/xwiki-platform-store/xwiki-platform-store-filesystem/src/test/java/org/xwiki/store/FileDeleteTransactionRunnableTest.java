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

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FileDeleteTransactionRunnable
 *
 * @version $Id$
 * @since 3.0M2
 */
@ExtendWith(XWikiTempDirExtension.class)
public class FileDeleteTransactionRunnableTest
{
    private static final String[] FILE_PATH = { "path", "to", "file" };

    private File storageLocation;

    private File toDelete;

    private File temp;

    private ReadWriteLock lock;

    private FileDeleteTransactionRunnable runnable;

    @XWikiTempDir
    private File tmpDir;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.storageLocation = new File(this.tmpDir, "test-storage" + System.identityHashCode(this.getClass()));

        this.toDelete = this.storageLocation;
        for (int i = 0; i < FILE_PATH.length; i++) {
            this.toDelete = new File(this.toDelete, FILE_PATH[i]);
        }
        this.temp = new File(this.toDelete.getParentFile(), FILE_PATH[FILE_PATH.length - 1] + "~tmp");
        this.toDelete.getParentFile().mkdirs();
        IOUtils.write("Delete me!", new FileOutputStream(this.toDelete));
        IOUtils.write("HAHA I am here to trip you up!", new FileOutputStream(this.temp));

        this.lock = new ReentrantReadWriteLock();

        this.runnable = new FileDeleteTransactionRunnable(this.toDelete, this.temp, this.lock);
    }

    @AfterEach
    public void tearDown()
    {
        recursiveDelete(this.storageLocation);
    }

    @Test
    public void simpleTest() throws Exception
    {
        assertTrue(this.toDelete.exists());
        this.runnable.start();
        assertFalse(this.toDelete.exists());
        assertFalse(this.temp.exists());
    }

    @Test
    public void rollbackAfterPreRunTest()
    {
        assertTrue(this.toDelete.exists());

        // After preRun(), before run.
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                assertFalse(temp.exists());
                assertTrue(toDelete.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        failRunnable.runIn(str);
        runnable.runIn(str);
        this.validateRollback(str);
    }

    @Test
    public void rollbackAfterRunTest()
    {
        assertTrue(this.toDelete.exists());

        // After run() before onCommit()
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                assertTrue(temp.exists());
                assertFalse(toDelete.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        failRunnable.runIn(str);
        this.validateRollback(str);
    }

    @Test
    public void deleteNonexistantTest() throws Exception
    {
        this.toDelete.delete();
        assertFalse(this.toDelete.exists());
        this.runnable.start();
        assertFalse(this.toDelete.exists());
        assertFalse(this.temp.exists());
    }

    @Test
    public void rollbackDeleteNonexistantTest()
    {
        this.toDelete.delete();
        assertFalse(this.toDelete.exists());

        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                assertFalse(temp.exists());
                assertFalse(toDelete.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };

        assertThrows(Exception.class, () -> {
            try {
                final StartableTransactionRunnable str = new StartableTransactionRunnable();
                runnable.runIn(str);
                failRunnable.runIn(str);
                str.start();
            } catch (Exception e) {
                assertFalse(this.toDelete.exists());
                assertFalse(this.temp.exists());
                throw e;
            }
        });
    }

    private void validateRollback(final StartableTransactionRunnable str)
    {
        assertThrows(Exception.class, () -> {
            str.start();
        });

        assertTrue(this.toDelete.exists());
        assertFalse(this.temp.exists());
    }

    private static void recursiveDelete(final File toDelete)
    {
        if (toDelete.isDirectory()) {
            final File[] children = toDelete.listFiles();
            for (int i = 0; i < children.length; i++) {
                recursiveDelete(children[i]);
            }
        }
        toDelete.delete();
    }
}
