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
import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.mentions.internal.MentionsBlockingQueueProvider;

import ch.rasc.xodusqueue.XodusBlockingQueue;

/**
 * Default implementation of {@link MentionsBlockingQueueProvider}.
 *
 * The {@link BlockingQueue} provided here is the {@link XodusBlockingQueue} implemention. This queue is persisted to 
 * disk, in the /mentions/queue directory of the permanent directory.
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

    private XodusBlockingQueue<MentionsData> queue;

    @Override
    public BlockingQueue<MentionsData> initBlockingQueue()
    {
        File queueDirectory = new File(new File(this.environment.getPermanentDirectory(), "mentions"), "queue");
        this.queue =
            new XodusBlockingQueue<>(queueDirectory.getAbsolutePath(), MentionsData.class, Long.MAX_VALUE, true);
        return this.queue;
    }

    @Override
    public void closeQueue()
    {
        this.queue.close();
    }
}
