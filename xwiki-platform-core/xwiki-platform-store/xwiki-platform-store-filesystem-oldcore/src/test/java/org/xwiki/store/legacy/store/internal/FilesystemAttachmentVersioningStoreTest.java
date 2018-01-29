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
package org.xwiki.store.legacy.store.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.filesystem.internal.AttachmentFileProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileStringEntityReferenceSerializer;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.legacy.doc.internal.ListAttachmentArchive;
import org.xwiki.store.locks.dummy.internal.DummyLockProvider;
import org.xwiki.store.serialization.xml.internal.AttachmentListMetadataSerializer;
import org.xwiki.store.serialization.xml.internal.AttachmentMetadataSerializer;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.web.Utils;

/**
 * Tests for FilesystemAttachmentVersioningStore.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class FilesystemAttachmentVersioningStoreTest extends AbstractFilesystemAttachmentStoreTest
{
    private FilesystemStoreTools fileTools;

    private AttachmentVersioningStore versionStore;

    private XWikiAttachmentArchive archive;

    private AttachmentFileProvider provider;

    private File storageLocation;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        Utils.setComponentManager(this.getComponentManager());

        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        this.storageLocation = new File(tmpDir, "test-storage-location");

        this.fileTools = new FilesystemStoreTools(new FileStringEntityReferenceSerializer(), storageLocation,
            new DummyLockProvider());
        final AttachmentListMetadataSerializer serializer =
            new AttachmentListMetadataSerializer(new AttachmentMetadataSerializer());
        this.versionStore = new FilesystemAttachmentVersioningStore();
        FieldUtils.writeDeclaredField(this.versionStore, "fileTools", this.fileTools, true);
        FieldUtils.writeDeclaredField(this.versionStore, "metaSerializer", serializer, true);

        final XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", "Main", "WebHome"));

        final XWikiAttachment version1 = new XWikiAttachment();
        version1.setVersion("1.1");
        version1.setFilename("attachment.txt");
        version1.setDoc(doc);
        version1.setAttachment_content(new StringAttachmentContent("I am version 1.1"));

        final XWikiAttachment version2 = new XWikiAttachment();
        version2.setVersion("1.2");
        version2.setFilename("attachment.txt");
        version2.setDoc(doc);
        version2.setAttachment_content(new StringAttachmentContent("I am version 1.2"));

        final XWikiAttachment version3 = new XWikiAttachment();
        version3.setVersion("1.3");
        version3.setFilename("attachment.txt");
        version3.setDoc(doc);
        version3.setAttachment_content(new StringAttachmentContent("I am version 1.3"));

        this.provider = this.fileTools.getAttachmentFileProvider(version1.getReference());
        this.archive = new ListAttachmentArchive(new ArrayList<XWikiAttachment>()
        {
            {
                add(version1);
                add(version2);
                add(version3);
            }
        });
    }

    @After
    @Override
    public void tearDown() throws IOException
    {
        resursiveDelete(this.storageLocation);
    }

    @Test
    public void saveArchiveTest() throws Exception
    {
        final XWikiAttachmentContent content = this.archive.getAttachment().getAttachment_content();
        final XWikiAttachment attach = this.archive.getAttachment();

        Assert.assertFalse(this.provider.getAttachmentVersioningMetaFile().exists());
        Assert.assertFalse(this.provider.getAttachmentVersionContentFile("1.1").exists());
        Assert.assertFalse(this.provider.getAttachmentVersionContentFile("1.2").exists());
        Assert.assertFalse(this.provider.getAttachmentVersionContentFile("1.3").exists());

        // Because the context is only used by the legacy implementation, it is safe to pass null.
        this.versionStore.saveArchive(this.archive, null, false);

        Assert.assertTrue(this.provider.getAttachmentVersioningMetaFile().exists());

        // Make sure it's not just:
        // <?xml version="1.0" encoding="UTF-8"?>
        // <attachment-list serializer="attachment-list-meta/1.0">
        // </attachment-list>
        Assert.assertTrue(this.provider.getAttachmentVersioningMetaFile().length() > 120);

        Assert.assertTrue(this.provider.getAttachmentVersionContentFile("1.1").exists());
        Assert.assertTrue(this.provider.getAttachmentVersionContentFile("1.2").exists());
        Assert.assertTrue(this.provider.getAttachmentVersionContentFile("1.3").exists());

        // Prove that the attachment and attachment content are the same after saving.
        Assert.assertSame(attach, this.archive.getAttachment());
        Assert.assertSame(content, this.archive.getAttachment().getAttachment_content());
    }

    @Test
    public void loadArchiveTest() throws Exception
    {
        this.versionStore.saveArchive(this.archive, null, false);
        final XWikiAttachmentArchive newArch = this.versionStore.loadArchive(archive.getAttachment(), null, false);
        Assert.assertTrue(newArch.getVersions().length == 3);
        final XWikiAttachment version1 = newArch.getRevision(archive.getAttachment(), "1.1", null);
        final XWikiAttachment version2 = newArch.getRevision(archive.getAttachment(), "1.2", null);
        final XWikiAttachment version3 = newArch.getRevision(archive.getAttachment(), "1.3", null);

        Assert.assertTrue(version1.getVersion().equals("1.1"));
        Assert.assertTrue(version1.getFilename().equals("attachment.txt"));
        Assert.assertEquals("I am version 1.1", IOUtils.toString(version1.getContentInputStream(null)));
        Assert.assertSame(version1.getDoc(), this.archive.getAttachment().getDoc());

        Assert.assertTrue(version2.getVersion().equals("1.2"));
        Assert.assertTrue(version2.getFilename().equals("attachment.txt"));
        Assert.assertEquals("I am version 1.2", IOUtils.toString(version2.getContentInputStream(null)));
        Assert.assertSame(version2.getDoc(), this.archive.getAttachment().getDoc());

        Assert.assertTrue(version3.getVersion().equals("1.3"));
        Assert.assertTrue(version3.getFilename().equals("attachment.txt"));
        Assert.assertEquals("I am version 1.3", IOUtils.toString(version3.getContentInputStream(null)));
        Assert.assertSame(version3.getDoc(), this.archive.getAttachment().getDoc());
    }

    @Test
    public void deleteArchiveTest() throws Exception
    {
        this.versionStore.saveArchive(this.archive, null, false);

        Assert.assertTrue(this.provider.getAttachmentVersioningMetaFile().exists());
        Assert.assertTrue(this.provider.getAttachmentVersionContentFile("1.1").exists());
        Assert.assertTrue(this.provider.getAttachmentVersionContentFile("1.2").exists());
        Assert.assertTrue(this.provider.getAttachmentVersionContentFile("1.3").exists());

        this.versionStore.deleteArchive(this.archive.getAttachment(), null, false);

        Assert.assertFalse(this.provider.getAttachmentVersioningMetaFile().exists());
        Assert.assertFalse(this.provider.getAttachmentVersionContentFile("1.1").exists());
        Assert.assertFalse(this.provider.getAttachmentVersionContentFile("1.2").exists());
        Assert.assertFalse(this.provider.getAttachmentVersionContentFile("1.3").exists());
    }

    /* -------------------- Helpers -------------------- */

    private static void resursiveDelete(final File toDelete) throws IOException
    {
        if (toDelete == null || !toDelete.exists()) {
            return;
        }
        if (toDelete.isDirectory()) {
            final File[] children = toDelete.listFiles();
            for (int i = 0; i < children.length; i++) {
                resursiveDelete(children[i]);
            }
        }
        toDelete.delete();
    }

    private static class StringAttachmentContent extends XWikiAttachmentContent
    {
        private final String content;

        public StringAttachmentContent(final String content)
        {
            this.content = content;
        }

        @Override
        public InputStream getContentInputStream()
        {
            return new ByteArrayInputStream(this.content.getBytes());
        }

        @Override
        public boolean isContentDirty()
        {
            return true;
        }

        @Override
        public StringAttachmentContent clone()
        {
            return this;
        }
    }
}
