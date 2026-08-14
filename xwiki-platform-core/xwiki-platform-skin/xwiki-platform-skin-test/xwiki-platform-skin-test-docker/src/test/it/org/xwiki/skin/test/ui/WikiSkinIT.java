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
package org.xwiki.skin.test.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.skin.test.po.SkinInlinePage;
import org.xwiki.skin.test.po.SkinTemplateElement;
import org.xwiki.skin.test.po.SkinViewPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the behavior of wiki based skins.
 *
 * @version $Id$
 */
@UITest
class WikiSkinIT
{
    private static final String SKIN = "XWiki.DefaultSkin";

    private static final DocumentReference SKIN_REFERENCE = new DocumentReference("xwiki", "XWiki", "DefaultSkin");

    private static final String SKIN_PREFERENCE = "skin";

    /**
     * Attachment overriding the {@code icons/xwiki/favicon.svg} skin resource: the "/" of the resource path become "."
     * in the attachment name.
     */
    private static final String FAVICON_SVG = "icons.xwiki.favicon.svg";

    /**
     * Attachment overriding the {@code icons/xwiki/favicon16.png} skin resource.
     */
    private static final String FAVICON_PNG = "icons.xwiki.favicon16.png";

    /**
     * Attachment overriding the {@code icons/xwiki/favicon144.png} skin resource.
     */
    private static final String FAVICON_APPLE_TOUCH = "icons.xwiki.favicon144.png";

    private static final String SVG_LINK = "link[rel='icon'][type='image/svg+xml']";

    private static final String PNG_LINK = "link[rel='icon'][type='image/png']";

    private static final String APPLE_TOUCH_LINK = "link[rel='apple-touch-icon']";

    private static final String SVG_CONTENT = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 16 16\">"
        + "<rect width=\"16\" height=\"16\"/></svg>";

    /**
     * Make sure it's possible to provide a template as xobject in a wiki skin.
     */
    @Test
    @Order(1)
    void modifySkinObjectTemplate(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Set the default skin
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        ThemesAdministrationSectionPage themesPage = adminPage.clickThemesSection();
        themesPage.setSkin(SKIN);
        themesPage.clickSave();

        // Customize the skin
        themesPage.clickOnCustomizeSkin();
        SkinViewPage viewSkinPage = new SkinViewPage();

        // Add a custom template
        SkinInlinePage editSkinPage = viewSkinPage.editSkin();
        SkinTemplateElement templateElement = editSkinPage.addTemplate("test.vm");
        templateElement.setContent("content");
        editSkinPage.clickSaveAndContinue();

        // Use the template
        assertEquals("content", setup.executeWiki("{{template name='test.vm'/}}", Syntax.XWIKI_2_1));

        // Modify the template
        adminPage = AdministrationPage.gotoPage();
        themesPage = adminPage.clickThemesSection();
        themesPage.clickOnCustomizeSkin();
        viewSkinPage = new SkinViewPage();
        editSkinPage = viewSkinPage.editSkin();
        templateElement = editSkinPage.getTemplate("test.vm", true);
        templateElement.setContent("modified content");
        editSkinPage.clickSaveAndContinue();

        // Make sure the template result changes
        assertEquals("modified content", setup.executeWiki("{{template name='test.vm'/}}", Syntax.XWIKI_2_1));
    }

