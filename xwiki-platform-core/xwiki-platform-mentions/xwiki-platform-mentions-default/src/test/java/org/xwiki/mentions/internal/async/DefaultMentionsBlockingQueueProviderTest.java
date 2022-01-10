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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.environment.Environment;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @XWikiTempDir
    private File tmpDir;

    @BeforeEach
    void setUp()
    {
        when(this.environment.getPermanentDirectory()).thenReturn(this.tmpDir);
    }

    @Test
    void initBlockingQueue() throws Exception
    {
        BlockingQueue<MentionsData> actual = this.provider.initBlockingQueue();
        assertTrue(actual instanceof MapBasedLinkedBlockingQueue);
    }

    @Test
    void initBlockingQueueTwice() throws Exception
    {
        this.provider.initBlockingQueue();
        this.provider.closeQueue();
        BlockingQueue<MentionsData> actual = this.provider.initBlockingQueue();
        assertTrue(actual instanceof MapBasedLinkedBlockingQueue);
    }

    @Test
    void initBlockingQueueCreateDirectoryFail() throws Exception
    {
        URL url = DefaultMentionsBlockingQueueProviderTest.class.getClassLoader().getResource("v1");
        Path v1Dir = Paths.get(url.toURI());
        Path mentionsDir = v1Dir.resolve("mentions");
        // Recopy a reference ".test" file at the expected location to allow the test to be executed several times in a
        // row.
        Files.copy(mentionsDir.resolve("mvqueue.test"), mentionsDir.resolve("mvqueue"), REPLACE_EXISTING);

        when(this.environment.getPermanentDirectory()).thenReturn(v1Dir.toFile());
        BlockingQueue<MentionsData> mentionsData = this.provider.initBlockingQueue();
        assertTrue(mentionsData.isEmpty());
        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).matches(
            "^Unsupported file format for \\[.+/v1/mentions/mvqueue]. "
                + "It will be saved in \\[.+/v1/mentions/mvqueue\\.\\d+\\.old] and replaced by a new file."));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
        this.provider.closeQueue();
    }
}
