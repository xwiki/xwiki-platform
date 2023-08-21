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
package org.xwiki.flamingo.test.docker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ComparePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.RenderedChanges;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the compare versions feature.
 *
 * @version $Id$
 */
@UITest
class CompareIT
{
    private static final String ATTACHMENT_NAME_1 = "image.gif";

    private static final String ATTACHMENT_NAME_2 = "image2.gif";

    private static final String ATTACHMENT_NAME_3 = "image.png";

    private static final String IMAGE_SYNTAX = "[[image:%s]]";

    private String getLocalAttachmentURL(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration, String attachmentName)
        throws URISyntaxException
    {
        // If the Servlet Engine is standalone, then we need to replace the host and port in the attachment URL,
        // otherwise we just return the attachment name.
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        if (servletEngine == ServletEngine.JETTY_STANDALONE) {
            AttachmentReference attachmentReference = new AttachmentReference(attachmentName, testReference);
            URI attachmentURL = new URI(setup.getURL(attachmentReference, "download", null));
            // Replace host and port with host and port of the servlet engine as the outside port doesn't work from
            // the inside.
            return (new URI("http", null, servletEngine.getHostIP(), servletEngine.getPort(), attachmentURL.getPath(),
                attachmentURL.getQuery(), attachmentURL.getFragment())).toString();
        } else {
            return attachmentName;
        }
    }

    @Test
    @Order(1)
    void compareRenderedImageChanges(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.attachFile(testReference, ATTACHMENT_NAME_1,
            getClass().getResourceAsStream("/AttachmentIT/image.gif"), false);
        // Upload the image a second time under a different name to check that the content and not the URL is used
        // for comparison when changing the URL to the second image.
        setup.attachFile(testReference, ATTACHMENT_NAME_2,
            getClass().getResourceAsStream("/AttachmentIT/image.gif"), false);
        String url1 = getLocalAttachmentURL(setup, testReference, testConfiguration, ATTACHMENT_NAME_1);
        ViewPage viewPage = setup.createPage(testReference, String.format(IMAGE_SYNTAX, url1));
        String firstRevision = viewPage.getMetaDataValue("version");
        // Create a second revision with the new image.
        String url2 = getLocalAttachmentURL(setup, testReference, testConfiguration, ATTACHMENT_NAME_2);
        viewPage = setup.createPage(testReference, String.format(IMAGE_SYNTAX, url2));
        String secondRevision = viewPage.getMetaDataValue("version");

        // Open the history pane.
        ComparePage compare = viewPage.openHistoryDocExtraPane().compare(firstRevision, secondRevision);
        RenderedChanges renderedChanges = compare.getChangesPane().getRenderedChanges();
        assertTrue(renderedChanges.hasNoChanges());

        // Upload a new image with different content to verify that the changes are detected.
        setup.attachFile(testReference, ATTACHMENT_NAME_3,
            getClass().getResourceAsStream("/AttachmentIT/SmallSizeAttachment.png"), false);

        // Create a third revision with the new image.
        String url3 = getLocalAttachmentURL(setup, testReference, testConfiguration, ATTACHMENT_NAME_3);
        viewPage = setup.createPage(testReference, String.format(IMAGE_SYNTAX, url3));
        String thirdRevision = viewPage.getMetaDataValue("version");

        // Open the history pane.
        compare = viewPage.openHistoryDocExtraPane().compare(secondRevision, thirdRevision);
        renderedChanges = compare.getChangesPane().getRenderedChanges();
        assertFalse(renderedChanges.hasNoChanges());
        List<WebElement> changes = renderedChanges.getChangedBlocks();
        assertEquals(2, changes.size());

        // Check that the first change is the deletion and the second change the insertion of the new image.
        WebElement firstChange = changes.get(0);
        WebElement secondChange = changes.get(1);
        assertEquals("deleted", firstChange.getAttribute("data-xwiki-html-diff-block"));
        assertEquals("inserted", secondChange.getAttribute("data-xwiki-html-diff-block"));
        WebElement deletedImage = firstChange.findElement(By.tagName("img"));
        WebElement insertedImage = secondChange.findElement(By.tagName("img"));

        // Check that the src attribute of the deleted image contains image2 (the concrete value depends on the
        // servlet container as we either use an attachment link which will contain a revision or a plain URL).
        assertThat(deletedImage.getAttribute("src"), containsString(ATTACHMENT_NAME_2));

        // Compute the expected base64-encoded content of the inserted image. The HTML diff embeds both images but
        // replaces the deleted image by the original URL again after the diff computation.
        String expectedInsertedImageContent = Base64.getEncoder().encodeToString(
            IOUtils.toByteArray(getClass().getResourceAsStream("/AttachmentIT/SmallSizeAttachment.png")));
        assertEquals("data:image/png;base64," + expectedInsertedImageContent, insertedImage.getAttribute("src"));
    }
}
