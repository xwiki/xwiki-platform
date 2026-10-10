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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.index.test.po.DeletedAttachmentsPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
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
@UITest(properties = {
    // Sorting the Deleted Attachments table on another column than the attachment name requires the programming right
    // for the author of XWiki.DeletedAttachments.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.DeletedAttachments"
})
public class DeletedAttachmentsIT
{
    private static String FIRST_LIVETABLE_COLUMN_ID = "xwiki-livetable-attachmentTrash-filter-1";

    private static String THIRD_LIVETABLE_COLUMN_ID = "xwiki-livetable-attachmentTrash-filter-3";

    private static final String ATTACHMENT_COLUMN = "Attachment";

    private static final String PAGE_COLUMN = "Page";

    private static final String DELETER_COLUMN = "Deleted by";

    private static final String NESTED_FILE = "nestedFile.txt";

    private static final String TERMINAL_FILE_1 = "terminalFile1.txt";

    private static final String TERMINAL_FILE_2 = "terminalFile2.txt";

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
    @Order(1)
    void filterLivetableByName() throws Exception
    {
        DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
        LiveTableElement livetable = deletedAttachmentsPage.getDeletedAttachmentsLiveTable();

        livetable.filterColumn(FIRST_LIVETABLE_COLUMN_ID, "deletedFile1");
        assertEquals("deletedFile1.txt", livetable.getCell(livetable.getRow(1), 1).getText());
    }

    @Test
    @Order(2)
    void filterLivetableByDate() throws Exception
    {
        DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
        LiveTableElement livetable = deletedAttachmentsPage.getDeletedAttachmentsLiveTable();

        livetable.filterColumn(THIRD_LIVETABLE_COLUMN_ID, getNowDateInterval());
        assertEquals("deletedFile1.txt", livetable.getCell(livetable.getRow(1), 1).getText());
    }

    /**
     * Attachments deleted from a nested page and from a terminal page, by two different users, are all listed, and
     * can be sorted, filtered and permanently deleted.
     */
    @Test
    @Order(3)
    void sortFilterAndDeletePermanently(TestUtils setup, TestReference testReference) throws Exception
    {
        // The nested page and its deleted "deletedFile1.txt" attachment are created by setUp(). Note that the
        // attachment names are chosen to be sorted after "deletedFile1.txt", since the other tests of this class
        // expect it in the first row of the table, which is sorted by attachment name by default.
        XWikiCredentials deleter = new XWikiCredentials("DeletedAttachmentsDeleter", "pass");
        setup.rest().createUser(deleter);
        setup.attachFile(testReference, NESTED_FILE, new ByteArrayInputStream("nested".getBytes()), true);
        DocumentReference terminalPage = new DocumentReference("Terminal", testReference.getLastSpaceReference());
        setup.deletePage(terminalPage);
        setup.createPageWithAttachment(terminalPage, "", "Terminal Attachments", TERMINAL_FILE_1,
            new ByteArrayInputStream("terminal1".getBytes()), TestUtils.SUPER_ADMIN_CREDENTIALS);
        setup.attachFile(terminalPage, TERMINAL_FILE_2, new ByteArrayInputStream("terminal2".getBytes()), true);
        setup.deleteAttachement(new AttachmentReference(NESTED_FILE, testReference));
        setup.deleteAttachement(new AttachmentReference(TERMINAL_FILE_1, terminalPage));
        setup.rest().runAs(deleter,
            rest -> rest.deleteAttachement(new AttachmentReference(TERMINAL_FILE_2, terminalPage)));

        DeletedAttachmentsPage deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
        LiveTableElement livetable = deletedAttachmentsPage.getDeletedAttachmentsLiveTable();

        // Only list the attachments deleted by this test.
        String nestedPageLocation = setup.serializeLocalReference(testReference);
        String terminalPageLocation = setup.serializeLocalReference(terminalPage);
        deletedAttachmentsPage.filterByLocation(nestedPageLocation.replace(".WebHome", ""));
        assertEquals(List.of("deletedFile1.txt", NESTED_FILE, TERMINAL_FILE_1, TERMINAL_FILE_2),
            livetable.getColumnValues(ATTACHMENT_COLUMN));

        // Sort by attachment name.
        livetable.sortDescending(ATTACHMENT_COLUMN);
        assertEquals(List.of(TERMINAL_FILE_2, TERMINAL_FILE_1, NESTED_FILE, "deletedFile1.txt"),
            livetable.getColumnValues(ATTACHMENT_COLUMN));
        livetable.sortAscending(ATTACHMENT_COLUMN);
        assertEquals(List.of("deletedFile1.txt", NESTED_FILE, TERMINAL_FILE_1, TERMINAL_FILE_2),
            livetable.getColumnValues(ATTACHMENT_COLUMN));

        // Sort by page: "...Terminal" comes before "...WebHome".
        String nestedPage = "Test Attachments (" + nestedPageLocation + ")";
        String terminalPageText = "Terminal Attachments (" + terminalPageLocation + ")";
        livetable.sortAscending(PAGE_COLUMN);
        assertEquals(List.of(terminalPageText, terminalPageText, nestedPage, nestedPage),
            livetable.getColumnValues(PAGE_COLUMN));
        livetable.sortDescending(PAGE_COLUMN);
        assertEquals(List.of(nestedPage, nestedPage, terminalPageText, terminalPageText),
            livetable.getColumnValues(PAGE_COLUMN));

        // Sort by deleter: "XWiki.DeletedAttachmentsDeleter" comes before "XWiki.superadmin".
        livetable.sortAscending(DELETER_COLUMN);
        assertEquals(List.of(deleter.getUserName(), "superadmin", "superadmin", "superadmin"),
            livetable.getColumnValues(DELETER_COLUMN));
        livetable.sortDescending(DELETER_COLUMN);
        assertEquals(List.of("superadmin", "superadmin", "superadmin", deleter.getUserName()),
            livetable.getColumnValues(DELETER_COLUMN));

        // Filter by deleter.
        deletedAttachmentsPage.filterByDeleter(deleter.getUserName());
        assertEquals(List.of(TERMINAL_FILE_2), livetable.getColumnValues(ATTACHMENT_COLUMN));
        deletedAttachmentsPage.filterByDeleter("");

        // Filter by the location of the terminal page.
        deletedAttachmentsPage.filterByLocation(terminalPageLocation);
        livetable.sortAscending(ATTACHMENT_COLUMN);
        assertEquals(List.of(TERMINAL_FILE_1, TERMINAL_FILE_2), livetable.getColumnValues(ATTACHMENT_COLUMN));

        // Permanently delete an attachment of the nested page: it's no longer listed, even after a reload.
        deletedAttachmentsPage.filterByLocation(nestedPageLocation.replace(".WebHome", ""));
        deletedAttachmentsPage.deletePermanently(NESTED_FILE);
        assertEquals(List.of("deletedFile1.txt", TERMINAL_FILE_1, TERMINAL_FILE_2),
            livetable.getColumnValues(ATTACHMENT_COLUMN));
        deletedAttachmentsPage = DeletedAttachmentsPage.gotoPage();
        livetable = deletedAttachmentsPage.getDeletedAttachmentsLiveTable();
        deletedAttachmentsPage.filterByLocation(nestedPageLocation.replace(".WebHome", ""));
        assertEquals(List.of("deletedFile1.txt", TERMINAL_FILE_1, TERMINAL_FILE_2),
            livetable.getColumnValues(ATTACHMENT_COLUMN));
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
