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
package org.xwiki.cluster.test;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import org.junit.Test;
import org.xwiki.cluster.test.framework.AbstractClusterHttpTest;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.test.ui.AbstractTest;

/**
 * Verify the document cache update based on distributed events.
 * 
 * @version $Id$
 */
public class DocumentCacheTest extends AbstractClusterHttpTest
{
    private final static String TEST_SPACE = "Test";

    @Test
    public void documentModifiedCacheSync() throws Exception
    {
        Page page = new Page();
        page.setSpace(TEST_SPACE);
        page.setName("CacheSync");

        LocalDocumentReference documentReference = new LocalDocumentReference(page.getSpace(), page.getName());

        // 1) Edit a page on XWiki 0
        AbstractTest.getUtil().switchExecutor(0);
        page.setContent("content");
        AbstractTest.getUtil().rest().save(page);
        assertEquals("content", AbstractTest.getUtil().rest().<Page>get(documentReference).getContent());

        // 2) Modify content of the page on XWiki 1
        AbstractTest.getUtil().switchExecutor(1);
        page.setContent("modified content");
        AbstractTest.getUtil().rest().save(page);
        assertEquals("modified content", AbstractTest.getUtil().rest().<Page>get(documentReference).getContent());

        // ASSERT) The content in XWiki 0 should be the one set than in XWiki 1
        // Since it can take time for the Cluster to propagate the change, we need to wait and set up a timeout.
        AbstractTest.getUtil().switchExecutor(0);
        assertEqualsWithTimeout("modified content", () -> {
            try {
                return AbstractTest.getUtil().rest().<Page>get(documentReference).getContent();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void documentDeletedCacheSync() throws Exception
    {
        Page page = new Page();
        page.setSpace(TEST_SPACE);
        page.setName("CacheSync");

        LocalDocumentReference documentReference = new LocalDocumentReference(page.getSpace(), page.getName());

        // 1) Make sure page exist on XWiki 0
        AbstractTest.getUtil().switchExecutor(0);
        page.setContent("content");
        AbstractTest.getUtil().rest().save(page);
        assertEquals("content", AbstractTest.getUtil().rest().<Page>get(documentReference).getContent());

        // 2) Delete page on XWiki 1
        AbstractTest.getUtil().switchExecutor(1);
        // Need superadmin to delete document
        AbstractTest.getUtil().loginAsSuperAdmin();
        AbstractTest.getUtil().rest().delete(documentReference);
        assertFalse(AbstractTest.getUtil().rest().exists(documentReference));

        // ASSERT) The document should be deleted on XWiki 0
        // Since it can take time for the Cluster to propagate the change, we need to wait and set up a timeout.
        AbstractTest.getUtil().switchExecutor(0);
        assertTrueWithTimeout("Document should not exist anymore", () -> {
            try {
                return AbstractTest.getUtil().rest().exists(documentReference);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void documentCacheSyncForAttachments() throws Exception
    {
        Page page = new Page();
        page.setSpace(TEST_SPACE);
        page.setName("AttachementCacheSync");

        AttachmentReference attachmentReference = new AttachmentReference("file.ext",
            new DocumentReference(AbstractTest.getUtil().getCurrentWiki(), page.getSpace(), page.getName()));

        // 1) Edit a page on XWiki 0
        AbstractTest.getUtil().switchExecutor(0);
        page.setContent("content");
        AbstractTest.getUtil().rest().save(page);

        // 2) Add attachment to the page on XWiki 1
        AbstractTest.getUtil().switchExecutor(1);
        AbstractTest.getUtil().rest().attachFile(attachmentReference, "content".getBytes(), true);

        // ASSERT) The content in XWiki 0 should be the one set than in XWiki 1
        // Since it can take time for the Cluster to propagate the change, we need to wait and set up a timeout.
        AbstractTest.getUtil().switchExecutor(0);
        assertTrueWithTimeout("Failed to find attachment", () -> {
            try {
                return !hasAttachment(attachmentReference);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean hasAttachment(AttachmentReference attachmentReference) throws Exception
    {
        Attachments attachments =
            AbstractTest.getUtil().rest().get(AttachmentsResource.class, attachmentReference.getDocumentReference());
        for (Attachment attachment : attachments.getAttachments()) {
            System.out.println(attachment.getName());
            if (attachment.getName().equals(attachmentReference.getName())) {
                return true;
            }
        }

        return false;
    }

    private void assertTrueWithTimeout(String failMessage, BooleanSupplier booleanSupplier)
        throws InterruptedException
    {
        long t2;
        long t1 = System.currentTimeMillis();
        while (booleanSupplier.getAsBoolean()) {
            t2 = System.currentTimeMillis();
            if (t2 - t1 > 10000L) {
                fail(failMessage);
            }
            Thread.sleep(100L);
        }
    }

    private void assertEqualsWithTimeout(String expected, Supplier<String> supplier) throws InterruptedException
    {
        long t2;
        long t1 = System.currentTimeMillis();
        String result;
        while (!(result = supplier.get()).equalsIgnoreCase(expected)) {
            t2 = System.currentTimeMillis();
            if (t2 - t1 > 10000L) {
                fail(String.format("Content should have been [%s] but was [%s]", expected, result));
            }
            Thread.sleep(100L);
        }
    }
}
