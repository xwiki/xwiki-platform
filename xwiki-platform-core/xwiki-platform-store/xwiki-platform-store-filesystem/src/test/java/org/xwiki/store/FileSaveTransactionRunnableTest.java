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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for FileDeleteTransactionRunnable
 *
 * @version $Id$
 * @since 3.0M2
 */
public class FileSaveTransactionRunnableTest
{
    private static final String[] FILE_PATH = { "path", "to", "file" };

    private File storageLocation;

    private File toSave;

    private File temp;

    private File backup;

    private StreamProvider provider;

    private ReadWriteLock lock;

    private FileSaveTransactionRunnable runnable;

    @Before
    public void setUp() throws Exception
    {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        this.storageLocation = new File(tmpDir, "test-storage" + System.identityHashCode(this.getClass()));

        this.toSave = this.storageLocation;
        for (int i = 0; i < FILE_PATH.length; i++) {
            this.toSave = new File(this.toSave, FILE_PATH[i]);
        }
        this.temp = new File(this.toSave.getParentFile(), FILE_PATH[FILE_PATH.length - 1] + "~tmp");
        this.backup = new File(this.toSave.getParentFile(), FILE_PATH[FILE_PATH.length - 1] + "~bak");

        this.toSave.getParentFile().mkdirs();
        IOUtils.write("Version1", new FileOutputStream(this.toSave));
        IOUtils.write("HAHA I am here to trip you up!", new FileOutputStream(this.temp));
        IOUtils.write("I am also here to trip you up!", new FileOutputStream(this.backup));

        this.lock = new ReentrantReadWriteLock();

        this.provider = new StreamProvider()
        {
            public InputStream getStream()
            {
                return new ByteArrayInputStream("Version2".getBytes());
            }
        };

        this.runnable = new FileSaveTransactionRunnable(this.toSave,
            this.temp,
            this.backup,
            this.lock,
            this.provider);
    }

    @After
    public void tearDown() throws Exception
    {
        recursiveDelete(this.storageLocation);
    }

    @Test
    public void simpleTest() throws Exception
    {
        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version1");

        this.runnable.start();

        Assert.assertFalse(this.backup.exists());
        Assert.assertFalse(this.temp.exists());
        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version2");
    }

    @Test
    public void rollbackAfterPreRunTest() throws Exception
    {
        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version1");

        // After preRun(), before run.
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                Assert.assertFalse("Temp file was not cleared in preRun.", temp.exists());
                Assert.assertFalse("Backup file was not cleared in preRun.", backup.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        failRunnable.runIn(str);
        runnable.runIn(str);
        this.validateRollback(str);

        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version1");
    }

    @Test
    public void rollbackAfterRunTest() throws Exception
    {
        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version1");

        // After run() before onCommit()
        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                Assert.assertTrue("Content was not saved to temp file.", temp.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        final StartableTransactionRunnable str = new StartableTransactionRunnable();
        runnable.runIn(str);
        failRunnable.runIn(str);
        this.validateRollback(str);
    }

    @Test
    public void saveWithNonexistantOriginalTest() throws Exception
    {
        this.toSave.delete();
        Assert.assertFalse(this.toSave.exists());

        this.runnable.start();

        Assert.assertTrue(this.toSave.exists());
        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version2");

        Assert.assertFalse(this.temp.exists());
        Assert.assertFalse(this.backup.exists());
    }

    @Test(expected = Exception.class)
    public void rollbackWithNonexistantOriginalTest() throws Exception
    {
        this.toSave.delete();
        Assert.assertFalse(this.toSave.exists());

        final TransactionRunnable failRunnable = new TransactionRunnable()
        {
            public void onRun() throws Exception
            {
                Assert.assertFalse(backup.exists());
                Assert.assertTrue(temp.exists());
                Assert.assertFalse(toSave.exists());
                throw new Exception("Simulate something going wrong.");
            }
        };
        try {
            final StartableTransactionRunnable str = new StartableTransactionRunnable();
            runnable.runIn(str);
            failRunnable.runIn(str);
            str.start();
        } catch (Exception e) {
            Assert.assertFalse(this.toSave.exists());
            Assert.assertFalse(this.temp.exists());
            Assert.assertFalse(this.backup.exists());
            throw e;
        }
    }

    private void validateRollback(final StartableTransactionRunnable tr) throws Exception
    {
        try {
            tr.start();
            Assert.fail("TransactionRunnable#start() did not throw the exception thrown by run.");
        } catch (Exception expected) {
        }
        Assert.assertTrue(this.toSave.exists());
        Assert.assertEquals(IOUtils.toString(new FileInputStream(this.toSave)), "Version1");
        Assert.assertFalse(this.temp.exists());
        Assert.assertFalse(this.backup.exists());
    }

    private static void recursiveDelete(final File toDelete) throws Exception
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
