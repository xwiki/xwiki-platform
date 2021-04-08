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
import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.mentions.internal.MentionsBlockingQueueProvider;

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

    private MVStore mvStore;

    @Override
    public BlockingQueue<MentionsData> initBlockingQueue()
    {
        File parentDir = new File(this.environment.getPermanentDirectory(), "mentions");
        File queueFile = new File(parentDir, "mvqueue");
        if (!parentDir.exists()) {
            try {
                Files.createDirectory(parentDir.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Error when initializing directory", e);
            }
        }
        this.mvStore = MVStore.open(queueFile.getAbsolutePath());

        return new MapBasedLinkedBlockingQueue<>(this.mvStore.openMap("mentionsData"));
    }

    @Override
    public void closeQueue()
    {
        if (this.mvStore != null) {
            this.mvStore.close();
        }
    }
}
