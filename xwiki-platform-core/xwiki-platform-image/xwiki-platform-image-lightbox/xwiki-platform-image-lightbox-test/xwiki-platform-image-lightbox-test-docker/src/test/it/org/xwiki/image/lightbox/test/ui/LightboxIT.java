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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.xwiki.image.lightbox.test.po.ImagePage;
import org.xwiki.image.lightbox.test.po.Lightbox;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

@UITest(properties = {
    // Add the FileUploadPlugin which is needed by the test to upload attachment files
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"})
class LightboxIT
{
    private static final DocumentReference LIGHTBOX_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("XWiki", "Lightbox"), "LightboxConfiguration");

    private static final String LIGHTBOX_CONFIGURATION_CLASSNAME = "XWiki.Lightbox.LightboxConfigurationClass";

    private static final List<String> images = Arrays.asList("image1.png", "image2.png", "missingImage.png");

    ImagePage imagePage;

    @BeforeAll
    public void beforeAll(TestUtils testUtils)
    {
        testUtils.createUserAndLogin("JohnDoe", "pa$$word");
        testUtils.updateObject(LIGHTBOX_CONFIGURATION_REFERENCE, LIGHTBOX_CONFIGURATION_CLASSNAME, 0,
            "isLightboxEnabled", "1");
    }

    @Test
    @Order(1)
    public void openImageWithoutDescription(TestUtils testUtils, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(0)));
        imagePage = new ImagePage();

        String lastUploadDate =
            imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        assertEquals("0", lightbox.getSlideIndex());
        assertEquals(images.get(0), lightbox.getCaptionContent());
        assertEquals("", lightbox.getTitleContent());
        assertEquals("Posted by JohnDoe", lightbox.getPublisherContent());
        assertEquals(lastUploadDate, lightbox.getDateContent());
    }

    @Test
    @Order(2)
    public void openImageWithCaption(TestUtils testUtils, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // Setup page.
        testUtils.createPage(testReference, this.getImageWithCaption(images.get(0)));
        imagePage = new ImagePage();

        String lastUploadDate =
            imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        assertEquals("0", lightbox.getSlideIndex());
        assertEquals("Caption", lightbox.getCaptionContent());
        assertEquals(images.get(0), lightbox.getTitleContent());
        assertEquals("Posted by JohnDoe", lightbox.getPublisherContent());
        assertEquals(lastUploadDate, lightbox.getDateContent());
    }

    @Test
    @Order(3)
    public void openImageWithAlt(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        // Setup page.
        testUtils.createPage(testReference, this.getImageWithAlt(images.get(0)));
        imagePage = new ImagePage();

        String lastUploadDate =
            imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        assertEquals("0", lightbox.getSlideIndex());
        assertEquals("Alternative text", lightbox.getCaptionContent());
        assertEquals("", lightbox.getTitleContent());
        assertEquals("Posted by JohnDoe", lightbox.getPublisherContent());
        assertEquals(lastUploadDate, lightbox.getDateContent());
    }

    @Test
    @Order(4)
    public void clickLightboxEscape(TestUtils testUtils, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(0)));
        imagePage = new ImagePage();

        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        lightbox.clickEscape();
        assertFalse(imagePage.isLightboxOpen());
    }

    @Test
    @Order(5)
    public void navigateThroughImages(TestUtils testUtils, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(0)) + this.getSimpleImage(images.get(1)));
        imagePage = new ImagePage();

        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(1));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        // Using arrows.
        assertEquals("0", lightbox.getSlideIndex());
        lightbox.clickNext();
        assertEquals("1", lightbox.getSlideIndex());
        lightbox.clickPrev();
        assertEquals("0", lightbox.getSlideIndex());

        // Using thumbnails icons.
        lightbox.clickThumbnail(1);
        assertEquals("1", lightbox.getSlideIndex());
        lightbox.clickThumbnail(0);
        assertEquals("0", lightbox.getSlideIndex());
    }

    @Test
    @Order(6)
    public void playSlideshow(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(0)) + this.getSimpleImage(images.get(1))
            + this.getSimpleImage(images.get(2)));
        imagePage = new ImagePage();

        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));
        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(1));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        // Start auto play.
        lightbox.clickSlideshow();
        assertTrue(lightbox.waitUntilIsSlideDisplayed(1));
        assertTrue(lightbox.waitUntilIsSlideDisplayed(2));
        assertTrue(lightbox.waitUntilIsSlideDisplayed(0));

        // Stop auto play.
        lightbox.clickSlideshow();
        assertFalse(lightbox.waitUntilIsSlideDisplayed(1));
    }

    @Test
    @Order(7)
    public void openMissingImage(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(2)));
        imagePage = new ImagePage();

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        assertTrue(lightbox.isMissingImageOpen());
        assertEquals("0", lightbox.getSlideIndex());
        assertEquals(images.get(2), lightbox.getCaptionContent());
        assertEquals("", lightbox.getTitleContent());
        assertEquals("", lightbox.getDateContent());
    }

    @Test
    @Order(8)
    public void openFullscreen(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(0)));
        imagePage = new ImagePage();

        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();
        lightbox.clickFullscreen();

        JavascriptExecutor js = (JavascriptExecutor) testUtils.getDriver();
        WebElement fullScreen =
            (WebElement) js.executeScript("var element = document.fullscreenElement; return element");

        assertTrue(fullScreen.isDisplayed());
    }

    @Test
    @Order(9)
    public void verifyDownload(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Setup page.
        testUtils.createPage(testReference, this.getSimpleImage(images.get(0)));
        imagePage = new ImagePage();

        imagePage.attachFile(testConfiguration.getBrowser().getTestResourcesPath(), images.get(0));

        // Make sure that the images are displayed.
        testUtils.getDriver().navigate().refresh();

        imagePage.openLightboxAtImage(0);
        Lightbox lightbox = new Lightbox();

        WebElement slide = lightbox.getSlide();
        WebElement download = lightbox.getDownloadElement();
        assertEquals(slide.findElement(By.tagName("img")).getAttribute("src"), download.getAttribute("href"));
        assertEquals(images.get(0), download.getAttribute("download"));
    }

    private String getSimpleImage(String image)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("[[image:");
        sb.append(image);
        sb.append("|| width=120 height=120]]\n\n");

        return sb.toString();
    }

    private String getImageWithCaption(String image)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{{figure}}\n[[image:");
        sb.append(image);
        sb.append("||width=120 height=120]]\n\n");
        sb.append("{{figureCaption}}Caption{{/figureCaption}}\n\n");
        sb.append("{{/figure}}\n\n");

        return sb.toString();
    }

    private String getImageWithAlt(String image)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("[[image:");
        sb.append(image);
        sb.append("||alt=\"Alternative text\" width=120 height=120]]\n\n");

        return sb.toString();
    }
}
