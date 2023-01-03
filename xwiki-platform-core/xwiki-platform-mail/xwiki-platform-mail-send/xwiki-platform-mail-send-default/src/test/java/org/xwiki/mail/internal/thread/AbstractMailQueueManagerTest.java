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
package org.xwiki.mail.internal.thread;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AbstractMailQueueManager}.
 *
 * @version $Id$
 */
class AbstractMailQueueManagerTest
{
    @Test
    void addMessageWhenTimeout() throws Exception
    {
        AbstractMailQueueManager<PrepareMailQueueItem> manager = new AbstractMailQueueManager<PrepareMailQueueItem>()
        {
            @Override
            public void initialize()
            {
                this.mailQueue = new LinkedBlockingQueue<>(1);
            }
        };
        manager.initialize();
        Throwable exception = assertThrows(InterruptedException.class, () -> {
            // Add 2 messages since the queue has a max size of 1, and we want to the second message to time out.
            manager.addMessage(new PrepareMailQueueItem(null, null, null, null, null), 1, TimeUnit.MILLISECONDS);
            manager.addMessage(new PrepareMailQueueItem(null, null, null, null, null), 1, TimeUnit.MILLISECONDS);
        });
        assertLinesMatch(List.of("Failed to add the message \\[.*\\] to the queue as it was full, even after waiting "
            + "\\[1\\] \\[MILLISECONDS\\]"), List.of(exception.getMessage()));
    }
}