    /**
     * Make sure that attaching a favicon to a wiki skin overrides the favicon provided by the WAR: the {@code <link>}
     * elements of the HTML head must point to the skin action serving the attachment, and that URL must serve the
     * attached image.
     * <p>
     * Which of the declared favicons a browser ends up painting in its tab is a browser decision (e.g. Chrome prefers
     * the SVG one), and no WebDriver API exposes the tab icon, so that last step of the manual test cannot be
     * automated. Everything XWiki is responsible for is verified here.
     * <p>
     * The skin is set through the REST API rather than through the Themes administration section since
     * {@link #modifySkinObjectTemplate(TestUtils)} already covers that part of the UI.
     */
    @Test
    @Order(2)
    void overrideFaviconWithSkinAttachment(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        byte[] svg = SVG_CONTENT.getBytes(StandardCharsets.UTF_8);
        // The images have different sizes so that serving the wrong one is detected.
        byte[] png = createPNG(16);
        byte[] appleTouch = createPNG(144);

        // Only the resources of a wiki skin can be overridden by an attachment, so make sure one is in use.
        String previousSkin = setup.setWikiPreference(SKIN_PREFERENCE, SKIN);
        try {
            // As long as nothing is attached to the skin, the favicon is the one shipped in the WAR.
            setup.gotoPage("Main", "WebHome");
            assertEquals(setup.getBaseURL() + "resources/icons/xwiki/favicon.svg",
                stripQueryString(getFaviconURL(setup, SVG_LINK)));

            setup.rest().attachFile(getAttachmentReference(FAVICON_SVG), svg, false);
            setup.rest().attachFile(getAttachmentReference(FAVICON_PNG), png, false);
            setup.rest().attachFile(getAttachmentReference(FAVICON_APPLE_TOUCH), appleTouch, false);

            setup.gotoPage("Main", "WebHome");

            assertFavicon(setup, SVG_LINK, FAVICON_SVG, svg);
            assertFavicon(setup, PNG_LINK, FAVICON_PNG, png);
            assertFavicon(setup, APPLE_TOUCH_LINK, FAVICON_APPLE_TOUCH, appleTouch);
        } finally {
            // Restore the state of the skin and of the wiki so that this test doesn't affect the other tests.
            setup.rest().deleteAttachement(getAttachmentReference(FAVICON_SVG));
            setup.rest().deleteAttachement(getAttachmentReference(FAVICON_PNG));
            setup.rest().deleteAttachement(getAttachmentReference(FAVICON_APPLE_TOUCH));
            setup.setWikiPreference(SKIN_PREFERENCE, Objects.toString(previousSkin, ""));
        }
    }

    private void assertFavicon(TestUtils setup, String linkSelector, String attachmentName, byte[] expectedContent)
        throws Exception
    {
        // Path of the skin action serving the attachment, e.g. "bin/skin/XWiki/DefaultSkin/icons.xwiki.favicon.svg".
        String path = stripQueryString(setup.getPath(getAttachmentReference(attachmentName), "skin", null));

        // The <link> element points to that path, instead of to the resource shipped in the WAR.
        assertEquals(setup.getBaseURL() + path, stripQueryString(getFaviconURL(setup, linkSelector)));

        // And the skin action serves there the image that was attached. Note that getInputStream() already asserts
        // that the resource is served with a 200 status code.
        assertArrayEquals(expectedContent, IOUtils.toByteArray(setup.getInputStream(path, null)),
            String.format("Wrong content served for the [%s] skin resource", attachmentName));
    }

    /**
     * @return the URL the single {@code <link>} element matching the passed selector points to
     */
    private String getFaviconURL(TestUtils setup, String linkSelector)
    {
        // Note that findElements() is used since the <link> elements of the head cannot be scrolled to.
        List<WebElement> links = setup.getDriver().findElementsWithoutWaiting(By.cssSelector(linkSelector));
        assertEquals(1, links.size(),
            String.format("Expected a single [%s] element in the HTML head", linkSelector));

        return links.get(0).getAttribute("href");
    }

    private AttachmentReference getAttachmentReference(String attachmentName)
    {
        return new AttachmentReference(attachmentName, SKIN_REFERENCE);
    }

    /**
     * @param size the width and the height of the image, in pixels
     * @return the bytes of a PNG image of the passed size
     */
    private byte[] createPNG(int size) throws IOException
    {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB), "png", content);

        return content.toByteArray();
    }

    /**
     * @return the passed URL without its query string, which carries a cache version that is not relevant here
     */
    private String stripQueryString(String url)
    {
        return StringUtils.substringBefore(url, "?");
    }
}
