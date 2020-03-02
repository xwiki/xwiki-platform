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
package org.xwiki.index.test.ui;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.index.test.po.DeletedAttachmentsPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

 /**
  * Tests for the DeleteAttachments page.
  *
  * @since 12.2RC1
  */
 public class DeletedAttachmentsTest extends AbstractTest
 {
     @Before
     public void setUp() throws Exception
     {
         getUtil().loginAsSuperAdmin();

         if (!getUtil().pageExists(getTestClassName(), "TestAttachments")) {
             getUtil().createPageWithAttachment(getTestClassName(), "TestAttachments",
                 null, "Test Attachments", null, getTestClassName(), "deletedFile1.txt",
                 new ByteArrayInputStream("attachment content1".getBytes()), TestUtils.SUPER_ADMIN_CREDENTIALS);

             ViewPage testPage = getUtil().gotoPage(getTestClassName(), "TestAttachments");
             AttachmentsPane attachmentsPane = testPage.openAttachmentsDocExtraPane();
             attachmentsPane.deleteAttachmentByFileByName("deletedFile1.txt");
         }
     }

     @Test
     public void testDeletedAttachmentsLivetableFilterByName() throws Exception
     {
         DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
         LiveTableElement livetable = deletedAttachmentsPage.getDocumentsAttachmentsLiveTable();

         livetable.filterColumn("xwiki-livetable-attachmentTrash-filter-1", "deletedFile1");
         Assert.assertEquals(1, livetable.getRowCount());
         Assert.assertEquals("deletedFile1.txt", livetable.getCell(livetable.getRow(1), 1).getText());
     }

     @Test
     public void testDeletedAttachmentsLivetableFilterByDate() throws Exception
     {
         DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
         LiveTableElement livetable = deletedAttachmentsPage.getDocumentsAttachmentsLiveTable();

         livetable.filterColumn("xwiki-livetable-attachmentTrash-filter-3", getNowDateInterval());
         Assert.assertEquals(1, livetable.getRowCount());
         Assert.assertEquals("deletedFile1.txt", livetable.getCell(livetable.getRow(1), 1).getText());
     }

     private String getNowDateInterval() {
         Calendar cal = Calendar.getInstance();
         Date endDate = cal.getTime();
         cal.add(Calendar.DATE, -1);
         Date startDate = cal.getTime();

         return Long.toString(startDate.getTime()) + '-' + Long.toString(endDate.getTime());
     }
 }
