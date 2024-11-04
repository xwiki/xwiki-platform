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
package org.xwiki.attachment.validation.test.ui.docker;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.attachment.validation.test.po.AttachmentRestrictionAdministrationSectionPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Docker test of the attachment validation feature.
 *
 * @version $Id$
 * @since 14.10
 */
@UITest
class AttachmentValidationIT
{
    private static final String TEXT_FILE_NAME = "text.txt";

    private static final String IMAGE_FILE_NAME = "image.gif";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deleteSpace(testReference.getLastSpaceReference());
    }

    @Test
    void validateAttachment(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.createPage(testReference, "");
        // Set the file size limit to 10b.
        setup.setWikiPreference(UPLOAD_MAXSIZE_PARAMETER, "10");
        // Only allow images to be uploaded on this space.
        AttachmentRestrictionAdministrationSectionPage administrationSectionPage =
            AttachmentRestrictionAdministrationSectionPage.goToPage(testReference.getLastSpaceReference());
        administrationSectionPage.setAllowedMimetypes("image/*");
        administrationSectionPage.save();

        // Go to a sub-page before uploading to validate that the space configuration is used when validating.
        DocumentReference subPage = new DocumentReference("SubPage", testReference.getLastSpaceReference());
        ViewPage page = setup.createPage(subPage, "");
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();

        // Upload a test file, not allowed because it is not an image.
        upload(attachmentsPane, testConfiguration, TEXT_FILE_NAME);
        WebElement errorMimetype = getNotificationError(setup);
        assertEquals("File text.txt has an invalid mimetype text/plain.\n"
            + "Allowed mimetypes: image/*", errorMimetype.getText());

        cleanNotification(setup, errorMimetype);

        // Upload an image, not allowed because it is larger than 10b.
        upload(attachmentsPane, testConfiguration, IMAGE_FILE_NAME);
        WebElement errorSize = getNotificationError(setup);
        assertEquals("File image.gif is too large (194 bytes). Max file size: 10 bytes.", errorSize.getText());

        // Check if the attachment validators are also executed when uploading attachment through the rest API.
        PutMethod putMethodText = restUploadImage(setup, subPage, TEXT_FILE_NAME);
        assertEquals(415, putMethodText.getStatusCode());
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> mapResult = objectMapper.readValue(putMethodText.getResponseBodyAsStream(), Map.class);
        Map<String, Object> expectedMap = Map.of(
            "message", "Invalid mimetype [text/plain]",
            "translationKey", "attachment.validation.mimetype.rejected",
            "translationParameters", List.of(List.of("image/*"), List.of())
        );
        assertEquals(expectedMap, mapResult);

        PutMethod putMethodImage = restUploadImage(setup, subPage, IMAGE_FILE_NAME);
        assertEquals(413, putMethodImage.getStatusCode());
        mapResult = objectMapper.readValue(putMethodImage.getResponseBodyAsStream(), Map.class);
        expectedMap = Map.of(
            "message", "File size too big",
            "translationKey", "attachment.validation.filesize.rejected",
            "translationParameters", List.of("10 bytes")
        );
        assertEquals(expectedMap, mapResult);
        // Check that no image are saved to the document after the various upload tries.
        page.reloadPage();
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(0, attachmentsPane.getNumberOfAttachments());
    }

    private PutMethod restUploadImage(TestUtils setup, DocumentReference subPage, String imageFileName) throws Exception
    {
        return setup.rest().executePut(AttachmentResource.class,
            IOUtils.toByteArray(this.getClass().getResourceAsStream("/" + imageFileName)),
            setup.rest().toElements(new AttachmentReference(imageFileName, subPage)));
    }

    private WebElement getNotificationError(TestUtils setup)
    {
        return setup.getDriver().findElement(By.cssSelector(".xnotification-error"));
    }

    private void cleanNotification(TestUtils setup, WebElement notificationElement)
    {
        int initialSize = getNotificationErrors(setup).size();
        notificationElement.click();
        setup.getDriver().waitUntilCondition(webDriver -> getNotificationErrors(setup).size() == initialSize - 1);
    }

    private List<WebElement> getNotificationErrors(TestUtils setup)
    {
        return setup.getDriver().findElements(By.cssSelector(".xnotification-error"));
    }

    private void upload(AttachmentsPane attachmentsPane, TestConfiguration testConfiguration, String fileName)
    {
        attachmentsPane.setFileToUpload(getFile(testConfiguration, fileName).getAbsolutePath());
    }

    private File getFile(TestConfiguration testConfiguration, String fileName)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), fileName);
    }
}
