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
import org.xwiki.attachment.picker.test.po.AttachmentGalleryPicker;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
}, properties = {
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.skinx.JsResourceSkinExtensionPlugin,"
        + "com.xpn.xwiki.plugin.skinx.CssResourceSkinExtensionPlugin"
})
class AttachmentGalleryPickerMacroIT
{
    @Test
    @Order(1)
    void attachmentGalleryPickerMacro(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        // Login to be able to delete the page.
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);
        setup.createPage(testReference, "{{attachmentGalleryPicker id='testPicker1' /}}\n"
            + "\n"
            + "{{attachmentGalleryPicker id='testPicker2' limit=2 /}}\n"
            + "\n"
            + "{{attachmentGalleryPicker id='testPicker3' filter='image/*' /}}\n");
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
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
        new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmpyQueue();

        // Reload the page to see the file after the uploads and solr indexing.
        setup.getDriver().navigate().refresh();

        // Test initial display
        AttachmentGalleryPicker testPicker1 = new AttachmentGalleryPicker("testPicker1").waitUntilReady();
        List<String> picker1Attachments = testPicker1.getAttachmentTitles();
        assertTrue(picker1Attachments.size() >= 3);
        assertThat(picker1Attachments.subList(0, 3), containsInAnyOrder("image1.png", "image2.png", "textcontent.txt"));

        // Test on a first filter matching two attachments.
        testPicker1.setSearch("image").waitUntilAttachmentsCount(2);
        assertThat(testPicker1.getAttachmentTitles(), containsInAnyOrder("image1.png", "image2.png"));

        // Test on a filter matching no attachment, a warning message is expected.
        testPicker1.setSearch("doesnotexists").waitUntilAttachmentsCount(0);
        testPicker1.waitNoResultMessageDisplayed();

        // Test with another filter.
        testPicker1.setSearch("textcontent").waitUntilAttachmentsCount(1);
        assertThat(testPicker1.getAttachmentTitles(), containsInAnyOrder("textcontent.txt"));

        // Validate that the limit is taken into account.
        AttachmentGalleryPicker testPicker2 = new AttachmentGalleryPicker("testPicker2").waitUntilReady();
        assertEquals(2, testPicker2.getAttachmentTitles().size());
        // We can't guarantee the order of the results returned by solr. Since the results for the current document are
        // returned first, we check that the returned attachments are part of the attachments from the current document.
        List<String> localAttachments = List.of("image1.png", "image2.png", "textcontent.txt");
        assertTrue(localAttachments.contains(testPicker2.getAttachmentTitles().get(0)));
        assertTrue(localAttachments.contains(testPicker2.getAttachmentTitles().get(1)));

        // Validate that only images are returned.
        AttachmentGalleryPicker testPicker3 = new AttachmentGalleryPicker("testPicker3").waitUntilReady();
        List<String> picker3Attachments = testPicker3.getAttachmentTitles();
        assertTrue(picker3Attachments.size() >= 2);
        assertThat(picker3Attachments.subList(0, 2), containsInAnyOrder("image1.png", "image2.png"));
        testPicker3.setSearch("textcontent").waitUntilAttachmentsCount(0);
        testPicker3.waitNoResultMessageDisplayed();
    }

    private String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
    }
}
