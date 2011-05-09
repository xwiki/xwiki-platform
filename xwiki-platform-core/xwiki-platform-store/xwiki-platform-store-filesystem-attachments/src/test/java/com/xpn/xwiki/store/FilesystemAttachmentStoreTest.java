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
package com.xpn.xwiki.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.xwiki.model.internal.reference.PathStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.filesystem.internal.DefaultFilesystemStoreTools;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.FilesystemAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;

import org.hibernate.Session;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.lib.action.CustomAction;
import org.jmock.api.Invocation;

import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.store.locks.preemptive.internal.PreemptiveLockProvider;

import org.apache.commons.io.IOUtils;


/**
 * Tests for FilesystemAttachmentStore.
 *
 * @version $Id$
 * @since 3.0M2
 */
@RunWith(JMock.class)
public class FilesystemAttachmentStoreTest extends AbstractMockingComponentTestCase
{
    private static final String HELLO = "Hello World";

    private static final byte[] HELLO_BYTES;

    private static final InputStream HELLO_STREAM;

    private Mockery jmockContext = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private XWikiContext mockContext;

    private XWikiAttachment mockAttach;

    private FilesystemAttachmentStore attachStore;

    private FilesystemStoreTools fileTools;

    private AttachmentVersioningStore mockAttachVersionStore;

    private XWikiAttachmentArchive mockArchive;

    private XWikiHibernateStore mockHibernate;

    private Session mockHibernateSession;

    private XWikiDocument doc;

    /** The file which will hold content for this attachment. */
    private File storeFile;

    /** The dir in /tmp/ which we use as our sandbox. */
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
    public void setUp() throws Exception
    {
        super.setUp();
        Utils.setComponentManager(this.getComponentManager());

        this.mockContext = this.jmockContext.mock(XWikiContext.class);
        final XWiki mockXWiki = this.jmockContext.mock(XWiki.class);
        this.mockHibernate = this.jmockContext.mock(XWikiHibernateStore.class);
        final XWikiAttachmentContent mockDirtyContent =
           this.jmockContext.mock(XWikiAttachmentContent.class);
        this.mockAttachVersionStore = this.jmockContext.mock(AttachmentVersioningStore.class);
        this.mockArchive = this.jmockContext.mock(XWikiAttachmentArchive.class);
        this.mockHibernateSession = this.jmockContext.mock(Session.class);
        this.doc = new XWikiDocument(new DocumentReference("xwiki", "Main", "WebHome"));

        this.mockAttach = this.jmockContext.mock(XWikiAttachment.class);
        this.jmockContext.checking(new Expectations() {{
            allowing(mockContext).getWiki(); will(returnValue(mockXWiki));

            allowing(mockXWiki).getStore(); will(returnValue(mockHibernate));
            allowing(mockXWiki).getHibernateStore(); will(returnValue(mockHibernate));
            allowing(mockHibernate).checkHibernate(mockContext);
            allowing(mockHibernate).beginTransaction(mockContext);

            allowing(mockHibernate).getSession(mockContext); will(returnValue(mockHibernateSession));

            allowing(mockXWiki).getAttachmentVersioningStore(); will(returnValue(mockAttachVersionStore));
            allowing(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);

            allowing(mockAttach).getContentInputStream(mockContext); will(returnValue(HELLO_STREAM));
            allowing(mockAttach).getDoc(); will(returnValue(doc));
            allowing(mockAttach).getFilename(); will(returnValue("file.name"));
            allowing(mockAttach).updateContentArchive(mockContext);
            allowing(mockAttach).getAttachment_archive(); will(returnValue(mockArchive));
            allowing(mockAttach).getAttachment_content(); will(returnValue(mockDirtyContent));
            allowing(mockAttach).isContentDirty(); will(returnValue(true));
            allowing(mockDirtyContent).isContentDirty(); will(returnValue(true));
        }});

        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        this.storageLocation = new File(tmpDir, "test-storage-location");

        this.fileTools =
            new DefaultFilesystemStoreTools(new PathStringEntityReferenceSerializer(),
                                            storageLocation,
                                            new PreemptiveLockProvider());

        this.attachStore = new FilesystemAttachmentStore(fileTools);
        this.storeFile =
            this.fileTools.getAttachmentFileProvider(this.mockAttach).getAttachmentContentFile();
        HELLO_STREAM.reset();
    }

    @After
    public void tearDown() throws IOException
    {
        resursiveDelete(this.storageLocation);
    }

    @Test
    public void saveContentTest() throws Exception
    {
        final File storeFile =
            this.fileTools.getAttachmentFileProvider(this.mockAttach).getAttachmentContentFile();
        Assert.assertFalse(this.storeFile.exists());
        this.attachStore.saveAttachmentContent(this.mockAttach, false, this.mockContext, false);
        Assert.assertTrue("The attachment file was not created.", this.storeFile.exists());

        final InputStream is = new FileInputStream(storeFile);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        is.close();
        byte[] array = os.toByteArray();
        Assert.assertEquals("The attachment file contained the wrong content",
                            HELLO,
                            new String(array, "UTF-8"));
    }

    @Test
    public void loadContentTest() throws Exception
    {
        this.storeFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(this.storeFile, false);
        IOUtils.copy(HELLO_STREAM, os);
        os.close();

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockAttach).setAttachment_content(with(any(FilesystemAttachmentContent.class)));
                will(new CustomAction("Check to make sure the attachment content is correct.") {
                    public Object invoke(final Invocation invoc)
                    {
                        final FilesystemAttachmentContent content =
                            (FilesystemAttachmentContent) invoc.getParameter(0);

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
        }});

        this.attachStore.loadAttachmentContent(this.mockAttach, this.mockContext, false);
    }

    @Test
    public void deleteAttachmentTest() throws Exception
    {
        this.jmockContext.checking(new Expectations() {{
            oneOf(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
            exactly(2).of(mockHibernateSession).delete(with(anything()));
        }});
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

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
            exactly(2).of(mockHibernateSession).delete(with(anything()));
            oneOf(mockHibernate).saveXWikiDoc(doc, mockContext, false);
                will(new CustomAction("Make sure the attachment has been removed from the list.") {
                    public Object invoke(final Invocation invoc)
                    {
                        final XWikiDocument document = (XWikiDocument) invoc.getParameter(0);
                        Assert.assertTrue("Attachment was not removed from the list.",
                                          document.getAttachmentList().size() == 0);
                        return null;
                    }
                });
        }});
        this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, true, this.mockContext, false);
    }

    @Test
    public void documentUpdateOnSaveTest() throws Exception
    {
        this.jmockContext.checking(new Expectations() {{
            oneOf(mockHibernate).saveXWikiDoc(doc, mockContext, false);
        }});

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
