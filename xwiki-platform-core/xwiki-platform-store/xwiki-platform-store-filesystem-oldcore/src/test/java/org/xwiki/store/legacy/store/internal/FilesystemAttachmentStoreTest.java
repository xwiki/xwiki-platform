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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.locks.dummy.internal.DummyLockProvider;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachment.AttachmentContainer;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.Utils;

/**
 * Tests for FilesystemAttachmentStore.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class FilesystemAttachmentStoreTest extends AbstractFilesystemAttachmentStoreTest
{
    private static final String HELLO = "Hello World";

    private static final byte[] HELLO_BYTES;

    private static final InputStream HELLO_STREAM;

    private XWikiContext mockContext;

    private XWikiAttachment mockAttach;

    private AttachmentReference mockAttachReference;

    private FilesystemAttachmentStore attachStore;

    private FilesystemStoreTools fileTools;

    private AttachmentVersioningStore mockAttachVersionStore;

    private XWikiAttachmentArchive mockArchive;

    private XWikiHibernateStore mockHibernate;

    private Session mockHibernateSession;

    private XWikiDocument doc;

    /**
     * The file which will hold content for this attachment.
     */
    private File storeFile;

    /**
     * The dir in /tmp/ which we use as our sandbox.
     */
    private File storageLocation;

    static {
        try {
            HELLO_BYTES = HELLO.getBytes("UTF-8");
            HELLO_STREAM = new ByteArrayInputStream(HELLO_BYTES);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8!!");
        }
    }

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);

        Utils.setComponentManager(this.getComponentManager());

        this.mockContext = getMockery().mock(XWikiContext.class);
        final XWiki mockXWiki = getMockery().mock(XWiki.class);
        this.mockHibernate = getMockery().mock(XWikiHibernateStore.class);
        final XWikiAttachmentContent mockDirtyContent = getMockery().mock(XWikiAttachmentContent.class);
        this.mockAttachVersionStore = getMockery().mock(AttachmentVersioningStore.class);
        this.mockArchive = getMockery().mock(XWikiAttachmentArchive.class);
        this.mockHibernateSession = getMockery().mock(Session.class);
        this.doc = new XWikiDocument(new DocumentReference("xwiki", "Main", "WebHome"));

        this.mockAttachReference = new AttachmentReference("file.name", doc.getDocumentReference());
        this.mockAttach = getMockery().mock(XWikiAttachment.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockContext).getWiki();
                will(returnValue(mockXWiki));

                allowing(mockXWiki).getStore();
                will(returnValue(mockHibernate));
                allowing(mockXWiki).getHibernateStore();
                will(returnValue(mockHibernate));
                allowing(mockHibernate).checkHibernate(mockContext);
                allowing(mockHibernate).beginTransaction(mockContext);

                allowing(mockHibernate).getSession(mockContext);
                will(returnValue(mockHibernateSession));

                allowing(mockXWiki).getDefaultAttachmentArchiveStore();
                will(returnValue(mockAttachVersionStore));

                allowing(mockAttach).getContentInputStream(mockContext);
                will(returnValue(HELLO_STREAM));
                allowing(mockAttach).setDoc(doc);
                allowing(mockAttach).getDoc();
                will(returnValue(doc));
                allowing(mockAttach).getFilename();
                will(returnValue(mockAttachReference.getName()));
                allowing(mockAttach).getReference();
                will(returnValue(mockAttachReference));
                allowing(mockAttach).updateContentArchive(mockContext);
                allowing(mockAttach).getAttachment_archive();
                will(returnValue(mockArchive));
                allowing(mockAttach).isArchiveStoreSet();
                will(returnValue(false));
                allowing(mockAttach).getArchiveStore();
                will(returnValue("file"));
                allowing(mockAttach).getAttachment_content();
                will(returnValue(mockDirtyContent));
                allowing(mockAttach).isContentStoreSet();
                will(returnValue(false));
                allowing(mockAttach).getContentStore();
                will(returnValue("file"));
                allowing(mockAttach).isContentDirty();
                will(returnValue(true));
                allowing(mockDirtyContent).isContentDirty();
                will(returnValue(true));
                allowing(mockAttach).setAttachmentContainer(with(any(AttachmentContainer.class)));
            }
        });

        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        this.storageLocation = new File(tmpDir, "test-storage-location");

        this.fileTools = new FilesystemStoreTools(storageLocation, new DummyLockProvider());

        this.attachStore = new FilesystemAttachmentStore();
        FieldUtils.writeField(this.attachStore, "fileTools", this.fileTools, true);

        this.storeFile = this.fileTools.getAttachmentFileProvider(this.mockAttachReference).getAttachmentContentFile();
        HELLO_STREAM.reset();
    }

    @After
    @Override
    public void tearDown() throws IOException
    {
        resursiveDelete(this.storageLocation);
    }

    @Test
    public void saveContentTest() throws Exception
    {
        final File storeFile =
            this.fileTools.getAttachmentFileProvider(this.mockAttachReference).getAttachmentContentFile();
        Assert.assertFalse(this.storeFile.exists());

        getMockery().checking(new Expectations()
        {
            {
                // Make sure an archive is saved
                oneOf(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);
            }
        });

        this.attachStore.saveAttachmentContent(this.mockAttach, false, this.mockContext, false);

        Assert.assertTrue("The attachment file was not created.", this.storeFile.exists());
        Assert.assertEquals("The attachment file contained the wrong content", HELLO,
            FileUtils.readFileToString(storeFile, StandardCharsets.UTF_8));
    }

    @Test
    public void saveTwoOfSameAttachmentInOneTransactionTest() throws Exception
    {
        final File storeFile =
            this.fileTools.getAttachmentFileProvider(this.mockAttachReference).getAttachmentContentFile();
        Assert.assertFalse(this.storeFile.exists());

        getMockery().checking(new Expectations()
        {
            {
                // Make sure an archive is saved (twice)
                oneOf(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);
                oneOf(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);
            }
        });

        final List<XWikiAttachment> attachments = new ArrayList<XWikiAttachment>();
        attachments.add(this.mockAttach);
        attachments.add(this.mockAttach);
        this.attachStore.saveAttachmentsContent(attachments, this.doc, false, this.mockContext, false);

        Assert.assertTrue("The attachment file was not created.", this.storeFile.exists());
        Assert.assertEquals("The attachment file contained the wrong content", HELLO,
            FileUtils.readFileToString(storeFile, StandardCharsets.UTF_8));
    }

    @Test
    public void loadContentTest() throws Exception
    {
        this.storeFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(this.storeFile, false);
        IOUtils.copy(HELLO_STREAM, os);
        os.close();

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockAttach).setAttachment_content(with(any(FilesystemAttachmentContent.class)));
                will(new CustomAction("Check to make sure the attachment content is correct.")
                {
                    @Override
                    public Object invoke(final Invocation invoc)
                    {
                        final FilesystemAttachmentContent content = (FilesystemAttachmentContent) invoc.getParameter(0);

                        try {
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            IOUtils.copy(content.getContentInputStream(), baos);

                            final String output = new String(baos.toByteArray(), "UTF-8");

                            Assert.assertEquals("Not the same attachment content.", HELLO, output);
                            return null;
                        } catch (IOException e) {
                            throw new RuntimeException("Exception getting attachment content.", e);
                        }
                    }
                });
                oneOf(mockAttach).setContentStore("file");
            }
        });

        this.attachStore.loadAttachmentContent(this.mockAttach, this.mockContext, false);
    }

    @Test
    public void deleteAttachmentTest() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
                exactly(2).of(mockHibernateSession).delete(with(any(Object.class)));
            }
        });
        this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, false, this.mockContext, false);

        Assert.assertFalse("The attachment file was not deleted.", this.storeFile.exists());
    }

    @Test
    public void documentUpdateOnDeleteTest() throws Exception
    {
        final List<XWikiAttachment> attachList = new ArrayList<XWikiAttachment>();
        attachList.add(this.mockAttach);
        this.doc.setAttachmentList(attachList);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
                exactly(2).of(mockHibernateSession).delete(with(any(Object.class)));
                oneOf(mockHibernate).saveXWikiDoc(doc, mockContext, false);
                will(new CustomAction("Make sure the attachment has been removed from the list.")
                {
                    public Object invoke(final Invocation invoc)
                    {
                        final XWikiDocument document = (XWikiDocument) invoc.getParameter(0);
                        Assert.assertTrue("Attachment was not removed from the list.",
                            document.getAttachmentList().size() == 0);
                        return null;
                    }
                });
            }
        });
        this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, true, this.mockContext, false);
    }

    @Test
    public void documentUpdateOnSaveTest() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockHibernate).saveXWikiDoc(doc, mockContext, false);
                oneOf(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);

            }
        });

        this.attachStore.saveAttachmentContent(this.mockAttach, true, this.mockContext, false);
    }

    /* -------------------- Helpers -------------------- */

    private void createFile() throws IOException
    {
        this.storeFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(this.storeFile, false);
        IOUtils.copy(HELLO_STREAM, os);
        os.close();
        Assert.assertTrue("The attachment file not created for the test.", this.storeFile.exists());
    }

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
}
