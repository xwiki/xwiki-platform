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

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;

import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobStoreException;

/**
 * A TransactionRunnable for deleting a blob safely.
 * The operation can be rolled back even after the onCommit() function is called.
 * It is only final when the onComplete function is called.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
public class BlobDeleteTransactionRunnable extends StartableTransactionRunnable<TransactionRunnable<?>>
{
    /**
     * The location of the file to sdelete.
     */
    private final Blob toDelete;

    /**
     * The location of the backup file.
     */
    private final Blob backupFile;

    /**
     * A lock to hold while running this TransactionRunnable.
     */
    private final ReadWriteLock lock;

    /**
     * False until preRun() has complete. If false then we know there is nothing to rollback and
     * more importantly, we do not know if files in the temporary and backup locations are not
     * from a previous (catastrophically failed) delete or save operation.
     */
    private boolean preRunComplete;

    /**
     * The Constructor.
     *
     * @param toDelete the blob to delete.
     * @param backupFile a temporary blob, this should not contain anything important as it will be deleted
     * and must not be altered while the operation is running. This will contain whatever
     * was in the toDelete blob prior, just in case onRollback must be called.
     * @param lock a ReadWriteLock whose writeLock will be locked as the beginning of the process and
     * unlocked when complete.
     */
    public BlobDeleteTransactionRunnable(final Blob toDelete,
        final Blob backupFile,
        final ReadWriteLock lock)
    {
        this.toDelete = toDelete;
        this.backupFile = backupFile;
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Obtain the lock and make sure the temporary and backup files are deleted.
     * </p>
     *
     * @see StartableTransactionRunnable#onPreRun()
     */
    @Override
    protected void onPreRun() throws BlobStoreException, IOException
    {
        this.lock.writeLock().lock();
        this.clearBackup();
        this.preRunComplete = true;
    }

    @Override
    protected void onRun() throws BlobStoreException
    {
        if (this.toDelete.exists()) {
            this.toDelete.getStore().moveBlob(this.toDelete.getPath(), this.backupFile.getPath());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * There are a few possibilities. If preRun() has not completed then there may be an old backup from a previous
     * delete, anyway if preRun() has not completed then we know there is nothing to rollback. Otherwise:
     * </p>
     * <ol>
     * <li>There is a backup file but no main file, it has been renamed, rename it back to the main location.</li>
     * <li>There is a main file and no backup. Nothing has probably happened, do nothing to rollback.</li>
     * <li>There are neither backup nor main files, this means we tried to delete a file which didn't exist to begin
     * with.</li>
     * <li>There are both main and backup files. AAAAAaaa what do we do?! Throw an exception which will be reported.
     * </li>
     * </ol>
     *
     * @see StartableTransactionRunnable#onRollback()
     */
    @Override
    protected void onRollback() throws BlobStoreException
    {
        // If this is false then we know run() has not yet happened and we know there is nothing to do.
        if (this.preRunComplete) {
            boolean isBackupFile = this.backupFile.exists();
            boolean isMainFile = this.toDelete.exists();

            // 1.
            if (isBackupFile && !isMainFile) {
                this.backupFile.getStore().moveBlob(this.backupFile.getPath(), this.toDelete.getPath());
                return;
            }

            // 2.
            if (!isBackupFile && isMainFile) {
                return;
            }

            // 3.
            if (!isBackupFile && !isMainFile) {
                return;
            }

            // 4.
            if (isBackupFile && isMainFile) {
                throw new IllegalStateException("Tried to rollback the deletion of file "
                    + this.toDelete.getPath() + " and encountered a "
                    + "backup and a main file. Since the main file is renamed "
                    + "to a backup location before deleting, this should never "
                    + "happen.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Once this is called, there is no going back.
     * Remove backup file and unlock the lock.
     * </p>
     *
     * @see StartableTransactionRunnable#onComplete()
     */
    @Override
    protected void onComplete() throws BlobStoreException, IOException
    {
        if (!this.preRunComplete) {
            throw new IllegalStateException("Deleting file: " + this.toDelete.getPath()
                + " onPreRun has not been called, maybe the class was extended "
                + "and it was overridden?");
        }
        try {
            this.clearBackup();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Remove backup file.
     *
     * @throws IOException if removing file fails or file still exists after delete() is called.
     */
    private void clearBackup() throws BlobStoreException, IOException
    {
        if (this.backupFile.exists()) {
            this.backupFile.getStore().deleteBlob(this.backupFile.getPath());
        }
        if (this.backupFile.exists()) {
            throw new IOException("Could not remove backup file, cannot proceed.");
        }
    }

}
