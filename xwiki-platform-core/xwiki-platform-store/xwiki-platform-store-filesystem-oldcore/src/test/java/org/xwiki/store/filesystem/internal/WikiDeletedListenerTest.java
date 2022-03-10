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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class WikiDeletedListenerTest
{
    @InjectMockComponents
    private WikiDeletedListener wikiDeletedListener;

    @MockComponent
    private FilesystemStoreTools filesystemStoreTools;

    @Test
    void onEvent() throws IOException
    {
        WikiDeletedEvent wikiDeletedEvent = mock(WikiDeletedEvent.class);
        String wikiId = "foo";
        when(wikiDeletedEvent.getWikiId()).thenReturn(wikiId);

        File fooWikiTestDir = Files.createTempDirectory("fooWikiTestDir").toFile();
        when(filesystemStoreTools.getWikiDir(wikiId)).thenReturn(fooWikiTestDir);
        assertTrue(fooWikiTestDir.exists());
        wikiDeletedListener.onEvent(wikiDeletedEvent, null, null);
        assertFalse(fooWikiTestDir.exists());

        File barWikiTestFile = Files.createTempFile("wikitest", "tempfile").toFile();
        when(filesystemStoreTools.getWikiDir(wikiId)).thenReturn(barWikiTestFile);
        assertTrue(barWikiTestFile.exists());
        wikiDeletedListener.onEvent(wikiDeletedEvent, null, null);
        assertTrue(barWikiTestFile.exists());
    }
}
