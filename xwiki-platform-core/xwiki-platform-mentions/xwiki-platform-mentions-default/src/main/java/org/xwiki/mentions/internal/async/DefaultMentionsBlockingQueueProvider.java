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
package org.xwiki.mentions.internal.async;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.mentions.MentionException;
import org.xwiki.mentions.internal.MentionsBlockingQueueProvider;

import static org.h2.mvstore.DataUtils.ERROR_UNSUPPORTED_FORMAT;

/**
 * Default implementation of {@link MentionsBlockingQueueProvider}.
 *
 * The {@link BlockingQueue} provided here is based on {@link MVMap} and {@link MapBasedLinkedBlockingQueue}.
 * This queue is persisted to disk, in the /mentions/mvqueue directory of the permanent directory.
 * The queue is persisted to disk. If a directory is already existing when 
 * {@link DefaultMentionsBlockingQueueProvider#initBlockingQueue()} is called (for instance, after restart), the queue
 * is loaded with the elements stored in disk.
 *
 * @version $Id$
 * @since 12.6
 */
@Component
@Singleton
public class DefaultMentionsBlockingQueueProvider implements MentionsBlockingQueueProvider
{
    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    private MVStore mvStore;

    @Override
    public BlockingQueue<MentionsData> initBlockingQueue() throws MentionException
    {
        File parentDir = new File(this.environment.getPermanentDirectory(), "mentions");
        File queueFile = new File(parentDir, "mvqueue");
        if (!parentDir.exists()) {
            try {
                Files.createDirectory(parentDir.toPath());
            } catch (IOException e) {
                throw new MentionException("Error when initializing directory", e);
            }
        }
        try {
            this.mvStore = MVStore.open(queueFile.getAbsolutePath());
        } catch (MVStoreException e) {
            // When migrating from h2mvstore v1.x to h2mvstore v2.x, the file format changes. In this case, the old file
            // is removed and a new one is created. Note that if some unprocessed MentionsData are still in the store
            // during the initialization, they will be lost and the mentioned users will not be notified. 
            if (e.getErrorCode() == ERROR_UNSUPPORTED_FORMAT) {
                try {
                    backupQueueFile(queueFile);
                } catch (IOException moveException) {
                    throw new MentionException(
                        String.format("Failed to backup [%s] to [].", queueFile.getAbsolutePath()), e);
                }
                this.mvStore = MVStore.open(queueFile.getAbsolutePath());
            } else {
                // If the exception is not related to the file format, rethrow the exception.
                throw e;
            }
        }

        return new MapBasedLinkedBlockingQueue<>(this.mvStore.openMap("mentionsData"));
    }

    @Override
    public void closeQueue()
    {
        if (this.mvStore != null) {
            this.mvStore.close();
        }
    }

    private void backupQueueFile(File queueFile) throws IOException
    {
        long currentTimeMillis = System.currentTimeMillis();
        Path backupFile = queueFile.toPath().getParent().resolve(String.format("mvqueue.%d.old", currentTimeMillis));
        this.logger.info("Unsupported file format for [{}]. It will be saved in [{}] and replaced by a new file.",
            queueFile.getAbsolutePath(),
            backupFile.toFile().getAbsolutePath());
        Files.move(queueFile.toPath(), backupFile);
    }
}
