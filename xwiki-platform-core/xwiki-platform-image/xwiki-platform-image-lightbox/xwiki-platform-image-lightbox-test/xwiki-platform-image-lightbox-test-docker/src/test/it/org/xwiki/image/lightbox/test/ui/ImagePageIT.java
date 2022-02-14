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
package org.xwiki.image.lightbox.test.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.image.lightbox.test.po.ImagePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

@UITest(properties = {
    // Add the FileUploadPlugin which is needed by the test to upload attachment files
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"})
class ImagePageIT
{
    private static final DocumentReference LIGHTBOX_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Lightbox"), "LightboxConfiguration");

    private static final String LIGHTBOX_CONFIGURATION_CLASSNAME = "XWiki.Lightbox.LightboxConfigurationClass";

    private static final List<String> images = Arrays.asList("image1.png", "image2.png", "missingImage.png");

    @BeforeAll
    public void beforeAll(TestUtils testUtils)
    {
        testUtils.createUserAndLogin("JohnDoe", "pa$$word");
    }

    @Test
    @Order(1)
    public void disabledLightbox(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        testUtils.updateObject(LIGHTBOX_CONFIGURATION_REFERENCE, LIGHTBOX_CONFIGURATION_CLASSNAME, 0,
            "isLightboxEnabled", "0");

        testUtils.createPage(testReference, multipleImagesPageContent(), "Disabled Lightbox");
        ImagePage imagePage = new ImagePage();
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.hoverImage(0);
        assertFalse(imagePage.isToolbarOpen());
    }

    @Test
    @Order(2)
    public void openLightbox(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        testUtils.updateObject(LIGHTBOX_CONFIGURATION_REFERENCE, LIGHTBOX_CONFIGURATION_CLASSNAME, 0,
            "isLightboxEnabled", "1");

        testUtils.createPage(testReference, multipleImagesPageContent(), "Open Lightbox");
        ImagePage imagePage = new ImagePage();
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(1));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
    }

    @Test
    @Order(3)
    public void openLightboxOnMissingImage(TestUtils testUtils, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        testUtils.updateObject(LIGHTBOX_CONFIGURATION_REFERENCE, LIGHTBOX_CONFIGURATION_CLASSNAME, 0,
            "isLightboxEnabled", "1");

        testUtils.createPage(testReference, multipleImagesPageContent(), "Missing image");
        ImagePage imagePage = new ImagePage();

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(2);
    }

    @Test
    @Order(4)
    public void verifyDownload(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        testUtils.updateObject(LIGHTBOX_CONFIGURATION_REFERENCE, LIGHTBOX_CONFIGURATION_CLASSNAME, 0,
            "isLightboxEnabled", "1");

        testUtils.createPage(testReference, multipleImagesPageContent(), "Open Lightbox");
        ImagePage imagePage = new ImagePage();
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(1));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.hoverImage(0);
        assertFalse(imagePage.isToolbarOpen());

        WebElement download = imagePage.getToolbarDownload();
        assertEquals(imagePage.getImage(0).getAttribute("src"), download.getAttribute("href"));
        assertEquals("image1.png", download.getAttribute("download"));
    }

    private String multipleImagesPageContent()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("[[image:%s||width=140]]\n", images.get(0)));
        sb.append(String.format("[[image:%s||width=200 height=200]]\n", images.get(1)));
        sb.append(String.format("[[image:%s]]\n", images.get(2)));

        return sb.toString();
    }
}
