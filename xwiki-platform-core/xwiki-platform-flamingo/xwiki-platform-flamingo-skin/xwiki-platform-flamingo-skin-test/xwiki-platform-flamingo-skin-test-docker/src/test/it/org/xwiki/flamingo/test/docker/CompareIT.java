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
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ComparePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.RenderedChanges;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the compare versions feature.
 *
 * @version $Id$
 */
@UITest(properties = {
    // Trust picsum.photos to allow the rendered diff to download images from it
    "xwikiPropertiesAdditionalProperties=url.trustedDomains=picsum.photos"
})
class CompareIT
{
    private static final String ATTACHMENT_NAME_1 = "image.gif";

    private static final String ATTACHMENT_NAME_2 = "image2.gif";

    private static final String ATTACHMENT_NAME_3 = "image.png";

    private static final String IMAGE_SYNTAX = "[[image:%s]]";

    private String getLocalAttachmentURL(TestUtils setup, TestReference testReference, String attachmentName)
        throws URISyntaxException
    {
        AttachmentReference attachmentReference = new AttachmentReference(attachmentName, testReference);
        URI attachmentURL = new URI(setup.getURL(attachmentReference, "download", null));
        // Replace host and port with localhost and 8080 to make the URL usable from the container.
        return (new URI("http", null, "localhost", 8080, attachmentURL.getPath(),
            attachmentURL.getQuery(), attachmentURL.getFragment())).toString();
    }

    @Test
    @Order(1)
    void compareRenderedImageChanges(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.attachFile(testReference, ATTACHMENT_NAME_1,
            getClass().getResourceAsStream("/AttachmentIT/image.gif"), false);
        // Upload the image a second time under a different name to check that the content and not the URL is used
        // for comparison when changing the URL to the second image.
        setup.attachFile(testReference, ATTACHMENT_NAME_2,
            getClass().getResourceAsStream("/AttachmentIT/image.gif"), false);
        String url1 = getLocalAttachmentURL(setup, testReference, ATTACHMENT_NAME_1);
        ViewPage viewPage = setup.createPage(testReference, String.format(IMAGE_SYNTAX, url1));
        String firstRevision = viewPage.getMetaDataValue("version");
        // Create a second revision with the new image.
        String url2 = getLocalAttachmentURL(setup, testReference, ATTACHMENT_NAME_2);
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
        String url3 = getLocalAttachmentURL(setup, testReference, ATTACHMENT_NAME_3);
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

        // Check that the src attribute of the deleted image ends with the image2 (don't check the start as it
        // depends on the container setup and the nested/non-nested test execution).
        assertEquals(url2, deletedImage.getAttribute("src"));

        // Compute the expected base64-encoded content of the inserted image. The HTML diff embeds both images but
        // replaces the deleted image by the original URL again after the diff computation.
        String expectedInsertedImageContent = Base64.getEncoder().encodeToString(
            IOUtils.toByteArray(getClass().getResourceAsStream("/AttachmentIT/SmallSizeAttachment.png")));
        assertEquals("data:image/png;base64," + expectedInsertedImageContent, insertedImage.getAttribute("src"));
    }
}
