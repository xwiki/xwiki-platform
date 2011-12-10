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
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A TransactionRunnable for deleting a file safely.
 * The operation can be rolled back even after the onCommit() function is called.
 * It is only final when the onComplete function is called.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class FileDeleteTransactionRunnable extends StartableTransactionRunnable<TransactionRunnable>
{
    /**
     * The location of the file to sdelete.
     */
    private final File toDelete;

    /**
     * The location of the backup file.
     */
    private final File backupFile;

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
     * @param toDelete the file to delete.
     * @param backupFile a temporary file, this should not contain anything important as it will be deleted
     * and must not be altered while the operation is running. This will contain whatever
     * was in the toDelete file prior, just in case onRollback must be called.
     * @param lock a ReadWriteLock whose writeLock will be locked as the beginning of the process and
     * unlocked when complete.
     */
    public FileDeleteTransactionRunnable(final File toDelete,
        final File backupFile,
        final ReadWriteLock lock)
    {
        this.toDelete = toDelete;
        this.backupFile = backupFile;
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     * Obtain the lock and make sure the temporary and backup files are deleted.
     *
     * @see StartableTransactionRunnable#onPreRun()
     */
    protected void onPreRun() throws IOException
    {
        this.lock.writeLock().lock();
        this.clearBackup();
        this.preRunComplete = true;
    }

    /**
     * {@inheritDoc}
     *
     * @see StartableTransactionRunnable#onRun()
     */
    protected void onRun() throws IOException
    {
        if (this.toDelete.exists()) {
            this.toDelete.renameTo(this.backupFile);
        }
    }

    /**
     * {@inheritDoc}
     * There are a few possibilities.
     * If preRun() has not completed then there may be an old backup from a previous delete, anyway
     * if preRun() has not completed then we know there is nothing to rollback.
     * Otherwise:
     * 1. There is a backup file but no main file, it has been renamed, rename it back to the main location.
     * 2. There is a main file and no backup. Nothing has probably happened, do nothing to rollback.
     * 3. There are neither backup nor main files, this means we tried to delete a file which didn't exist
     *    to begin with.
     * 4. There are both main and backup files. AAAAAaaa what do we do?! Throw an exception which will be
     * reported.
     *
     * @see StartableTransactionRunnable#onRollback()
     */
    protected void onRollback()
    {
        // If this is false then we know run() has not yet happened and we know there is nothing to do.
        if (this.preRunComplete) {
            boolean isBackupFile = this.backupFile.exists();
            boolean isMainFile = this.toDelete.exists();

            // 1.
            if (isBackupFile && !isMainFile) {
                this.backupFile.renameTo(this.toDelete);
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
                    + this.toDelete.getAbsolutePath() + " and encountered a "
                    + "backup and a main file. Since the main file is renamed "
                    + "to a backup location before deleting, this should never "
                    + "happen.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * Once this is called, there is no going back.
     * Remove backup file and unlock the lock.
     *
     * @see StartableTransactionRunnable#onComplete()
     */
    protected void onComplete() throws IOException
    {
        if (!this.preRunComplete) {
            throw new IllegalStateException("Deleting file: " + this.toDelete.getAbsolutePath()
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
    private void clearBackup() throws IOException
    {
        if (this.backupFile.exists()) {
            this.backupFile.delete();
        }
        if (this.backupFile.exists()) {
            throw new IOException("Could not remove backup file, cannot proceed.");
        }
    }
}
