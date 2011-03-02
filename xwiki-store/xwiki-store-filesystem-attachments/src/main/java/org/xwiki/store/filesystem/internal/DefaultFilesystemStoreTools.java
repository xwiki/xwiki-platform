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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.HashMap;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.WeakHashMap;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;

/**
 * Default tools for getting files to store data in the filesystem.
 * This should be replaced by a module which provides a secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
public class DefaultFilesystemStoreTools implements FilesystemStoreTools, Initializable
{
    /** The name of the directory in the work directory where the hirearchy will be stored. */
    private static final String STORAGE_DIR_NAME = "storage";

    /**
     * The name of the directory where document information is stored.
     * This must have a URL illegal character in it,
     * otherwise it will be confused if/when nested spaces are implemented.
     */
    private static final String DOCUMENT_DIR_NAME = "~this";

    /** The directory within each document's directory where the document's attachments are stored. */
    private static final String ATTACHMENT_DIR_NAME = "attachments";

    /** The directory within each document's directory for attachments which have been deleted. */
    private static final String DELETED_ATTACHMENT_DIR_NAME = "deleted-attachments";

    /**
     * The part of the deleted attachment directory name after this is the date of deletion,
     * The part before this is the URL encoded attachment filename.
     */
    private static final String DELETED_ATTACHMENT_NAME_SEPARATOR = "-";

    /**
     * When a file is being saved, the original will be moved to the same name with this after it.
     * If the save operation fails then this file will be moved back to the regular position to come as
     * close as possible to ACID transaction handling.
     */
    private static final String BACKUP_FILE_SUFFIX = "~bak";

    /**
     * When a file is being deleted, it will be renamed with this at the end of the filename in the
     * transaction. If the transaction succeeds then the temp file will be deleted, if it fails then the
     * temp file will be renamed back to the original filename.
     */
    private static final String TEMP_FILE_SUFFIX = "~tmp";

    /** Serializer used for obtaining a safe file path from a document reference. */
    @Requirement("path")
    private EntityReferenceSerializer<String> pathSerializer;

    /**
     * We need to get the XWiki object in order to get the work directory.
     */
    @Requirement
    private Execution exec;

    /** This is the directory where all of the attachments will stored. */
    private File storageDir;

    /** A map which holds locks by the file path so that the same lock is used for the same file. */
    private final Map<String, WeakReference<ReadWriteLock>> fileLockMap =
        new WeakHashMap<String, WeakReference<ReadWriteLock>>();

    /** Used by DeadlockBreakingLock. */
    private final Map<Thread, Set<DeadlockBreakingLock>> locksHeldByThread =
        new HashMap<Thread, Set<DeadlockBreakingLock>>();

    /** Used by DeadlockBreakingLock. */
    private final Map<Thread, DeadlockBreakingLock> lockBlockingThread =
        new HashMap<Thread, DeadlockBreakingLock>();

    /**
     * Testing Constructor.
     *
     * @param pathSerializer an EntityReferenceSerializer for generating file paths.
     * @param storageDir the directory to store the content in.
     */
    public DefaultFilesystemStoreTools(final EntityReferenceSerializer<String> pathSerializer,
                                       final File storageDir)
    {
        this.pathSerializer = pathSerializer;
        this.storageDir = storageDir;
    }

    /** Constructor for component manager. */
    public DefaultFilesystemStoreTools()
    {
    }

    /** {@inheritDoc} */
    public void initialize()
    {
        final XWikiContext context = ((XWikiContext) this.exec.getContext().getProperty("xwikicontext"));
        final File workDir = context.getWiki().getWorkDirectory(context);
        this.storageDir = new File(workDir, STORAGE_DIR_NAME);

        deleteEmptyDirs(this.storageDir);
    }

    /**
     * Delete all empty directories under the given directory.
     * A directory which contains only empty directories is also considered an empty ditectory.
     *
     * @param location a directory to delete.
     * @return true if the directory existed, was empty and was deleted.
     */
    private static boolean deleteEmptyDirs(final File location)
    {
        if (location != null && location.exists() && location.isDirectory()) {
            final File[] dirs = location.listFiles();
            boolean empty = true;
            for (int i = 0; i < dirs.length; i++) {
                if (!deleteEmptyDirs(dirs[i])) {
                    empty = false;
                }
            }
            if (empty) {
                location.delete();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getBackupFile(File)
     */
    public File getBackupFile(final File storageFile)
    {
        return new File(storageFile.getAbsolutePath() + BACKUP_FILE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getTempFile(File)
     */
    public File getTempFile(final File storageFile)
    {
        return new File(storageFile.getAbsolutePath() + TEMP_FILE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getDeletedAttachmentFileProvider(XWikiAttachment, Date)
     */
    public DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(final XWikiAttachment attachment,
                                                                          final Date deleteDate)
    {
        return new DefaultDeletedAttachmentFileProvider(
            this.getDeletedAttachmentDir(attachment, deleteDate), attachment.getFilename());
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getDeletedAttachmentFileProvider(String)
     */
    public DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(final String pathToDirectory)
    {
        final File attachDir = new File(this.storageDir, this.getStorageLocationPath());
        return new DefaultDeletedAttachmentFileProvider(
            attachDir, getFilenameFromDeletedAttachmentDirectory(attachDir));
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#deletedAttachmentsForDocument(DocumentReference)
     */
    public Map<String, Map<Date, DeletedAttachmentFileProvider>>
    deletedAttachmentsForDocument(final DocumentReference docRef)
    {
        final File docDir = getDocumentDir(docRef, this.storageDir, this.pathSerializer);
        final File deletedAttachmentsDir = new File(docDir, DELETED_ATTACHMENT_DIR_NAME);
        final Map<String, Map<Date, DeletedAttachmentFileProvider>> out =
            new HashMap<String, Map<Date, DeletedAttachmentFileProvider>>();

        if (!deletedAttachmentsDir.exists()) {
            return out;
        }

        for (File file : Arrays.asList(deletedAttachmentsDir.listFiles())) {
            final String currentName = getFilenameFromDeletedAttachmentDirectory(file);
            if (out.get(currentName) == null) {
                out.put(currentName, new HashMap<Date, DeletedAttachmentFileProvider>());
            }
            out.get(currentName).put(getDeleteDateFromDeletedAttachmentDirectory(file),
                                     new DefaultDeletedAttachmentFileProvider(file,
                                         getFilenameFromDeletedAttachmentDirectory(file)));
        }
        return out;
    }

    /**
     * @param directory the location of the data for the deleted attachment.
     * @return the name of the attachment file as extracted from the directory name.
     */
    private static String getFilenameFromDeletedAttachmentDirectory(final File directory)
    {
        final String name = directory.getName();
        final String encodedOut = name.substring(0, name.lastIndexOf(DELETED_ATTACHMENT_NAME_SEPARATOR));
        return GenericFileUtils.getURLDecoded(encodedOut);
    }

    /**
     * @param directory the location of the data for the deleted attachment.
     * @return the deletion date as extracted from the directory name.
     */
    private static Date getDeleteDateFromDeletedAttachmentDirectory(final File directory)
    {
        final String name = directory.getName();
        // no need to url decode this since it should only contain numbers 0-9.
        long time = Long.parseLong(name.substring(name.lastIndexOf(DELETED_ATTACHMENT_NAME_SEPARATOR) + 1));
        return new Date(time);
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getStorageLocationPath()
     */
    public String getStorageLocationPath()
    {
        return this.storageDir.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getGlobalFile(String)
     */
    public File getGlobalFile(final String name)
    {
        return new File(this.storageDir, "~GLOBAL_" + GenericFileUtils.getURLEncoded(name));
    }

    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getAttachmentFileProvider(XWikiAttachment)
     */
    public AttachmentFileProvider getAttachmentFileProvider(final XWikiAttachment attachment)
    {
        return new DefaultAttachmentFileProvider(this.getAttachmentDir(attachment),
                                                 attachment.getFilename());
    }

    /**
     * Get the directory for storing files for an attachment.
     * This will look like storage/xwiki/Main/WebHome/~this/attachments/file.name/
     *
     * @param attachment the attachment to get the directory for.
     * @return a File representing the directory. Note: The directory may not exist.
     */
    private File getAttachmentDir(final XWikiAttachment attachment)
    {
        final XWikiDocument doc = attachment.getDoc();
        if (doc == null) {
            throw new NullPointerException("Could not store attachment because it is not "
                                           + "associated with a document.");
        }
        final File docDir = getDocumentDir(doc.getDocumentReference(),
                                           this.storageDir,
                                           this.pathSerializer);
        final File attachmentsDir = new File(docDir, ATTACHMENT_DIR_NAME);
        return new File(attachmentsDir, GenericFileUtils.getURLEncoded(attachment.getFilename()));
    }

    /**
     * Get a directory for storing the contentes of a deleted attachment.
     * The format is <document name>/~this/deleted-attachments/<attachment name>-<delete date>/
     * <delete date> is expressed in "unix time" so it might look like:
     * WebHome/~this/deleted-attachments/file.txt-0123456789/
     *
     * @param attachment the attachment to get the file for.
     * @param deleteDate the date the attachment was deleted.
     * @return a directory which will be repeatable only with the same inputs.
     */
    private File getDeletedAttachmentDir(final XWikiAttachment attachment,
                                         final Date deleteDate)
    {
        final XWikiDocument doc = attachment.getDoc();
        if (doc == null) {
            throw new NullPointerException("Could not store deleted attachment because "
                                           + "it is not attached to any document.");
        }
        final File docDir = getDocumentDir(doc.getDocumentReference(),
                                           this.storageDir,
                                           this.pathSerializer);
        final File deletedAttachmentsDir = new File(docDir, DELETED_ATTACHMENT_DIR_NAME);
        final String fileName =
            attachment.getFilename() + DELETED_ATTACHMENT_NAME_SEPARATOR + deleteDate.getTime();
        return new File(deletedAttachmentsDir, GenericFileUtils.getURLEncoded(fileName));
    }

    /**
     * Get the directory associated with this document.
     * This is a path obtained from the owner document reference, where each reference segment
     * (wiki, spaces, document name) contributes to the final path.
     * For a document called xwiki:Main.WebHome, the directory will be:
     * <code>(storageDir)/xwiki/Main/WebHome/~this/</code>
     *
     * @param docRef the DocumentReference for the document to get the directory for.
     * @param storageDir the directory to place the directory hirearcy for attachments in.
     * @param pathSerializer an EntityReferenceSerializer which will make a directory path from an
     *                       an EntityReference.
     * @return a file path corresponding to the attachment location; each segment in the path is
     *         URL-encoded in order to be safe.
     */
    private static File getDocumentDir(final DocumentReference docRef,
                                       final File storageDir,
                                       final EntityReferenceSerializer<String> pathSerializer)
    {
        final File path = new File(storageDir, pathSerializer.serialize(docRef));
        return new File(path, DOCUMENT_DIR_NAME);
    }


    /**
     * {@inheritDoc}
     *
     * @see FilesystemStoreTools#getLockForFile(File)
     */
    public synchronized ReadWriteLock getLockForFile(final File toLock)
    {
        final String path = toLock.getAbsolutePath();
        WeakReference<ReadWriteLock> lock = this.fileLockMap.get(path);
        ReadWriteLock strongLock = null;
        if (lock != null) {
            strongLock = lock.get();
        }
        if (strongLock == null) {
            strongLock = new DeadlockBreakingReadWriteLock(this.locksHeldByThread, this.lockBlockingThread)
            {
                /**
                 * A strong reference on the string to make sure that the
                 * mere existence of the lock will keep it in the map.
                 */
                private final String lockMapReference = path;
            };
            this.fileLockMap.put(path, new WeakReference<ReadWriteLock>(strongLock));
        }
        return strongLock;
    }


    /**
     * This ReadWriteLock implementation is currently naive and uses only one lock for both.
     */
    private static class DeadlockBreakingReadWriteLock implements ReadWriteLock
    {
        /** The only lock in this readWriteLock. */
        private final Lock onlyLock;

        /**
         * The Constructor.
         *
         * @param locksHeldByThread a map which is used internally to detect and break deadlock
         *        conditions, the same map must be passed to all new locks if there is a possibility of
         *        them deadlocking and the map must not be interfered with externally.
         * @param lockBlockingThread another map used internally to detect and break deadlock.
         *        the same map must be passed to all new locks if there is a possibility of
         *        them deadlocking and the map must not be interfered with externally.
         */
        public DeadlockBreakingReadWriteLock(final Map<Thread, Set<DeadlockBreakingLock>> locksHeldByThread,
                                             final Map<Thread, DeadlockBreakingLock> lockBlockingThread)
        {
            this.onlyLock = new DeadlockBreakingLock(locksHeldByThread, lockBlockingThread);
        }

        /**
         * {@inheritDoc}
         *
         * @see java.util.concurrent.locks.ReadWriteLock#readLock()
         */
        public Lock readLock()
        {
            return this.onlyLock;
        }

        /**
         * {@inheritDoc}
         *
         * @see java.util.concurrent.locks.ReadWriteLock#writeLock()
         */
        public Lock writeLock()
        {
            return this.onlyLock;
        }
    }

    /**
     * Every deadlock situation can be boiled down to Alice and Bob grabbing X and Y respectively
     * then reaching for the other. There may be N threads in between but if Alice blocks out Charley
     * who blocks Dave how blocks Elanor who blocks Bob who blocks Alice, the underlying situation
     * is the same; Alice and Bob are blocking each other.
     * If Bob is waiting for a lock held by Alice then there is no reason why Alice cannot proceed without
     * the lock Bob holds because we can be assured that Bob will not move until Alice has released the
     * lock which he is waiting on.
     *
     * DeadlockBreakingLock walks around the ring of blocked lockholders to determine if there is a
     * deadlock situation and if there is, it does 2 things:
     * 1. It lies to Alice, telling her she has acquired lock X which in fact belongs to Bob.
     * 2. It alters lock Y (the one which is blocking Bob out) by making Bob unable to acquire it unless he
     *    none of the locks which he owns have been forced.
     *
     * Multiple deadlock situations:
     * If Alice acquires X, Bob acquires Y, Alice reaches for Y and Bob reaches for X. The algorithm will
     * happily give Alice a pass to fake acquire Y and then it will make sure that X is not available to
     * Bob until Y is released. Now imagine Charley has acquired Z which Alice now needs and
     * as Alice begins waiting on Z, Charley reaches for Y. If he reaches for X then it is a simple
     * situation of 2 separate deadlocks but when he reaches for Y, a single lock is forced to break 2
     * deadlock situations. We could always prefer Alice because she has already forced the lock once but
     * suppose Charley had to force Z when he acquired it. One way or another, a lock is going to have to
     * be forced multiple times. To add to the confusion, Charley may also want X. It is okay for him to
     * take X but when he releases it, Bob may not acquire it but Alice may acquire Z.
     *
     * This operation can be expressed graphically:
     * A running Thread is represented by <code>----</code>
     * A blocked thread is represented by whitespace,
     * A successful lock is represented by a capital letter.
     * An attempted lock which blocks the thread is represented by a lowercase letter.
     * An unlock operation is represented by a capital letter in parentheses.
     * <code>
     * Alice ----X--Y--z                Z----(Y)-(X)----(Z)---
     *
     * Bob ------Y--x                             X-----(X)---
     *
     * Charley --Z-----Y---X---(Z)-(Y)-(X)--------------------
     *
     * Event:    1  2  3   4    5   6   7     8   9      10
     * </code>
     *
     * This implementation currently uses a naive method of simply checking that a thread has not had any
     * of it's locks forced and then puts threads into a wait cycle when trying to acquire a lock and
     * notifies all waiters on unlock and lets them decide who is qualified to take over the lock.
     */
    private static class DeadlockBreakingLock implements Lock
    {
        /** Exception to throw when a function is called which has not been written. */
        private static final String NOT_IMPLEMENTED = "Function not implemented.";

        /**
         * A set of all locks held by each lock-holding thread.
         * Used so that we can make sure a thread owns all of the locks which it acquired, otherwise
         * it may not acquire any more, specifically the one which it is blocked on.
         */
        private final Map<Thread, Set<DeadlockBreakingLock>> locksHeldByThread;

        /**
         * Find out which lock a given thread is waiting on.
         * This is needed in order to find the loop causing deadlock using an algorithm like:
         * What are you waiting for, who owns that, what is he waiting for...
         */
        private final Map<Thread, DeadlockBreakingLock> lockBlockingThread;

        /**
         * The owner of this lock, if the owner calls multiple times then it is added again.
         * If another thread ceases the lock then it is added.
         */
        private Stack<Thread> owners = new Stack<Thread>();

        /**
         * The Constructor.
         *
         * @param locksHeldByThread a map which is used internally to detect and break deadlock
         *        conditions, the same map must be passed to all new locks if there is a possibility of
         *        them deadlocking and the map must not be interfered with externally.
         * @param lockBlockingThread another map used internally to detect and break deadlock.
         *        the same map must be passed to all new locks if there is a possibility of
         *        them deadlocking and the map must not be interfered with externally.
         */
        public DeadlockBreakingLock(final Map<Thread, Set<DeadlockBreakingLock>> locksHeldByThread,
                                    final Map<Thread, DeadlockBreakingLock> lockBlockingThread)
        {
            this.locksHeldByThread = locksHeldByThread;
            this.lockBlockingThread = lockBlockingThread;
        }

        /**
         * Make sure a given thread still has control of all the locks it locked.
         *
         * @param owner the thread to test.
         * @return true if all of the locks which owner has acquired are still in it's possession.
         */
        private synchronized boolean ownsAllLocks(final Thread owner)
        {
            for (DeadlockBreakingLock lock : locksHeldByThread.get(owner)) {
                if (lock.owners.peek() != owner) {
                    return false;
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see java.util.concurrent.locks.Lock#lock()
         */
        public synchronized void lock()
        {
            final Thread currentThread = Thread.currentThread();

            if (this.locksHeldByThread.get(currentThread) == null) {
                this.locksHeldByThread.put(currentThread, new HashSet<DeadlockBreakingLock>());
            }
            this.lockBlockingThread.put(currentThread, this);

            for (;;)
            {
                // We must insure that the thread which owns this lock does not force the lock which it is
                // waiting on while at the same time this thread forces this lock.
                // Specifically, we want to make sure that this.ownsAllLocks(currentThread) remains
                // the same throughout.
                //
                // For now, sync on a global object.
                // TODO We only need to sync on this "ring" of blocked threads, can that be done?
                synchronized (this.locksHeldByThread)
                {
                    // If the current thread does not own all of it's locks then by no means should it be
                    // forcing this lock, it should not be able to acquire it at all.
                    if (this.ownsAllLocks(currentThread))
                    {
                        // If 1. the lock is unlocked, 2. reentrance, or 3. it's deadlocked.
                        if (this.owners.empty()
                            || this.owners.peek() == currentThread
                            || this.isDeadlocked(null))
                        {
                            this.owners.push(currentThread);
                            this.locksHeldByThread.get(currentThread).add(this);
                            this.lockBlockingThread.remove(currentThread);
                            return;
                        }
                    }
                }

                try {
                    this.wait(100);
                } catch (InterruptedException e) {
                    this.lockBlockingThread.remove(currentThread);
                    throw new RuntimeException("The thread was interrupted while waiting on the lock.");
                }
            }
        }

        /**
         * Method to detect a deadlock situation in linear time.
         *
         * @param toCheckForLoop unless recursing, this should be null.
         * @return true if the current lock is known to be deadlocked.
         */
        private boolean isDeadlocked(final DeadlockBreakingLock toCheckForLoop)
        {
            // We have looped around, it's deadlocked, make a list and start adding locks.
            if (toCheckForLoop == this) {
                return true;
            }

            if (!this.owners.empty()) {
                // Get the lock that's holding up the thread that's holding this lock.
                // We only care about the last owner in the stack because if any others are deadlocked,
                // the last owner will finish and they will be exposed.
                final DeadlockBreakingLock blocker = this.lockBlockingThread.get(this.owners.peek());

                // If the lock blocking the thread which holds this lock exists (ie the thread is blocked)
                if (blocker != null) {
                    // Start case. We cannot call getLockLoop(this) otherwise it will always report false
                    // positive so we must call getLockLoop(null) to begin.
                    final DeadlockBreakingLock testCase = (toCheckForLoop == null) ? this : toCheckForLoop;

                    // recurse on the lock which is blocking the thread which holds this lock and if that
                    // lock is (transitively) blocked by this one then we have a loop.
                    return blocker.isDeadlocked(testCase);
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @see java.util.concurrent.locks.Lock#unlock()
         */
        public synchronized void unlock()
        {
            final Thread currentThread = Thread.currentThread();
            if (!this.owners.contains(currentThread)) {
                throw new IllegalMonitorStateException("Cannot unlock this lock as this "
                                                       + "thread does not own it.");
            }

            synchronized (this.locksHeldByThread.get(currentThread))
            {
                this.owners.remove(this.owners.lastIndexOf(currentThread));
                this.locksHeldByThread.get(currentThread).remove(this);
            }
            this.notify();
        }

        /**
         * {@inheritDoc}
         * Not implemented.
         *
         * @see java.util.concurrent.locks.Lock#lockInterruptibly()
         */
        public void lockInterruptibly()
        {
            throw new RuntimeException(NOT_IMPLEMENTED);
        }

        /**
         * {@inheritDoc}
         * Not implemented.
         *
         * @see java.util.concurrent.locks.Lock#newCondition()
         */
        public Condition newCondition()
        {
            throw new RuntimeException(NOT_IMPLEMENTED);
        }

        /**
         * {@inheritDoc}
         * Not implemented.
         *
         * @see java.util.concurrent.locks.Lock#tryLock()
         */
        public boolean tryLock()
        {
            throw new RuntimeException(NOT_IMPLEMENTED);
        }

        /**
         * {@inheritDoc}
         * Not implemented.
         *
         * @see java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)
         */
        public boolean tryLock(final long time, final TimeUnit unit)
        {
            throw new RuntimeException(NOT_IMPLEMENTED);
        }
    }
}
