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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(XWikiTempDirExtension.class)
class WikiDeletedListenerTest
{
    @InjectMockComponents
    private WikiDeletedListener wikiDeletedListener;

    @MockComponent
    private FilesystemStoreTools filesystemStoreTools;

    @XWikiTempDir
    private File tempDir;

    private BlobStore blobStore;

    @BeforeEach
    void setUp()
    {
        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(this.tempDir.toPath());
        this.blobStore = new FileSystemBlobStore("Test", properties);
        when(this.filesystemStoreTools.getStore()).thenReturn(this.blobStore);
    }

    @Test
    void onEvent() throws Exception
    {
        WikiDeletedEvent wikiDeletedEvent = mock(WikiDeletedEvent.class);
        String wikiId = "foo";
        when(wikiDeletedEvent.getWikiId()).thenReturn(wikiId);

        BlobPath wikiPath = BlobPath.of(List.of(wikiId));
        BlobPath file1 = wikiPath.resolve("file1");
        this.blobStore.getBlob(file1).writeFromStream(new ByteArrayInputStream("File 1".getBytes()));

        when(this.filesystemStoreTools.getWikiDir(wikiId)).thenReturn(wikiPath);
        assertTrue(this.blobStore.getBlob(file1).exists());
        assertFalse(this.blobStore.isEmptyDirectory(wikiPath));
        this.wikiDeletedListener.onEvent(wikiDeletedEvent, null, null);
        assertFalse(this.blobStore.getBlob(file1).exists());
        assertTrue(this.blobStore.isEmptyDirectory(wikiPath));

        // Create the wiki path as a blob.
        this.blobStore.getBlob(wikiPath).writeFromStream(new ByteArrayInputStream("I am a blob!".getBytes()));
        assertTrue(this.blobStore.getBlob(wikiPath).exists());
        this.wikiDeletedListener.onEvent(wikiDeletedEvent, null, null);
        assertTrue(this.blobStore.getBlob(wikiPath).exists());
    }
}
