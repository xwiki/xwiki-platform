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

import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.rasc.xodusqueue.XodusBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultMentionsBlockingQueueProvider}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@ComponentTest
class DefaultMentionsBlockingQueueProviderTest
{
    @InjectMockComponents
    private DefaultMentionsBlockingQueueProvider provider;

    @MockComponent
    private Environment environment;

    @Test
    void initBlockingQueue(@XWikiTempDir File tmpDir)
    {
        when(this.environment.getPermanentDirectory()).thenReturn(tmpDir);
        BlockingQueue<MentionsData> actual = this.provider.initBlockingQueue();
        assertTrue(actual instanceof XodusBlockingQueue);
    }

    @Test
    void initBlockingQueueTwice(@XWikiTempDir File tmpDir)
    {
        when(this.environment.getPermanentDirectory()).thenReturn(tmpDir);
        this.provider.initBlockingQueue();
        this.provider.closeQueue();
        BlockingQueue<MentionsData> actual = this.provider.initBlockingQueue();
        assertTrue(actual instanceof XodusBlockingQueue);
    }
}
