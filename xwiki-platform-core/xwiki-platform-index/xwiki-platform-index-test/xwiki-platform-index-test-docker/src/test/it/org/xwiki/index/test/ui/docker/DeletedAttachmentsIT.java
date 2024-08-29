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
package org.xwiki.index.test.ui.docker;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.index.test.po.DeletedAttachmentsPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the DeletedAttachments page.
 *
 * @since 12.2
 */
@UITest
public class DeletedAttachmentsIT
{
    private static String FIRST_LIVETABLE_COLUMN_ID = "xwiki-livetable-attachmentTrash-filter-1";

    private static String THIRD_LIVETABLE_COLUMN_ID = "xwiki-livetable-attachmentTrash-filter-3";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        setup.deletePage(testReference);
        setup.createPageWithAttachment(testReference, "", "Test Attachments", "deletedFile1.txt",
            new ByteArrayInputStream("attachment content1".getBytes()));

        ViewPage testPage = setup.gotoPage(testReference);
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        attachmentsPane.deleteAttachmentByFileByName("deletedFile1.txt");
    }

    @Test
    void filterLivetableByName() throws Exception
    {
        DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
        LiveTableElement livetable = deletedAttachmentsPage.getDeletedAttachmentsLiveTable();

        livetable.filterColumn(FIRST_LIVETABLE_COLUMN_ID, "deletedFile1");
        assertEquals("deletedFile1.txt", livetable.getCell(livetable.getRow(1), 1).getText());
    }

    @Test
    void filterLivetableByDate() throws Exception
    {
        DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
        LiveTableElement livetable = deletedAttachmentsPage.getDeletedAttachmentsLiveTable();

        livetable.filterColumn(THIRD_LIVETABLE_COLUMN_ID, getNowDateInterval());
        assertEquals("deletedFile1.txt", livetable.getCell(livetable.getRow(1), 1).getText());
    }

    private String getNowDateInterval()
    {
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        cal.add(Calendar.DATE, -1);
        Date startDate = cal.getTime();

        return Long.toString(startDate.getTime()) + '-' + Long.toString(endDate.getTime());
    }
}
