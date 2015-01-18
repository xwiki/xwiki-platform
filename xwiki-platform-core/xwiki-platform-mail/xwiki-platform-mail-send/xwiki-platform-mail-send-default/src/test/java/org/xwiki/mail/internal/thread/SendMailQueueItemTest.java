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

import java.util.Properties;
import java.util.UUID;

import javax.mail.Session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.xwiki.mail.internal.thread.SendMailQueueItem}.
 *
 * @version $Id$
 * @since 6.4
 */
public class SendMailQueueItemTest
{
    @Test
    public void verifyToString() throws Exception
    {
        Session session = Session.getDefaultInstance(new Properties());
        String batchId = UUID.randomUUID().toString();
        SendMailQueueItem item = new SendMailQueueItem("messageId", session, null, batchId, "wiki");

        assertEquals("batchId = [" + batchId + "], wikiId = [wiki], messageId = [messageId]", item.toString());
    }
}
