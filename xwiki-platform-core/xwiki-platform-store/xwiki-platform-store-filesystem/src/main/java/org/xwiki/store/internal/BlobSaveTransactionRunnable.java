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

import org.xwiki.store.BlobSerializer;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.StreamProviderBlobSerializer;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobWriteMode;

/**
 * A TransactionRunnable for saving a blob safely.
 * The operation can be rolled back even after the onCommit() function is called.
 * It is only final when the onComplete function is called.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
public class BlobSaveTransactionRunnable extends StartableTransactionRunnable<TransactionRunnable<?>>
{
    /**
     * The location of the file to save the attachment content in.
     */
    private final Blob toSave;

    /**
     * The location of the temporary file.
     */
    private final Blob tempFile;

    /**
     * The location of the backup file.
     */
    private final Blob backupFile;

    /**
     * A lock to hold while running this TransactionRunnable.
     */
    private final ReadWriteLock lock;

    /**
     * The serializer.
     */
    private final BlobSerializer serializer;

    /**
     * False until run() has complete. If false then we know there is nothing to rollback and
     * more importantly, we do not know if files in the temporary and backup locations are not
     * from a previous (catastrophically failed) save operation.
     */
    private boolean runComplete;

    /**
     * False until the move to the backup location is complete. If false, then onRollback knows that the backup file
     * does not contain anything useful.
     */
    private boolean moveToBackupComplete;

    /**
     * The Constructor.
     *
     * @param toSave the file to put the content in.
     * @param tempFile a temporary file, this should not contain anything important as it will be deleted
     * and must not be altered while the operation is running. This will contain the data
     * until onCommit when it is renamed to the toSave file.
     * @param backupFile a temporary file, this should not contain anything important as it will be deleted
     * and must not be altered while the operation is running. This will contain whatever
     * was in the toSave file prior, just in case onRollback must be called.
     * @param lock a ReadWriteLock whose writeLock will be locked as the beginning of the process and
     * unlocked when complete.
     * @param provider a StreamProvider to get the data to put into the file.
     */
    public BlobSaveTransactionRunnable(final Blob toSave,
        final Blob tempFile,
        final Blob backupFile,
        final ReadWriteLock lock,
        final StreamProvider provider)
    {
        this.toSave = toSave;
        this.tempFile = tempFile;
        this.backupFile = backupFile;
        this.lock = lock;
        this.serializer = new StreamProviderBlobSerializer(provider);
    }

    /**
     * The Constructor.
     *
     * @param toSave the blob to put the content in.
     * @param tempFile a temporary blob, this should not contain anything important as it will be deleted
     * and must not be altered while the operation is running. This will contain the data
     * until onCommit when it is renamed to the toSave blob.
     * @param backupFile a temporary blob, this should not contain anything important as it will be deleted
     * and must not be altered while the operation is running. This will contain whatever
     * was in the toSave blob prior, just in case onRollback must be called.
     * @param lock a ReadWriteLock whose writeLock will be locked at the beginning of the process and
     * unlocked when complete.
     * @param serializer a BlobSerializer in charge of serializing what needs to be serialized to the blob.
     */
    public BlobSaveTransactionRunnable(final Blob toSave,
        final Blob tempFile,
        final Blob backupFile,
        final ReadWriteLock lock,
        final BlobSerializer serializer)
    {
        this.toSave = toSave;
        this.tempFile = tempFile;
        this.backupFile = backupFile;
        this.lock = lock;
        this.serializer = serializer;
    }



    /**
     * {@inheritDoc}
     * <p>
     * Obtain the lock and make sure the temporary and backup files are deleted.
     * </p>
     *
     * @see TransactionRunnable#preRun()
     */
    @Override
    protected void onPreRun() throws IOException, BlobStoreException
    {
        this.lock.writeLock().lock();
        this.clearTempAndBackup();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write the data from the provider to the temporary file.
     * </p>
     *
     * @see TransactionRunnable#run()
     */
    @Override
    protected void onRun() throws Exception
    {
        try {
            this.serializer.serialize(this.tempFile);
        } finally {
            this.runComplete = true;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Move whatever is in the main file location into backup and move
     * the temp file into the main location.
     * </p>
     *
     * @see TransactionRunnable#onCommit()
     */
    @Override
    protected void onCommit() throws BlobStoreException
    {
        if (this.toSave.exists()) {
            this.toSave.getStore().moveBlob(this.toSave.getPath(), this.backupFile.getPath());
            this.moveToBackupComplete = true;
        }
        this.tempFile.getStore().moveBlob(this.tempFile.getPath(), this.toSave.getPath());
    }

    @Override
    protected void onRollback() throws BlobStoreException
    {
        // If this is false then we know run() has not yet happened and we know there is nothing to do.
        if (this.runComplete) {
            if (this.tempFile.exists()) {
                this.onRollbackWithTempFile();
            } else {
                this.onRollbackNoTempFile();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Once this is called, there is no going back.
     * Remove temporary and backup files and unlock the lock.
     * </p>
     *
     * @see TransactionRunnable#onComplete()
     */
    @Override
    protected void onComplete() throws IOException, BlobStoreException
    {
        try {
            this.clearTempAndBackup();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Knowing there is no temp file, we can determine that one of 4 possible things happened.
     *
     * 1. No backup file and no main file. There was probably no file to begin with
     * and it failed before anything could be saved in the temp file. Do nothing.
     *
     * 2. No backup file but there is a main file, assume onCommit happened successfully but there
     * was no file here to begin with so there was nothing to back up. Move the main file back
     * to the temporary location.
     *
     * 3. If there is a backup file, but no main file, this is unexpected but since the backup file
     * should be the previous main file, move it back to the main location and log a warning
     * that the storage engine encountered an unexpected albeit probably recoverable state.
     *
     * 4. If there is a backup file and a main file, onCommit probably went smoothly and a problem
     * was encountered somewhere else forcing the rollback. Move the main file back to the
     * temporary location and the backup file back to the main location.
     */
    private void onRollbackNoTempFile() throws BlobStoreException
    {
        boolean isBackupFile = this.backupFile.exists();
        boolean isMainFile = this.toSave.exists();

        // 1.
        if (!isBackupFile && !isMainFile) {
            return;
        }

        // 2.
        if (!isBackupFile) {
            this.toSave.getStore().moveBlob(this.toSave.getPath(), this.tempFile.getPath());
            return;
        }

        // 3.
        if (!isMainFile) {
            this.backupFile.getStore().moveBlob(this.backupFile.getPath(), this.toSave.getPath());
            // TODO log a low severity warning.
            return;
        }

        // 4.
        this.toSave.getStore().moveBlob(this.toSave.getPath(), this.tempFile.getPath());
        this.backupFile.getStore().moveBlob(this.backupFile.getPath(), this.toSave.getPath());
    }

    /**
     * Knowing there is a temp file, one of 3 things might have happened:
     *
     * 1. If there is no backup file, assume onCommit did not occur, do nothing regardless
     * of whether there is or isn't an (existing) main file.
     *
     * 2. If there is a backup file but no main file, there must have been a failure half way
     * through onCommit, it was able to move the existing main file to the backup
     * location but did not move the temporary file to the main location. Move the backup file
     * back to the main location.
     *
     * 3. If there is a file in each location, we probably used copy + delete instead of rename on a blob store which
     * does not support rename. In this case, there are two scenarios: we copied the existing main file to the backup
     * location but failed to delete the original before moving the temporary file to the main location, or we started
     * moving the temporary file to the main location but failed before deleting the original temporary file. We can
     * check which scenario happened by checking the moveToBackupComplete flag. If it is true, then we know we moved
     * the main file to the backup location and the main file is the new temporary file content, so we move the backup
     * file back to the main location. If it is false, then we know we failed during the move of the main file to the
     * backup location, so we can just delete the backup file as the main file is still the original content.
     */
    private void onRollbackWithTempFile() throws BlobStoreException
    {
        boolean isBackupFile = this.backupFile.exists();
        boolean isMainFile = this.toSave.exists();

        // 1.
        if (!isBackupFile) {
            return;
        }

        // 2.
        if (!isMainFile) {
            this.backupFile.getStore().moveBlob(this.backupFile.getPath(), this.toSave.getPath());
            return;
        }

        // 3.
        if (this.moveToBackupComplete) {
            // We successfully moved the main file to backup, so restore it.
            this.backupFile.getStore().moveBlob(this.backupFile.getPath(), this.toSave.getPath(),
                BlobWriteMode.REPLACE_EXISTING);
        } else {
            // We failed to move the main file to backup, so just delete the backup file.
            this.backupFile.getStore().deleteBlob(this.backupFile.getPath());
        }
    }

    /**
     * Remove temporary and backup files.
     *
     * @throws IOException if removing files fails or files still exist after delete() is called.
     */
    private void clearTempAndBackup() throws IOException, BlobStoreException
    {
        if (this.tempFile.exists()) {
            this.tempFile.getStore().deleteBlob(this.tempFile.getPath());
        }
        if (this.tempFile.exists()) {
            throw new IOException("Could not remove temporary file, cannot proceed.");
        }
        if (this.backupFile.exists()) {
            this.backupFile.getStore().deleteBlob(this.backupFile.getPath());
        }
        if (this.backupFile.exists()) {
            throw new IOException("Could not remove backup file, cannot proceed.");
        }
    }

}
