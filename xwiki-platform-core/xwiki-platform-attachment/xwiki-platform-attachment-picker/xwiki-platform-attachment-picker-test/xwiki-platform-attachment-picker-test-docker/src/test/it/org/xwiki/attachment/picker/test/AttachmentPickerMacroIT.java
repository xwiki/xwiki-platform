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
package org.xwiki.attachment.picker.test;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.attachment.picker.test.po.AttachmentPicker;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the attachment picker macro.
 *
 * @version $Id$
 * @since 14.4R1
 */
@UITest(extraJARs = {
    "org.xwiki.platform:xwiki-platform-search-solr-query"
})
class AttachmentPickerMacroIT
{
    @Test
    @Order(1)
    void insertMacro(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        // Login to be able to delete the page.
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);
        ViewPage page = setup.createPage(testReference, "{{attachmentPicker id='testPicker1' /}}\n"
            + "\n"
            + "{{attachmentPicker id='testPicker2' limit=2 /}}\n"
            + "\n"
            + "{{attachmentPicker id='testPicker3' filter='image/*' /}}\n");
        AttachmentsPane attachmentsPane = page.openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(
            new File(testConfiguration.getBrowser().getTestResourcesPath(), "image1.png").getAbsolutePath());
        attachmentsPane.waitForUploadToFinish("image1.png");
        attachmentsPane.setFileToUpload(
            new File(testConfiguration.getBrowser().getTestResourcesPath(), "image2.png").getAbsolutePath());
        attachmentsPane.waitForUploadToFinish("image2.png");
        attachmentsPane.setFileToUpload(
            new File(testConfiguration.getBrowser().getTestResourcesPath(), "textcontent.txt").getAbsolutePath());
        attachmentsPane.waitForUploadToFinish("textcontent.txt");

        // Waits for the uploaded files to be indexed before continuing.
        // TODO: find out why test utils not working with external servlet!

        new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmpyQueue();

        // Reload the page to see the file after the uploads and solr indexing.
        setup.getDriver().navigate().refresh();

        // Test initial display
        AttachmentPicker testPicker1 = new AttachmentPicker("testPicker1").waitUntilReady();
        List<String> picker1Attachments = testPicker1.getAttachmentTitles();
        assertTrue(picker1Attachments.size() >= 3);
        assertEquals(List.of("image1.png", "image2.png", "textcontent.txt"), picker1Attachments.subList(0, 3));

        // Test on a first filter matching two attachments.
        testPicker1.setSearch("image").waitUntilAttachmentsCount(2);
        assertEquals(List.of("image1.png", "image2.png"), testPicker1.getAttachmentTitles());

        // Test on a filter matching no attachment, a warning message is expected.
        testPicker1.setSearch("doesnotexists").waitUntilAttachmentsCount(0);
        assertTrue(testPicker1.isNoResultMessageDisplayed());

        // Test with another filter.
        testPicker1.setSearch("textcontent").waitUntilAttachmentsCount(1);
        assertEquals(List.of("textcontent.txt"), testPicker1.getAttachmentTitles());

        // Validate that the limit is taken into account.
        AttachmentPicker testPicker2 = new AttachmentPicker("testPicker2").waitUntilReady();
        assertEquals(List.of("image1.png", "image2.png"), testPicker2.getAttachmentTitles());

        // Validate that only images are returned.
        AttachmentPicker testPicker3 = new AttachmentPicker("testPicker3").waitUntilReady();
        List<String> picker3Attachments = testPicker3.getAttachmentTitles();
        assertTrue(picker3Attachments.size() >= 2);
        assertEquals(List.of("image1.png", "image2.png"), picker3Attachments.subList(0, 2));
        testPicker3.setSearch("textcontent").waitUntilAttachmentsCount(1);
        assertTrue(testPicker3.isNoResultMessageDisplayed());
    }

    private String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
    }
}
